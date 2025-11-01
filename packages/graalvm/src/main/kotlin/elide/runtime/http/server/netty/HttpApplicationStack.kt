/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

package elide.runtime.http.server.netty

import io.netty.channel.Channel
import io.netty.channel.EventLoopGroup
import io.netty.channel.unix.DomainSocketAddress
import java.net.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.pathString
import kotlin.time.TimeSource
import elide.runtime.Logging
import elide.runtime.http.server.CallContext
import elide.runtime.http.server.HttpApplication
import elide.runtime.http.server.HttpApplicationOptions
import elide.runtime.http.server.netty.HttpApplicationStack.Companion.bind

/** A future returned by [HttpApplicationStack.close] to track errors during server shutdown. */
public typealias ServerCloseFuture = Future<List<Throwable>>

/**
 * A group of HTTP [services] associated with an [HttpApplication], each corresponding to a bound server socket that
 * accepts connections for a specific protocol.
 *
 * Use [HttpApplicationStack.bind] to configure and start a stack for an application. Services may fail to start
 * individually, in which case the [Service.bindResult] field will contain the failure cause.
 *
 * ```kotlin
 * // set the handler for incoming calls
 * val app = object : HttpApplication<CallContext.Empty> {
 *   override fun newContext(request: HttpRequest) = CallContext.Empty
 *
 *   override fun handle(call: HttpCall<CallContext.Empty>) {
 *     call.response.setHeader(HttpHeaderNames.CONTENT_LENGTH, 5)
 *     call.responseBody.source { it.write("Hello"); it.end() }
 *     call.send()
 *   }
 * }
 *
 * // binding options for each service
 * val options = HttpApplicationOptions(
 *   http = CleartextOptions(InetSocketAddress("localhost", 3000)),
 *   https = HttpsOptions(certificate = CertificateSource.SelfSigned()),
 * )
 *
 * // configure server and wait for it to bind
 * val server = HttpServerBootstrap.bind(app, options)
 *
 * println("Server started:")
 * server.services.forEach {
 *   val message = it.bindResult.fold(
 *     onSuccess = { bind -> "  - ${it.label}: ${bind.baseUrl}" },
 *     onFailure = { cause -> "  x ${it.label}: $cause" }
 *   )
 *   println(message)
 * }
 *
 * // async shutdown
 * server.close().addListener { errors ->
 *   if (errors.isNotEmpty()) println("Some services failed to close:")
 *   for (error in errors) println(it.stackTraceToString())
 * }
 * ```
 *
 * The server stack *must* be closed manually to release its bound resources (sockets and event loops). Calling [close]
 * will start asynchronously shutting down all services and their components, and the returned future (the same
 * instance as [onClose]) can be used to observe the result of the process and inspect any failures.
 *
 * @see bind
 * @see close
 * @see HttpApplication
 * @see Service
 */
public class HttpApplicationStack private constructor(
  /**
   * Services assigned to this server stack. This list contains both services that started successfully and those that
   * failed; the result can be accessed through the [Service.bindResult] field.
   */
  public val services: List<Service>,
  private val channels: List<Channel>,
  private val groups: List<EventLoopGroup>,
) {
  /**
   * An element of the server stack, corresponding to one of the protocols configured when
   * [binding][HttpApplicationStack.bind] the application.
   *
   * Each service has a unique readable [label] used for debugging and a [bindResult] showing the outcome of the
   * socket binding; on a successful start, the binding provides the base URL for the service and the bound address.
   *
   * Note that a failed service cannot be retried individually, the entire stack must be stopped and restarted.
   */
  public data class Service(
    /** A unique label for this service, for debugging purposes, e.g. `"http"` or `"https"`. */
    val label: String,
    /** The result of binding this service's socket; if successful, indicates the bound address. */
    val bindResult: Result<ServiceBinding>,
  )

  /** Provides information about a successfully bound service. */
  public data class ServiceBinding(
    /**
     * The local address the service is bound to; this may be different from the configured address, e.g. if the port
     * was chosen by the OS automatically.
     *
     * Combined with the [scheme] this can be used to assemble a base URL.
     */
    val address: SocketAddress,
    /** The scheme used by the service; combined with the bound [address] this can be used to assemble a base URL. */
    val scheme: String,
  )

  /**
   * A future that completes after the server is [closed][close] and the shutdown process completes.
   *
   * The result contains any errors raised when closing the bound socket channels and event loops, and exceptions
   * thrown by the shutdown coordinator code itself.
   *
   * Calling [close] will return this future instance.
   */
  public val onClose: ServerCloseFuture get() = closeFuture
  private val closeFuture = CompletableFuture<List<Throwable>>()
  private val closing = AtomicBoolean(false)

  /**
   * Shut down all services running on the server and their associated executors.
   *
   * The returned [Future] completes once every service shutdown is finished, and contains any errors encountered
   * by the individual close sequences.
   *
   * ```kotlin
   * server.close().addListener { errors ->
   *   if (errors.isNotEmpty()) println("Some services failed to close:")
   *   for (error in errors) println(it.stackTraceToString())
   * }
   * ```
   *
   * Use [force] to request immediate shutdown without a grace period or a waiting period; this is not recommended in
   * production as it may interrupt shutdown tasks scheduled by the application layer, but can be useful during
   * development or testing to speed up the shutdown sequence.
   *
   * @return the server's [onClose] future
   */
  public fun close(force: Boolean = false): ServerCloseFuture {
    if (!closing.compareAndSet(false, true)) return onClose
    log.debug("Shutting down HTTP server stack")
    val started = TimeSource.Monotonic.markNow()

    val errors = CopyOnWriteArrayList<Throwable>()
    val pendingChannels = AtomicInteger(channels.size)
    val pendingGroups = AtomicInteger(groups.size)
    val channelsClosed = CompletableFuture<Unit>()

    // begin closing bound sockets
    for (channel in channels) channel.close().addListener { future ->
      future.cause()?.let(errors::add)
      if (pendingChannels.decrementAndGet() == 0) {
        channelsClosed.complete(Unit)
        log.debug(
          "Shutdown in progress, ${channels.size} channels closed in {} ({} errors)",
          started.elapsedNow(), errors.size,
        )
      }
    }

    // once all server channels are closed, shut down the executors
    channelsClosed.whenComplete { _, throwable ->
      // also include failures in the close coordinator
      if (throwable != null) errors.add(throwable)

      for (group in groups) {
        val future = if (force) group.shutdownGracefully(0L, 0L, TimeUnit.MILLISECONDS)
        else group.shutdownGracefully()

        future.addListener { future ->
          future.cause()?.let(errors::add)
          // once shutdown completes, mark the server as closed
          if (pendingGroups.decrementAndGet() == 0) {
            closeFuture.complete(errors.toList())
            log.debug("Shutdown completed in {} ({} errors)", started.elapsedNow(), errors.size)
          }
        }
      }
    }

    return onClose
  }

  public companion object {
    private val log = Logging.of(HttpApplicationStack::class)

    /**
     * Configure and bind an HTTP [application] stack with the given [options].
     *
     * Multiple server sockets may be created for a single application, depending on the services requested in the
     * [options]. For example, configuring both [HTTP][HttpApplicationOptions.http] and
     * [HTTPS][HttpApplicationOptions.https] will result in two server sockets being bound on their respective ports.
     * All services will share the same [application] for handling incoming requests.
     *
     * ```kotlin
     * val options = HttpApplicationOptions(
     *   http = CleartextOptions(InetSocketAddress("localhost", 3000)),
     *   https = HttpsOptions(certificate = CertificateSource.SelfSigned()),
     * )
     *
     * // configure services and wait for them to bind
     * val server = HttpServerBootstrap.bind(MyApplication(), options)
     * ```
     *
     * This method may succeed partially if some of the services fail to bind to their configured address. The returned
     * instance should be used to check the result of binding each service.
     *
     * Errors arising from configuration (before the services are bound) will still cause this method to throw, and any
     * services that were already started (and associated resources) will be closed gracefully.
     *
     * A manual [transportOverride] can be provided to force the use of a specific transport instead of resolving the
     * preferred implementation for each service. Note that overriding the transport will bypass feature checks, which
     * can lead to an error being thrown if an unsupported feature is required (e.g. UDP over domain sockets on NIO).
     */
    public fun <C : CallContext> bind(
      application: HttpApplication<C>,
      options: HttpApplicationOptions,
      transportOverride: HttpServerTransport? = null,
    ): HttpApplicationStack {
      log.debug("Binding HTTP server stack for {}", application)
      val bindStarted = TimeSource.Monotonic.markNow()

      // passed to the initializers so they can access the bound stack
      val deferredStack = CompletableFuture<HttpApplicationStack>()

      @Suppress("UNCHECKED_CAST")
      val scope = StackServiceContributor.BindingScope(
        application = application as HttpApplication<CallContext>,
        options = options,
        transportOverride = transportOverride,
        deferredStack = deferredStack,
      )

      return try {
        // prepare services and wait for all channels to bind; there is a small
        // order coupling for simplicity, to allow the HTTPS service to reuse
        // the event loop groups set up by the cleartext service if it applies
        arrayOf(
          HttpCleartextService,
          HttpsService,
          Http3Service,
        ).forEach {
          it.contribute(scope)
        }

        val services = scope.bindFuture().get()
        require(services.isNotEmpty()) {
          "No services were configured, please ensure the options always specify at least one service"
        }

        val failed = services.count { it.bindResult.isFailure }
        log.debug("Started ${services.size} service(s) in ${bindStarted.elapsedNow()} ($failed failed)")

        val stack = HttpApplicationStack(
          services = services,
          channels = scope.channels.values.toList(),
          groups = scope.groups.values.toList(),
        )

        // notify handlers that we are now ready to go
        // stack.channels.forEach { it.pipeline().fireUserEventTriggered(ApplicationBoundEvent(stack)) }
        deferredStack.complete(stack)
        stack
      } catch (e: Exception) {
        // cleanup any groups and channels that were created
        scope.groups.values.forEach { it.shutdownGracefully() }
        scope.channels.values.forEach { it.close() }

        throw e
      }
    }
  }
}

/** Assemble a URI for this service from its scheme and bound address. */
public fun HttpApplicationStack.ServiceBinding.assembleUri(): URI {
  return when (address) {
    is InetSocketAddress -> URI(
      /* scheme = */ scheme,
      /* userInfo = */ null,
      /* host = */ address.hostName,
      /* port = */ address.port,
      /* path = */ null,
      /* query = */ null,
      /* fragment = */ null,
    )

    is DomainSocketAddress -> URI.create("unix://${URLEncoder.encode(address.path(), Charsets.UTF_8)}")
    is UnixDomainSocketAddress -> URI.create("unix://${URLEncoder.encode(address.path.pathString, Charsets.UTF_8)}")

    else -> error("Unsupported address type $address")
  }
}

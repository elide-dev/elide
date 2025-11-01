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

import io.netty.bootstrap.AbstractBootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandler
import io.netty.channel.EventLoopGroup
import io.netty.channel.unix.DomainSocketAddress
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.net.URI
import java.net.UnixDomainSocketAddress
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.absolutePathString
import elide.runtime.Logging
import elide.runtime.http.server.CallContext
import elide.runtime.http.server.HttpApplication
import elide.runtime.http.server.HttpApplicationOptions
import elide.runtime.http.server.netty.HttpApplicationStack.Service
import elide.runtime.http.server.netty.StackServiceContributor.BindingScope

/**
 * A contributor that configures HTTP services added to an [HttpApplicationStack], providing the base logic to create,
 * customize, and bind a Netty server bootstrap for an application.
 *
 * Implementation are intended to be fully static and well-known at compile time, dynamic registration and resolution
 * is not intended for this API.
 *
 * @see HttpCleartextService
 * @see HttpsService
 * @see Http3Service
 */
internal abstract class StackServiceContributor(val label: String) {
  /**
   * Encapsulates the state during the [HttpApplicationStack] configuration phase,allowing a [StackServiceContributor]
   * to register services and associated resources.
   *
   * Once all the contributor have been applied, obtain a [bindFuture] to wait for all services to bind and access the
   * resulting instances.
   */
  internal class BindingScope(
    /** Application that should be used to handle incoming calls. */
    val application: HttpApplication<CallContext>,
    /** Options used to configure the stack. */
    val options: HttpApplicationOptions,
    /** Server transport override that should be used by all services. */
    val transportOverride: HttpServerTransport?,
    /** A deferred reference to the application stack that will be created from this scope. */
    val deferredStack: Future<HttpApplicationStack>,
  ) {
    /** Resolved [HttpServerTransport] instances registered by contributors using their labels. */
    val transports: Map<String, HttpServerTransport> get() = mutableTransports
    private val mutableTransports = mutableMapOf<String, HttpServerTransport>()

    /** Event loop groups registered by contributors. See [parentGroupFor] and [childGroupFor] extensions. */
    val groups: Map<String, EventLoopGroup> get() = mutableGroups.toMap()
    private val mutableGroups = mutableMapOf<String, EventLoopGroup>()

    /** Server channels registered by contributors after they are bound to an address. */
    val channels: Map<String, Channel> get() = mutableChannels
    private val mutableChannels = mutableMapOf<String, Channel>()

    private val mutableServices = ConcurrentHashMap<String, Service>()
    private val pending = mutableMapOf<String, ChannelFuture>()

    /** Register a [transport] using the given [key]. Only one transport may be registered with a single key. */
    fun registerTransport(key: String, transport: HttpServerTransport) {
      require(mutableTransports.put(key, transport) == null) { "Duplicate transport for key $key: $transport" }
    }

    /** Register an [eventLoopGroup] using the given [key]. Only one group may be registered with a single key. */
    fun registerGroup(key: String, eventLoopGroup: EventLoopGroup) {
      require(mutableGroups.put(key, eventLoopGroup) == null) {
        "Duplicate event loop group for key $key: $eventLoopGroup"
      }
    }

    /** Register a bound [channel] using the given [key]. Only one channel may be registered with a single key. */
    fun registerChannel(key: String, channel: Channel) {
      require(mutableChannels.put(key, channel) == null) { "Duplicate channel for key $key: $channel" }
    }

    /**
     * Notify the scope about a binding in progress for a given service [key]. The [bindFuture] is used to wait until
     * the channel is bound before finalizing the scope.
     */
    fun bindingService(key: String, bindFuture: ChannelFuture) {
      require(pending.put(key, bindFuture) == null) { "Duplicate binding service for $key: $bindFuture" }
    }

    /**
     * Register a service after its binding future completes. Note that the binding itself need not be successful, the
     * outcome is stored in the [Service.bindResult] field.
     */
    fun registerService(key: String, service: Service) {
      require(mutableServices.put(key, service) == null) { "Duplicate service for $key: $service" }
    }

    /**
     * Returns a future that waits for all registered services to finalize binding their channels, and returns a list
     * of [Service] records with the results.
     */
    fun bindFuture(): Future<List<Service>> = CompletableFuture<List<Service>>().apply {
      val remaining = AtomicInteger(pending.size)
      for (future in pending.values) future.addListener {
        // complete the promise once all pending futures are done
        if (remaining.decrementAndGet() == 0) complete(mutableServices.values.toList())
      }
    }
  }

  /**
   * Whether the service uses UDP instead of TCP. This is used to select the channel type and request specific
   * transport features.
   */
  internal abstract val useUdp: Boolean

  /** The URL scheme for this service, used to construct its base URL after binding. */
  internal abstract val targetScheme: String

  /** Whether this contributor is applicable to a specific [scope]. */
  internal abstract fun isApplicable(scope: BindingScope): Boolean

  /**
   * Select the target address to bind to. Note that the actual address value used may differ if the resolved transport
   * requires conversion to a specific address type.
   */
  internal abstract fun selectAddress(scope: BindingScope): SocketAddress

  /**
   * Prepare a base bootstrap instance to be configured and bound. Defaults to using a new [ServerBootstrap], but
   * classes may override this as long as the returned instance is compatible with the channel types provided by
   * the resolved transport.
   */
  internal open fun prepareBootstrap(): AbstractBootstrap<*, Channel> {
    @Suppress("UNCHECKED_CAST")
    return ServerBootstrap() as AbstractBootstrap<*, Channel>
  }

  /**
   * Prepare the [ChannelHandler] instance that will be added to the bootstrap to handle client connections.
   *
   * When the selected bootstrap is a [ServerBootstrap], this is set as the [ServerBootstrap.childHandler], otherwise
   * it is set as the [AbstractBootstrap.handler].
   */
  internal abstract fun prepareHandler(scope: BindingScope): ChannelHandler

  /**
   * Select the preferred transport for this service; implementations are not required to respect the override value
   * specified in the options but doing so is encouraged to allow proper testing.
   */
  internal open fun resolveTransport(scope: BindingScope, address: SocketAddress): HttpServerTransport {
    return resolveTransport(scope, address, udp = useUdp)
  }

  /**
   * Return an [EventLoopGroup] instance that should be used for this service. The instance is automatically registered
   * with the [scope]
   *
   * When [child] is `true`, the returned group will be used as a child group in a [ServerBootstrap], otherwise it will
   * be used a parent group or as the single group of an [AbstractBootstrap].
   */
  internal open fun newGroup(scope: BindingScope, transport: HttpServerTransport, child: Boolean): EventLoopGroup {
    return transport.eventLoopGroup()
  }

  /**
   * Contribute to a binding [scope], preparing a service instance and registering its components as they are created.
   */
  internal fun contribute(scope: BindingScope) {
    if (!isApplicable(scope)) return

    // resolve address and transport
    val targetAddress = selectAddress(scope)

    val resolvedTransport = resolveTransport(scope, targetAddress)
    val resolvedAddress = resolvedTransport.mapAddress(targetAddress)

    scope.registerTransport(label, resolvedTransport)

    log.debug(
      "Configuring $label server for address={}, transport={} {}",
      resolvedAddress, resolvedTransport, if (scope.transportOverride != null) "(manual override)" else "(resolved)",
    )

    prepareBootstrap().apply {
      // set up event loop groups
      if (this is ServerBootstrap) {
        val parent = newGroup(scope, resolvedTransport, child = false)
        scope.registerGroup("$label:$GROUP_PARENT_SUFFIX", parent)

        val child = newGroup(scope, resolvedTransport, child = true)
        scope.registerGroup("$label:$GROUP_CHILD_SUFFIX", child)

        group(parent, child)
      } else {
        val single = newGroup(scope, resolvedTransport, child = false)
        scope.registerGroup(label, single)

        group(single)
      }

      // select the channel class
      val channelType = if (useUdp) resolvedTransport.udpChannel(resolvedAddress.isDomainSocket())
      else resolvedTransport.tcpChannel(resolvedAddress.isDomainSocket())
      channel(channelType)

      // set the target address (use the transport-specific version)
      localAddress(resolvedAddress)

      // set channel handler
      if (this is ServerBootstrap) childHandler(prepareHandler(scope))
      else handler(prepareHandler(scope))

      // apply final transport options
      resolvedTransport.configure(this)
    }.bindAndRegister(
      provider = this,
      scope = scope,
      configuredAddress = targetAddress,
      serviceScheme = targetScheme,
    )
  }

  companion object {
    private val log = Logging.of(StackServiceContributor::class)

    const val GROUP_PARENT_SUFFIX = ":parent"
    const val GROUP_CHILD_SUFFIX = ":child"
  }
}

/** Returns the "parent" [EventLoopGroup] registered for the given [contributor] in this scope. */
internal fun BindingScope.parentGroupFor(contributor: StackServiceContributor): EventLoopGroup? {
  return groups["${contributor.label}${StackServiceContributor.GROUP_PARENT_SUFFIX}"]
}

/** Returns the "child" [EventLoopGroup] registered for the given [contributor] in this scope. */
internal fun BindingScope.childGroupFor(contributor: StackServiceContributor): EventLoopGroup? {
  return groups["${contributor.label}${StackServiceContributor.GROUP_CHILD_SUFFIX}"]
}

/**
 * Resolve the default transport for this service given a target [address] and preferred protocol. If the [scope]
 * defines a transport override, it will be used, otherwise the preferred transport for the current platform is used.
 *
 * If no transport supporting the requested features can be resolved an exception will be thrown. Domain socket support
 * is requested based on the target [address].
 */
internal fun StackServiceContributor.resolveTransport(
  scope: BindingScope,
  address: SocketAddress,
  udp: Boolean,
): HttpServerTransport {
  scope.transportOverride?.let { return it }
  val isDomain = address.isDomainSocket()

  return HttpServerTransport.resolve(
    tcpDomainSockets = !udp && isDomain,
    udpDomainSockets = udp && isDomain,
  ) ?: error("Failed to resolve a server transport for service $label and address $address")
}

/**
 * Returns the base path for this service given its [configuredAddress], [boundAddress], and [scheme]. Note that the
 * returned value need not be a URL, such as for domain sockets.
 *
 * When the service is bound to a non-domain socket, the host segment of the URL authority is taken from the
 * [configuredAddress], and the port from the [boundAddress] is used. For domain sockets the absolute path to the
 * file is used.
 */
@Suppress("UnusedReceiverParameter")
internal fun StackServiceContributor.basePath(
  scheme: String,
  configuredAddress: SocketAddress,
  boundAddress: SocketAddress,
): String = when (configuredAddress) {
  is DomainSocketAddress -> configuredAddress.path()
  is UnixDomainSocketAddress -> configuredAddress.path.absolutePathString()
  else -> {
    val host = (configuredAddress as? InetSocketAddress)?.hostString ?: "localhost"
    val port = (boundAddress as? InetSocketAddress)?.port ?: -1
    URI(scheme, null, host, port, null, null, null).toString()
  }
}

/**
 * Bind this bootstrap to its target address, registering the binding future with the scope. A listener is configured
 * to register the resulting channel and service binding once the operation is complete.
 */
internal fun AbstractBootstrap<*, *>.bindAndRegister(
  provider: StackServiceContributor,
  scope: BindingScope,
  configuredAddress: SocketAddress,
  serviceScheme: String
) {
  val future = bind()
  scope.bindingService(provider.label, future)

  future.addListener {
    val result = runCatching { future.get(); future.channel() }.map {
      val channel = future.channel()
      scope.registerChannel(provider.label, channel)

      HttpApplicationStack.ServiceBinding(
        address = channel.localAddress(),
        scheme = serviceScheme,
      )
    }

    val service = Service(provider.label, result)
    scope.registerService(provider.label, service)
  }
}

/**
 * Returns the [SocketAddress] used for this service in the given [scope], or `null` if the service is not applicable
 * in this context.
 */
internal fun StackServiceContributor.bindingAddress(scope: BindingScope): SocketAddress? {
  if (!isApplicable(scope)) return null

  val baseAddress = selectAddress(scope)
  val resolvedTransport = resolveTransport(scope, baseAddress, useUdp)

  return resolvedTransport.mapAddress(baseAddress)
}

/**
 * Builds an [AltService] record for this service with respect to a [sponsor]; the [protocol] should be an ALPN ID that
 * will be used to advertise this service.
 *
 * The authority for the record depends on the binding address of both the source and this service; if the hostnames
 * do not match, the host for this service will appear in the authority string, otherwise only the port will be set.
 *
 * Using domain sockets this service will disable advertising.
 */
internal fun StackServiceContributor.altServiceFor(
  protocol: String,
  scope: BindingScope,
  sponsor: StackServiceContributor
): AltServiceSource? {
  if (!HttpsService.isApplicable(scope)) return null
  // domain sockets are not allowed here
  val bindingAddress = bindingAddress(scope) as? InetSocketAddress ?: return null

  return AltServiceSource(
    protocol = protocol,
    sponsorService = sponsor.label,
    altService = label,
    altServiceHost = bindingAddress.hostName,
  )
}

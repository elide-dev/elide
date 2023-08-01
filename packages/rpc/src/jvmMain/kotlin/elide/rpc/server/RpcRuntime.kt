@file:Suppress("unused")

package elide.rpc.server

import io.grpc.*
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService
import io.grpc.reflection.v1alpha.ServerReflectionGrpc
import io.grpc.stub.AbstractStub
import io.micronaut.context.annotation.Context
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import jakarta.inject.Inject
import jakarta.inject.Singleton
import elide.runtime.Logger
import elide.runtime.Logging

/**
 * Manages access to gRPC services at runtime, primarily for the purpose of dispatching arbitrary gRPC methods from RPCs
 * encountered via Elide's Micronaut frontend; responsible for safely initializing and registering the full set of RPC
 * services supported by a developer's application, resolving those services, and mediating state of service health.
 *
 * Elide's RPC runtime is built on top of Micronaut's built-in gRPC runtime support. When a gRPC server is initialized
 * during server startup, a Bean creation event is received via [GrpcConfigurator] and handled here, to start up the
 * RPC runtime.
 *
 * Later, when RPCs arrive at the server, the handler resolves the appropriate service and method via this logic.
 *
 * @param healthManager Service health manager, which keeps track of each service's health state.
 */
@Context @Singleton
internal class RpcRuntime
  @Inject
  constructor(
  private val healthManager: ServiceHealthManager,
) {
  // Private logger.
  private val logging: Logger = Logging.of(RpcRuntime::class)

  // Whether the RPC runtime has initialized.
  private var initialized: AtomicBoolean = AtomicBoolean(false)

  // Whether the RPC runtime is ready for requests yet.
  private var ready: AtomicBoolean = AtomicBoolean(false)

  // Set of registered gRPC services.
  private var registeredServices: MutableMap<String, ServerServiceDefinition> = WeakHashMap()

  // Registry of applicable compressors.
  private val compressorRegistry: CompressorRegistry = CompressorRegistry.getDefaultInstance()

  // Registry of applicable de-compressors.
  private val decompressorRegistry: DecompressorRegistry = DecompressorRegistry.getDefaultInstance()

  // Name of the in-process gRPC server mirror.
  private var serviceName: String? = null

  // In-process gRPC server mirror.
  private lateinit var serviceRelay: Server

  // In-process gRPC channel.
  private lateinit var internalChannel: ManagedChannel

  // Notify the RPC runtime that the RPC server is ready to handle requests.
  internal fun notifyReady() {
    ready.compareAndExchange(false, true)
  }

  // Configure an arbitrary `builder` for use as an Elide-enabled gRPC server.
  internal fun configureServer(builder: ServerBuilder<*>, compression: Boolean = true): ServerBuilder<*> {
    // setup any server-level settings
    if (compression) {
      builder.compressorRegistry(compressorRegistry)
      builder.decompressorRegistry(decompressorRegistry)
    }

    // add reflection service and indicate it is up
    builder.addService(
      ProtoReflectionService.newInstance(),
    )
    healthManager.notifyServing(
      ServerReflectionGrpc.getServiceDescriptor(),
    )

    // indicate health service as up
    builder.addService(
      healthManager.service,
    )
    return builder
  }

  // Acquire a managed channel which is connected to the in-process service relay.
  internal fun inProcessChannel(): ManagedChannel {
    return this.internalChannel
  }

  // Prepare a stub with settings that integrate execution, compression, etc., with Elide settings.
  internal fun prepareStub(stub: AbstractStub<*>, interceptors: List<ClientInterceptor>): AbstractStub<*> {
    return stub.withInterceptors(
      *interceptors.toTypedArray(),
    )
  }

  // Prepare an in-process server which mirrors the real one's behavior.
  private fun prepInProcessServer() {
    val serverName = InProcessServerBuilder.generateName()
    logging.debug {
      "Generated inprocess gRPC server name: '$serverName'"
    }

    val builder = configureServer(
      InProcessServerBuilder.forName(serverName),
      compression = false,  // force-disable compression so we don't double-compress with the backing server
    )
    registeredServices.forEach { (_, binding) ->
      builder.addService(binding)
    }

    // build and start the in-proc server
    val server = builder.build()
    serviceRelay = server
    serviceName = serverName
    server.start()

    // set up an internal channel to the new server
    internalChannel = InProcessChannelBuilder.forName(
      serverName,
    ).build()

    initialized.compareAndExchange(
      false,
      true,
    )
  }

  /**
   * Register the provided list of service [descriptors] with the RPC runtime as the exhaustive list of supported gRPC
   * services available for dispatch via Elide.
   *
   * @param descriptors Set of service descriptors to register with the RPC runtime.
   */
  @Synchronized internal fun registerServices(descriptors: List<ServerServiceDefinition>) {
    // gate initialization
    descriptors.map { svc ->
      registeredServices[svc.serviceDescriptor.name] = svc
      logging.trace("Registered gRPC service with Elide RPC runtime: '${svc.serviceDescriptor.name}'")
    }
    prepInProcessServer()
  }

  /**
   * Resolve the service matching the specified [name], or provide `null` instead to indicate that there is no service
   * at that [name].
   *
   * @param name Fully-qualified name of the gRPC service to resolve.
   * @return Corresponding service descriptor, or `null` if none could be located.
   */
  fun resolveService(name: String): ServerServiceDefinition? {
    return registeredServices[name]
  }
}

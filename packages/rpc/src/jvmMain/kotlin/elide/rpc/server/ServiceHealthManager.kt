package elide.rpc.server

import com.google.common.annotations.VisibleForTesting
import io.grpc.BindableService
import io.grpc.ServiceDescriptor
import io.grpc.health.v1.HealthCheckResponse.ServingStatus
import io.grpc.protobuf.services.HealthStatusManager
import io.micronaut.context.annotation.Context
import jakarta.inject.Singleton
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Dedicated manager for service health signals; controls the central gRPC health checking service.
 *
 * When a service is mounted via the [RpcRuntime], it is registered with the health service. Settings for Elide's RPC
 * layer govern whether health methods are exposed to callers.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
@Context @Singleton public class ServiceHealthManager {
  // Holds onto active service health and provides the service implementation.
  private val healthMonitor: HealthStatusManager = HealthStatusManager()

  // Private current-status state.
  @VisibleForTesting internal val currentStatus: MutableMap<String, ServingStatus> = ConcurrentSkipListMap()

  // Indicates whether the server is shutting down.
  @VisibleForTesting internal val terminalState: AtomicBoolean = AtomicBoolean(false)

  // Read-only access to health service.
  @VisibleForTesting public val service: BindableService get() = healthMonitor.healthService

  /**
   * Notify the central service health system that the provided [service] should *currently* be considered under the
   * provided [status]; all health-check calls after this moment should reflect the aforementioned state. Any current
   * status for the service, as applicable, is cleared and replaced.
   *
   * Passing `null` for the [status] value forcibly clears any active status for the specified [service] (not
   * recommended except in extreme circumstances).
   *
   * @see notifyPending shorthand for pending status.
   * @see notifyServing shorthand for active status.
   * @see notifyUnknown shorthand for unknown status.
   * @param service Name for the service we are reporting status for.
   * @param status Status we are reporting for the specified service.
   */
  public fun notify(service: String, status: ServingStatus?) {
    if (status == null) {
      currentStatus.remove(service)
      healthMonitor.clearStatus(
        service
      )
    } else {
      if (status == ServingStatus.SERVICE_UNKNOWN) throw IllegalArgumentException(
        "Cannot set service status to `SERVICE_UNKNOWN`: it is output-only"
      )
      currentStatus[service] = status
      healthMonitor.setStatus(
        service,
        status
      )
    }
  }

  /**
   * Notify the central service health system that the provided [service] is currently in a `PENDING` state; a
   * corresponding [ServingStatus] will be used under the hood.
   *
   * @see notify for full control over status reporting.
   * @param service Descriptor for the service we are reporting status for.
   */
  public fun notifyPending(service: ServiceDescriptor) {
    notify(
      service.name,
      ServingStatus.UNKNOWN
    )
  }

  /**
   * Notify the central service health system that the provided [service] is currently in a `SERVING` state; a
   * corresponding [ServingStatus] will be used under the hood.
   *
   * @see notify for full control over status reporting.
   * @param service Descriptor for the service we are reporting status for.
   */
  public fun notifyServing(service: ServiceDescriptor) {
    notify(
      service.name,
      ServingStatus.SERVING
    )
  }

  /**
   * Notify the central service health system that the provided [service] is currently in a `NOT_SERVING` state; a
   * corresponding [ServingStatus] will be used under the hood.
   *
   * @see notify for full control over status reporting.
   * @param service Descriptor for the service we are reporting status for.
   */
  public fun notifyNotServing(service: ServiceDescriptor) {
    notify(
      service.name,
      ServingStatus.NOT_SERVING
    )
  }

  /**
   * Notify the central service health system that the provided [service] is currently in an `UNKNOWN` state; a
   * corresponding [ServingStatus] will be used under the hood.
   *
   * @see notify for full control over status reporting.
   * @param service Descriptor for the service we are reporting status for.
   */
  public fun notifyUnknown(service: ServiceDescriptor) {
    notify(
      service.name,
      ServingStatus.UNKNOWN
    )
  }

  /**
   * Query for the current service status for the service at [name]. If no status is available, return `UNKNOWN`.
   *
   * @param name Name of the service we wish to query status for.
   * @return Current serving status, or [ServingStatus.UNKNOWN] if unknown.
   */
  public fun currentStatus(name: String): ServingStatus {
    if (terminalState.get()) {
      return ServingStatus.NOT_SERVING
    }
    return currentStatus[name] ?: ServingStatus.SERVICE_UNKNOWN
  }

  /**
   * Query for the current service status for the service by the named service described by the provided [descriptor].
   * If no status is available, return `UNKNOWN`.
   *
   * @param descriptor Service descriptor to query status for.
   * @return Current serving status, or [ServingStatus.UNKNOWN] if unknown.
   */
  public fun currentStatus(descriptor: ServiceDescriptor): ServingStatus {
    return currentStatus(descriptor.name)
  }

  /**
   * Notify the central service health system that the API service is experiencing a total and terminal shutdown,
   * which should result in negative-status calls for all services queried on the health service. **This state is not
   * recoverable,** and should only be used for system shutdown events.
   */
  public fun terminalShutdown() {
    if (!terminalState.get()) {
      terminalState.compareAndSet(false, true)
      healthMonitor.enterTerminalState()
    }
  }
}

package elide.rpc.server

import io.grpc.health.v1.HealthCheckResponse
import io.grpc.health.v1.HealthGrpc
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.*

/** Tests for the built-in [ServiceHealthManager]. */
class ServiceHealthManagerTest {
  @Test fun testCreateEmptyManager() {
    assertDoesNotThrow {
      ServiceHealthManager()
    }
  }

  @Test fun testEmptyHealthManagerState() {
    val manager = ServiceHealthManager()
    assertFalse(
      manager.terminalState.get(),
      "terminal state for health manager should start as `false`"
    )
    assertTrue(
      manager.currentStatus.isEmpty(),
      "status map for empty health manager should start as empty"
    )
    assertNotNull(
      manager.service,
      "should be able to resolve health service descriptor from manager"
    )
  }

  @Test fun testHealthManagerUnknownService() {
    val manager = ServiceHealthManager()
    assertEquals(
      HealthCheckResponse.ServingStatus.SERVICE_UNKNOWN,
      manager.currentStatus("i.do.not.Exist"),
      "status for an unknown service should be `UNKNOWN`"
    )
  }

  @Test fun testHealthManagerStateSafety() {
    val manager1 = ServiceHealthManager()
    val manager2 = ServiceHealthManager()
    manager1.notify(
      "some.sample.Service",
      HealthCheckResponse.ServingStatus.SERVING
    )
    assertEquals(
      HealthCheckResponse.ServingStatus.SERVING,
      manager1.currentStatus("some.sample.Service"),
      "status for a service should be preserved in a single manager"
    )
    assertEquals(
      HealthCheckResponse.ServingStatus.SERVICE_UNKNOWN,
      manager2.currentStatus("some.sample.Service"),
      "status for a service should not bleed across managers"
    )
  }

  @Test fun testHealthManagerServiceLifecycle() {
    val manager = ServiceHealthManager()
    val serviceDescriptor = HealthGrpc.getServiceDescriptor()
    assertEquals(
      HealthCheckResponse.ServingStatus.SERVICE_UNKNOWN,
      manager.currentStatus(serviceDescriptor),
      "status should start at `SERVICE_UNKNOWN` before registration"
    )
    assertDoesNotThrow {
      manager.notifyPending(
        serviceDescriptor
      )
    }
    assertEquals(
      HealthCheckResponse.ServingStatus.UNKNOWN,
      manager.currentStatus(serviceDescriptor),
      "UNKNOWN status should become active after `notifyPending`"
    )
    assertDoesNotThrow {
      manager.notifyServing(
        serviceDescriptor
      )
    }
    assertEquals(
      HealthCheckResponse.ServingStatus.SERVING,
      manager.currentStatus(serviceDescriptor),
      "SERVING status should become active after `notifyServing`"
    )
    assertDoesNotThrow {
      manager.notifyNotServing(
        serviceDescriptor
      )
    }
    assertEquals(
      HealthCheckResponse.ServingStatus.NOT_SERVING,
      manager.currentStatus(serviceDescriptor),
      "NOT_SERVING status should become active after `notifyNotServing`"
    )
    assertDoesNotThrow {
      manager.notifyUnknown(
        serviceDescriptor
      )
    }
    assertEquals(
      HealthCheckResponse.ServingStatus.UNKNOWN,
      manager.currentStatus(serviceDescriptor),
      "UNKNOWN status should become active after `notifyUnknown`"
    )
  }

  @Test fun testHealthManagerServiceLifecycleLowLevel() {
    val manager = ServiceHealthManager()
    val serviceDescriptor = HealthGrpc.getServiceDescriptor()
    assertEquals(
      HealthCheckResponse.ServingStatus.SERVICE_UNKNOWN,
      manager.currentStatus(serviceDescriptor),
      "status should start at `SERVICE_UNKNOWN` before registration"
    )
    assertDoesNotThrow {
      manager.notify(
        serviceDescriptor.name,
        HealthCheckResponse.ServingStatus.UNKNOWN,
      )
    }
    assertEquals(
      HealthCheckResponse.ServingStatus.UNKNOWN,
      manager.currentStatus(serviceDescriptor),
      "UNKNOWN status should become active after `notifyPending`"
    )
    assertDoesNotThrow {
      manager.notify(
        serviceDescriptor.name,
        HealthCheckResponse.ServingStatus.SERVING,
      )
    }
    assertEquals(
      HealthCheckResponse.ServingStatus.SERVING,
      manager.currentStatus(serviceDescriptor),
      "SERVING status should become active after `notifyServing`"
    )
    assertDoesNotThrow {
      manager.notify(
        serviceDescriptor.name,
        HealthCheckResponse.ServingStatus.NOT_SERVING,
      )
    }
    assertEquals(
      HealthCheckResponse.ServingStatus.NOT_SERVING,
      manager.currentStatus(serviceDescriptor),
      "NOT_SERVING status should become active after `notifyNotServing`"
    )
    assertDoesNotThrow {
      manager.notify(
        serviceDescriptor.name,
        HealthCheckResponse.ServingStatus.UNKNOWN,
      )
    }
    assertEquals(
      HealthCheckResponse.ServingStatus.UNKNOWN,
      manager.currentStatus(serviceDescriptor),
      "UNKNOWN status should become active after `notifyUnknown`"
    )

    // `SERVICE_UNKNOWN` is output-only
    assertThrows<IllegalArgumentException> {
      manager.notify(
        serviceDescriptor.name,
        HealthCheckResponse.ServingStatus.SERVICE_UNKNOWN,
      )
    }
  }

  @Test fun testHealthManagerTerminalState() {
    val manager = ServiceHealthManager()
    val manager2 = ServiceHealthManager()
    assertFalse(
      manager.terminalState.get(),
      "terminal state for health manager should start as `false`"
    )
    manager.notify(
      "some.service.Here",
      HealthCheckResponse.ServingStatus.SERVING
    )
    manager.notify(
      "some.other.service.Here",
      HealthCheckResponse.ServingStatus.NOT_SERVING
    )
    manager.notify(
      "some.third.service.Here",
      HealthCheckResponse.ServingStatus.UNKNOWN
    )
    assertEquals(
      HealthCheckResponse.ServingStatus.SERVING,
      manager.currentStatus("some.service.Here"),
      "status for a service should be preserved in a single manager (SERVING)"
    )
    assertEquals(
      HealthCheckResponse.ServingStatus.NOT_SERVING,
      manager.currentStatus("some.other.service.Here"),
      "status for a service should be preserved in a single manager (NOT_SERVING)"
    )
    assertEquals(
      HealthCheckResponse.ServingStatus.UNKNOWN,
      manager.currentStatus("some.third.service.Here"),
      "status for a service should be preserved in a single manager (UNKNOWN)"
    )
    assertEquals(
      HealthCheckResponse.ServingStatus.SERVICE_UNKNOWN,
      manager.currentStatus("some.service.that.doesnt.exist.Here"),
      "status for an unknown service in non-terminal state should be SERVICE_UNKNOWN"
    )

    // turn on terminal state
    assertDoesNotThrow {
      manager.terminalShutdown()
    }
    assertTrue(
      manager.terminalState.get(),
      "terminal state for health manager should report as `true` after terminal shutdown"
    )
    assertEquals(
      HealthCheckResponse.ServingStatus.NOT_SERVING,
      manager.currentStatus("some.service.Here"),
      "status for every service should report as NOT_SERVING after terminal shutdown"
    )
    assertEquals(
      HealthCheckResponse.ServingStatus.NOT_SERVING,
      manager.currentStatus("some.other.service.Here"),
      "status for every service should report as NOT_SERVING after terminal shutdown"
    )
    assertEquals(
      HealthCheckResponse.ServingStatus.NOT_SERVING,
      manager.currentStatus("some.third.service.Here"),
      "status for every service should report as NOT_SERVING after terminal shutdown"
    )
    assertEquals(
      HealthCheckResponse.ServingStatus.NOT_SERVING,
      manager.currentStatus("some.service.that.doesnt.exist.Here"),
      "status for every service should report as NOT_SERVING after terminal shutdown"
    )
    assertFalse(
      manager2.terminalState.get(),
      "terminal state for health manager should not bleed between managers"
    )
  }

  @Test fun testHealthUnmountService() {
    val manager = ServiceHealthManager()
    manager.notify(
      "some.service.Here",
      HealthCheckResponse.ServingStatus.SERVING
    )
    manager.notify(
      "some.other.service.Here",
      HealthCheckResponse.ServingStatus.SERVING
    )
    assertEquals(
      HealthCheckResponse.ServingStatus.SERVING,
      manager.currentStatus("some.service.Here"),
      "status for a service should be preserved in a single manager"
    )
    assertEquals(
      HealthCheckResponse.ServingStatus.SERVING,
      manager.currentStatus("some.other.service.Here"),
      "status for a service should be preserved in a single manager"
    )

    // "unmount" the service
    assertDoesNotThrow {
      manager.notify(
        "some.service.Here",
        null
      )
    }
    assertEquals(
      HealthCheckResponse.ServingStatus.SERVICE_UNKNOWN,
      manager.currentStatus("some.service.Here"),
      "status for un-mounted service should be `SERVICE_UNKNOWN`"
    )
    assertEquals(
      HealthCheckResponse.ServingStatus.SERVING,
      manager.currentStatus("some.other.service.Here"),
      "status for not change for service which was not un-mounted"
    )
  }
}

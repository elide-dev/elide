package elide.runtime.gvm.internals.vfs

import elide.runtime.gvm.internals.DualTestCase
import elide.testing.annotations.Test
import elide.runtime.gvm.internals.GuestVFS
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.nio.file.AccessMode
import java.nio.file.Path
import java.util.EnumSet
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Baseline abstract tests for VFS implementations ([GuestVFS]). */
internal abstract class AbstractVFSTest<VFS, Builder, Factory> : DualTestCase()
  where VFS: AbstractBaseVFS<VFS>,
        Builder: AbstractBaseVFS.VFSBuilder<VFS>,
        Factory: AbstractBaseVFS.VFSFactory<VFS, Builder> {
  /** @return Empty builder for this VFS implementation. */
  abstract fun newBuilder(): Builder

  /** @return Factory to create new VFS implementation instances. */
  abstract fun factory(): Factory

  /** @return Indication of whether host files should actually be created or modified by this adapter. */
  abstract fun shouldUseHost(): Boolean

  /** Assert that the provided [response] represents an allowed I/O call. */
  private fun assertAllowed(response: AccessResponse, message: String? = null) {
    assertEquals(AccessResult.ALLOW, response.policy, if (message.isNullOrBlank()) {
      "Expected access to be allowed, but was denied: $response"
    } else {
      message
    })
  }

  /** Assert that the provided [response] represents a disallowed I/O call. */
  private fun assertDenied(response: AccessResponse, message: String? = null) {
    assertEquals(AccessResult.DENY, response.policy, if (message.isNullOrBlank()) {
      "Expected access to be denied, but was allowed: $response"
    } else {
      message
    })
  }

  /** Test: Acquire a new builder for this VFS implementation. */
  @Test fun testAcquireBuilder() {
    assertNotNull(newBuilder(), "should not get `null` when acquiring a new VFS impl builder")
  }

  /** Test: Create an empty instance of this VFS implementation. */
  @Test fun testCreateEmpty() {
    assertNotNull(newBuilder(), "should not get `null` when acquiring a new VFS impl builder")
    val empty = newBuilder().build()
    assertNotNull(empty, "should not get `null` when building an empty VFS instance")
  }

  /** Test: Create a VFS implementation from an on-hand config. */
  @Test fun testCreateConfigured() {
    val config = EffectiveGuestVFSConfig(readOnly = false)
    assertNotNull(factory().create(config), "should be able to create a VFS implementation from config")
  }

  /** Test: Read-only filesystem policy rejects writes. */
  @Test fun testPolicyReadOnly() {
    val config = EffectiveGuestVFSConfig(readOnly = true)
    assertEquals(true, config.readOnly)
    val vfs = factory().create(config)
    assertEquals(true, vfs.config.readOnly)

    // should allow reads
    assertAllowed(vfs.checkPolicy(
      type = AccessType.READ,
      path = Path.of("/some-path.txt"),
      domain = AccessDomain.GUEST,
    ), "read operation should be allowed in read-only mode")

    // should deny writes
    assertDenied(vfs.checkPolicy(
      type = AccessType.WRITE,
      path = Path.of("/some-path.txt"),
      domain = AccessDomain.GUEST,
    ), "write operation should be denied in read-only mode")

    // should deny deletes
    assertDenied(vfs.checkPolicy(
      type = AccessType.DELETE,
      path = Path.of("/some-path.txt"),
      domain = AccessDomain.GUEST,
    ), "delete operation should be denied in read-only mode")

    // should deny multiple access with write
    assertDenied(vfs.checkPolicy(
      type = EnumSet.of(AccessType.READ, AccessType.WRITE),
      path = Path.of("/some-path.txt"),
      domain = AccessDomain.GUEST,
    ), "write operation should be denied in read-only mode")

    // should deny multiple access with write
    assertDenied(vfs.checkPolicy(
      type = EnumSet.of(AccessType.READ, AccessType.DELETE),
      path = Path.of("/some-path.txt"),
      domain = AccessDomain.GUEST,
    ), "delete operation should be denied in read-only mode")
  }

  /** Test: Writable filesystem policy does not reject writes. */
  @Test fun testPolicyWritable() {
    val config = EffectiveGuestVFSConfig(readOnly = false)
    assertEquals(false, config.readOnly)
    val vfs = factory().create(config)
    assertEquals(false, vfs.config.readOnly)

    // should allow reads
    assertAllowed(vfs.checkPolicy(
      type = AccessType.READ,
      path = Path.of("/some-path.txt"),
      domain = AccessDomain.GUEST,
    ), "read operation should be allowed in writable mode")

    // should allow writes
    assertAllowed(vfs.checkPolicy(
      type = AccessType.WRITE,
      path = Path.of("/some-path.txt"),
      domain = AccessDomain.GUEST,
    ), "write operation should be allowed in writable mode")

    // should allow deletes
    assertAllowed(vfs.checkPolicy(
      type = AccessType.DELETE,
      path = Path.of("/some-path.txt"),
      domain = AccessDomain.GUEST,
    ), "delete operation should be allowed in writable mode")

    // should allow multiple access with write
    assertAllowed(vfs.checkPolicy(
      type = EnumSet.of(AccessType.READ, AccessType.WRITE),
      path = Path.of("/some-path.txt"),
      domain = AccessDomain.GUEST,
    ), "write operation should be allowed in writable mode")

    // should deny multiple access with write
    assertAllowed(vfs.checkPolicy(
      type = EnumSet.of(AccessType.READ, AccessType.DELETE),
      path = Path.of("/some-path.txt"),
      domain = AccessDomain.GUEST,
    ), "delete operation should be allowed in writable mode")
  }

  /** Test: Read-only filesystem access check rejects writes. */
  @Test fun testAccessCheckReadOnly() {
    val config = EffectiveGuestVFSConfig(readOnly = true)
    assertEquals(true, config.readOnly)
    val vfs = factory().create(config)
    assertEquals(true, vfs.config.readOnly)

    // should allow reads
    assertDoesNotThrow("read operation should be allowed in read-only mode") {
      vfs.checkAccess(
        Path.of("/some-path.txt"),
        mutableSetOf(AccessMode.READ),
      )
    }

    // should deny writes
    assertThrows<GuestIOException>("write operation should be denied in read-only mode") {
      vfs.checkAccess(
        Path.of("/some-path.txt"),
        mutableSetOf(AccessMode.WRITE),
      )
    }
  }

  /** Test: Writable filesystem access check rejects writes. */
  @Test fun testAccessCheckWritable() {
    val config = EffectiveGuestVFSConfig(readOnly = false)
    assertEquals(false, config.readOnly)
    val vfs = factory().create(config)
    assertEquals(false, vfs.config.readOnly)

    // should allow reads
    assertDoesNotThrow("read operation should be allowed in writable mode") {
      vfs.checkAccess(
        Path.of("/some-path.txt"),
        mutableSetOf(AccessMode.READ),
      )
    }

    // should allow writes
    assertDoesNotThrow("write operation should be allowed in writable mode") {
      vfs.checkAccess(
        Path.of("/some-path.txt"),
        mutableSetOf(AccessMode.WRITE),
      )
    }
  }
}

package elide.embedded

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.junit.jupiter.api.io.TempDir
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.nio.file.Path
import kotlinx.coroutines.test.runTest
import kotlin.io.path.absolutePathString
import kotlin.test.assertEquals
import elide.embedded.native.*
import elide.embedded.native.ElideNativeLibrary.EmbeddedAppNativeConfig
import elide.embedded.native.ElideNativeLibrary.EmbeddedNativeConfig
import elide.embedded.native.ElideNativeLibrary.GraalVM

@EnabledIfSystemProperty(
  named = "elide.embedded.tests.interop",
  matches = "true",
  disabledReason = "Native interop tests are disabled",
)
class NativeEmbeddedRuntimeTest {
  /** Temporary root for generated guest applications. */
  @TempDir lateinit var testGuestRoot: Path

  /** Create a GraalVM native isolate thread in the current arena and return a pointer to it. */
  context(Arena) private fun prepareIsolate(): MemorySegment {
    // allocate the pointer for the thread
    val threadPointer = allocatePointer()

    // prepare the GraalVM isolate, note that we pass pointers to pointers (e.g. graal_isolate_t**)
    val isolateResult = GraalVM.createIsolate(MemorySegment.NULL, MemorySegment.NULL, threadPointer)
    assertEquals(expected = 0, actual = isolateResult, "expected isolate creation to succeed")

    return threadPointer.pointerValue()
  }

  /** Allocate and initialize a runtime configuration struct with the specified values. */
  context(Arena) private fun prepareRuntimeConfig(
    version: Int = 0,
    format: Int = 0,
    guestRoot: String = testGuestRoot.absolutePathString(),
  ): MemorySegment {
    val config = allocate(EmbeddedNativeConfig.LAYOUT)

    EmbeddedNativeConfig.version.set(config, version)
    EmbeddedNativeConfig.format.set(config, format)
    EmbeddedNativeConfig.guestRoot.set(config, allocateUtf8String(guestRoot))

    return config
  }

  /** Allocate and initialize an runtime configuration struct with the specified values. */
  context(Arena) private fun prepareAppConfig(
    id: String = "test-app",
    language: String = "js",
    entrypoint: String = "index.js",
    mode: Int = 0,
  ): MemorySegment {
    val appConfig = allocate(EmbeddedAppNativeConfig.LAYOUT)

    EmbeddedAppNativeConfig.id.set(appConfig, allocateUtf8String(id))
    EmbeddedAppNativeConfig.language.set(appConfig, allocateUtf8String(language))
    EmbeddedAppNativeConfig.entrypoint.set(appConfig, allocateUtf8String(entrypoint))
    EmbeddedAppNativeConfig.mode.set(appConfig, mode)

    return appConfig
  }

  @Test fun `should reject duplicate initialization`() {
    withArena {
      // initialize a gvm native isolate
      val thread = prepareIsolate()

      // initialize and configure the runtime
      val initResult = ElideNativeLibrary.initialize(thread, prepareRuntimeConfig())
      assertEquals(expected = 0, actual = initResult, "expected first init call to succeed")

      // second init call should fail with code 3 (ELIDE_ERR_ALREADY_INITIALIZED)
      val reinitResult = ElideNativeLibrary.initialize(thread, prepareRuntimeConfig())
      assertEquals(expected = 3, actual = reinitResult, "expected second init call to fail with code 3")
    }
  }

  @Test fun `should require initialization`() {
    withArena {
      // initialize a gvm native isolate
      val thread = prepareIsolate()

      val cases = mapOf(
        "start" to ElideNativeLibrary.start(thread),
        "dispatch" to ElideNativeLibrary.dispatch(thread),
        "stop" to ElideNativeLibrary.stop(thread),
      )

      // all operations should fail with code 2 (ELIDE_ERR_UNINITIALIZED)
      for ((function, code) in cases) assertEquals(
        expected = 2,
        actual = code,
        message = "expected '$function' call to fail with code 2",
      )
    }
  }

  @Test fun `should allow managing apps`() = runTest {
    withArena {
      // initialize a gvm native isolate
      val thread = prepareIsolate()

      // initialize and configure the runtime
      assertNativeSuccess("expected init call to succeed") {
        ElideNativeLibrary.initialize(thread, prepareRuntimeConfig())
      }

      assertNativeSuccess("expected start call to succeed") {
        ElideNativeLibrary.start(thread)
      }

      // prepare the app config struct
      val appConfig = prepareAppConfig()

      // allocate the app handle and create the app
      val appHandle = allocatePointer()
      assertNativeSuccess("expected app creation to succeed") {
        ElideNativeLibrary.createApp(thread, appConfig, appHandle)
      }

      // start the app
      assertNativeSuccessSuspending("expected startup to succeed") { callback ->
        ElideNativeLibrary.startApp(thread, appHandle.pointerValue(), callback)
      }

      // stop the app
      assertNativeSuccessSuspending("expected startup to succeed") { callback ->
        ElideNativeLibrary.stopApp(thread, appHandle.pointerValue(), callback)
      }
    }
  }
}
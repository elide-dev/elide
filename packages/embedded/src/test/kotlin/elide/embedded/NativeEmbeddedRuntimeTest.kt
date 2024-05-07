/*
 * Copyright (c) 2024 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

package elide.embedded

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.junit.jupiter.api.io.TempDir
import tools.elide.call.callMetadata
import tools.elide.call.v1alpha1.fetchRequest
import tools.elide.call.v1alpha1.unaryInvocationRequest
import tools.elide.http.HttpMethod
import tools.elide.http.httpHeader
import tools.elide.http.httpHeaders
import tools.elide.http.httpRequest
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.nio.file.Path
import kotlinx.coroutines.test.runTest
import kotlin.io.path.absolutePathString
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText
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
  private fun Arena.prepareIsolate(): MemorySegment {
    // allocate the pointer for the thread
    val threadPointer = allocatePointer()

    // prepare the GraalVM isolate, note that we pass pointers to pointers (e.g. graal_isolate_t**)
    val isolateResult = GraalVM.createIsolate(MemorySegment.NULL, MemorySegment.NULL, threadPointer)
    assertEquals(expected = 0, actual = isolateResult, "expected isolate creation to succeed")

    return threadPointer.pointerValue()
  }

  /** Allocate and initialize a runtime configuration struct with the specified values. */
  private fun Arena.prepareRuntimeConfig(
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
    entrypoint: String = "index.js",
    language: Int = 0,
    mode: Int = 0,
  ): MemorySegment {
    val appConfig = allocate(EmbeddedAppNativeConfig.LAYOUT)

    EmbeddedAppNativeConfig.id.set(appConfig, allocateUtf8String(id))
    EmbeddedAppNativeConfig.entrypoint.set(appConfig, allocateUtf8String(entrypoint))
    EmbeddedAppNativeConfig.language.set(appConfig, language)
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

  @Test fun `should handle dispatch`() = runTest {
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

      val entrypoint = testGuestRoot.resolve("test-app").resolve("index.js")
      entrypoint.createParentDirectories()
      entrypoint.writeText(
        """
        function fetch(request) {
          const response = new Response();

          // response.statusCode = 418;
          // response.statusMessage = "I'm a teapot 🫖";

          return response;
        }

        module.exports = { fetch }
        """.trimIndent(),
      )

      // allocate the app handle and create the app
      val appHandle = allocatePointer()
      ElideNativeLibrary.createApp(thread, appConfig, appHandle)

      // start the app
      assertNativeSuccessSuspending("expected startup to succeed") { callback ->
        ElideNativeLibrary.startApp(thread, appHandle.pointerValue(), callback)
      }

      // allocate stub request
      val requestMessage = unaryInvocationRequest {
        fetch = fetchRequest {
          metadata = callMetadata {
            appId = "test-app"
            requestId = "test-request"
          }

          request = httpRequest {
            standard = HttpMethod.GET
            path = "/hello/world"
            query = "from=me"
            headers = httpHeaders {
              httpHeader {
                name = "user-agent"
                value = "junit"
              }
            }
          }
        }
      }

      val messageBytes = requestMessage.toByteArray()
      val messagePointer = allocateArray(ValueLayout.JAVA_BYTE, *messageBytes)

      assertNativeSuccessSuspending("expected dispatch call to succeed") { callback ->
        ElideNativeLibrary.dispatch(thread, appHandle.pointerValue(), messagePointer, messageBytes.size, callback)
      }
    }
  }
}

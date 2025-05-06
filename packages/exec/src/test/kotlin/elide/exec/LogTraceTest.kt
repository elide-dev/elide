/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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
package elide.exec

import org.junit.jupiter.api.BeforeEach
import org.slf4j.event.Level
import kotlin.test.*

class LogTraceTest {
  @BeforeEach fun loadNatives() {
    Tracing.ensureLoaded()
  }

  @Test fun testTracingEnsureLoaded() {
    Tracing.ensureLoaded()
  }

  @Ignore
  @Test fun testNativeLogDelivery() {
    Tracing.ensureLoaded()
    Tracing.nativeLog("INFO", "Test log 1")
    Thread.sleep(100)
    val recent = TraceNative.allRecentTraces()
    val hasLog = recent.any { it.severity == Level.INFO && it.message?.contains("Test log 1") == true }
    assertTrue(hasLog, "should have native-delivered log")
  }

  @Ignore("Not yet supported")
  @Test fun testNativeTraceDelivery() {
    Tracing.nativeTrace("INFO", "Test trace 1")
    Thread.sleep(100)
    val recent = TraceNative.allRecentTraces()
    val hasLog = recent.any { it.severity == Level.INFO && it.message?.contains("Test trace 1") == true }
    assertTrue(hasLog, "should have native-delivered trace")
  }
}

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
package elide.runtime.diag

import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DiagnosticsTest {
  init {
    System.loadLibrary("umbrella")
  }

  @Test fun testCreateDiagnosticNative() {
    assertNotNull(assertDoesNotThrow { NativeDiagnostics.createDiagnostic() }).let { diag ->
      assertEquals("There was an issue", diag.message)
      assertEquals(Severity.WARN, diag.severity)
    }
  }

  @Test fun testCreateDiagnostic() {
    assertNotNull(assertDoesNotThrow { DiagnosticInfo.mutable() })
    val diag = Diagnostics.mutable().apply {
      message = "test"
      severity = Severity.WARN
    }
    assertEquals("test", diag.message)
    assertEquals(Severity.WARN, diag.severity)
  }

  @Test fun testCreateImmutableDiagnostic() {
    val diag = assertNotNull(assertDoesNotThrow {
      Diagnostics.mutable().apply {
        message = "test"
        severity = Severity.WARN
      }.build()
    })
    assertEquals("test", diag.message)
    assertEquals(Severity.WARN, diag.severity)
  }

  @Test fun testReportNativeDiagnostic() {
    assertDoesNotThrow {
      NativeDiagnostics.createDiagnostic()
    }
    assertTrue(
      Diagnostics.dirty()
    )
    Diagnostics.clear()
  }
}

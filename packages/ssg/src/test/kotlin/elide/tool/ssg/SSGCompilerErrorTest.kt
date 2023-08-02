/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.tool.ssg

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tests for custom [SSGCompilerError] exceptions. */
class SSGCompilerErrorTest {
  @Test fun testGenericError() {
    val error = SSGCompilerError.Generic()
    assertNotNull(error.message, "should have a message")
    assertTrue(error.message!!.isNotEmpty(), "message should not be empty")
    assertTrue(error.exitCode != 0, "exit code should be non-zero")
    assertEquals(-1, error.exitCode, "exit code should be -1 for generic error")
    assertNull(error.cause, "should preserve cause")
  }

  @Test fun testGenericErrorWithCause() {
    val cause = RuntimeException("test")
    val error = SSGCompilerError.Generic(cause)
    assertNotNull(error.message, "should have a message")
    assertTrue(error.message!!.isNotEmpty(), "message should not be empty")
    assertTrue(error.exitCode != 0, "exit code should be non-zero")
    assertEquals(-1, error.exitCode, "exit code should be -1 for generic error")
    assertNotNull(error.cause, "should preserve cause")
  }

  @Test fun testInvalidArgument() {
    val error = SSGCompilerError.InvalidArgument("an argument was invalid")
    assertNotNull(error.message, "should have a message")
    assertTrue(error.message!!.isNotEmpty(), "message should not be empty")
    assertTrue(error.exitCode != 0, "exit code should be non-zero")
    assertEquals(-2, error.exitCode, "exit code should be -2 for invalid argument error")
    assertNull(error.cause, "should preserve cause")
  }

  @Test fun testIOError() {
    val error = SSGCompilerError.IOError("an I/O operation failed")
    assertNotNull(error.message, "should have a message")
    assertTrue(error.message!!.isNotEmpty(), "message should not be empty")
    assertTrue(error.exitCode != 0, "exit code should be non-zero")
    assertEquals(-3, error.exitCode, "exit code should be -3 for I/O error")
    assertNull(error.cause, "should preserve cause")
  }

  @Test fun testOutputError() {
    val error = SSGCompilerError.OutputError("an output operation failed")
    assertNotNull(error.message, "should have a message")
    assertTrue(error.message!!.isNotEmpty(), "message should not be empty")
    assertTrue(error.exitCode != 0, "exit code should be non-zero")
    assertEquals(-4, error.exitCode, "exit code should be -4 for output error")
    assertNull(error.cause, "should preserve cause")
  }
}

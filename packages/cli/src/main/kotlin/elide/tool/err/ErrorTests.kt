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

package elide.tool.err

import dev.elide.uuid.uuid4
import io.micronaut.context.annotation.Bean
import elide.tool.annotations.EmbeddedTest
import elide.tool.err.ErrorHandler.ErrorContext
import elide.tool.err.ErrorHandler.ErrorEvent
import elide.tool.testing.SelfTest
import elide.tool.testing.TestContext

/** Test acquiring the error recorder. */
@Bean @EmbeddedTest class ErrorRecorderAcquireTest : SelfTest() {
  override suspend fun SelfTestContext.test() = TestContext.assertDoesNotThrow {
    DefaultStructuredErrorRecorder.acquire()
  }.let { recorder ->
    assertNotNull(recorder, "should be able to acquire the error recorder")
  }
}

/** Test writing an error. */
@Bean @EmbeddedTest class ErrorRecorderWriteTest : SelfTest() {
  override suspend fun SelfTestContext.test() = TestContext.assertDoesNotThrow {
    DefaultStructuredErrorRecorder.acquire()
  }.let { recorder ->
    assertNotNull(recorder, "should be able to acquire the error recorder")
    recorder.recordError(ErrorEvent.of(
      IllegalArgumentException("test - test - test"),
      ErrorContext.DEFAULT.copy(
        uuid = uuid4(),
        fatal = false,
        guest = false,
        thread = Thread.currentThread(),
      )
    )).join()
  }
}

/** Test acquiring the error handler. */
@Bean @EmbeddedTest class ErrorHandlerAcquireTest : SelfTest() {
  override suspend fun SelfTestContext.test() = TestContext.assertDoesNotThrow {
    DefaultErrorHandler.acquire()
  }.let { handler ->
    assertNotNull(handler, "should be able to acquire the error handler")
  }
}

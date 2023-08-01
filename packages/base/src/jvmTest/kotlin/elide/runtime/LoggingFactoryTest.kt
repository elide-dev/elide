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

package elide.runtime

import kotlin.test.Test
import kotlin.test.assertNotNull

/** Tests for acquiring [Logger] instances on the JVM. */
class LoggingFactoryTest {
  @Test fun testAcquireRootLogger() {
    assertNotNull(
      Logging.root(),
      "should be able to acquire root logger on JVM",
    )
  }

  @Test fun testAcquireNamedLogger() {
    assertNotNull(
      Logging.named("some.logger"),
      "should be able to acquire a logger with an arbitrary name on JVM",
    )
  }

  @Test fun testAcquireLoggerForClass() {
    assertNotNull(
      Logging.of(LoggingFactoryTest::class),
      "should be able to acquire a logger for a Kotlin class",
    )
    assertNotNull(
      Logging.of(LoggingFactoryTest::class.java),
      "should be able to acquire a logger for a Java class",
    )
  }
}

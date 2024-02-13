/*
 * Copyright (c) 2023-2024 Elide Ventures, LLC.
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

package elide.proto.test

import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.Test
import elide.proto.ElideProtocol

/** Tests for the protocol facade for each implementation. */
abstract class AbstractProtocolFacadeTest<Facade: ElideProtocol> {
  /** Provide a loader instance for automatic testing. */
  abstract fun provide(): Facade

  /** Test a static load of the protocol suite with the implementation under test. */
  @Test fun testLoader() {
    provide()
  }

  /** Make sure the `engine()` query never returns `null`. */
  @Test fun testEngine() {
    assertNotNull(provide().engine(), "should never get `null` for `engine()`")
  }

  /** Fetch the model strategy for the loaded protocol implementation. */
  @Test fun testModelStrategy() {
    val model = provide().strategy()
    assertNotNull(model, "should not get `null` for model strategy object")
  }

  /** Make sure the model strategy implementation behaves as a singleton. */
  @Test fun testModelStrategySingleton() {
    val model = provide().strategy()
    val model2 = provide().strategy()
    assertSame(model, model2, "model strategy implementation should be a singleton")
  }

  /** Fetch supported dialects for the implementation. */
  @Test fun testDialects() {
    val dialects = provide().dialects()
    assertNotNull(dialects, "dialects result should never be `null`")
    assertTrue(dialects.isNotEmpty(), "dialects should never be completely empty")
  }

  /** JSON should always be supported. */
  @Test fun testDialectJSON() {
    val dialects = provide().dialects()
    assertNotNull(dialects, "dialects result should never be `null`")
    assertTrue(dialects.isNotEmpty(), "dialects should never be completely empty")
    assertTrue(dialects.contains(ElideProtocol.Dialect.JSON), "JSON should always be supported")
  }

  /** Child test classes should test for their expected dialects. */
  abstract fun testExpectedDialects()

  /** Test that the declared implementation library is correct. */
  abstract fun testExpectedLibrary()

  // -- Interfaces: Factories -- //

  /** Test acquiring a timestamps factory. */
  @Test fun testTimestampFactoryAcquire() {
    val factory = provide().strategy().model().timestamps()
    assertNotNull(factory, "should never get `null` for timestamps factory")
  }

  /** Test acquiring a data container factory. */
  @Test fun testDataContainerFactoryAcquire() {
    val factory = provide().strategy().model().containers()
    assertNotNull(factory, "should never get `null` for data container factory")
  }

  /** Test acquiring a data fingerprint factory. */
  @Test fun testDataFingerprintFactoryAcquire() {
    val factory = provide().strategy().model().fingerprints()
    assertNotNull(factory, "should never get `null` for data fingerprint factory")
  }
}

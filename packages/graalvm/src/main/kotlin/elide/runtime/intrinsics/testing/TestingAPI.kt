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
package elide.runtime.intrinsics.testing

import io.micronaut.core.annotation.ReflectiveAccess
import org.graalvm.polyglot.Value
import elide.annotations.API
import elide.vm.annotations.Polyglot

/**
 * # Testing API
 */
@API @ReflectiveAccess public interface TestingAPI {
  public sealed interface TestGraphNode {
    public val simpleName: String
    public val qualifiedName: String

    public interface Suite : TestGraphNode

    public interface Test : TestGraphNode {
      public val block: Value
    }

    public interface Assertion : TestGraphNode
  }

  /**
   * ## Suite
   */
  @Polyglot public fun suite(block: Value): TestGraphNode.Suite = suite(label = null, block)

  /**
   * ## Suite
   */
  @Polyglot public fun suite(label: String?, block: Value): TestGraphNode.Suite

  /**
   * ## Describe
   */
  @Polyglot public fun describe(block: Value): TestGraphNode.Suite = describe(label = null, block)

  /**
   * ## Describe
   */
  @Polyglot public fun describe(label: String?, block: Value): TestGraphNode.Suite = suite(label, block)

  /**
   * ## Test
   */
  @Polyglot public fun test(value: Value): TestGraphNode.Test = test(label = null, value, scope = null)

  /**
   * ## Test
   */
  @Polyglot public fun test(value: Value, scope: TestGraphNode.Suite): TestGraphNode.Test =
    test(label = null, value, scope = scope)

  /**
   * ## Test
   */
  @Polyglot public fun test(label: String?, block: Value, scope: TestGraphNode.Suite?): TestGraphNode.Test

  /**
   * ## Expectation
   */
  @Polyglot public fun expect(value: Value?): TestGraphNode.Assertion
}

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
package elide.tooling.testing

import java.util.ServiceLoader

/**
 * Typealias for a sequence of test post-processor factories.
 */
public typealias TestPostProcessorFactories = Sequence<TestPostProcessorFactory<TestPostProcessor>>

/**
 * # Test Post-Processing
 */
public object TestPostProcessors {
  /**
   * Gather all [TestPostProcessor] from the classpath.
   *
   * @return Sequence of test-post-processor factories.
   */
  @JvmStatic public fun all(): TestPostProcessorFactories {
    @Suppress("UNCHECKED_CAST")
    return ServiceLoader.load(TestPostProcessorFactory::class.java).asSequence() as TestPostProcessorFactories
  }

  /**
   * Gather all [TestPostProcessor] from the classpath matching the provided [options].
   *
   * @return Sequence of matching test-post-processor factories.
   */
  @JvmStatic public fun matching(options: TestPostProcessingOptions): TestPostProcessorFactories {
    return all().filter { it.eligible(options) }
  }

  /**
   * Gather all [TestPostProcessor] from the classpath matching the provided [options].
   *
   * @return Sequence of matching test-post-processor factories.
   */
  @JvmStatic public fun suite(options: TestPostProcessingOptions): Sequence<TestPostProcessor> {
    return matching(options).mapNotNull { it.create(options) }
  }
}

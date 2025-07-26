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

/**
 * ## Test Post-Processor Factory
 */
public fun interface TestPostProcessorFactory<T> where T: TestPostProcessor {
  /**
   * Decide whether this processor factory is eligible for processing based on the provided [options].
   *
   * This method is consulted by outside classes instead of [create], just in case [create] is expensive; the default
   * implementation runs [create] and checks `null` state. When using this default, processors should cache their
   * constructed state into a singleton.
   *
   * @param options Test post-processing options to evaluate.
   */
  public fun eligible(options: TestPostProcessingOptions): Boolean = create(options) != null

  /**
   * Create an instance of the [TestPostProcessor] managed by this factory, if the provided [options] explain a test
   * context where this post-processor is relevant.
   *
   * @param options Test post-processing options.
   * @return Test post-processor, or `null` to opt-out of processing.
   */
  public fun create(options: TestPostProcessingOptions): TestPostProcessor?
}

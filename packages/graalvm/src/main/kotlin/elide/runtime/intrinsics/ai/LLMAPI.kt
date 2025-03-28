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
package elide.runtime.intrinsics.ai

import org.graalvm.polyglot.Value
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.localai.Parameters
import elide.runtime.localai.Model
import elide.vm.annotations.Polyglot

/**
 * ## LLM API
 */
public interface LLMAPI : ReadOnlyProxyObject {
  /**
   * Return the active version of Elide's LLM API.
   *
   * @return Version of the active LLM API.
   */
  @Polyglot public fun version(): String

  /**
   * Generate or build model parameters for use with LLMs; provided values override defaults.
   *
   * @param options Options to use for generation.
   * @return Generated model parameters.
   */
  @Polyglot public fun params(options: Value?): Parameters

  /**
   * Generate a model spec for an on-disk model.
   *
   * @param options Model options.
   * @return Generated model parameters.
   */
  @Polyglot public fun localModel(options: Value): Model

  /**
   * Generate a model spec for a remote model based on HuggingFace.
   *
   * @param options Model options.
   * @return Generated model parameters.
   */
  @Polyglot public fun huggingface(options: Value): Model
}

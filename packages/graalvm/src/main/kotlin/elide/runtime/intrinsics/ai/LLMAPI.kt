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

import com.google.common.util.concurrent.ListenableFuture
import org.graalvm.polyglot.Value
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.localai.InferenceResults
import elide.runtime.localai.Parameters
import elide.runtime.localai.Model
import elide.vm.annotations.Polyglot

/**
 * # LLM API
 *
 * Describes the surface provided for LLM dispatch via Elide's built-in AI APIs; multiple implementations are provided
 * for dispatching local vs. remote instances of LLMs.
 *
 * ## Usage
 *
 * In all cases, the API follows a familiar pattern:
 *
 * - [Parameters] are prepared by the user (using a set of sensible defaults as a baseline)
 * - A [Model] is specified by the user, via local or remote means
 * - The model and parameters are combined with primary input (a prompt), and sent to the LLM
 * - The model performs inference synchronously, and returns the result to the caller, or
 * - The model performs inference asynchronously, and chunks are streamed to the caller, ultimately resulting in a
 *   resolved Future (Promise)
 *
 * ## Local vs. Remote
 *
 * Local LLMs can be dispatched by providing a `llama.cpp`-compatible GGUF model path. This circumvents the model
 * download step and instead dispatches directly through `llama.cpp` to the model on disk.
 *
 * For remote models, Elide supports referencing via HuggingFace. The developer specifies the HuggingFace coordinates of
 * the model, and Elide will download and cache the model locally for use. In effect, this approach applies download
 * logic to the existing local model dispatch mechanism.
 *
 * ## Prompt Inputs
 *
 * Elide is capable of handling several types of prompt inputs; each is specified as part of the [PromptInput] sealed
 * hierarchy of classes. Prompts are typically simple strings, and can take the form:
 *
 * - Functions, including async functions
 * - Objects which can be converted to strings
 * - Lists of strings, which are joined together
 *
 * @see LLMEngine LLM engine dispatch API
 * @see Parameters `Parameters` for LLM usage
 * @see Model Specifying a `Model`
 * @see PromptInput Specifying a `PromptInput`
 * @see InferenceOperation Async inference operations
 * @see InferenceResults Gathering inference results
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

  /**
   * ### Infer (Host/Synchronous)
   *
   * Perform synchronous inference with the provided inputs.
   *
   * In this mode, the model is dispatched directly, and a return value is provided only when inference has fully
   * completed. As a result, calls to this method can be extremely slow; [infer] should be preferred in nearly all
   * cases, with this method mostly intended for testing and background dispatch.
   *
   * This variant is intended for implementation and dispatch from host code.
   *
   * @param params Parameters to use for model dispatch.
   * @param model Model to perform inference with.
   * @param input Prompt input for this inference call.
   * @return The result of the inference operation.
   */
  @Polyglot public fun inferSync(params: Parameters, model: Model, input: PromptInput): String

  /**
   * ### Infer (Guest/Synchronous)
   *
   * Perform synchronous inference with the provided inputs.
   *
   * In this mode, the model is dispatched directly, and a return value is provided only when inference has fully
   * completed. As a result, calls to this method can be extremely slow; [infer] should be preferred in nearly all
   * cases, with this method mostly intended for testing and background dispatch.
   *
   * This variant is intended for dispatch from guest code.
   *
   * @param args Arguments to use for model dispatch.
   * @return The result of the inference operation.
   */
  @Polyglot public fun inferSync(args: Array<Value>): String

  /**
   * ### Infer (Host/Asynchronous)
   *
   * Perform asynchronous + streamed inference with the provided inputs.
   *
   * In this mode, a background job is queued to supervise an inference operation, which is farmed out to a pool of
   * background workers. As inference output is produced, chunks are yielded to the caller through a suspending flow of
   * strings.
   *
   * This variant is intended for implementation and dispatch from host code.
   *
   * @param params Parameters to use for model dispatch.
   * @param model Model to perform inference with.
   * @param input Prompt input for this inference call.
   * @return A handle to the inference operation.
   */
  @Polyglot public fun infer(params: Parameters, model: Model, input: PromptInput): JsPromise<String>

  /**
   * ### Infer (Guest/Asynchronous)
   *
   * Perform asynchronous + streamed inference with the provided inputs.
   *
   * In this mode, a background job is queued to supervise an inference operation, which is farmed out to a pool of
   * background workers. As inference output is produced, chunks are yielded to the caller through a suspending flow of
   * strings.
   *
   * This variant is intended for dispatch from guest code.
   *
   * @param args Arguments to use for model dispatch.
   * @return A handle to the inference operation.
   */
  @Polyglot public fun infer(args: Array<Value>): JsPromise<String>
}

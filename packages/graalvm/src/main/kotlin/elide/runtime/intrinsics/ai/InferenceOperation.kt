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

import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.node.events.EventTarget
import elide.runtime.localai.InferenceResults
import elide.vm.annotations.Polyglot

// Object methods and properties.
private const val INFERENCE_OP_PROMISE_PROP = "promise"

// Full set of properties and methods on this object.
private val inferenceOperationProps = arrayOf(
  INFERENCE_OP_PROMISE_PROP,
)

/**
 * ## Inference Operation
 *
 * Describes a (potentially in-flight) inference operation, performed asynchronously by the AI engine. Async inference
 * is chunked and streamed under the hood; this object exists as an extended promise-like object that can be used to
 * await chunks of results, or the completion of the entire inference operation.
 *
 * See [InferenceResults] and [LLMAPI] for more information about the results of an inference operation.
 *
 * @see InferenceResults main inference results
 * @see LLMAPI module surface (LLM API)
 */
public interface InferenceOperation : ReadOnlyProxyObject, EventTarget {
  /**
   * Promise for the inference operation; this future value completes when the inference operation is fully resolved,
   * and all output chunks are available.
   */
  @get:Polyglot public val promise: JsPromise<InferenceResults>

  override fun getMemberKeys(): Array<String> = inferenceOperationProps

  override fun getMember(key: String?): Any? = when (key) {
    INFERENCE_OP_PROMISE_PROP -> promise
    else -> null
  }
}

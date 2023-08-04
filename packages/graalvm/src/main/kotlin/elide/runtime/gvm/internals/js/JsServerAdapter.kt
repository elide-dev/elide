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

package elide.runtime.gvm.internals.js

import io.micronaut.http.HttpResponse
import org.reactivestreams.Publisher
import java.io.InputStream
import java.util.concurrent.CompletableFuture
import elide.runtime.gvm.internals.AbstractVMAdapter

/** Implements an adapter to [JsRuntime] for server-side use. */
internal class JsServerAdapter : AbstractVMAdapter<
  HttpResponse<Publisher<InputStream>>,
  JsInvocationBindings,
  JsExecutableScript,
  JsMicronautRequestExecutionInputs,
>() {
  /** JavaScript server execution wrapper (for an in-flight VM execution). */
  inner class JsServerExecution (
    op: CompletableFuture<HttpResponse<Publisher<InputStream>>>,
  ) : VMStreamingExecution(op) {
    override val done: Boolean get() = op.isDone

    override fun execute(): PromiseLike<HttpResponse<Publisher<InputStream>>> {
      TODO("Not yet implemented")
    }
  }

  /** @inheritDoc */
  override fun bind(
    script: JsExecutableScript,
    bindings: JsInvocationBindings,
    inputs: JsMicronautRequestExecutionInputs
  ): AbstractVMExecution<HttpResponse<Publisher<InputStream>>> {
    TODO("Not yet implemented")
  }
}

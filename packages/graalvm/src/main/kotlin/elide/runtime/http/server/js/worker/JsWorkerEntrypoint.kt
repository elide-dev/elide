/*
 *  Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 *  Licensed under the MIT license (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *    https://opensource.org/license/mit/
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under the License.
 */

package elide.runtime.http.server.js.worker

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import elide.runtime.gvm.internals.js.JsExecutableScript
import elide.runtime.gvm.internals.js.JsInvocationBindings
import elide.runtime.http.server.js.worker.JsWorkerEntrypoint.Companion.resolve
import elide.runtime.intrinsics.js.FetchRequest
import elide.runtime.intrinsics.js.FetchResponse
import elide.runtime.intrinsics.js.JsPromise

/**
 * A type-safe wrapper over a [guestFunction] that accepts a [FetchRequest] and a [JsWorkerEnv] object and returns
 * a [FetchResponse] or a [JsPromise] that resolves to a [FetchResponse].
 *
 * Use [resolve] to obtain an instance from guest source code.
 */
@JvmInline public value class JsWorkerEntrypoint(public val guestFunction: Value) {
  private fun unwrapResponse(value: Value?): FetchResponse {
    require(value != null && !value.isNull) { "Response cannot be null" }

    if (value.isHostObject) runCatching { value.asHostObject<FetchResponse>() }
      .onSuccess { return it }

    error("Unsupported response: fetch function must return a 'Response' object, received '$value'")
  }

  public operator fun invoke(request: FetchRequest, env: JsWorkerEnv): JsPromise<FetchResponse> {
    val result = guestFunction.execute(request, env)

    return JsPromise.wrapOrNull(
      value = result,
      unwrapFulfilled = { unwrapResponse(it) },
    ) ?: JsPromise.resolved(unwrapResponse(result))
  }

  public companion object {
    private val supportedTypes = arrayOf(
      JsInvocationBindings.JsEntrypointType.ASYNC_FUNCTION,
      JsInvocationBindings.JsEntrypointType.FUNCTION,
      JsInvocationBindings.JsEntrypointType.SERVER,
      JsInvocationBindings.JsEntrypointType.COMPOUND,
    )

    /** Resolve an entrypoint from the given [source], evaluated using [context]. ƒ*/
    @JvmStatic public fun resolve(source: Source, context: Context): JsWorkerEntrypoint {
      val result = context.eval(source)
      val entry = JsInvocationBindings.entrypoint(
        script = JsExecutableScript.of(source),
        value = result,
      )

      // resolve the server entrypoint
      val boundEntry = when (val binding = entry.bindings) {
        is JsInvocationBindings.JsFunction,
        is JsInvocationBindings.JsServer -> requireNotNull(
          binding.mapped.values.find { it.info.type in supportedTypes },
        )

        else -> error("Unsupported binding: Please export a fetch handler from the entrypoint")
      }

      return JsWorkerEntrypoint(boundEntry.value)
    }
  }
}


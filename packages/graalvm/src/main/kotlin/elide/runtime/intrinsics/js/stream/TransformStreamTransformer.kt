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
package elide.runtime.intrinsics.js.stream

import org.graalvm.polyglot.Value
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.ReadableStream

public interface TransformStreamTransformer {
  public val readableType: ReadableStream.Type get() = ReadableStream.Type.Default
  public val writableType: Any get() = Unit

  public fun start(controller: TransformStreamDefaultController): JsPromise<Unit> = JsPromise.resolved(Unit)
  public fun flush(controller: TransformStreamDefaultController): JsPromise<Unit> = JsPromise.resolved(Unit)

  public fun transform(chunk: Value, controller: TransformStreamDefaultController): JsPromise<Unit> {
    controller.enqueue(chunk)
    return JsPromise.resolved(Unit)
  }

  public fun cancel(reason: Any? = null): JsPromise<Unit> = JsPromise.resolved(Unit)
}

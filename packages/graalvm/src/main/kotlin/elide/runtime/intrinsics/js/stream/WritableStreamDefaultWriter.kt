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

import elide.runtime.intrinsics.js.JsPromise
import elide.vm.annotations.Polyglot

public interface WritableStreamDefaultWriter {
  @get:Polyglot public val closed: JsPromise<Unit>
  @get:Polyglot public val ready: JsPromise<Unit>
  @get:Polyglot public val desiredSize: Double?

  @Polyglot public fun write(chunk: Any? = null): JsPromise<Unit>
  @Polyglot public fun releaseLock()
  @Polyglot public fun abort(reason: Any? = null): JsPromise<Unit>
  @Polyglot public fun close()
}

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

import elide.runtime.intrinsics.js.CompletableJsPromise
import elide.runtime.intrinsics.js.JsPromise
import elide.vm.annotations.Polyglot

/**
 * Interface for stream reader implementations, providing the shared methods specified in the
 * [WHATWG standard](https://streams.spec.whatwg.org/#generic-reader-mixin).
 */
public sealed interface ReadableStreamReader {
  /** A promised that completes when the reader is closed, and rejects when the reader errors. */
  @get:Polyglot public val closed: JsPromise<Unit>

  /**
   * Release this reader's lock on the stream, allowing a new reader to be acquired and invalidating this instance.
   * Pending reads will still complete normally.
   */
  @Polyglot public fun releaseLock()

  /** Cancel the stream for this reader. */
  @Polyglot public fun cancel()
}

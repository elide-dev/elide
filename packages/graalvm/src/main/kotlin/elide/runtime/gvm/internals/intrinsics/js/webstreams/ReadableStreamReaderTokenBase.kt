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
package elide.runtime.gvm.internals.intrinsics.js.webstreams

/**
 * An internal contract for stream reader implementations, allowing streams to call common operations without mutating
 * the state of the readers directly.
 *
 * Streams may call [error] to reject the reader's `closed` promise with an optional reason, or [close] to resolve it
 * normally. Methods in this contract should not call back to the stream, to avoid accidental recursive loops.
 */
internal abstract class ReadableStreamReaderTokenBase {
  /**
   * Close this reader with an error, optionally using a given [reason]. A [ReadableStreamBase] may call this method
   * to finalize the reader after releasing the lock.
   */
  internal abstract fun error(reason: Any?)

  /**
   * Close this reader normally, preventing further reads from being requested. A [ReadableStreamBase] may call this
   * method to finalize the reader after releasing the lock.
   */
  internal abstract fun close()
}

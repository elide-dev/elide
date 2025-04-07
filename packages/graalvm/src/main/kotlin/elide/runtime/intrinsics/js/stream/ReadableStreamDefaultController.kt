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
import elide.vm.annotations.Polyglot

/** A controller used by streams with a 'default' source, allowing arbitrary chunks to be enqueued. */
public interface ReadableStreamDefaultController : ReadableStreamController {
  /**
   * Close the controller and the associated stream. If there are undelivered chunks, the stream will not be closed
   * until they are claimed.
   */
  @Polyglot public fun close()

  /**
   * Shut down the controller and associated stream with the given error [reason]. Unlike [close], this method does
   * not wait for undelivered elements to be claimed, all unread data will be lost.
   */
  @Polyglot public fun error(reason: Any? = null)

  /**
   * Enqueue a new chunk, making it available immediately for readers. If any pending read requests are found, the
   * chunk will be delivered directly without using the queue.
   */
  @Polyglot public fun enqueue(chunk: Value? = null)
}

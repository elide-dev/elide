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
import org.graalvm.polyglot.proxy.ProxyExecutable
import elide.runtime.interop.ReadOnlyProxyObject
import elide.vm.annotations.Polyglot

/** A controller used by streams with a 'default' source, allowing arbitrary chunks to be enqueued. */
public interface ReadableStreamDefaultController : ReadableStreamController, ReadOnlyProxyObject {
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

  override fun getMemberKeys(): Array<String> = MEMBERS
  override fun getMember(key: String?): Any? = when (key) {
    MEMBER_DESIRED_SIZE -> desiredSize
    MEMBER_CLOSE -> ProxyExecutable { close() }
    MEMBER_ERROR -> ProxyExecutable { error(it.firstOrNull()) }
    MEMBER_ENQUEUE -> ProxyExecutable { enqueue(it.firstOrNull()) }
    else -> null
  }

  private companion object {
    private const val MEMBER_CLOSE = "closed"
    private const val MEMBER_ERROR = "error"
    private const val MEMBER_ENQUEUE = "enqueue"
    private const val MEMBER_DESIRED_SIZE = "desiredSize"
    private val MEMBERS = arrayOf(MEMBER_CLOSE, MEMBER_ERROR, MEMBER_ENQUEUE, MEMBER_DESIRED_SIZE)
  }
}

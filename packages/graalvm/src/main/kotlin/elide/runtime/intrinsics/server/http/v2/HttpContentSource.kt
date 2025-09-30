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
package elide.runtime.intrinsics.server.http.v2

import io.netty.handler.codec.http.HttpContent

/**
 * Represents an arbitrary source of [HttpContent] that can deliver chunks asynchronously.
 *
 * Sources have at most one [Consumer] attached at the time, which receives the data as it becomes available. Consumers
 * are responsible for calling [Handle.pull] to request new content, thus taking control of backpressure.
 */
public interface HttpContentSource : AutoCloseable {
  /**
   * A handle for a [Consumer] attached to an [HttpContentSource]. This handle can be used to request new data from the
   * source and to release the consumer.
   */
  public interface Handle {
    /**
     * Request a new content chunk to be sent to the active [Consumer] from the source. This method has no effect if a
     * chunk is already available or currently being processed by the consumer.
     */
    public fun pull()

    /**
     * Release the consumer attached to this handle, allowing new [HttpContentSource.sink] calls. The consumer's
     * [released][Consumer.released] method will be called as a result.
     */
    public fun release()
  }

  /**
   * A consumer of [HttpContent] chunks that can be attached to an [HttpContentSource] to handle data using callbacks.
   *
   * Consumers are attached via [HttpContentSource.sink], and must manually request data by calling [Handle.pull]; once
   * data becomes available after pulling, the source will call the [consume] method to deliver the content.
   *
   * Blocking in consumer callbacks is generally discouraged, unless caused by backpressure that cannot be handled
   * otherwise. Since requesting a specific data length is not possible, sources accept that some calls to [consume]
   * must necessarily block until the content is consumed fully downstream.
   *
   * Once no more data is available, the consumer is released, or the source is manually closed, the consumer's
   * [released][Consumer.released] method will be called.
   */
  public interface Consumer {
    /**
     * Called by the [HttpContentSource] when a new consumer is attached to the source. The [handle] can be used to
     * place an initial request for data or detach the consumer.
     *
     * The default implementation of this method requests data from the source immediately.
     *
     * @param handle Handle that can be used to request new data or detach the consumer.
     */
    public fun attached(handle: Handle) {
      handle.pull()
    }

    /**
     * Called by the [HttpContentSource] when a new chunk of data is available. The [handle] can be used to request new
     * data from the source or release this consumer at will.
     *
     * This method is only called after the consumer uses [Handle.pull] to request new data.
     *
     * Implementations should avoid blocking in this method if possible, but this is not a hard constraint, e.g.:
     * blocking while waiting for input is highly discouraged, whereas blocking work on the data is generally ok.
     *
     * @param content Content chunk received from the source.
     * @param handle Handle that can be used to request new data or detach the consumer.
     */
    public fun consume(content: HttpContent, handle: Handle)

    /**
     * Called when the consumer is released from the source. After this method is called, no more data will be
     * received.
     *
     * Sources automatically release consumers when no more data is available or when manually closed.
     */
    public fun released() {
      // noop
    }
  }

  /**
   * Attach a [consumer] to this source, directing all incoming chunks to it.
   *
   * Only one [Consumer] may be attached to the source at the same time. Attached consumers can be [released][release]
   * to allow new [sink] calls.
   *
   * @param consumer Consumer to attach to this source. The consumer's [attached][Consumer.attached] method will be
   * called immediately after.
   */
  public fun sink(consumer: Consumer)

  /** Release the current consumer for this source if one exists. */
  public fun release()

  /**
   * Close this source and discard all remaining data, automatically releasing the attached consumer if present. After
   * closing, [sink] calls will throw an exception.
   */
  public override fun close()
}

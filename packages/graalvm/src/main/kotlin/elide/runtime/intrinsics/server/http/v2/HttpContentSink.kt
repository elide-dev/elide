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
 * Represents an arbitrary sink of [HttpContent] that consumes data asynchronously.
 *
 * Sinks have at most one [Producer] attached at the time, which enqueues new data using [push][Handle.push] on a
 * provided handle. The sink calls [Producer.pull] to request new data, automatically managing backpressure.
 */
public interface HttpContentSink {
  /**
   * A handle for a [Producer] attached to an [HttpContentSink]. This handle can be used to enqueue data to the sink
   * and release the producer.
   */
  public interface Handle {
    /**
     * Enqueue a [content] chunk to the sink. This method should only be called in response to the producer's
     * [pull][Producer.pull] callback to prevent blocking the producer due to backpressure.
     *
     * Implementations must avoid blocking in this method to maintain the backpressure contract guaranteed by the
     * push-pull flow.
     *
     * @param content Content chunk to enqueue.
     */
    public fun push(content: HttpContent)

    /**
     * Release the producer attached to this handle, optionally closing the sink afterward. The producer's
     * [released][Producer.released] method will be called as a result.
     *
     * @param close Whether the sink should be closed after releasing the producer. Defaults to `false`.
     */
    public fun release(close: Boolean = false)
  }

  /**
   * A producer of [HttpContent] chunks that can be attached to an [HttpContentSink] to enqueue data.
   *
   * Producers should only enqueue data by calling [Handle.push] after [pull] is called; in cases where the underlying
   * source is exhausted and no more data will be received, [Handle.release] should be called instead.
   */
  public interface Producer {
    /**
     * Called by the [HttpContentSink] to request new data to be enqueued via [Handle.push]. After this method is
     * called, [Handle.push] is guaranteed not to block.
     *
     * If no more data is available, [HttpContentSink.close] should be called instead.
     *
     * @param handle A handle to enqueue data or release the producer.
     */
    public fun pull(handle: Handle)

    /**
     * Called by the [HttpContentSink] to indicate no more data will be requested. After this method is called, the
     * producer should not make any calls to the sink.
     */
    public fun released() {
      // noop
    }
  }

  /**
   * Attach a [producer] to this sink; the sink may not immediately request new data, depending on the implementation.
   *
   * At most one producer may be active in a sink at a time. Attached consumers can be [released][release] to allow
   * new [source] calls.
   *
   * @param producer Producer to attach to this sink. The producer's [pull][Producer.pull] method may not be called
   * immediately.
   */
  public fun source(producer: Producer)

  /** Release the current producer for this source if one exists. */
  public fun release()

  /**
   * Close this sink, releasing any attached producers. Depending on the implementation, this may result in additional
   * resources being disposed of, among other side effects (e.g., flushing an underlying network channel).
   */
  public fun close()

  public companion object {
    @JvmStatic public fun singleValueProducer(value: HttpContent): Producer = object : Producer {
      override fun pull(handle: Handle) {
        handle.push(value)
        handle.release(close = true)
      }
    }
  }
}

public fun HttpContentSink.source(value: HttpContent) {
  source(HttpContentSink.singleValueProducer(value))
}

public fun HttpContentSink.source(onPull: (HttpContentSink.Handle) -> Unit) {
  source(
    object : HttpContentSink.Producer {
      override fun pull(handle: HttpContentSink.Handle) = onPull(handle)
    },
  )
}

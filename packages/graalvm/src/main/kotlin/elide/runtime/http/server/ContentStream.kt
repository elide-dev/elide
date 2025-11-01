/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

package elide.runtime.http.server

import io.netty.buffer.ByteBuf

public open class StreamBusyException : IllegalStateException("Stream is already in use")

public open class StreamClosedException(override val cause: Throwable?) :
  IllegalStateException("Stream is already closed", cause)

public interface ContentStream {
  /**
   * Close the stream with an optional [error]. This will detach any active consumers and producers, and mark the
   * stream as failed, surfacing the error to any consumer or producer that attempts to use it in the future.
   *
   * This method can be used to safely interrupt the stream externally from any thread.
   */
  public fun close(error: Throwable? = null)
}

/**
 * Represents the content of an incoming HTTP request that can be consumed asynchronously.
 *
 * Use [consume] to attach a listener and control backpressure through a [Reader]. Consumers manually pull data from
 * the stream and can request to be detached from it early or discard the rest of the content.
 *
 * The request body is closed automatically by the server components once the call handling is complete, after which
 * attempting to [consume] it will throw an exception.
 */
public interface ReadableContentStream : ContentStream {
  /** A reader used by an [ContentStreamConsumer] to control the request body it is attached to. */
  public interface Reader {
    /**
     * Request more data from the transport. A single [pull] call will result in at most one chunk being delivered.
     * Calling this method after the consumer is detached will have no effect.
     */
    public fun pull()

    /** Detach the consumer from the request body, discarding any remaining data in the stream. */
    public fun release()
  }

  /**
   * Attach a [consumer] to the request body and pull from the stream. Only one consumer may be attached to the body,
   * once it is released, any unconsumed data will be discarded, and new consumers will silently fail to attach.
   *
   * The consumer is automatically detached when the body is closed or the end of the stream is reached.
   * Calling this method after the body is closed will cause the [consumer] to fail with an exception.
   */
  public fun consume(consumer: ContentStreamConsumer)
}

/**
 * Represents the content of an outgoing HTTP response. An [ContentStreamSource] must be attached as a [source] to
 * send data to the client, otherwise the request will have an empty body when sent.
 *
 * Producers are asynchronous and must send data when the engine issues a pull.
 */
public interface WritableContentStream : ContentStream {
  /** A writer used by an [ContentStreamSource] to [write] data or mark the [end] of the stream. */
  public interface Writer {
    /**
     * Write a [content] chunk to the stream.
     *
     * This method will fail silently and release the [content] if it is called after [end]; note that concurrent calls
     * to [end] may not prevent the content from being written, but there is a guarantee that the EOS marker will never
     * be enqueued before the [content].
     */
    public fun write(content: ByteBuf)

    /**
     * Mark the end of the response content, detaching the producer and preventing any more data from being requested.
     * After this method is called, [source] will reject new producers without attaching.
     *
     * If an [error] is provided, it will cause the stream to close, and it will be surfaced to any active or future
     * consumers on the next pull.
     *
     * It is safe to call this method multiple times, only the first call will take effect.
     */
    public fun end(error: Throwable? = null)
  }

  /**
   * Set the content [producer] for the response body without sending it. Once the call's [send][HttpCall.send] method
   * is invoked, the engine will begin requesting data from the [producer].
   *
   * Only one producer may be attached to the response. After the producer is released, the body will be marked as
   * finished. The producer is automatically released when it signals the end of the stream or when the response is
   * closed.
   */
  public fun source(producer: ContentStreamSource)
}

/**
 * A content producer for an [WritableContentStream] that pushes data asynchronously as a response to [onPull] calls. The
 * response body calls [onClose] on the producer when it is no longer needed (e.g. after indicating end of stream) or
 * when an error occurs.
 */
public interface ContentStreamSource {
  /**
   * Called when the producer is attached to a [WritableContentStream].
   *
   * This method is always called, even when the producer cannot be attached to the body; in cases where the producer
   * cannot be attached (e.g. stream failed or closed before attaching), the [writer] will be inert, and [onClose]
   * will be called immediately after.
   *
   * If this method throws an exception, the producer will be detached immediately and [onClose] will be called with
   * the failure cause.
   */
  public fun onAttached(writer: WritableContentStream.Writer)

  /**
   * Called when the transport is ready to send response data to the client.
   *
   * Implementations must either push a new content chunk with [push][WritableContentStream.Writer.write] or end the stream
   * with [end][WritableContentStream.Writer.end], otherwise the response will stall.
   *
   * Pushing multiple content chunks for a single pull is allowed, as they will be buffered by the underlying Netty
   * channel automatically.
   *
   * If this method throws an exception, the producer will be detached and [onClose] will be called with the failure
   * cause.
   */
  public fun onPull()

  /**
   * Called when the producer is detached from the response body, due to an end-of-stream call or the body itself
   * being closed. The [failure] cause is used to indicate an error that caused the producer to be detached.
   *
   * This method should never throw, otherwise the failure will be logged and discarded by the stream.
   */
  public fun onClose(failure: Throwable? = null)
}

/**
 * A consumer for an [ReadableContentStream] that receives content asynchronously and requests more data by pulling.
 *
 * After attaching, the consumer can request to be released to stop receiving data, or discard the rest of the request
 * content, so that future consumers will not observe any data at all.
 */
public interface ContentStreamConsumer {
  /**
   * Called when the consumer is attached to a [ReadableContentStream].
   *
   * This method is always called, even when the consumer cannot be attached to the body; in cases where the consumer
   * cannot be attached (e.g. stream failed or closed before attaching), the [reader] will be inert, and [onClose] will
   * be called immediately after to indicate the error.
   *
   * If this method throws an exception, the consumer will be detached immediately and [onClose] will be called with
   * the failure cause.
   */
  public fun onAttached(reader: ReadableContentStream.Reader)

  /**
   * Called when a [content] element is received from the underlying transport. The [content] is owned by the request
   * body and will be released automatically after this method returns.
   *
   * Content is only received after [ReadableContentStream.Reader.pull] is called; each pull will result in *at most* one
   * element being delivered.
   *
   * If this method throws an exception, the consumer will be detached and [onClose] will be called with the failure
   * cause.
   */
  public fun onRead(content: ByteBuf)

  /**
   * Called when the consumer is detached from the request body, due to an end-of-stream call or the body itself
   * being closed. The [failure] cause is used to indicate an error that caused the consumer to be detached.
   *
   * This method should never throw, otherwise the failure will be logged and discarded by the stream.
   */
  public fun onClose(failure: Throwable? = null)
}

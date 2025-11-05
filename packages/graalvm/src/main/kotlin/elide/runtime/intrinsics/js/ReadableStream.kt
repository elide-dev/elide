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
package elide.runtime.intrinsics.js

import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyInstantiable
import org.graalvm.polyglot.proxy.ProxyIterable
import org.graalvm.polyglot.proxy.ProxyObject
import java.io.InputStream
import java.io.Reader
import java.nio.ByteBuffer
import elide.annotations.API
import elide.runtime.exec.GuestExecution
import elide.runtime.gvm.internals.intrinsics.js.webstreams.*
import elide.runtime.intrinsics.js.stream.*
import elide.vm.annotations.Polyglot

/**
 * # Web Streams: Readable Stream.
 *
 * Models the `ReadableStream` interface, defined by the [WhatWG Streams Standard](https://streams.spec.whatwg.org/).
 * Readable streams implement streams of arbitrary data which can be consumed by an interested developer, and form part
 * of the wider Web Streams API.
 */
@API @HostAccess.Implementable public interface ReadableStream : Stream, ProxyIterable, ProxyObject {
  /**
   * Encapsulates the result of a read; [done] indicates whether the [value] is the final value that will be available
   * from the source.
   */
  public data class ReadResult(
    @Polyglot val value: Value?,
    @Polyglot val done: Boolean,
  )

  /** Defines the type of streams implemented by the standard. */
  public enum class Type {
    /**
     * The default stream type, which uses a `ReadableStreamDefaultController` and `ReadableStreamDefaultReader`
     * instances.
     */
    Default,

    /**
     * A stream optimized for byte operations, using `ReadableStreamBYOBController and `ReadableStreamBYOBReader`
     * types, which allow consumers to provide their own buffers for sources to write the data in.
     */
    BYOB;

    public companion object {
      /**
       * Select a stream/source [Type] from the given string [value]. This is intended to be used on values returned
       * by [ReadableStreamSource.type]. The [Default] type will be used in every case except when [value] is the
       * string "bytes"s.
       */
      public fun fromGuestValue(value: String): Type {
        return if (value == "bytes") BYOB
        else Default
      }
    }
  }

  /**
   * When used as part of the reader options in [ReadableStream.getReader], the returned reader instance will match
   * this setting. Note that using [BYOB] mode on a default stream will result in an error (but [Default] is always
   * allowed).
   */
  public enum class ReaderMode {
    /** Requests a default reader, which moves elements from the stream as they are. Available for all stream types. */
    Default,

    /**
     * Requests a BYOB reader, which allows passing a custom buffer for the data to be written into. Available only for
     * BYOB streams.
     */
    BYOB,
  }

  /** Whether the stream is currently [locked](https://streams.spec.whatwg.org/#lock) to a [reader]. */
  @get:Polyglot public val locked: Boolean

  /** Cancel the stream with an optional reason, releasing the underlying source. */
  @Polyglot public fun cancel(reason: Any? = null): JsPromise<Unit>

  /**
   * Obtain a new [ReadableStreamReader] instance and lock the stream to it, preventing any new readers from being
   * acquired until it is released.
   *
   * If the stream is already [locked], an error will be thrown.
   */
  @Polyglot public fun getReader(options: Value? = null): ReadableStreamReader

  @Polyglot public fun pipeThrough(transform: TransformStream, options: Value? = null): ReadableStream

  @Polyglot public fun pipeTo(destination: WritableStream, options: Value? = null): JsPromise<Unit>

  @Polyglot public fun tee(): Array<ReadableStream>

  /**
   * ## Readable Stream: Factory.
   *
   * Describes constructors available both in guest and host contexts, which create [ReadableStream] implementation
   * instances. Generally, each implementation has a factory implementation as well, from which instances can be
   * acquired by host-side code, and, where supported, by guest-side code.
   *
   * @param Impl Implementation of [ReadableStream] which is created by this factory.
   */
  public interface Factory<Impl> : ProxyInstantiable where Impl : ReadableStream {
    /**
     * Constructor for a custom [ReadableStream], which implements the provided [source] and [queuingStrategy], if
     * provided.
     *
     * @param source Source for the [ReadableStream].
     * @param queuingStrategy Optional [QueuingStrategy] for the [ReadableStream].
     * @return A new [ReadableStream] instance.
     */
    @Polyglot public fun create(source: ReadableStreamSource, queuingStrategy: QueuingStrategy?): ReadableStream

    /**
     * Constructor for a custom [ReadableStream], which implements the provided [source].
     *
     * @param source Source for the [ReadableStream].
     * @return A new [ReadableStream] instance.
     */
    @Polyglot public fun create(source: ReadableStreamSource): ReadableStream = create(source, null)

    /**
     * Create an empty [ReadableStream] instance of shape [Impl].
     *
     * @return New [ReadableStream] instance.
     */
    public fun empty(): Impl

    /**
     * Use the [Impl] factory to create a new [ReadableStream] instance which wraps the given [InputStream].
     *
     * @param input [InputStream] to wrap.
     * @return New [ReadableStream] instance wrapping the given [InputStream].
     */
    public fun wrap(input: InputStream): Impl

    /**
     * Use the [Impl] factory to create a new [ReadableStream] instance which wraps the given [Reader].
     *
     * @param reader [Reader] to wrap.
     * @return New [ReadableStream] instance wrapping the given [Reader].
     */
    public fun wrap(reader: Reader): Impl

    /**
     * Use the [Impl] factory to create a new [ReadableStream] instance which wraps the given [ByteArray].
     *
     * @param bytes [ByteArray] to wrap.
     * @return New [ReadableStream] instance wrapping the given [ByteArray].
     */
    public fun wrap(bytes: ByteArray): Impl

    /**
     * Use the [Impl] factory to create a new [ReadableStream] instance which wraps the given [ByteBuffer].
     *
     * @param buffer [ByteBuffer] to wrap.
     * @return New [ReadableStream] instance wrapping the given [ByteBuffer].
     */
    public fun wrap(buffer: ByteBuffer): Impl

    /**
     * Use the [Impl] factory to create a new [ReadableStream] instance which wraps the given [hostIterable].
     *
     * @param hostIterable A host iterable used to emit values.
     * @return New [ReadableStream] instance wrapping the given [ByteBuffer].
     */
    public fun from(hostIterable: Iterable<Any>): ReadableStream
  }

  /** Default constructors/factory methods for [ReadableStream] instances. */
  public companion object DefaultFactory : ProxyInstantiable, Factory<ReadableStream> {
    private val streamExecutor = GuestExecution.workStealing()

    @Polyglot public fun from(asyncIterable: Value): ReadableStream {
      return create(ReadableStreamAsyncIteratorSource(asyncIterable.iterator))
    }

    override fun from(hostIterable: Iterable<Any>): ReadableStream {
      return create(ReadableStreamHostIteratorSource(hostIterable))
    }

    override fun create(source: ReadableStreamSource, queuingStrategy: QueuingStrategy?): ReadableStream {
      return when (source.type) {
        Type.Default -> ReadableDefaultStream(
          source,
          queuingStrategy ?: QueuingStrategy.DefaultReadStrategy,
          streamExecutor,
        )

        Type.BYOB -> ReadableByteStream(
          source,
          queuingStrategy ?: QueuingStrategy.DefaultReadStrategy,
          streamExecutor,
        )
      }
    }

    override fun newInstance(vararg arguments: Value?): Any {
      val source = arguments.getOrNull(0)?.let(::GuestReadableStreamSource) ?: ReadableStreamSource.Empty
      val strategy = arguments.getOrNull(1)?.let(GuestQueuingStrategy::from) ?: QueuingStrategy.DefaultReadStrategy

      return create(source, strategy)
    }

    override fun empty(): ReadableStream = create(ReadableStreamEmptySource)
    override fun wrap(input: InputStream): ReadableStream = create(
      ReadableStreamInputStreamSource(input),
      queuingStrategy = ByteLengthQueuingStrategy(DEFAULT_BUFFER_SIZE.toDouble()),
    )

    override fun wrap(reader: Reader): ReadableStream = create(ReadableStreamReaderSource(reader))
    override fun wrap(bytes: ByteArray): ReadableStream = wrap(ByteBuffer.wrap(bytes))
    override fun wrap(buffer: ByteBuffer): ReadableStream = create(
      source = ReadableStreamBufferSource(buffer),
      queuingStrategy = ByteLengthQueuingStrategy(buffer.remaining().toDouble()),
    )
  }
}

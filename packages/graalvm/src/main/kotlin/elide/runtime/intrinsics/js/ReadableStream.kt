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
import org.graalvm.polyglot.proxy.ProxyInstantiable
import java.io.InputStream
import java.io.Reader
import java.nio.ByteBuffer
import elide.annotations.API
import elide.runtime.gvm.internals.intrinsics.js.webstreams.WebStreamsIntrinsic.ReadableStreamImpl
import elide.runtime.intrinsics.js.stream.QueuingStrategy
import elide.runtime.intrinsics.js.stream.ReadableStreamSource
import elide.vm.annotations.Polyglot

/**
 * # Web Streams: Readable Stream.
 *
 * Models the `ReadableStream` interface, defined by the WhatWG Streams Standard (https://streams.spec.whatwg.org/).
 * Readable streams implement streams of arbitrary data which can be consumed by an interested developer, and form part
 * of the wider Web Streams API.
 */
@API @HostAccess.Implementable public interface ReadableStream : Stream {
  /**
   * ## Readable Stream: Factory.
   *
   * Describes constructors available both in guest and host contexts, which create [ReadableStream] implementation
   * instances. Generally, each implementation has a factory implementation as well, from which instances can be
   * acquired by host-side code, and, where supported, by guest-side code.
   *
   * @param Impl Implementation of [ReadableStream] which is created by this factory.
   */
  public interface Factory<Impl> : ProxyInstantiable where Impl: ReadableStream {
    /**
     * Constructor for a custom [ReadableStream], which implements the provided [source] and [queueingStrategy], if
     * provided.
     *
     * @param source Source for the [ReadableStream].
     * @param queueingStrategy Optional [QueuingStrategy] for the [ReadableStream].
     * @return A new [ReadableStream] instance.
     */
    @Polyglot public fun create(source: ReadableStreamSource, queueingStrategy: QueuingStrategy?): ReadableStream

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
  }

  /** Default constructors/factory methods for [ReadableStream] instances. */
  public companion object DefaultFactory : ProxyInstantiable by ReadableStreamImpl.Factory, Factory<ReadableStream> {
    @Polyglot override fun create(source: ReadableStreamSource, queueingStrategy: QueuingStrategy?): ReadableStream =
      ReadableStreamImpl.create(source, queueingStrategy)
    override fun empty(): ReadableStream = ReadableStreamImpl.empty()
    override fun wrap(input: InputStream): ReadableStream = ReadableStreamImpl.wrap(input)
    override fun wrap(reader: Reader): ReadableStream = ReadableStreamImpl.wrap(reader)
    override fun wrap(bytes: ByteArray): ReadableStream = ReadableStreamImpl.wrap(bytes)
    override fun wrap(buffer: ByteBuffer): ReadableStream = ReadableStreamImpl.wrap(buffer)
  }
}

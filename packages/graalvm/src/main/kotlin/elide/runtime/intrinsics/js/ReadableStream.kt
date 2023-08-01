package elide.runtime.intrinsics.js

import java.io.InputStream
import java.io.Reader
import java.nio.ByteBuffer
import elide.runtime.gvm.internals.intrinsics.js.webstreams.ReadableStreamIntrinsic

/**
 * # Web Streams: Readable Stream.
 *
 * Models the `ReadableStream` interface, defined by the WhatWG Streams Standard (https://streams.spec.whatwg.org/).
 * Readable streams implement streams of arbitrary data which can be consumed by an interested developer, and form part
 * of the wider Web Streams API.
 */
public interface ReadableStream {
  /**
   * ## Readable Stream: Factory.
   *
   * Describes constructors available both in guest and host contexts, which create [ReadableStream] implementation
   * instances. Generally, each implementation has a factory implementation as well, from which instances can be
   * acquired by host-side code, and, where supported, by guest-side code.
   *
   * @param Impl Implementation of [ReadableStream] which is created by this factory.
   */
  public interface Factory<Impl> where Impl: ReadableStream {
    /**
     * TBD.
     */
    public fun empty(): Impl

    /**
     * TBD.
     */
    public fun wrap(input: InputStream): Impl

    /**
     * TBD.
     */
    public fun wrap(reader: Reader): Impl

    /**
     * TBD.
     */
    public fun wrap(bytes: ByteArray): Impl

    /**
     * TBD.
     */
    public fun wrap(buffer: ByteBuffer): Impl
  }

  public companion object DefaultFactory : Factory<ReadableStream> {
    /**
     * TBD.
     */
    override fun empty(): ReadableStream = ReadableStreamIntrinsic.empty()

    /**
     * TBD.
     */
    override fun wrap(input: InputStream): ReadableStream = ReadableStreamIntrinsic.wrap(input)

    /**
     * TBD.
     */
    override fun wrap(reader: Reader): ReadableStream = ReadableStreamIntrinsic.wrap(reader)

    /**
     * TBD.
     */
    override fun wrap(bytes: ByteArray): ReadableStream = ReadableStreamIntrinsic.wrap(bytes)

    /**
     * TBD.
     */
    override fun wrap(buffer: ByteBuffer): ReadableStream = ReadableStreamIntrinsic.wrap(buffer)
  }
}

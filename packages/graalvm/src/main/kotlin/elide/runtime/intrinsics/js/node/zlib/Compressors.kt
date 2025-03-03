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
package elide.runtime.intrinsics.js.node.zlib

import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Value
import java.io.Closeable
import elide.annotations.API
import elide.runtime.intrinsics.js.node.stream.Readable
import elide.runtime.intrinsics.js.node.stream.Transform
import elide.runtime.intrinsics.js.node.stream.Writable
import elide.vm.annotations.Polyglot

/**
 * ## Close Stream Callback
 *
 * Describes a callback type for compression stream close methods.
 */
@FunctionalInterface
@HostAccess.Implementable
@API public fun interface CloseStreamCallback {
  /**
   * Invoke the callback.
   */
  public operator fun invoke()

  public companion object {
    /**
     * Construct a [CloseStreamCallback] from a [Value].
     *
     * @param cbk Value to use as a callback.
     * @return [CloseStreamCallback] wrapping the provided [Value].
     */
    @JvmStatic public fun from(cbk: Value?): CloseStreamCallback = CloseStreamCallback {
      cbk?.executeVoid()
    }
  }
}

/**
 * ## Flush Stream Callback
 *
 * Describes a callback type for compression stream flush methods.
 */
@FunctionalInterface
@HostAccess.Implementable
@API public fun interface FlushStreamCallback {
  /**
   * Invoke the callback.
   */
  public operator fun invoke()

  public companion object {
    /**
     * Construct a [FlushStreamCallback] from a [Value].
     *
     * @param cbk Value to use as a callback.
     * @return [FlushStreamCallback] wrapping the provided [Value].
     */
    @JvmStatic public fun from(cbk: Value?): FlushStreamCallback = FlushStreamCallback {
      cbk?.executeVoid()
    }
  }
}

/**
 * ## Compression API
 *
 * Base interface for all compression streams.
 */
@API public interface CompressorAPI : Closeable, AutoCloseable {
  /**
   * Number of bytes written through this compression stream so far.
   *
   * From the Node.js documentation:
   *
   * > The zlib.bytesWritten property specifies the number of bytes written to the engine, before the bytes are
   * > processed (compressed or decompressed, as appropriate for the derived class).
   */
  @get:Polyglot public val bytesWritten: Int

  /**
   * Regular close interface, as provided by Java's [AutoCloseable] and [Closeable] interfaces.
   *
   * Overridden here so that it can be made [Polyglot]-accessible.
   */
  @Polyglot override fun close()

  /**
   * Closes the underlying compression stream, and dispatches the provided callback.
   *
   * Note: This method cannot synthesize defaults for a no-parameter implementation, because it would collide with the
   * [close] method provided by [AutoCloseable].
   *
   * @param callback Callback to dispatch when the stream is closed.
   */
  @Polyglot public fun close(callback: CloseStreamCallback) {
    close()
    callback()
  }

  /**
   * Closes the underlying compression stream, and dispatches the provided callback.
   *
   * This method variant works with [Value] as the callback type.
   *
   * @param callback Callback to dispatch when the stream is closed.
   */
  @Polyglot public fun close(callback: Value?): Unit = close(CloseStreamCallback.from(callback))

  /**
   * Flush the underlying compression stream, and then invoke the provided callback.
   *
   * @param callback Callback to dispatch when the stream is flushed.
   */
  @Polyglot public fun flush(callback: FlushStreamCallback) {
    flush()
    callback()
  }

  /**
   * Flush the underlying compression stream, and then invoke the provided callback.
   */
  @Polyglot public fun flush()

  /**
   * Flush the underlying compression stream, and then invoke the provided callback.
   *
   * This method variant works with [Value] as the callback type.
   *
   * @param callback Callback to dispatch when the stream is flushed.
   */
  @Polyglot public fun flush(callback: Value?): Unit = flush(FlushStreamCallback.from(callback))
}

/**
 * ## Abstract Compression Actor
 *
 * Describes, in a sealed interface hierarchy, all compression actors (streams for compression or decompression).
 */
@API public sealed interface CompressActor : Transform, CompressorAPI

/**
 * ## Abstract Compression Actor: Compressor
 *
 * Marker sealed interface for a compressor stream.
 */
@API public sealed interface Compressor : CompressActor

/**
 * ## Abstract Compression Actor: Decompressor
 *
 * Marker sealed interface for a decompressor stream.
 */
@API public sealed interface Decompressor : CompressActor

/**
 * ## Node Zlib: `Deflate`
 *
 * Implements a zlib deflation stream which is provided at `zlib.Deflate`. Instances which comply with this interface
 * can be obtained via the `zlib.createDeflate()` method.
 */
@API public interface Deflate : Writable, Compressor

/**
 * ## Node Zlib: `Inflate`
 *
 * Implements a zlib inflation stream which is provided at `zlib.Inflate`. Instances which comply with this interface
 * can be obtained via the `zlib.createInflate()` method.
 */
@API public interface Inflate : Readable, Decompressor

/**
 * ## Node Zlib: `Unzip`
 *
 * Implements a zip inflation stream which is provided at `zlib.Unzip`. Instances which comply with this interface
 * can be obtained via the `zlib.createUnzip()` method.
 */
@API public interface Unzip : Readable, Decompressor

/**
 * ## Node Zlib: `BrotliCompress`
 *
 * Implements a Brotli compression stream which is provided at `zlib.BrotliCompress`. Instances which comply with this
 * interface can be obtained via the `zlib.createBrotliCompress()` method.
 */
@API public interface BrotliCompress : Writable, Compressor

/**
 * ## Node Zlib: `BrotliDecompress`
 *
 * Implements a Brotli decompression stream which is provided at `zlib.BrotliDecompress`. Instances which comply with
 * this interface can be obtained via the `zlib.createBrotliDecompress()` method.
 */
@API public interface BrotliDecompress : Readable, Decompressor

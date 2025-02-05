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
package elide.runtime.intrinsics.js.node

import org.graalvm.polyglot.Value
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import elide.annotations.API
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.js.JsError
import elide.runtime.intrinsics.js.node.zlib.*
import elide.runtime.node.buffer.NodeHostBuffer
import elide.vm.annotations.Polyglot

/**
 * Alias for the buffer types used by the zlib module.
 */
public typealias ZlibBuffer = ByteBuffer

// Converts a buffer into raw data to compress.
@OptIn(DelicateElideApi::class)
private fun bytesForCompression(buffer: Value?): ByteArray = when {
  buffer == null || buffer.isNull -> JsError.error("The 'buffer' argument must not be null.")
  buffer.isString -> buffer.asString().toByteArray(StandardCharsets.UTF_8)
  buffer.isHostObject -> when (val target = buffer.asHostObject<Any>()) {
    is NodeHostBuffer -> target.byteBuffer.array()
    is ByteBuffer -> target.array()
    is ByteArray -> target
    else -> JsError.error("The 'buffer' argument must be a Buffer instance.")
  }
  else -> buffer.`as`(ByteArray::class.java)
}

// Converts a buffer and options into a pair of effective Zlib options and buffer.
private fun zlibOpts(buffer: Value?, options: Value?): Pair<ZlibOptions, ByteArray> = when (options) {
  null -> ImmutableZlibOptions.defaults()
  else -> ImmutableZlibOptions.fromValue(options)
} to bytesForCompression(buffer)

// Converts a buffer and options into a pair of effective Brotli options and buffer.
private fun brotliOpts(buffer: Value?, options: Value?): Pair<BrotliOptions, ByteArray> = when (options) {
  null -> ImmutableBrotliOptions.defaults()
  else -> ImmutableBrotliOptions.fromValue(options)
} to bytesForCompression(buffer)

/**
 * ## Node API: Zlib
 *
 * Implements routines for compressing and decompressing data with various algorithms, including Deflate, Gzip, Zip, and
 * Brotli. This API is available through the `zlib` module.
 */
@API public interface ZlibAPI : NodeAPI {
  /**
   * ## Property: `zlib.constants`
   *
   * Returns an object containing commonly-used constants exposed by the underlying zlib implementation.
   */
  @get:Polyglot public val constants: NodeZlibConstants

  /**
   * ## Method: `zlib.crc32(data[, value])`
   *
   * Calculates the CRC32 checksum of the given data.
   *
   * @param data The data to calculate the checksum for. This can be a string, a buffer, a typed array, or any other
   *   array-like raw data.
   * @param value An optional initial value for the checksum. This can be used to calculate the checksum of a stream of
   *   data in multiple parts.
   */
  @Polyglot public fun crc32(data: ByteArray, value: ULong = 0u): Long

  /**
   * ## Method: `zlib.crc32(data[, value])`
   *
   * Calculates the CRC32 checksum of the given data; this method variant operates on foreign [Value] instances.
   *
   * @param data The data to calculate the checksum for. This can be a string, a buffer, a typed array, or any other
   *   array-like raw data.
   * @param value An optional initial value for the checksum. This can be used to calculate the checksum of a stream of
   *   data in multiple parts.
   */
  @Polyglot public fun crc32(data: Value?, value: Value? = null): Long {
    val dataVal: ByteArray = when {
      data == null || data.isNull -> JsError.error("The 'data' argument must not be null.")
      data.isString -> data.asString().toByteArray(StandardCharsets.UTF_8)
      else -> data.`as`(ByteArray::class.java)
    }
    val valueVal: ULong = (value?.let {
      when {
        it.isNull -> null
        it.isNumber -> it.asLong().also { sum ->
          require(sum > 0) { "Cannot start with negative CRC32 checksum" }
        }.toULong()

        else -> JsError.error("The 'value' argument must be a number.")
      }
    }) ?: 0u

    return crc32(
      dataVal,
      valueVal,
    )
  }

  /**
   * ## Method: `zlib.createDeflate([options])`
   *
   * Creates an instance of the [Deflate] object, using the provided [options], if any.
   *
   * @param options An optional object containing configuration options for the deflate stream.
   * @return A new [Deflate] object.
   */
  @Polyglot public fun createDeflate(options: ZlibOptions): Deflate

  /**
   * ## Method: `zlib.createDeflate([options])`
   *
   * Creates an instance of the [Deflate] object, using the provided [options], if any.
   *
   * @param options An optional object containing configuration options for the deflate stream.
   * @return A new [Deflate] object.
   */
  @Polyglot public fun createDeflate(options: Value? = null): Deflate = when (options) {
    null -> createDeflate(ImmutableZlibOptions.defaults())
    else -> createDeflate(ImmutableZlibOptions.fromValue(options))
  }

  /**
   * ## Method: `zlib.createInflate([options])`
   *
   * Creates an instance of the [Inflate] object, using the provided [options], if any.
   *
   * @param options An optional object containing configuration options for the inflate stream.
   * @return A new [Inflate] object.
   */
  @Polyglot public fun createInflate(options: ZlibOptions): Inflate

  /**
   * ## Method: `zlib.createDeflate([options])`
   *
   * Creates an instance of the [Inflate] object, using the provided [options], if any.
   *
   * @param options An optional object containing configuration options for the inflate stream.
   * @return A new [Inflate] object.
   */
  @Polyglot public fun createInflate(options: Value? = null): Inflate = when (options) {
    null -> createInflate(ImmutableZlibOptions.defaults())
    else -> createInflate(ImmutableZlibOptions.fromValue(options))
  }

  /**
   * ## Method: `zlib.createUnzip([options])`
   *
   * Creates an instance of the [Unzip] object, using the provided [options], if any.
   *
   * @param options An optional object containing configuration options for the unzip stream.
   * @return A new [Unzip] object.
   */
  @Polyglot public fun createUnzip(options: ZlibOptions): Unzip

  /**
   * ## Method: `zlib.createUnzip([options])`
   *
   * Creates an instance of the [Unzip] object, using the provided [options], if any.
   *
   * @param options An optional object containing configuration options for the unzip stream.
   * @return A new [Unzip] object.
   */
  @Polyglot public fun createUnzip(options: Value? = null): Unzip = when (options) {
    null -> createUnzip(ImmutableZlibOptions.defaults())
    else -> createUnzip(ImmutableZlibOptions.fromValue(options))
  }

  /**
   * ## Method: `zlib.deflate(buffer[, options], callback)`
   *
   * Asynchronously compresses the given [buffer] using the DEFLATE algorithm; apply the provided [options], or use
   * defaults if no options are specified.
   *
   * @param buffer The buffer to compress.
   * @param options An optional object containing configuration options for the compression.
   * @param cbk Callback which is passed the resulting buffer.
   */
  @Polyglot public fun deflate(buffer: ZlibBuffer, options: ZlibOptions? = null, cbk: CompressCallback)

  /**
   * ## Method: `zlib.deflate(buffer[, options], callback)`
   *
   * Asynchronously compresses the given [buffer] using the DEFLATE algorithm; apply the provided [options], or use
   * defaults if no options are specified.
   *
   * This method variant works with foreign [Value] instances.
   *
   * @param buffer The buffer to compress.
   * @param options An optional object containing configuration options for the compression.
   * @param cbk Callback which is passed the resulting buffer.
   */
  @Polyglot public fun deflate(buffer: Value, options: Value? = null, cbk: Value) {
    zlibOpts(buffer, options).let { deflate(ByteBuffer.wrap(it.second), it.first, CompressCallback.from(cbk)) }
  }

  /**
   * ## Method: `zlib.deflateSync(buffer, options)`
   *
   * Synchronously compresses the given [buffer] using the DEFLATE algorithm; apply the provided [options], or use
   * defaults if no options are specified.
   *
   * @param buffer The buffer to compress.
   * @param options An optional object containing configuration options for the compression.
   * @return The compressed buffer.
   */
  @Polyglot public fun deflateSync(buffer: ZlibBuffer, options: ZlibOptions? = null): ZlibBuffer

  /**
   * ## Method: `zlib.deflateSync(buffer, options)`
   *
   * Synchronously compresses the given [buffer] using the DEFLATE algorithm; apply the provided [options], or use
   * defaults if no options are specified.
   *
   * This method variant works with foreign [Value] instances.
   *
   * @param buffer The buffer to compress.
   * @param options An optional object containing configuration options for the compression.
   * @return The compressed buffer.
   */
  @Polyglot public fun deflateSync(buffer: Value?, options: Value? = null): ZlibBuffer =
    zlibOpts(buffer, options).let { deflateSync(ByteBuffer.wrap(it.second), it.first) }

  /**
   * ## Method: `zlib.inflate(buffer[, options], callback)`
   *
   * Asynchronously decompresses the given [buffer] using the DEFLATE algorithm; apply the provided [options], or use
   * defaults if no options are specified.
   *
   * @param buffer The buffer to decompress.
   * @param options An optional object containing configuration options for the decompression.
   * @param cbk Callback which is passed the resulting buffer.
   */
  @Polyglot public fun inflate(buffer: ZlibBuffer, options: ZlibOptions? = null, cbk: CompressCallback)

  /**
   * ## Method: `zlib.inflate(buffer[, options], callback)`
   *
   * Asynchronously decompresses the given [buffer] using the DEFLATE algorithm; apply the provided [options], or use
   * defaults if no options are specified.
   *
   * This method variant works with foreign [Value] instances.
   *
   * @param buffer The buffer to decompress.
   * @param options An optional object containing configuration options for the decompression.
   * @param cbk Callback which is passed the resulting buffer.
   */
  @Polyglot public fun inflate(buffer: Value, options: Value? = null, cbk: Value) {
    zlibOpts(buffer, options).let { inflate(ByteBuffer.wrap(it.second), it.first, CompressCallback.from(cbk)) }
  }

  /**
   * ## Method: `zlib.inflateSync(buffer, options)`
   *
   * Synchronously decompresses the given [buffer] using the DEFLATE algorithm; apply the provided [options], or use
   * defaults if no options are specified.
   *
   * @param buffer The buffer to decompress.
   * @param options An optional object containing configuration options for the decompression.
   * @return The decompressed buffer.
   */
  @Polyglot public fun inflateSync(buffer: ZlibBuffer, options: ZlibOptions? = null): ZlibBuffer

  /**
   * ## Method: `zlib.inflateSync(buffer, options)`
   *
   * Synchronously decompresses the given [buffer] using the DEFLATE algorithm; apply the provided [options], or use
   * defaults if no options are specified.
   *
   * This method variant works with foreign [Value] instances.
   *
   * @param buffer The buffer to decompress.
   * @param options An optional object containing configuration options for the decompression.
   * @return The decompressed buffer.
   */
  @Polyglot public fun inflateSync(buffer: Value?, options: Value? = null): ZlibBuffer =
    zlibOpts(buffer, options).let { inflateSync(ByteBuffer.wrap(it.second), it.first) }

  /**
   * ## Method: `zlib.gzip(buffer[, options], callback)`
   *
   * Asynchronously compresses the given [buffer] using the gzip algorithm; apply the provided [options], or use
   * defaults if no options are specified.
   *
   * @param buffer The buffer to compress.
   * @param options An optional object containing configuration options for the compression.
   * @param cbk Callback which is passed the resulting buffer.
   */
  @Polyglot public fun gzip(buffer: ZlibBuffer, options: ZlibOptions? = null, cbk: CompressCallback)

  /**
   * ## Method: `zlib.gzip(buffer[, options], callback)`
   *
   * Asynchronously compresses the given [buffer] using the gzip algorithm; apply the provided [options], or use
   * defaults if no options are specified.
   *
   * This method variant works with foreign [Value] instances.
   *
   * @param buffer The buffer to compress.
   * @param options An optional object containing configuration options for the compression.
   * @param cbk Callback which is passed the resulting buffer.
   */
  @Polyglot public fun gzip(buffer: Value, options: Value? = null, cbk: Value) {
    zlibOpts(buffer, options).let { gzip(ByteBuffer.wrap(it.second), it.first, CompressCallback.from(cbk)) }
  }

  /**
   * ## Method: `zlib.gzipSync(buffer, options)`
   *
   * Synchronously compresses the given [buffer] using the GZIP algorithm; apply the provided [options], or use defaults
   * if no options are specified.
   *
   * @param buffer The buffer to compress.
   * @param options An optional object containing configuration options for the compression.
   * @return The compressed buffer.
   */
  @Polyglot public fun gzipSync(buffer: ZlibBuffer, options: ZlibOptions? = null): ZlibBuffer

  /**
   * ## Method: `zlib.gzipSync(buffer, options)`
   *
   * Synchronously compresses the given [buffer] using the GZIP algorithm; apply the provided [options], or use defaults
   * if no options are specified.
   *
   * This method variant works with foreign [Value] instances.
   *
   * @param buffer The buffer to compress.
   * @param options An optional object containing configuration options for the compression.
   * @return The compressed buffer.
   */
  @Polyglot public fun gzipSync(buffer: Value?, options: Value? = null): ByteBuffer =
    zlibOpts(buffer, options).let { gzipSync(ByteBuffer.wrap(it.second), it.first) }

  /**
   * ## Method: `zlib.gunzip(buffer[, options], callback)`
   *
   * Asynchronously decompresses the given [buffer] using the gunzip algorithm; apply the provided [options], or use
   * defaults if no options are specified.
   *
   * @param buffer The buffer to decompress.
   * @param options An optional object containing configuration options for the decompression.
   * @param cbk Callback which is passed the resulting buffer.
   */
  @Polyglot public fun gunzip(buffer: ZlibBuffer, options: ZlibOptions? = null, cbk: CompressCallback)

  /**
   * ## Method: `zlib.gunzip(buffer[, options], callback)`
   *
   * Asynchronously decompresses the given [buffer] using the gunzip algorithm; apply the provided [options], or use
   * defaults if no options are specified.
   *
   * This method variant works with foreign [Value] instances.
   *
   * @param buffer The buffer to decompress.
   * @param options An optional object containing configuration options for the decompression.
   * @param cbk Callback which is passed the resulting buffer.
   */
  @Polyglot public fun gunzip(buffer: Value, options: Value? = null, cbk: Value) {
    zlibOpts(buffer, options).let { gunzip(ByteBuffer.wrap(it.second), it.first, CompressCallback.from(cbk)) }
  }

  /**
   * ## Method: `zlib.gunzipSync(buffer, options)`
   *
   * Synchronously decompresses the given [buffer] using the GZIP algorithm; apply the provided [options], or use
   * defaults if no options are specified.
   *
   * @param buffer The buffer to decompress.
   * @param options An optional object containing configuration options for the decompression.
   * @return The decompressed buffer.
   */
  @Polyglot public fun gunzipSync(buffer: ZlibBuffer, options: ZlibOptions? = null): ZlibBuffer

  /**
   * ## Method: `zlib.gunzipSync(buffer, options)`
   *
   * Synchronously decompresses the given [buffer] using the GZIP algorithm; apply the provided [options], or use
   * defaults if no options are specified.
   *
   * This method variant works with foreign [Value] instances.
   *
   * @param buffer The buffer to decompress.
   * @param options An optional object containing configuration options for the decompression.
   * @return The decompressed buffer.
   */
  @Polyglot public fun gunzipSync(buffer: Value?, options: Value? = null): ZlibBuffer =
    zlibOpts(buffer, options).let { gunzipSync(ByteBuffer.wrap(it.second), it.first) }

  /**
   * ## Method: `zlib.unzip(buffer[, options], callback)`
   *
   * Asynchronously decompresses the given [buffer] using the zip algorithm; apply the provided [options], or use
   * defaults if no options are specified.
   *
   * @param buffer The buffer to decompress.
   * @param options An optional object containing configuration options for the decompression.
   * @param cbk Callback which is passed the resulting buffer.
   */
  @Polyglot public fun unzip(buffer: ZlibBuffer, options: ZlibOptions? = null, cbk: CompressCallback)

  /**
   * ## Method: `zlib.unzip(buffer[, options], callback)`
   *
   * Asynchronously decompresses the given [buffer] using the zip algorithm; apply the provided [options], or use
   * defaults if no options are specified.
   *
   * This method variant works with foreign [Value] instances.
   *
   * @param buffer The buffer to decompress.
   * @param options An optional object containing configuration options for the decompression.
   * @param cbk Callback which is passed the resulting buffer.
   */
  @Polyglot public fun unzip(buffer: Value, options: Value? = null, cbk: Value) {
    zlibOpts(buffer, options).let { unzip(ByteBuffer.wrap(it.second), it.first, CompressCallback.from(cbk)) }
  }

  /**
   * ## Method: `zlib.unzipSync(buffer, options)`
   *
   * Synchronously decompresses the given [buffer] using the Zip algorithm; apply the provided [options], or use
   * defaults if no options are specified.
   *
   * @param buffer The buffer to decompress.
   * @param options An optional object containing configuration options for the decompression.
   * @return The decompressed buffer.
   */
  @Polyglot public fun unzipSync(buffer: ZlibBuffer, options: ZlibOptions? = null): ZlibBuffer

  /**
   * ## Method: `zlib.unzipSync(buffer, options)`
   *
   * Synchronously decompresses the given [buffer] using the Zip algorithm; apply the provided [options], or use
   * defaults if no options are specified.
   *
   * This method variant works with foreign [Value] instances.
   *
   * @param buffer The buffer to decompress.
   * @param options An optional object containing configuration options for the decompression.
   * @return The decompressed buffer.
   */
  @Polyglot public fun unzipSync(buffer: Value?, options: Value? = null): ZlibBuffer =
    zlibOpts(buffer, options).let { unzipSync(ByteBuffer.wrap(it.second), it.first) }

  /**
   * ## Method: `zlib.createBrotliCompress([options])`
   *
   * Creates an instance of the [BrotliCompress] object, using the provided [options], if any.
   *
   * @param options An optional object containing configuration options for the deflate stream.
   * @return A new [BrotliCompress] object.
   */
  @Polyglot public fun createBrotliCompress(options: BrotliOptions): BrotliCompress

  /**
   * ## Method: `zlib.createBrotliCompress([options])`
   *
   * Creates an instance of the [BrotliCompress] object, using the provided [options], if any.
   *
   * @param options An optional object containing configuration options for the deflate stream.
   * @return A new [BrotliCompress] object.
   */
  @Polyglot public fun createBrotliCompress(options: Value? = null): BrotliCompress = when (options) {
    null -> createBrotliCompress(ImmutableBrotliOptions.defaults())
    else -> createBrotliCompress(ImmutableBrotliOptions.fromValue(options))
  }

  /**
   * ## Method: `zlib.createBrotliDecompress([options])`
   *
   * Creates an instance of the [BrotliDecompress] object, using the provided [options], if any.
   *
   * @param options An optional object containing configuration options for the inflate stream.
   * @return A new [BrotliDecompress] object.
   */
  @Polyglot public fun createBrotliDecompress(options: BrotliOptions): BrotliDecompress

  /**
   * ## Method: `zlib.createBrotliDecompress([options])`
   *
   * Creates an instance of the [BrotliDecompress] object, using the provided [options], if any.
   *
   * @param options An optional object containing configuration options for the inflate stream.
   * @return A new [BrotliDecompress] object.
   */
  @Polyglot public fun createBrotliDecompress(options: Value? = null): BrotliDecompress = when (options) {
    null -> createBrotliDecompress(ImmutableBrotliOptions.defaults())
    else -> createBrotliDecompress(ImmutableBrotliOptions.fromValue(options))
  }

  /**
   * ## Method: `zlib.brotliCompress(buffer[, options], callback)`
   *
   * Asynchronously compresses the given [buffer] using the Brotli algorithm; apply the provided [options], or use
   * defaults if no options are specified.
   *
   * @param buffer The buffer to compress.
   * @param options An optional object containing configuration options for the compression.
   * @param cbk Callback which is passed the resulting buffer.
   */
  @Polyglot public fun brotliCompress(buffer: ZlibBuffer, options: BrotliOptions? = null, cbk: CompressCallback)

  /**
   * ## Method: `zlib.brotliCompress(buffer[, options], callback)`
   *
   * Asynchronously compresses the given [buffer] using the Brotli algorithm; apply the provided [options], or use
   * defaults if no options are specified.
   *
   * This method variant works with foreign [Value] instances.
   *
   * @param buffer The buffer to compress.
   * @param options An optional object containing configuration options for the compression.
   * @param cbk Callback which is passed the resulting buffer.
   */
  @Polyglot public fun brotliCompress(buffer: Value, options: Value? = null, cbk: Value) {
    brotliOpts(buffer, options).let { brotliCompress(ByteBuffer.wrap(it.second), it.first, CompressCallback.from(cbk)) }
  }

  /**
   * ## Method: `zlib.brotliCompressSync(buffer, options)`
   *
   * Synchronously compresses the given [buffer] using the Brotli algorithm; apply the provided [options], or use
   * defaults if no options are specified.
   *
   * @param buffer The buffer to compress.
   * @param options An optional object containing configuration options for the compression.
   * @return The compressed buffer.
   */
  @Polyglot public fun brotliCompressSync(buffer: ZlibBuffer, options: BrotliOptions? = null): ZlibBuffer

  /**
   * ## Method: `zlib.brotliCompressSync(buffer, options)`
   *
   * Synchronously compresses the given [buffer] using the Brotli algorithm; apply the provided [options], or use
   * defaults if no options are specified.
   *
   * This method variant works with foreign [Value] instances.
   *
   * @param buffer The buffer to compress.
   * @param options An optional object containing configuration options for the compression.
   * @return The compressed buffer.
   */
  @Polyglot public fun brotliCompressSync(buffer: Value?, options: Value? = null): ZlibBuffer =
    brotliOpts(buffer, options).let { brotliCompressSync(ByteBuffer.wrap(it.second), it.first) }

  /**
   * ## Method: `zlib.brotliDecompress(buffer[, options], callback)`
   *
   * Asynchronously decompresses the given [buffer] using the Brotli algorithm; apply the provided [options], or use
   * defaults if no options are specified.
   *
   * @param buffer The buffer to decompress.
   * @param options An optional object containing configuration options for the decompression.
   * @param cbk Callback which is passed the resulting buffer.
   */
  @Polyglot public fun brotliDecompress(buffer: ZlibBuffer, options: BrotliOptions? = null, cbk: CompressCallback)

  /**
   * ## Method: `zlib.brotliDecompress(buffer[, options], callback)`
   *
   * Asynchronously decompresses the given [buffer] using the Brotli algorithm; apply the provided [options], or use
   * defaults if no options are specified.
   *
   * This method variant works with foreign [Value] instances.
   *
   * @param buffer The buffer to decompress.
   * @param options An optional object containing configuration options for the decompression.
   * @param cbk Callback which is passed the resulting buffer.
   */
  @Polyglot public fun brotliDecompress(buffer: Value, options: Value? = null, cbk: Value) {
    brotliOpts(buffer, options).let {
      brotliDecompress(ByteBuffer.wrap(it.second), it.first, CompressCallback.from(cbk))
    }
  }

  /**
   * ## Method: `zlib.brotliDecompressSync(buffer, options)`
   *
   * Synchronously decompresses the given [buffer] using the Brotli algorithm; apply the provided [options], or use
   * defaults if no options are specified.
   *
   * @param buffer The buffer to decompress.
   * @param options An optional object containing configuration options for the decompression.
   * @return The decompressed buffer.
   */
  @Polyglot public fun brotliDecompressSync(buffer: ZlibBuffer, options: BrotliOptions? = null): ZlibBuffer

  /**
   * ## Method: `zlib.brotliDecompressSync(buffer, options)`
   *
   * Synchronously decompresses the given [buffer] using the Brotli algorithm; apply the provided [options], or use
   * defaults if no options are specified.
   *
   * This method variant works with foreign [Value] instances.
   *
   * @param buffer The buffer to decompress.
   * @param options An optional object containing configuration options for the decompression.
   * @return The decompressed buffer.
   */
  @Polyglot public fun brotliDecompressSync(buffer: Value?, options: Value? = null): ZlibBuffer =
    brotliOpts(buffer, options).let { brotliDecompressSync(ByteBuffer.wrap(it.second), it.first) }
}

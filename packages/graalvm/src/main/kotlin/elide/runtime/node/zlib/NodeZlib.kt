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
@file:Suppress("WildcardImport", "NOTHING_TO_INLINE", "MaxLineLength")

package elide.runtime.node.zlib

import com.aayushatharva.brotli4j.Brotli4jLoader
import com.aayushatharva.brotli4j.decoder.BrotliInputStream
import com.aayushatharva.brotli4j.encoder.BrotliOutputStream
import com.aayushatharva.brotli4j.encoder.Encoder
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.graalvm.nativeimage.ImageInfo
import org.graalvm.polyglot.PolyglotException
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyInstantiable
import org.graalvm.polyglot.proxy.ProxyObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.zip.*
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.js.JsError
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.ZlibAPI
import elide.runtime.intrinsics.js.node.ZlibBuffer
import elide.runtime.intrinsics.js.node.stream.Readable
import elide.runtime.intrinsics.js.node.stream.Writable
import elide.runtime.intrinsics.js.node.zlib.*
import elide.runtime.lang.javascript.NodeModuleName
import elide.runtime.lang.javascript.SyntheticJSModule
import elide.runtime.node.stream.AbstractReadable
import elide.runtime.node.stream.ColdInputStream
import elide.runtime.node.stream.WrappedInputStream
import elide.runtime.node.stream.WrappedOutputStream
import elide.vm.annotations.Polyglot

private const val K_FLUSH = "flush"
private const val K_FINISH_FLUSH = "finishFlush"
private const val K_CHUNK_SIZE = "chunkSize"
private const val K_PARAMS = "params"
private const val K_WINDOW_BITS = "windowBits"
private const val K_LEVEL = "level"
private const val K_MEM_LEVEL = "memLevel"
private const val K_STRATEGY = "strategy"
private const val K_DICTIONARY = "dictionary"
private const val K_INFO = "info"
private const val K_MAX_OUTPUT_LENGTH = "maxOutputLength"
private const val K_CONSTANTS = "constants"

private const val F_BROTLI_COMPRESS = "brotliCompress"
private const val F_BROTLI_COMPRESS_SYNC = "brotliCompressSync"
private const val F_BROTLI_DECOMPRESS = "brotliDecompress"
private const val F_BROTLI_DECOMPRESS_SYNC = "brotliDecompressSync"
private const val F_CRC32 = "crc32"
private const val F_CREATE_DEFLATE = "createDeflate"
private const val F_CREATE_INFLATE = "createInflate"
private const val F_CREATE_UNZIP = "createUnzip"
private const val F_DEFLATE = "deflate"
private const val F_DEFLATE_RAW = "deflateRaw"
private const val F_DEFLATE_RAW_SYNC = "deflateRawSync"
private const val F_DEFLATE_SYNC = "deflateSync"
private const val F_GUNZIP = "gunzip"
private const val F_GUNZIP_SYNC = "gunzipSync"
private const val F_GZIP = "gzip"
private const val F_GZIP_SYNC = "gzipSync"
private const val F_INFLATE = "inflate"
private const val F_INFLATE_SYNC = "inflateSync"
private const val F_UNZIP = "unzip"
private const val F_UNZIP_SYNC = "unzipSync"

private val moduleMembers = arrayOf(
  F_BROTLI_COMPRESS,
  F_BROTLI_COMPRESS_SYNC,
  F_BROTLI_DECOMPRESS,
  F_BROTLI_DECOMPRESS_SYNC,
  K_CONSTANTS,
  F_CRC32,
  F_CREATE_DEFLATE,
  F_CREATE_INFLATE,
  F_CREATE_UNZIP,
  F_DEFLATE,
  F_DEFLATE_RAW,
  F_DEFLATE_RAW_SYNC,
  F_DEFLATE_SYNC,
  F_GUNZIP,
  F_GUNZIP_SYNC,
  F_GZIP,
  F_GZIP_SYNC,
  F_INFLATE,
  F_INFLATE_SYNC,
  F_UNZIP,
  F_UNZIP_SYNC,
)

/**
 * ## Zlib Options (Mutable)
 *
 * Implements a mutable suite of Zlib options object.
 */
public data class MutableZlibOptions @JvmOverloads public constructor (
  @Polyglot override var flush: Int = ModernNodeZlibConstants.Z_NO_FLUSH,
  @Polyglot override var finishFlush: Int = ModernNodeZlibConstants.Z_FINISH,
  @Polyglot override var chunkSize: Int = ModernNodeZlibConstants.Z_DEFAULT_CHUNK,
  @Polyglot override var windowBits: Int = ModernNodeZlibConstants.Z_DEFAULT_WINDOWBITS,
  @Polyglot override var level: Int? = ModernNodeZlibConstants.Z_DEFAULT_LEVEL,
  @Polyglot override var memLevel: Int? = ModernNodeZlibConstants.Z_DEFAULT_MEMLEVEL,
  @Polyglot override var strategy: Int? = ModernNodeZlibConstants.Z_DEFAULT_STRATEGY,
  @Polyglot override var dictionary: Any? = null,
  @Polyglot override var info: Boolean = false,
  @Polyglot override var maxOutputLength: Int = 0,
)
  : elide.runtime.intrinsics.js.node.zlib.MutableZlibOptions, ZlibOptionsDefaults {
  /** Constructor definitions for [MutableZlibOptions]. */
  public companion object Factory : ProxyInstantiable {
    /**
     * Create a new [MutableZlibOptions] with defaults set.
     */
    @JvmStatic public fun defaults(): ZlibOptions = MutableZlibOptions()

    override fun newInstance(vararg arguments: Value?): Any {
      require(arguments.isEmpty()) { "Constructor for Zlib options does not take arguments" }
      return MutableZlibOptions()
    }
  }

  /**
   * Create an immutable copy of this [MutableZlibOptions].
   *
   * @return An immutable copy of this [MutableZlibOptions].
   */
  public fun toImmutable(): ImmutableZlibOptions = ImmutableZlibOptions(
    flush = flush,
    finishFlush = finishFlush,
    chunkSize = chunkSize,
    windowBits = windowBits,
    level = level ?: ModernNodeZlibConstants.Z_DEFAULT_LEVEL,
    memLevel = memLevel ?: ModernNodeZlibConstants.Z_DEFAULT_MEMLEVEL,
    strategy = strategy ?: ModernNodeZlibConstants.Z_DEFAULT_STRATEGY,
    dictionary = dictionary,
    info = info,
    maxOutputLength = maxOutputLength,
  )

  override fun putMember(key: String?, value: Value?): Unit = when (key) {
    K_FLUSH -> flush = value?.asInt() ?: ModernNodeZlibConstants.Z_NO_FLUSH
    K_FINISH_FLUSH -> finishFlush = value?.asInt() ?: ModernNodeZlibConstants.Z_FINISH
    K_CHUNK_SIZE -> chunkSize = value?.asInt() ?: ModernNodeZlibConstants.Z_DEFAULT_CHUNK
    K_WINDOW_BITS -> windowBits = value?.asInt() ?: ModernNodeZlibConstants.Z_DEFAULT_WINDOWBITS
    K_LEVEL -> level = value?.asInt() ?: ModernNodeZlibConstants.Z_DEFAULT_LEVEL
    K_MEM_LEVEL -> memLevel = value?.asInt() ?: ModernNodeZlibConstants.Z_DEFAULT_MEMLEVEL
    K_STRATEGY -> strategy = value?.asInt() ?: ModernNodeZlibConstants.Z_DEFAULT_STRATEGY
    K_DICTIONARY -> dictionary = value
    K_INFO -> info = value?.asBoolean() == true
    K_MAX_OUTPUT_LENGTH -> maxOutputLength = value?.asInt() ?: 0
    else -> JsError.error("Cannot set member '$key' on ZlibOptions")
  }
}

/**
 * ## Brotli Options (Mutable)
 *
 * Implements an mutable Brotli options object.
 */
public data class MutableBrotliOptions internal constructor (
  @Polyglot override var flush: Int = ModernNodeZlibConstants.BROTLI_OPERATION_PROCESS,
  @Polyglot override var finishFlush: Int = ModernNodeZlibConstants.BROTLI_OPERATION_FINISH,
  @Polyglot override var chunkSize: Int = ModernNodeZlibConstants.Z_DEFAULT_CHUNK,
  @Polyglot override var maxOutputLength: Int = 0,
  @Polyglot override var params: Int = 0,
)
  : elide.runtime.intrinsics.js.node.zlib.MutableBrotliOptions, BrotliOptionsDefaults {
  /** Constructor definitions for [MutableBrotliOptions]. */
  public companion object Factory : ProxyInstantiable {
    /**
     * Create a new [MutableBrotliOptions] with defaults set.
     */
    @JvmStatic public fun defaults(): BrotliOptions = MutableBrotliOptions()

    override fun newInstance(vararg arguments: Value?): Any {
      require(arguments.isEmpty()) { "Constructor for Brotli options does not take arguments" }
      return MutableBrotliOptions()
    }
  }

  /**
   * Create an immutable copy of this [MutableZlibOptions].
   *
   * @return An immutable copy of this [MutableZlibOptions].
   */
  public fun toImmutable(): ImmutableBrotliOptions = ImmutableBrotliOptions(
    flush = flush,
    finishFlush = finishFlush,
    chunkSize = chunkSize,
    params = params,
    maxOutputLength = maxOutputLength,
  )

  override fun putMember(key: String?, value: Value?): Unit = when (key) {
    K_FLUSH -> flush = value?.asInt() ?: ImmutableBrotliOptions.defaults().flush
    K_FINISH_FLUSH -> finishFlush = value?.asInt() ?: ImmutableBrotliOptions.defaults().finishFlush
    K_CHUNK_SIZE -> chunkSize = value?.asInt() ?: ImmutableBrotliOptions.defaults().chunkSize
    K_PARAMS -> params = value?.asInt() ?: ImmutableBrotliOptions.defaults().params
    K_MAX_OUTPUT_LENGTH -> maxOutputLength = value?.asInt() ?: ImmutableBrotliOptions.defaults().maxOutputLength
    else -> JsError.error("Cannot set member '$key' on ZlibOptions")
  }
}

/**
 * ## Node API: `zlib.constants`
 *
 * Implements constants which are provided by the Node Zlib module.
 */
public data object ModernNodeZlibConstants : NodeZlibConstants {
  @get:Polyglot override val Z_NO_FLUSH: Int = Z_NO_FLUSH_CONST
  @get:Polyglot override val Z_PARTIAL_FLUSH: Int = Z_PARTIAL_FLUSH_CONST
  @get:Polyglot override val Z_SYNC_FLUSH: Int = Z_SYNC_FLUSH_CONST
  @get:Polyglot override val Z_FULL_FLUSH: Int = Z_FULL_FLUSH_CONST
  @get:Polyglot override val Z_FINISH: Int = Z_FINISH_CONST
  @get:Polyglot override val Z_BLOCK: Int = Z_BLOCK_CONST
  @get:Polyglot override val Z_OK: Int = Z_OK_CONST
  @get:Polyglot override val Z_STREAM_END: Int = Z_STREAM_END_CONST
  @get:Polyglot override val Z_NEED_DICT: Int = Z_NEED_DICT_CONST
  @get:Polyglot override val Z_ERRNO: Int = Z_ERRNO_CONST
  @get:Polyglot override val Z_STREAM_ERROR: Int = Z_STREAM_ERROR_CONST
  @get:Polyglot override val Z_DATA_ERROR: Int = Z_DATA_ERROR_CONST
  @get:Polyglot override val Z_MEM_ERROR: Int = Z_MEM_ERROR_CONST
  @get:Polyglot override val Z_BUF_ERROR: Int = Z_BUF_ERROR_CONST
  @get:Polyglot override val Z_VERSION_ERROR: Int = Z_VERSION_ERROR_CONST
  @get:Polyglot override val Z_NO_COMPRESSION: Int = Z_NO_COMPRESSION_CONST
  @get:Polyglot override val Z_BEST_SPEED: Int = Z_BEST_SPEED_CONST
  @get:Polyglot override val Z_BEST_COMPRESSION: Int = Z_BEST_COMPRESSION_CONST
  @get:Polyglot override val Z_DEFAULT_COMPRESSION: Int = Z_DEFAULT_COMPRESSION_CONST
  @get:Polyglot override val Z_FILTERED: Int = Z_FILTERED_CONST
  @get:Polyglot override val Z_HUFFMAN_ONLY: Int = Z_HUFFMAN_ONLY_CONST
  @get:Polyglot override val Z_RLE: Int = Z_RLE_CONST
  @get:Polyglot override val Z_FIXED: Int = Z_FIXED_CONST
  @get:Polyglot override val Z_DEFAULT_STRATEGY: Int = Z_DEFAULT_STRATEGY_CONST
  @get:Polyglot override val ZLIB_VERNUM: Int = ZLIB_VERNUM_CONST
  @get:Polyglot override val DEFLATE: Int = DEFLATE_CONST
  @get:Polyglot override val INFLATE: Int = INFLATE_CONST
  @get:Polyglot override val GZIP: Int = GZIP_CONST
  @get:Polyglot override val GUNZIP: Int = GUNZIP_CONST
  @get:Polyglot override val DEFLATERAW: Int = DEFLATERAW_CONST
  @get:Polyglot override val INFLATERAW: Int = INFLATERAW_CONST
  @get:Polyglot override val UNZIP: Int = UNZIP_CONST
  @get:Polyglot override val Z_MIN_WINDOWBITS: Int = Z_MIN_WINDOWBITS_CONST
  @get:Polyglot override val Z_MAX_WINDOWBITS: Int = Z_MAX_WINDOWBITS_CONST
  @get:Polyglot override val Z_DEFAULT_WINDOWBITS: Int = Z_DEFAULT_WINDOWBITS_CONST
  @get:Polyglot override val Z_MIN_CHUNK: Int = Z_MIN_CHUNK_CONST
  @get:Polyglot override val Z_MAX_CHUNK: Int = Z_MAX_CHUNK_CONST
  @get:Polyglot override val Z_DEFAULT_CHUNK: Int = Z_DEFAULT_CHUNK_CONST
  @get:Polyglot override val Z_MIN_MEMLEVEL: Int = Z_MIN_MEMLEVEL_CONST
  @get:Polyglot override val Z_MAX_MEMLEVEL: Int = Z_MAX_MEMLEVEL_CONST
  @get:Polyglot override val Z_DEFAULT_MEMLEVEL: Int = Z_DEFAULT_MEMLEVEL_CONST
  @get:Polyglot override val Z_MIN_LEVEL: Int = Z_MIN_LEVEL_CONST
  @get:Polyglot override val Z_MAX_LEVEL: Int = Z_MAX_LEVEL_CONST
  @get:Polyglot override val Z_DEFAULT_LEVEL: Int = Z_DEFAULT_LEVEL_CONST
  @get:Polyglot override val BROTLI_OPERATION_PROCESS: Int = BROTLI_OPERATION_PROCESS_CONST
  @get:Polyglot override val BROTLI_OPERATION_FLUSH: Int = BROTLI_OPERATION_FLUSH_CONST
  @get:Polyglot override val BROTLI_OPERATION_FINISH: Int = BROTLI_OPERATION_FINISH_CONST
  @get:Polyglot override val BROTLI_OPERATION_EMIT_METADATA: Int = BROTLI_OPERATION_EMIT_METADATA_CONST
  @get:Polyglot override val BROTLI_PARAM_MODE: Int = BROTLI_PARAM_MODE_CONST
  @get:Polyglot override val BROTLI_MODE_GENERIC: Int = BROTLI_MODE_GENERIC_CONST
  @get:Polyglot override val BROTLI_MODE_TEXT: Int = BROTLI_MODE_TEXT_CONST
  @get:Polyglot override val BROTLI_MODE_FONT: Int = BROTLI_MODE_FONT_CONST
  @get:Polyglot override val BROTLI_DEFAULT_MODE: Int = BROTLI_DEFAULT_MODE_CONST
  @get:Polyglot override val BROTLI_PARAM_QUALITY: Int = BROTLI_PARAM_QUALITY_CONST
  @get:Polyglot override val BROTLI_MIN_QUALITY: Int = BROTLI_MIN_QUALITY_CONST
  @get:Polyglot override val BROTLI_MAX_QUALITY: Int = BROTLI_MAX_QUALITY_CONST
  @get:Polyglot override val BROTLI_DEFAULT_QUALITY: Int = BROTLI_DEFAULT_QUALITY_CONST
  @get:Polyglot override val BROTLI_PARAM_LGWIN: Int = BROTLI_PARAM_LGWIN_CONST
  @get:Polyglot override val BROTLI_MIN_WINDOW_BITS: Int = BROTLI_MIN_WINDOW_BITS_CONST
  @get:Polyglot override val BROTLI_MAX_WINDOW_BITS: Int = BROTLI_MAX_WINDOW_BITS_CONST
  @get:Polyglot override val BROTLI_LARGE_MAX_WINDOW_BITS: Int = BROTLI_LARGE_MAX_WINDOW_BITS_CONST
  @get:Polyglot override val BROTLI_DEFAULT_WINDOW: Int = BROTLI_DEFAULT_WINDOW_CONST
  @get:Polyglot override val BROTLI_PARAM_LGBLOCK: Int = BROTLI_PARAM_LGBLOCK_CONST
  @get:Polyglot override val BROTLI_MIN_INPUT_BLOCK_BITS: Int = BROTLI_MIN_INPUT_BLOCK_BITS_CONST
  @get:Polyglot override val BROTLI_MAX_INPUT_BLOCK_BITS: Int = BROTLI_MAX_INPUT_BLOCK_BITS_CONST
  @get:Polyglot override val BROTLI_PARAM_DISABLE_LITERAL_CONTEXT_MODELING: Int = BROTLI_PARAM_DISABLE_LITERAL_CONTEXT_MODELING_CONST
  @get:Polyglot override val BROTLI_PARAM_SIZE_HINT: Int = BROTLI_PARAM_SIZE_HINT_CONST
  @get:Polyglot override val BROTLI_PARAM_LARGE_WINDOW: Int = BROTLI_PARAM_LARGE_WINDOW_CONST
  @get:Polyglot override val BROTLI_PARAM_NPOSTFIX: Int = BROTLI_PARAM_NPOSTFIX_CONST
  @get:Polyglot override val BROTLI_PARAM_NDIRECT: Int = BROTLI_PARAM_NDIRECT_CONST
  @get:Polyglot override val BROTLI_DECODER_RESULT_ERROR: Int = BROTLI_DECODER_RESULT_ERROR_CONST
  @get:Polyglot override val BROTLI_DECODER_RESULT_SUCCESS: Int = BROTLI_DECODER_RESULT_SUCCESS_CONST
  @get:Polyglot override val BROTLI_DECODER_RESULT_NEEDS_MORE_INPUT: Int = BROTLI_DECODER_RESULT_NEEDS_MORE_INPUT_CONST
  @get:Polyglot override val BROTLI_DECODER_RESULT_NEEDS_MORE_OUTPUT: Int = BROTLI_DECODER_RESULT_NEEDS_MORE_OUTPUT_CONST
  @get:Polyglot override val BROTLI_DECODER_PARAM_DISABLE_RING_BUFFER_REALLOCATION: Int = BROTLI_DECODER_PARAM_DISABLE_RING_BUFFER_REALLOCATION_CONST
  @get:Polyglot override val BROTLI_DECODER_PARAM_LARGE_WINDOW: Int = BROTLI_DECODER_PARAM_LARGE_WINDOW_CONST
  @get:Polyglot override val BROTLI_DECODER_NO_ERROR: Int = BROTLI_DECODER_NO_ERROR_CONST
  @get:Polyglot override val BROTLI_DECODER_SUCCESS: Int = BROTLI_DECODER_SUCCESS_CONST
  @get:Polyglot override val BROTLI_DECODER_NEEDS_MORE_INPUT: Int = BROTLI_DECODER_NEEDS_MORE_INPUT_CONST
  @get:Polyglot override val BROTLI_DECODER_NEEDS_MORE_OUTPUT: Int = BROTLI_DECODER_NEEDS_MORE_OUTPUT_CONST
  @get:Polyglot override val BROTLI_DECODER_ERROR_FORMAT_EXUBERANT_NIBBLE: Int = BROTLI_DECODER_ERROR_FORMAT_EXUBERANT_NIBBLE_CONST
  @get:Polyglot override val BROTLI_DECODER_ERROR_FORMAT_RESERVED: Int = BROTLI_DECODER_ERROR_FORMAT_RESERVED_CONST
  @get:Polyglot override val BROTLI_DECODER_ERROR_FORMAT_EXUBERANT_META_NIBBLE: Int = BROTLI_DECODER_ERROR_FORMAT_EXUBERANT_META_NIBBLE_CONST
  @get:Polyglot override val BROTLI_DECODER_ERROR_FORMAT_SIMPLE_HUFFMAN_ALPHABET: Int = BROTLI_DECODER_ERROR_FORMAT_SIMPLE_HUFFMAN_ALPHABET_CONST
  @get:Polyglot override val BROTLI_DECODER_ERROR_FORMAT_SIMPLE_HUFFMAN_SAME: Int = BROTLI_DECODER_ERROR_FORMAT_SIMPLE_HUFFMAN_SAME_CONST
  @get:Polyglot override val BROTLI_DECODER_ERROR_FORMAT_CL_SPACE: Int = BROTLI_DECODER_ERROR_FORMAT_CL_SPACE_CONST
  @get:Polyglot override val BROTLI_DECODER_ERROR_FORMAT_HUFFMAN_SPACE: Int = BROTLI_DECODER_ERROR_FORMAT_HUFFMAN_SPACE_CONST
  @get:Polyglot override val BROTLI_DECODER_ERROR_FORMAT_CONTEXT_MAP_REPEAT: Int = BROTLI_DECODER_ERROR_FORMAT_CONTEXT_MAP_REPEAT_CONST
  @get:Polyglot override val BROTLI_DECODER_ERROR_FORMAT_BLOCK_LENGTH_1: Int = BROTLI_DECODER_ERROR_FORMAT_BLOCK_LENGTH_1_CONST
  @get:Polyglot override val BROTLI_DECODER_ERROR_FORMAT_BLOCK_LENGTH_2: Int = BROTLI_DECODER_ERROR_FORMAT_BLOCK_LENGTH_2_CONST
  @get:Polyglot override val BROTLI_DECODER_ERROR_FORMAT_TRANSFORM: Int = BROTLI_DECODER_ERROR_FORMAT_TRANSFORM_CONST
  @get:Polyglot override val BROTLI_DECODER_ERROR_FORMAT_DICTIONARY: Int = BROTLI_DECODER_ERROR_FORMAT_DICTIONARY_CONST
  @get:Polyglot override val BROTLI_DECODER_ERROR_FORMAT_WINDOW_BITS: Int = BROTLI_DECODER_ERROR_FORMAT_WINDOW_BITS_CONST
  @get:Polyglot override val BROTLI_DECODER_ERROR_FORMAT_PADDING_1: Int = BROTLI_DECODER_ERROR_FORMAT_PADDING_1_CONST
  @get:Polyglot override val BROTLI_DECODER_ERROR_FORMAT_PADDING_2: Int = BROTLI_DECODER_ERROR_FORMAT_PADDING_2_CONST
  @get:Polyglot override val BROTLI_DECODER_ERROR_FORMAT_DISTANCE: Int = BROTLI_DECODER_ERROR_FORMAT_DISTANCE_CONST
  @get:Polyglot override val BROTLI_DECODER_ERROR_DICTIONARY_NOT_SET: Int = BROTLI_DECODER_ERROR_DICTIONARY_NOT_SET_CONST
  @get:Polyglot override val BROTLI_DECODER_ERROR_INVALID_ARGUMENTS: Int = BROTLI_DECODER_ERROR_INVALID_ARGUMENTS_CONST
  @get:Polyglot override val BROTLI_DECODER_ERROR_ALLOC_CONTEXT_MODES: Int = BROTLI_DECODER_ERROR_ALLOC_CONTEXT_MODES_CONST
  @get:Polyglot override val BROTLI_DECODER_ERROR_ALLOC_TREE_GROUPS: Int = BROTLI_DECODER_ERROR_ALLOC_TREE_GROUPS_CONST
  @get:Polyglot override val BROTLI_DECODER_ERROR_ALLOC_CONTEXT_MAP: Int = BROTLI_DECODER_ERROR_ALLOC_CONTEXT_MAP_CONST
  @get:Polyglot override val BROTLI_DECODER_ERROR_ALLOC_RING_BUFFER_1: Int = BROTLI_DECODER_ERROR_ALLOC_RING_BUFFER_1_CONST
  @get:Polyglot override val BROTLI_DECODER_ERROR_ALLOC_RING_BUFFER_2: Int = BROTLI_DECODER_ERROR_ALLOC_RING_BUFFER_2_CONST
  @get:Polyglot override val BROTLI_DECODER_ERROR_ALLOC_BLOCK_TYPE_TREES: Int = BROTLI_DECODER_ERROR_ALLOC_BLOCK_TYPE_TREES_CONST
  @get:Polyglot override val BROTLI_DECODER_ERROR_UNREACHABLE: Int = BROTLI_DECODER_ERROR_UNREACHABLE_CONST

  override fun hasMember(key: String?): Boolean = key != null && key in memberKeys
  override fun putMember(key: String?, value: Value?): Unit = error("Cannot modify zlib constants")
  override fun getMemberKeys(): Array<String> = arrayOf(
    SYMBOL_Z_NO_FLUSH, SYMBOL_Z_PARTIAL_FLUSH, SYMBOL_Z_SYNC_FLUSH, SYMBOL_Z_FULL_FLUSH, SYMBOL_Z_FINISH,
    SYMBOL_Z_BLOCK, SYMBOL_Z_OK, SYMBOL_Z_STREAM_END, SYMBOL_Z_NEED_DICT, SYMBOL_Z_ERRNO, SYMBOL_Z_STREAM_ERROR,
    SYMBOL_Z_DATA_ERROR, SYMBOL_Z_MEM_ERROR, SYMBOL_Z_BUF_ERROR, SYMBOL_Z_VERSION_ERROR, SYMBOL_Z_NO_COMPRESSION,
    SYMBOL_Z_BEST_SPEED, SYMBOL_Z_BEST_COMPRESSION, SYMBOL_Z_DEFAULT_COMPRESSION, SYMBOL_Z_FILTERED,
    SYMBOL_Z_HUFFMAN_ONLY, SYMBOL_Z_RLE, SYMBOL_Z_FIXED, SYMBOL_Z_DEFAULT_STRATEGY, SYMBOL_ZLIB_VERNUM,
    SYMBOL_DEFLATE, SYMBOL_INFLATE, SYMBOL_GZIP, SYMBOL_GUNZIP, SYMBOL_DEFLATERAW, SYMBOL_INFLATERAW, SYMBOL_UNZIP,
    SYMBOL_Z_MIN_WINDOWBITS, SYMBOL_Z_MAX_WINDOWBITS, SYMBOL_Z_DEFAULT_WINDOWBITS, SYMBOL_Z_MIN_CHUNK,
    SYMBOL_Z_MAX_CHUNK, SYMBOL_Z_DEFAULT_CHUNK, SYMBOL_Z_MIN_MEMLEVEL, SYMBOL_Z_MAX_MEMLEVEL, SYMBOL_Z_DEFAULT_MEMLEVEL,
    SYMBOL_Z_MIN_LEVEL, SYMBOL_Z_MAX_LEVEL, SYMBOL_Z_DEFAULT_LEVEL,
    SYMBOL_BROTLI_OPERATION_PROCESS, SYMBOL_BROTLI_OPERATION_FLUSH, SYMBOL_BROTLI_OPERATION_FINISH,
    SYMBOL_BROTLI_OPERATION_EMIT_METADATA, SYMBOL_BROTLI_PARAM_MODE, SYMBOL_BROTLI_MODE_GENERIC,
    SYMBOL_BROTLI_MODE_TEXT, SYMBOL_BROTLI_MODE_FONT, SYMBOL_BROTLI_DEFAULT_MODE, SYMBOL_BROTLI_PARAM_QUALITY,
    SYMBOL_BROTLI_MIN_QUALITY, SYMBOL_BROTLI_MAX_QUALITY, SYMBOL_BROTLI_DEFAULT_QUALITY, SYMBOL_BROTLI_PARAM_LGWIN,
    SYMBOL_BROTLI_MIN_WINDOW_BITS, SYMBOL_BROTLI_MAX_WINDOW_BITS, SYMBOL_BROTLI_LARGE_MAX_WINDOW_BITS,
    SYMBOL_BROTLI_DEFAULT_WINDOW, SYMBOL_BROTLI_PARAM_LGBLOCK, SYMBOL_BROTLI_MIN_INPUT_BLOCK_BITS,
    SYMBOL_BROTLI_MAX_INPUT_BLOCK_BITS, SYMBOL_BROTLI_PARAM_DISABLE_LITERAL_CONTEXT_MODELING,
    SYMBOL_BROTLI_PARAM_SIZE_HINT, SYMBOL_BROTLI_PARAM_LARGE_WINDOW, SYMBOL_BROTLI_PARAM_NPOSTFIX,
    SYMBOL_BROTLI_PARAM_NDIRECT, SYMBOL_BROTLI_DECODER_RESULT_ERROR, SYMBOL_BROTLI_DECODER_RESULT_SUCCESS,
    SYMBOL_BROTLI_DECODER_RESULT_NEEDS_MORE_INPUT, SYMBOL_BROTLI_DECODER_RESULT_NEEDS_MORE_OUTPUT,
    SYMBOL_BROTLI_DECODER_PARAM_DISABLE_RING_BUFFER_REALLOCATION, SYMBOL_BROTLI_DECODER_PARAM_LARGE_WINDOW,
    SYMBOL_BROTLI_DECODER_NO_ERROR, SYMBOL_BROTLI_DECODER_SUCCESS, SYMBOL_BROTLI_DECODER_NEEDS_MORE_INPUT,
    SYMBOL_BROTLI_DECODER_NEEDS_MORE_OUTPUT, SYMBOL_BROTLI_DECODER_ERROR_FORMAT_EXUBERANT_NIBBLE,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_RESERVED, SYMBOL_BROTLI_DECODER_ERROR_FORMAT_EXUBERANT_META_NIBBLE,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_SIMPLE_HUFFMAN_ALPHABET, SYMBOL_BROTLI_DECODER_ERROR_FORMAT_SIMPLE_HUFFMAN_SAME,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_CL_SPACE, SYMBOL_BROTLI_DECODER_ERROR_FORMAT_HUFFMAN_SPACE,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_CONTEXT_MAP_REPEAT, SYMBOL_BROTLI_DECODER_ERROR_FORMAT_BLOCK_LENGTH_1,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_BLOCK_LENGTH_2, SYMBOL_BROTLI_DECODER_ERROR_FORMAT_TRANSFORM,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_DICTIONARY, SYMBOL_BROTLI_DECODER_ERROR_FORMAT_WINDOW_BITS,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_PADDING_1, SYMBOL_BROTLI_DECODER_ERROR_FORMAT_PADDING_2,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_DISTANCE, SYMBOL_BROTLI_DECODER_ERROR_DICTIONARY_NOT_SET,
    SYMBOL_BROTLI_DECODER_ERROR_INVALID_ARGUMENTS, SYMBOL_BROTLI_DECODER_ERROR_ALLOC_CONTEXT_MODES,
    SYMBOL_BROTLI_DECODER_ERROR_ALLOC_TREE_GROUPS, SYMBOL_BROTLI_DECODER_ERROR_ALLOC_CONTEXT_MAP,
    SYMBOL_BROTLI_DECODER_ERROR_ALLOC_RING_BUFFER_1, SYMBOL_BROTLI_DECODER_ERROR_ALLOC_RING_BUFFER_2,
    SYMBOL_BROTLI_DECODER_ERROR_ALLOC_BLOCK_TYPE_TREES, SYMBOL_BROTLI_DECODER_ERROR_UNREACHABLE
  )

  @Suppress("CyclomaticComplexMethod")
  override fun getMember(key: String?): Any? = when (key) {
    SYMBOL_Z_NO_FLUSH -> Z_NO_FLUSH_CONST
    SYMBOL_Z_PARTIAL_FLUSH -> Z_PARTIAL_FLUSH_CONST
    SYMBOL_Z_SYNC_FLUSH -> Z_SYNC_FLUSH_CONST
    SYMBOL_Z_FULL_FLUSH -> Z_FULL_FLUSH_CONST
    SYMBOL_Z_FINISH -> Z_FINISH_CONST
    SYMBOL_Z_BLOCK -> Z_BLOCK_CONST
    SYMBOL_Z_OK -> Z_OK_CONST
    SYMBOL_Z_STREAM_END -> Z_STREAM_END_CONST
    SYMBOL_Z_NEED_DICT -> Z_NEED_DICT_CONST
    SYMBOL_Z_ERRNO -> Z_ERRNO_CONST
    SYMBOL_Z_STREAM_ERROR -> Z_STREAM_ERROR_CONST
    SYMBOL_Z_DATA_ERROR -> Z_DATA_ERROR_CONST
    SYMBOL_Z_MEM_ERROR -> Z_MEM_ERROR_CONST
    SYMBOL_Z_BUF_ERROR -> Z_BUF_ERROR_CONST
    SYMBOL_Z_VERSION_ERROR -> Z_VERSION_ERROR_CONST
    SYMBOL_Z_NO_COMPRESSION -> Z_NO_COMPRESSION_CONST
    SYMBOL_Z_BEST_SPEED -> Z_BEST_SPEED_CONST
    SYMBOL_Z_BEST_COMPRESSION -> Z_BEST_COMPRESSION_CONST
    SYMBOL_Z_DEFAULT_COMPRESSION -> Z_DEFAULT_COMPRESSION_CONST
    SYMBOL_Z_FILTERED -> Z_FILTERED_CONST
    SYMBOL_Z_HUFFMAN_ONLY -> Z_HUFFMAN_ONLY_CONST
    SYMBOL_Z_RLE -> Z_RLE_CONST
    SYMBOL_Z_FIXED -> Z_FIXED_CONST
    SYMBOL_Z_DEFAULT_STRATEGY -> Z_DEFAULT_STRATEGY_CONST
    SYMBOL_ZLIB_VERNUM -> ZLIB_VERNUM_CONST
    SYMBOL_DEFLATE -> DEFLATE_CONST
    SYMBOL_INFLATE -> INFLATE_CONST
    SYMBOL_GZIP -> GZIP_CONST
    SYMBOL_GUNZIP -> GUNZIP_CONST
    SYMBOL_DEFLATERAW -> DEFLATERAW_CONST
    SYMBOL_INFLATERAW -> INFLATERAW_CONST
    SYMBOL_UNZIP -> UNZIP_CONST
    SYMBOL_Z_MIN_WINDOWBITS -> Z_MIN_WINDOWBITS_CONST
    SYMBOL_Z_MAX_WINDOWBITS -> Z_MAX_WINDOWBITS_CONST
    SYMBOL_Z_DEFAULT_WINDOWBITS -> Z_DEFAULT_WINDOWBITS_CONST
    SYMBOL_Z_MIN_CHUNK -> Z_MIN_CHUNK_CONST
    SYMBOL_Z_MAX_CHUNK -> Z_MAX_CHUNK_CONST
    SYMBOL_Z_DEFAULT_CHUNK -> Z_DEFAULT_CHUNK_CONST
    SYMBOL_Z_MIN_MEMLEVEL -> Z_MIN_MEMLEVEL_CONST
    SYMBOL_Z_MAX_MEMLEVEL -> Z_MAX_MEMLEVEL_CONST
    SYMBOL_Z_DEFAULT_MEMLEVEL -> Z_DEFAULT_MEMLEVEL_CONST
    SYMBOL_Z_MIN_LEVEL -> Z_MIN_LEVEL_CONST
    SYMBOL_Z_MAX_LEVEL -> Z_MAX_LEVEL_CONST
    SYMBOL_Z_DEFAULT_LEVEL -> Z_DEFAULT_LEVEL_CONST
    SYMBOL_BROTLI_OPERATION_PROCESS -> BROTLI_OPERATION_PROCESS_CONST
    SYMBOL_BROTLI_OPERATION_FLUSH -> BROTLI_OPERATION_FLUSH_CONST
    SYMBOL_BROTLI_OPERATION_FINISH -> BROTLI_OPERATION_FINISH_CONST
    SYMBOL_BROTLI_OPERATION_EMIT_METADATA -> BROTLI_OPERATION_EMIT_METADATA_CONST
    SYMBOL_BROTLI_PARAM_MODE -> BROTLI_PARAM_MODE_CONST
    SYMBOL_BROTLI_MODE_GENERIC -> BROTLI_MODE_GENERIC_CONST
    SYMBOL_BROTLI_MODE_TEXT -> BROTLI_MODE_TEXT_CONST
    SYMBOL_BROTLI_MODE_FONT -> BROTLI_MODE_FONT_CONST
    SYMBOL_BROTLI_DEFAULT_MODE -> BROTLI_DEFAULT_MODE_CONST
    SYMBOL_BROTLI_PARAM_QUALITY -> BROTLI_PARAM_QUALITY_CONST
    SYMBOL_BROTLI_MIN_QUALITY -> BROTLI_MIN_QUALITY_CONST
    SYMBOL_BROTLI_MAX_QUALITY -> BROTLI_MAX_QUALITY_CONST
    SYMBOL_BROTLI_DEFAULT_QUALITY -> BROTLI_DEFAULT_QUALITY_CONST
    SYMBOL_BROTLI_PARAM_LGWIN -> BROTLI_PARAM_LGWIN_CONST
    SYMBOL_BROTLI_MIN_WINDOW_BITS -> BROTLI_MIN_WINDOW_BITS_CONST
    SYMBOL_BROTLI_MAX_WINDOW_BITS -> BROTLI_MAX_WINDOW_BITS_CONST
    SYMBOL_BROTLI_LARGE_MAX_WINDOW_BITS -> BROTLI_LARGE_MAX_WINDOW_BITS_CONST
    SYMBOL_BROTLI_DEFAULT_WINDOW -> BROTLI_DEFAULT_WINDOW_CONST
    SYMBOL_BROTLI_PARAM_LGBLOCK -> BROTLI_PARAM_LGBLOCK_CONST
    SYMBOL_BROTLI_MIN_INPUT_BLOCK_BITS -> BROTLI_MIN_INPUT_BLOCK_BITS_CONST
    SYMBOL_BROTLI_MAX_INPUT_BLOCK_BITS -> BROTLI_MAX_INPUT_BLOCK_BITS_CONST
    SYMBOL_BROTLI_PARAM_DISABLE_LITERAL_CONTEXT_MODELING -> BROTLI_PARAM_DISABLE_LITERAL_CONTEXT_MODELING_CONST
    SYMBOL_BROTLI_PARAM_SIZE_HINT -> BROTLI_PARAM_SIZE_HINT_CONST
    SYMBOL_BROTLI_PARAM_LARGE_WINDOW -> BROTLI_PARAM_LARGE_WINDOW_CONST
    SYMBOL_BROTLI_PARAM_NPOSTFIX -> BROTLI_PARAM_NPOSTFIX_CONST
    SYMBOL_BROTLI_PARAM_NDIRECT -> BROTLI_PARAM_NDIRECT_CONST
    SYMBOL_BROTLI_DECODER_RESULT_ERROR -> BROTLI_DECODER_RESULT_ERROR_CONST
    SYMBOL_BROTLI_DECODER_RESULT_SUCCESS -> BROTLI_DECODER_RESULT_SUCCESS_CONST
    SYMBOL_BROTLI_DECODER_RESULT_NEEDS_MORE_INPUT -> BROTLI_DECODER_RESULT_NEEDS_MORE_INPUT_CONST
    SYMBOL_BROTLI_DECODER_RESULT_NEEDS_MORE_OUTPUT -> BROTLI_DECODER_RESULT_NEEDS_MORE_OUTPUT_CONST
    SYMBOL_BROTLI_DECODER_PARAM_DISABLE_RING_BUFFER_REALLOCATION -> BROTLI_DECODER_PARAM_DISABLE_RING_BUFFER_REALLOCATION_CONST
    SYMBOL_BROTLI_DECODER_PARAM_LARGE_WINDOW -> BROTLI_DECODER_PARAM_LARGE_WINDOW_CONST
    SYMBOL_BROTLI_DECODER_NO_ERROR -> BROTLI_DECODER_NO_ERROR_CONST
    SYMBOL_BROTLI_DECODER_SUCCESS -> BROTLI_DECODER_SUCCESS_CONST
    SYMBOL_BROTLI_DECODER_NEEDS_MORE_INPUT -> BROTLI_DECODER_NEEDS_MORE_INPUT_CONST
    SYMBOL_BROTLI_DECODER_NEEDS_MORE_OUTPUT -> BROTLI_DECODER_NEEDS_MORE_OUTPUT_CONST
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_EXUBERANT_NIBBLE -> BROTLI_DECODER_ERROR_FORMAT_EXUBERANT_NIBBLE_CONST
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_RESERVED -> BROTLI_DECODER_ERROR_FORMAT_RESERVED_CONST
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_EXUBERANT_META_NIBBLE -> BROTLI_DECODER_ERROR_FORMAT_EXUBERANT_META_NIBBLE_CONST
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_SIMPLE_HUFFMAN_ALPHABET -> BROTLI_DECODER_ERROR_FORMAT_SIMPLE_HUFFMAN_ALPHABET_CONST
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_SIMPLE_HUFFMAN_SAME -> BROTLI_DECODER_ERROR_FORMAT_SIMPLE_HUFFMAN_SAME_CONST
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_CL_SPACE -> BROTLI_DECODER_ERROR_FORMAT_CL_SPACE_CONST
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_HUFFMAN_SPACE -> BROTLI_DECODER_ERROR_FORMAT_HUFFMAN_SPACE_CONST
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_CONTEXT_MAP_REPEAT -> BROTLI_DECODER_ERROR_FORMAT_CONTEXT_MAP_REPEAT_CONST
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_BLOCK_LENGTH_1 -> BROTLI_DECODER_ERROR_FORMAT_BLOCK_LENGTH_1_CONST
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_BLOCK_LENGTH_2 -> BROTLI_DECODER_ERROR_FORMAT_BLOCK_LENGTH_2_CONST
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_TRANSFORM -> BROTLI_DECODER_ERROR_FORMAT_TRANSFORM_CONST
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_DICTIONARY -> BROTLI_DECODER_ERROR_FORMAT_DICTIONARY_CONST
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_WINDOW_BITS -> BROTLI_DECODER_ERROR_FORMAT_WINDOW_BITS_CONST
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_PADDING_1 -> BROTLI_DECODER_ERROR_FORMAT_PADDING_1_CONST
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_PADDING_2 -> BROTLI_DECODER_ERROR_FORMAT_PADDING_2_CONST
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_DISTANCE -> BROTLI_DECODER_ERROR_FORMAT_DISTANCE_CONST
    SYMBOL_BROTLI_DECODER_ERROR_DICTIONARY_NOT_SET -> BROTLI_DECODER_ERROR_DICTIONARY_NOT_SET_CONST
    SYMBOL_BROTLI_DECODER_ERROR_INVALID_ARGUMENTS -> BROTLI_DECODER_ERROR_INVALID_ARGUMENTS_CONST
    SYMBOL_BROTLI_DECODER_ERROR_ALLOC_CONTEXT_MODES -> BROTLI_DECODER_ERROR_ALLOC_CONTEXT_MODES_CONST
    SYMBOL_BROTLI_DECODER_ERROR_ALLOC_TREE_GROUPS -> BROTLI_DECODER_ERROR_ALLOC_TREE_GROUPS_CONST
    SYMBOL_BROTLI_DECODER_ERROR_ALLOC_CONTEXT_MAP -> BROTLI_DECODER_ERROR_ALLOC_CONTEXT_MAP_CONST
    SYMBOL_BROTLI_DECODER_ERROR_ALLOC_RING_BUFFER_1 -> BROTLI_DECODER_ERROR_ALLOC_RING_BUFFER_1_CONST
    SYMBOL_BROTLI_DECODER_ERROR_ALLOC_RING_BUFFER_2 -> BROTLI_DECODER_ERROR_ALLOC_RING_BUFFER_2_CONST
    SYMBOL_BROTLI_DECODER_ERROR_ALLOC_BLOCK_TYPE_TREES -> BROTLI_DECODER_ERROR_ALLOC_BLOCK_TYPE_TREES_CONST
    SYMBOL_BROTLI_DECODER_ERROR_UNREACHABLE -> BROTLI_DECODER_ERROR_UNREACHABLE_CONST
    else -> null
  }
}

// Installs the Node zlib module into the intrinsic bindings.
@Intrinsic internal class NodeZlibModule : SyntheticJSModule<NodeZlib>, AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeZlib.create() }
  override fun provide(): NodeZlib = singleton

  init {
    if (!ImageInfo.inImageCode()) {
      Brotli4jLoader.ensureAvailability()
    }
  }

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.ZLIB)) { singleton }
  }
}

private sealed class CompressImpl : CompressorAPI {
  override val bytesWritten: Int
    get() = TODO("Not yet implemented: `CompressImpl.bytesWritten`")
}

/**
 * Type which wraps a [WrappedOutputStream] as a [Writable] [Deflate] stream.
 */
private class DeflateStream(private val wrapped: WrappedOutputStream) :
  Writable by wrapped, Deflate, CompressImpl() {
  override fun close() {
    wrapped.close()
  }

  override fun flush() {
    wrapped.flush()
  }
}

/**
 * Type which wraps a [WrappedInputStream] as a [Readable] [Inflate] stream.
 */
private class InflateStream(private val wrapped: AbstractReadable<*>) :
  Readable by wrapped, Inflate, CompressImpl() {
  override fun close() {
    wrapped.close()
  }

  override fun flush() {
    // @TODO: flushing for readables
  }
}

/**
 * Type which wraps a [WrappedInputStream] as a [Readable] [Unzip] stream.
 */
private class UnzipStream(private val wrapped: AbstractReadable<*>) :
  Readable by wrapped, Unzip, CompressImpl() {
  override fun close() {
    wrapped.close()
  }

  override fun flush() {
    // @TODO: flushing for readables
  }
}

/**
 * Type which wraps a [WrappedInputStream] as a [Writable] [Inflate] stream.
 */
private class BrotliCompressStream(private val wrapped: WrappedOutputStream) :
  Writable by wrapped, BrotliCompress, CompressImpl() {
  init {
    Brotli4jLoader.ensureAvailability()
  }

  override fun close() {
    wrapped.close()
  }

  override fun flush() {
    wrapped.flush()
  }
}

/**
 * Type which wraps a [WrappedInputStream] as a [Readable] [Inflate] stream.
 */
private class BrotliDecompressStream(private val wrapped: AbstractReadable<*>) :
  Readable by wrapped, BrotliDecompress, CompressImpl() {
  init {
    Brotli4jLoader.ensureAvailability()
  }

  override fun close() {
    wrapped.close()
  }

  override fun flush() {
    // @TODO: flushing for readables
  }
}

/**
 * # Node API: `zlib`
 *
 * Implements the Node zlib module.
 */
internal class NodeZlib private constructor () : ProxyObject, ZlibAPI {
  companion object {
    @JvmStatic fun create(): NodeZlib = NodeZlib()

    // Decompressor which is enabled for EOF awareness.
    private val compressorFactory by lazy {
      CompressorStreamFactory(true)
    }
  }

  // @TODO guest executor access

  private fun <R> execute(block: () -> R) = block()

  // Decompress `buffer` data with the provided decompression `actor` (wraps an `InputStream`).
  private inline fun decompress(
    buffer: ZlibBuffer,
    actor: (InputStream) -> InputStream,
  ): ZlibBuffer = ByteArrayOutputStream().let { out ->
    out.use {
      ByteArrayInputStream(buffer.array()).use { `in` ->
        actor(`in`).use { stream ->
          stream.copyTo(out)
        }
      }
    }
    ByteBuffer.wrap(out.toByteArray())
  }

  // Compress `buffer` data with the provided compression `actor` (wraps an `OutputStream`).
  private inline fun compress(
    buffer: ZlibBuffer,
    actor: (OutputStream) -> OutputStream,
  ): ZlibBuffer = ByteArrayOutputStream().let { out ->
    out.use {
      ByteArrayInputStream(buffer.array()).use { `in` ->
        actor(out).use { stream ->
          `in`.copyTo(stream)
        }
      }
    }
    ByteBuffer.wrap(out.toByteArray())
  }

  // Run a compression operation asynchronously against the guest executor, and then dispatch the provided callback.
  private inline fun async(callback: CompressCallback, crossinline actor: () -> ZlibBuffer) = execute {
    try {
      actor.invoke()
    } catch (err: PolyglotException) {
      callback.failed(JsError.valueError("Failed to complete compression operation", err))
      return@execute
    }.also { buf ->
      callback.done(buf)
    }
  }

  // Compress the provided buffer with the provided compressor name.
  // @TODO: honor zlib options
  @Suppress("UNUSED_PARAMETER")
  private inline fun compressWith(
    buffer: ZlibBuffer,
    factory: String,
    options: ZlibOptions?,
  ): ZlibBuffer = compress(buffer) {
    compressorFactory.createCompressorOutputStream(factory, it)
  }

  // Decompress the provided buffer with the provided compressor.
  // @TODO: honor zlib options
  @Suppress("UNUSED_PARAMETER")
  private inline fun decompressWith(
    buffer: ZlibBuffer,
    factory: String,
    options: ZlibOptions?,
  ): ZlibBuffer = decompress(buffer) {
    compressorFactory.createCompressorInputStream(factory, it)
  }

  // Constants for zlib and brotli.
  @get:Polyglot override val constants: NodeZlibConstants get() = ModernNodeZlibConstants

  // Implements `zlib.crc32(...)`.
  @Polyglot override fun crc32(data: ByteArray, value: ULong): Long {
    val checksum = CRC32().apply { update(data) }
    return when (value > 0u) {
      false -> checksum.value
      true -> value.toLong() or checksum.value
    }
  }

  // Implements `zlib.createDeflate(...)`.
  @Polyglot override fun createDeflate(options: ZlibOptions): Deflate =
    // @TODO: honor zlib options
    DeflateStream(WrappedOutputStream.wrap(DeflaterOutputStream(ByteArrayOutputStream())))

  // Implements `zlib.createInflate(...)`.
  @Polyglot override fun createInflate(options: ZlibOptions): Inflate =
    // @TODO: honor zlib options
    InflateStream(WrappedInputStream.wrap(InflaterInputStream(ColdInputStream.create()), null))

  // Implements `zlib.createUnzip(...)`.
  @Polyglot override fun createUnzip(options: ZlibOptions): Unzip =
    // @TODO: honor zlib options
    UnzipStream(WrappedInputStream.wrap(InflaterInputStream(ColdInputStream.create()), null))

  // Implements `zlib.createBrotliCompress(...)`.
  @Polyglot override fun createBrotliCompress(options: BrotliOptions): BrotliCompress =
    // @TODO: honor zlib options
    BrotliCompressStream(WrappedOutputStream.wrap(DeflaterOutputStream(ByteArrayOutputStream())))

  // Implements `zlib.createBrotliDecompress(...)`.
  @Polyglot override fun createBrotliDecompress(options: BrotliOptions): BrotliDecompress =
    // @TODO: honor zlib options
    BrotliDecompressStream(WrappedInputStream.wrap(InflaterInputStream(ColdInputStream.create()), null))

  // Implements `zlib.gzip(...)`.
  @Polyglot override fun gzip(buffer: ZlibBuffer, options: ZlibOptions?, cbk: CompressCallback) = async(cbk) {
    gzipSync(buffer, options)
  }

  // Implements `zlib.gzipSync(...)`.
  @Polyglot override fun gzipSync(buffer: ZlibBuffer, options: ZlibOptions?): ZlibBuffer =
    compressWith(buffer, CompressorStreamFactory.GZIP, options)

  // Implements `zlib.gunzip(...)`.
  @Polyglot override fun gunzip(buffer: ZlibBuffer, options: ZlibOptions?, cbk: CompressCallback) = async(cbk) {
    gunzipSync(buffer, options)
  }

  // Implements `zlib.gunzipSync(...)`.
  @Polyglot override fun gunzipSync(buffer: ZlibBuffer, options: ZlibOptions?): ZlibBuffer =
    decompressWith(buffer, CompressorStreamFactory.GZIP, options)

  // Implements `zlib.deflate(...)`.
  @Polyglot override fun deflate(buffer: ZlibBuffer, options: ZlibOptions?, cbk: CompressCallback) = async(cbk) {
    deflateSync(buffer, options)
  }

  // Implements `zlib.deflateSync(...)`.
  @Polyglot override fun deflateSync(buffer: ZlibBuffer, options: ZlibOptions?): ZlibBuffer =
    compressWith(buffer, CompressorStreamFactory.DEFLATE, options)

  // Implements `zlib.inflate(...)`.
  @Polyglot override fun inflate(buffer: ZlibBuffer, options: ZlibOptions?, cbk: CompressCallback) = async(cbk) {
    inflateSync(buffer, options)
  }

  // Implements `zlib.inflateSync(...)`.
  @Polyglot override fun inflateSync(buffer: ZlibBuffer, options: ZlibOptions?): ZlibBuffer =
    decompressWith(buffer, CompressorStreamFactory.DEFLATE, options)

  // Implements `zlib.unzip(...)`.
  @Polyglot override fun unzip(buffer: ZlibBuffer, options: ZlibOptions?, cbk: CompressCallback) = async(cbk) {
    unzipSync(buffer, options)
  }

  // Implements `zlib.unzipSync(...)`.
  @Polyglot override fun unzipSync(buffer: ZlibBuffer, options: ZlibOptions?): ZlibBuffer =
    decompressWith(buffer, CompressorStreamFactory.GZIP, options)

  // Implements `zlib.brotliCompress(...)`.
  @Polyglot override fun brotliCompress(buffer: ZlibBuffer, options: BrotliOptions?, cbk: CompressCallback) =
    async(cbk) { brotliCompressSync(buffer, options) }

  // Implements `zlib.brotliDecompress(...)`.
  @Polyglot override fun brotliDecompress(buffer: ZlibBuffer, options: BrotliOptions?, cbk: CompressCallback) =
    async(cbk) { brotliDecompressSync(buffer, options) }

  // Implements `zlib.brotliCompressSync(...)`.
  @Polyglot override fun brotliCompressSync(buffer: ZlibBuffer, options: BrotliOptions?): ZlibBuffer =
    compress(buffer) { BrotliOutputStream(it, Encoder.Parameters.DEFAULT) }

  // Implements `zlib.brotliDecompressSync(...)`.
  @Polyglot override fun brotliDecompressSync(buffer: ZlibBuffer, options: BrotliOptions?) = decompress(buffer) {
    BrotliInputStream(it)
  }

  override fun getMemberKeys(): Array<String> = moduleMembers
  override fun hasMember(key: String?): Boolean = key != null && key in memberKeys
  override fun putMember(key: String?, value: Value?): Unit = JsError.error("Cannot modify zlib module")

  override fun getMember(key: String?): Any? = when (key) {
    K_CONSTANTS -> constants
    F_CRC32 -> ProxyExecutable { args -> crc32(args.getOrNull(0), args.getOrNull(1)) }
    F_CREATE_DEFLATE -> ProxyExecutable { args -> createDeflate(args.getOrNull(0)) }
    F_CREATE_INFLATE -> ProxyExecutable { args -> createInflate(args.getOrNull(0)) }
    F_GZIP_SYNC -> ProxyExecutable { args -> gzipSync(args.getOrNull(0), args.getOrNull(1)) }
    F_GUNZIP_SYNC -> ProxyExecutable { args -> gunzipSync(args.getOrNull(0), args.getOrNull(1)) }
    F_DEFLATE_SYNC -> ProxyExecutable { args -> deflateSync(args.getOrNull(0), args.getOrNull(1)) }
    F_INFLATE_SYNC -> ProxyExecutable { args -> inflateSync(args.getOrNull(0), args.getOrNull(1)) }
    F_UNZIP_SYNC -> ProxyExecutable { args -> unzipSync(args.getOrNull(0), args.getOrNull(1)) }
    F_BROTLI_COMPRESS_SYNC ->
      ProxyExecutable { args -> brotliCompressSync(args.getOrNull(0), args.getOrNull(1)) }
    F_BROTLI_DECOMPRESS_SYNC ->
      ProxyExecutable { args -> brotliDecompressSync(args.getOrNull(0), args.getOrNull(1)) }

    F_DEFLATE -> ProxyExecutable { args ->
      when (args.size) {
        2 -> deflate(args[0], options = null, cbk = args[1])
        3 -> deflate(args[0], options = args[1], cbk = args[2])
        else -> throw JsError.typeError("Invalid arguments for zlib.deflate")
      }
    }

    F_INFLATE -> ProxyExecutable { args ->
      when (args.size) {
        2 -> inflate(args[0], options = null, cbk = args[1])
        3 -> inflate(args[0], options = args[1], cbk = args[2])
        else -> throw JsError.typeError("Invalid arguments for zlib.inflate")
      }
    }

    F_BROTLI_COMPRESS -> ProxyExecutable { args ->
      when (args.size) {
        2 -> brotliCompress(args[0], options = null, cbk = args[1])
        3 -> brotliCompress(args[0], options = args[1], cbk = args[2])
        else -> throw JsError.typeError("Invalid arguments for zlib.brotliCompress")
      }
    }

    F_BROTLI_DECOMPRESS -> ProxyExecutable { args ->
      when (args.size) {
        2 -> brotliDecompress(args[0], options = null, cbk = args[1])
        3 -> brotliDecompress(args[0], options = args[1], cbk = args[2])
        else -> throw JsError.typeError("Invalid arguments for zlib.brotliDecompress")
      }
    }

    F_GZIP -> ProxyExecutable { args ->
      when (args.size) {
        2 -> gzip(args[0], options = null, cbk = args[1])
        3 -> gzip(args[0], options = args[1], cbk = args[2])
        else -> throw JsError.typeError("Invalid arguments for zlib.gzip")
      }
    }

    // @TODO: not yet implemented
    F_GUNZIP,
    F_UNZIP,
    F_CREATE_UNZIP,
    F_DEFLATE_RAW,
    F_DEFLATE_RAW_SYNC -> null
    else -> null
  }
}

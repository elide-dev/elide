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
@file:Suppress("MaxLineLength", "LargeClass")

package elide.runtime.node

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.*
import elide.annotations.Inject
import elide.runtime.intrinsics.js.node.ZlibAPI
import elide.runtime.intrinsics.js.node.zlib.*
import elide.runtime.node.zlib.MutableZlibOptions
import elide.runtime.node.zlib.NodeZlibModule
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `zlib` built-in module. */
@TestCase internal class NodeZlibTest : NodeModuleConformanceTest<NodeZlibModule>() {
  override val moduleName: String get() = "zlib"
  override fun provide(): NodeZlibModule = zlib
  private fun obtain(): ZlibAPI = assertNotNull(zlib.provide())
  @Inject lateinit var zlib: NodeZlibModule

  // @TODO(sgammon): Not yet fully supported
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("crc32")
    yield("constants")
    yield("createBrotliCompress")
    yield("createBrotliDecompress")
    yield("createDeflate")
    yield("createDeflateRaw")
    yield("createInflate")
    yield("createInflateRaw")
    yield("createUnzip")
    yield("brotliCompress")
    yield("brotliCompressSync")
    yield("brotliDecompress")
    yield("brotliDecompressSync")
    yield("deflate")
    yield("deflateSync")
    yield("deflateRaw")
    yield("deflateRawSync")
    yield("gunzip")
    yield("gunzipSync")
    yield("gzip")
    yield("gzipSync")
    yield("inflate")
    yield("inflateSync")
    yield("inflateRaw")
    yield("inflateRawSync")
    yield("unzip")
    yield("unzipSync")
  }

  @Test override fun testInjectable() {
    assertNotNull(zlib)
  }

  @Test fun `zlib provides constants`() {
    assertNotNull(zlib.provide().constants)
    assertIs<NodeZlibConstants>(zlib.provide().constants)
  }

  @CsvSource(
    SYMBOL_Z_NO_FLUSH,
    SYMBOL_Z_PARTIAL_FLUSH,
    SYMBOL_Z_SYNC_FLUSH,
    SYMBOL_Z_FULL_FLUSH,
    SYMBOL_Z_FINISH,
    SYMBOL_Z_BLOCK,
    SYMBOL_Z_OK,
    SYMBOL_Z_STREAM_END,
    SYMBOL_Z_NEED_DICT,
    SYMBOL_Z_ERRNO,
    SYMBOL_Z_STREAM_ERROR,
    SYMBOL_Z_DATA_ERROR,
    SYMBOL_Z_MEM_ERROR,
    SYMBOL_Z_BUF_ERROR,
    SYMBOL_Z_VERSION_ERROR,
    SYMBOL_Z_NO_COMPRESSION,
    SYMBOL_Z_BEST_SPEED,
    SYMBOL_Z_BEST_COMPRESSION,
    SYMBOL_Z_DEFAULT_COMPRESSION,
    SYMBOL_Z_FILTERED,
    SYMBOL_Z_HUFFMAN_ONLY,
    SYMBOL_Z_RLE,
    SYMBOL_Z_FIXED,
    SYMBOL_Z_DEFAULT_STRATEGY,
    SYMBOL_ZLIB_VERNUM,
    SYMBOL_DEFLATE,
    SYMBOL_INFLATE,
    SYMBOL_GZIP,
    SYMBOL_GUNZIP,
    SYMBOL_DEFLATERAW,
    SYMBOL_INFLATERAW,
    SYMBOL_UNZIP,
    SYMBOL_Z_MIN_WINDOWBITS,
    SYMBOL_Z_MAX_WINDOWBITS,
    SYMBOL_Z_DEFAULT_WINDOWBITS,
    SYMBOL_Z_MIN_CHUNK,
    SYMBOL_Z_MAX_CHUNK,
    SYMBOL_Z_DEFAULT_CHUNK,
    SYMBOL_Z_MIN_MEMLEVEL,
    SYMBOL_Z_MAX_MEMLEVEL,
    SYMBOL_Z_DEFAULT_MEMLEVEL,
    SYMBOL_Z_MIN_LEVEL,
    SYMBOL_Z_MAX_LEVEL,
    SYMBOL_Z_DEFAULT_LEVEL
  )
  @ParameterizedTest
  fun `zlib constants - zlib symbols`(symbol: String) {
    val constants = assertNotNull(zlib.provide().constants)
    assertIs<NodeZlibConstants>(constants)
    assertTrue(constants.hasMember(symbol))
    assertNotNull(constants.getMember(symbol))
    assertIs<Int>(assertNotNull(constants.getMember(symbol)))
  }

  @CsvSource(
    SYMBOL_BROTLI_OPERATION_PROCESS,
    SYMBOL_BROTLI_OPERATION_FLUSH,
    SYMBOL_BROTLI_OPERATION_FINISH,
    SYMBOL_BROTLI_OPERATION_EMIT_METADATA,
    SYMBOL_BROTLI_PARAM_MODE,
    SYMBOL_BROTLI_MODE_GENERIC,
    SYMBOL_BROTLI_MODE_TEXT,
    SYMBOL_BROTLI_MODE_FONT,
    SYMBOL_BROTLI_DEFAULT_MODE,
    SYMBOL_BROTLI_PARAM_QUALITY,
    SYMBOL_BROTLI_MIN_QUALITY,
    SYMBOL_BROTLI_MAX_QUALITY,
    SYMBOL_BROTLI_DEFAULT_QUALITY,
    SYMBOL_BROTLI_PARAM_LGWIN,
    SYMBOL_BROTLI_MIN_WINDOW_BITS,
    SYMBOL_BROTLI_MAX_WINDOW_BITS,
    SYMBOL_BROTLI_LARGE_MAX_WINDOW_BITS,
    SYMBOL_BROTLI_DEFAULT_WINDOW,
    SYMBOL_BROTLI_PARAM_LGBLOCK,
    SYMBOL_BROTLI_MIN_INPUT_BLOCK_BITS,
    SYMBOL_BROTLI_MAX_INPUT_BLOCK_BITS,
    SYMBOL_BROTLI_PARAM_DISABLE_LITERAL_CONTEXT_MODELING,
    SYMBOL_BROTLI_PARAM_SIZE_HINT,
    SYMBOL_BROTLI_PARAM_LARGE_WINDOW,
    SYMBOL_BROTLI_PARAM_NPOSTFIX,
    SYMBOL_BROTLI_PARAM_NDIRECT,
    SYMBOL_BROTLI_DECODER_RESULT_ERROR,
    SYMBOL_BROTLI_DECODER_RESULT_SUCCESS,
    SYMBOL_BROTLI_DECODER_RESULT_NEEDS_MORE_INPUT,
    SYMBOL_BROTLI_DECODER_RESULT_NEEDS_MORE_OUTPUT,
    SYMBOL_BROTLI_DECODER_PARAM_DISABLE_RING_BUFFER_REALLOCATION,
    SYMBOL_BROTLI_DECODER_PARAM_LARGE_WINDOW,
    SYMBOL_BROTLI_DECODER_NO_ERROR,
    SYMBOL_BROTLI_DECODER_SUCCESS,
    SYMBOL_BROTLI_DECODER_NEEDS_MORE_INPUT,
    SYMBOL_BROTLI_DECODER_NEEDS_MORE_OUTPUT,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_EXUBERANT_NIBBLE,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_RESERVED,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_EXUBERANT_META_NIBBLE,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_SIMPLE_HUFFMAN_ALPHABET,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_SIMPLE_HUFFMAN_SAME,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_CL_SPACE,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_HUFFMAN_SPACE,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_CONTEXT_MAP_REPEAT,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_BLOCK_LENGTH_1,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_BLOCK_LENGTH_2,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_TRANSFORM,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_DICTIONARY,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_WINDOW_BITS,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_PADDING_1,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_PADDING_2,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_DISTANCE,
    SYMBOL_BROTLI_DECODER_ERROR_DICTIONARY_NOT_SET,
    SYMBOL_BROTLI_DECODER_ERROR_INVALID_ARGUMENTS,
    SYMBOL_BROTLI_DECODER_ERROR_ALLOC_CONTEXT_MODES,
    SYMBOL_BROTLI_DECODER_ERROR_ALLOC_TREE_GROUPS,
    SYMBOL_BROTLI_DECODER_ERROR_ALLOC_CONTEXT_MAP,
    SYMBOL_BROTLI_DECODER_ERROR_ALLOC_RING_BUFFER_1,
    SYMBOL_BROTLI_DECODER_ERROR_ALLOC_RING_BUFFER_2,
    SYMBOL_BROTLI_DECODER_ERROR_ALLOC_BLOCK_TYPE_TREES,
    SYMBOL_BROTLI_DECODER_ERROR_UNREACHABLE
  )
  @ParameterizedTest
  fun `brotli constants - brotli symbols`(symbol: String) {
    val constants = assertNotNull(zlib.provide().constants)
    assertIs<NodeZlibConstants>(constants)
    assertTrue(constants.hasMember(symbol))
    assertNotNull(constants.getMember(symbol))
    assertIs<Int>(assertNotNull(constants.getMember(symbol)))
  }

  @CsvSource(
    "Z_NO_FLUSH, $SYMBOL_Z_NO_FLUSH, $Z_NO_FLUSH_CONST",
    "Z_PARTIAL_FLUSH, $SYMBOL_Z_PARTIAL_FLUSH, $Z_PARTIAL_FLUSH_CONST",
    "Z_SYNC_FLUSH, $SYMBOL_Z_SYNC_FLUSH, $Z_SYNC_FLUSH_CONST",
    "Z_FULL_FLUSH, $SYMBOL_Z_FULL_FLUSH, $Z_FULL_FLUSH_CONST",
    "Z_FINISH, $SYMBOL_Z_FINISH, $Z_FINISH_CONST",
    "Z_BLOCK, $SYMBOL_Z_BLOCK, $Z_BLOCK_CONST",
    "Z_OK, $SYMBOL_Z_OK, $Z_OK_CONST",
    "Z_STREAM_END, $SYMBOL_Z_STREAM_END, $Z_STREAM_END_CONST",
    "Z_NEED_DICT, $SYMBOL_Z_NEED_DICT, $Z_NEED_DICT_CONST",
    "Z_ERRNO, $SYMBOL_Z_ERRNO, $Z_ERRNO_CONST",
    "Z_STREAM_ERROR, $SYMBOL_Z_STREAM_ERROR, $Z_STREAM_ERROR_CONST",
    "Z_DATA_ERROR, $SYMBOL_Z_DATA_ERROR, $Z_DATA_ERROR_CONST",
    "Z_MEM_ERROR, $SYMBOL_Z_MEM_ERROR, $Z_MEM_ERROR_CONST",
    "Z_BUF_ERROR, $SYMBOL_Z_BUF_ERROR, $Z_BUF_ERROR_CONST",
    "Z_VERSION_ERROR, $SYMBOL_Z_VERSION_ERROR, $Z_VERSION_ERROR_CONST",
    "Z_NO_COMPRESSION, $SYMBOL_Z_NO_COMPRESSION, $Z_NO_COMPRESSION_CONST",
    "Z_BEST_SPEED, $SYMBOL_Z_BEST_SPEED, $Z_BEST_SPEED_CONST",
    "Z_BEST_COMPRESSION, $SYMBOL_Z_BEST_COMPRESSION, $Z_BEST_COMPRESSION_CONST",
    "Z_DEFAULT_COMPRESSION, $SYMBOL_Z_DEFAULT_COMPRESSION, $Z_DEFAULT_COMPRESSION_CONST",
    "Z_FILTERED, $SYMBOL_Z_FILTERED, $Z_FILTERED_CONST",
    "Z_HUFFMAN_ONLY, $SYMBOL_Z_HUFFMAN_ONLY, $Z_HUFFMAN_ONLY_CONST",
    "Z_RLE, $SYMBOL_Z_RLE, $Z_RLE_CONST",
    "Z_FIXED, $SYMBOL_Z_FIXED, $Z_FIXED_CONST",
    "Z_DEFAULT_STRATEGY, $SYMBOL_Z_DEFAULT_STRATEGY, $Z_DEFAULT_STRATEGY_CONST",
    "ZLIB_VERNUM, $SYMBOL_ZLIB_VERNUM, $ZLIB_VERNUM_CONST",
    "DEFLATE, $SYMBOL_DEFLATE, $DEFLATE_CONST",
    "INFLATE, $SYMBOL_INFLATE, $INFLATE_CONST",
    "GZIP, $SYMBOL_GZIP, $GZIP_CONST",
    "GUNZIP, $SYMBOL_GUNZIP, $GUNZIP_CONST",
    "DEFLATERAW, $SYMBOL_DEFLATERAW, $DEFLATERAW_CONST",
    "INFLATERAW, $SYMBOL_INFLATERAW, $INFLATERAW_CONST",
    "UNZIP, $SYMBOL_UNZIP, $UNZIP_CONST",
    "Z_MIN_WINDOWBITS, $SYMBOL_Z_MIN_WINDOWBITS, $Z_MIN_WINDOWBITS_CONST",
    "Z_MAX_WINDOWBITS, $SYMBOL_Z_MAX_WINDOWBITS, $Z_MAX_WINDOWBITS_CONST",
    "Z_DEFAULT_WINDOWBITS, $SYMBOL_Z_DEFAULT_WINDOWBITS, $Z_DEFAULT_WINDOWBITS_CONST",
    "Z_MIN_CHUNK, $SYMBOL_Z_MIN_CHUNK, $Z_MIN_CHUNK_CONST",
    "Z_MAX_CHUNK, $SYMBOL_Z_MAX_CHUNK, $Z_MAX_CHUNK_CONST",
    "Z_DEFAULT_CHUNK, $SYMBOL_Z_DEFAULT_CHUNK, $Z_DEFAULT_CHUNK_CONST",
    "Z_MIN_MEMLEVEL, $SYMBOL_Z_MIN_MEMLEVEL, $Z_MIN_MEMLEVEL_CONST",
    "Z_MAX_MEMLEVEL, $SYMBOL_Z_MAX_MEMLEVEL, $Z_MAX_MEMLEVEL_CONST",
    "Z_DEFAULT_MEMLEVEL, $SYMBOL_Z_DEFAULT_MEMLEVEL, $Z_DEFAULT_MEMLEVEL_CONST",
    "Z_MIN_LEVEL, $SYMBOL_Z_MIN_LEVEL, $Z_MIN_LEVEL_CONST",
    "Z_MAX_LEVEL, $SYMBOL_Z_MAX_LEVEL, $Z_MAX_LEVEL_CONST",
    "Z_DEFAULT_LEVEL, $SYMBOL_Z_DEFAULT_LEVEL, $Z_DEFAULT_LEVEL_CONST"
  )
  @ParameterizedTest
  fun `zlib constants - zlib symbol alignment`(expectedSymbol: String, symbol: String, expectedValue: Int) {
    val constants = assertNotNull(zlib.provide().constants)
    assertEquals(expectedSymbol, symbol)
    assertIs<NodeZlibConstants>(constants)
    assertTrue(constants.hasMember(symbol))
    assertNotNull(constants.getMember(symbol))
    assertIs<Int>(assertNotNull(constants.getMember(symbol)))
    assertEquals(expectedValue, assertNotNull(constants.getMember(symbol)))
  }

  @CsvSource(
    SYMBOL_Z_NO_FLUSH,
    SYMBOL_Z_PARTIAL_FLUSH,
    SYMBOL_Z_SYNC_FLUSH,
    SYMBOL_Z_FULL_FLUSH,
    SYMBOL_Z_FINISH,
    SYMBOL_Z_BLOCK,
    SYMBOL_Z_OK,
    SYMBOL_Z_STREAM_END,
    SYMBOL_Z_NEED_DICT,
    SYMBOL_Z_ERRNO,
    SYMBOL_Z_STREAM_ERROR,
    SYMBOL_Z_DATA_ERROR,
    SYMBOL_Z_MEM_ERROR,
    SYMBOL_Z_BUF_ERROR,
    SYMBOL_Z_VERSION_ERROR,
    SYMBOL_Z_NO_COMPRESSION,
    SYMBOL_Z_BEST_SPEED,
    SYMBOL_Z_BEST_COMPRESSION,
    SYMBOL_Z_DEFAULT_COMPRESSION,
    SYMBOL_Z_FILTERED,
    SYMBOL_Z_HUFFMAN_ONLY,
    SYMBOL_Z_RLE,
    SYMBOL_Z_FIXED,
    SYMBOL_Z_DEFAULT_STRATEGY,
    SYMBOL_ZLIB_VERNUM,
    SYMBOL_DEFLATE,
    SYMBOL_INFLATE,
    SYMBOL_GZIP,
    SYMBOL_GUNZIP,
    SYMBOL_DEFLATERAW,
    SYMBOL_INFLATERAW,
    SYMBOL_UNZIP,
    SYMBOL_Z_MIN_WINDOWBITS,
    SYMBOL_Z_MAX_WINDOWBITS,
    SYMBOL_Z_DEFAULT_WINDOWBITS,
    SYMBOL_Z_MIN_CHUNK,
    SYMBOL_Z_MAX_CHUNK,
    SYMBOL_Z_DEFAULT_CHUNK,
    SYMBOL_Z_MIN_MEMLEVEL,
    SYMBOL_Z_MAX_MEMLEVEL,
    SYMBOL_Z_DEFAULT_MEMLEVEL,
    SYMBOL_Z_MIN_LEVEL,
    SYMBOL_Z_MAX_LEVEL,
    SYMBOL_Z_DEFAULT_LEVEL
  )
  @ParameterizedTest fun `zlib options - guest constants`(symbol: String) = conforms {
    assertNotNull(obtain().constants, "should not get `null` from `zlib.constants`")
  }.guest {
    // language=javascript
    """
      const { ok } = require("assert");
      const { constants } = require("zlib");
      ok(!!constants);
      ok(typeof constants === "object");
      ok(typeof constants.${symbol} === "number");
    """
  }

  @CsvSource(
    SYMBOL_BROTLI_OPERATION_PROCESS,
    SYMBOL_BROTLI_OPERATION_FLUSH,
    SYMBOL_BROTLI_OPERATION_FINISH,
    SYMBOL_BROTLI_OPERATION_EMIT_METADATA,
    SYMBOL_BROTLI_PARAM_MODE,
    SYMBOL_BROTLI_MODE_GENERIC,
    SYMBOL_BROTLI_MODE_TEXT,
    SYMBOL_BROTLI_MODE_FONT,
    SYMBOL_BROTLI_DEFAULT_MODE,
    SYMBOL_BROTLI_PARAM_QUALITY,
    SYMBOL_BROTLI_MIN_QUALITY,
    SYMBOL_BROTLI_MAX_QUALITY,
    SYMBOL_BROTLI_DEFAULT_QUALITY,
    SYMBOL_BROTLI_PARAM_LGWIN,
    SYMBOL_BROTLI_MIN_WINDOW_BITS,
    SYMBOL_BROTLI_MAX_WINDOW_BITS,
    SYMBOL_BROTLI_LARGE_MAX_WINDOW_BITS,
    SYMBOL_BROTLI_DEFAULT_WINDOW,
    SYMBOL_BROTLI_PARAM_LGBLOCK,
    SYMBOL_BROTLI_MIN_INPUT_BLOCK_BITS,
    SYMBOL_BROTLI_MAX_INPUT_BLOCK_BITS,
    SYMBOL_BROTLI_PARAM_DISABLE_LITERAL_CONTEXT_MODELING,
    SYMBOL_BROTLI_PARAM_SIZE_HINT,
    SYMBOL_BROTLI_PARAM_LARGE_WINDOW,
    SYMBOL_BROTLI_PARAM_NPOSTFIX,
    SYMBOL_BROTLI_PARAM_NDIRECT,
    SYMBOL_BROTLI_DECODER_RESULT_ERROR,
    SYMBOL_BROTLI_DECODER_RESULT_SUCCESS,
    SYMBOL_BROTLI_DECODER_RESULT_NEEDS_MORE_INPUT,
    SYMBOL_BROTLI_DECODER_RESULT_NEEDS_MORE_OUTPUT,
    SYMBOL_BROTLI_DECODER_PARAM_DISABLE_RING_BUFFER_REALLOCATION,
    SYMBOL_BROTLI_DECODER_PARAM_LARGE_WINDOW,
    SYMBOL_BROTLI_DECODER_NO_ERROR,
    SYMBOL_BROTLI_DECODER_SUCCESS,
    SYMBOL_BROTLI_DECODER_NEEDS_MORE_INPUT,
    SYMBOL_BROTLI_DECODER_NEEDS_MORE_OUTPUT,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_EXUBERANT_NIBBLE,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_RESERVED,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_EXUBERANT_META_NIBBLE,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_SIMPLE_HUFFMAN_ALPHABET,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_SIMPLE_HUFFMAN_SAME,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_CL_SPACE,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_HUFFMAN_SPACE,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_CONTEXT_MAP_REPEAT,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_BLOCK_LENGTH_1,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_BLOCK_LENGTH_2,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_TRANSFORM,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_DICTIONARY,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_WINDOW_BITS,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_PADDING_1,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_PADDING_2,
    SYMBOL_BROTLI_DECODER_ERROR_FORMAT_DISTANCE,
    SYMBOL_BROTLI_DECODER_ERROR_DICTIONARY_NOT_SET,
    SYMBOL_BROTLI_DECODER_ERROR_INVALID_ARGUMENTS,
    SYMBOL_BROTLI_DECODER_ERROR_ALLOC_CONTEXT_MODES,
    SYMBOL_BROTLI_DECODER_ERROR_ALLOC_TREE_GROUPS,
    SYMBOL_BROTLI_DECODER_ERROR_ALLOC_CONTEXT_MAP,
    SYMBOL_BROTLI_DECODER_ERROR_ALLOC_RING_BUFFER_1,
    SYMBOL_BROTLI_DECODER_ERROR_ALLOC_RING_BUFFER_2,
    SYMBOL_BROTLI_DECODER_ERROR_ALLOC_BLOCK_TYPE_TREES,
    SYMBOL_BROTLI_DECODER_ERROR_UNREACHABLE
  )
  @ParameterizedTest fun `brotli options - guest constants`(symbol: String) = conforms {
    assertNotNull(obtain().constants, "should not get `null` from `zlib.constants`")
  }.guest {
    // language=javascript
    """
      const { ok } = require("assert");
      const { constants } = require("zlib");
      ok(!!constants);
      ok(typeof constants === "object");
      ok(typeof constants.${symbol} === "number");
    """
  }

  @CsvSource(
    "BROTLI_OPERATION_PROCESS, $SYMBOL_BROTLI_OPERATION_PROCESS, $BROTLI_OPERATION_PROCESS_CONST",
    "BROTLI_OPERATION_FLUSH, $SYMBOL_BROTLI_OPERATION_FLUSH, $BROTLI_OPERATION_FLUSH_CONST",
    "BROTLI_OPERATION_FINISH, $SYMBOL_BROTLI_OPERATION_FINISH, $BROTLI_OPERATION_FINISH_CONST",
    "BROTLI_OPERATION_EMIT_METADATA, $SYMBOL_BROTLI_OPERATION_EMIT_METADATA, $BROTLI_OPERATION_EMIT_METADATA_CONST",
    "BROTLI_PARAM_MODE, $SYMBOL_BROTLI_PARAM_MODE, $BROTLI_PARAM_MODE_CONST",
    "BROTLI_MODE_GENERIC, $SYMBOL_BROTLI_MODE_GENERIC, $BROTLI_MODE_GENERIC_CONST",
    "BROTLI_MODE_TEXT, $SYMBOL_BROTLI_MODE_TEXT, $BROTLI_MODE_TEXT_CONST",
    "BROTLI_MODE_FONT, $SYMBOL_BROTLI_MODE_FONT, $BROTLI_MODE_FONT_CONST",
    "BROTLI_DEFAULT_MODE, $SYMBOL_BROTLI_DEFAULT_MODE, $BROTLI_DEFAULT_MODE_CONST",
    "BROTLI_PARAM_QUALITY, $SYMBOL_BROTLI_PARAM_QUALITY, $BROTLI_PARAM_QUALITY_CONST",
    "BROTLI_MIN_QUALITY, $SYMBOL_BROTLI_MIN_QUALITY, $BROTLI_MIN_QUALITY_CONST",
    "BROTLI_MAX_QUALITY, $SYMBOL_BROTLI_MAX_QUALITY, $BROTLI_MAX_QUALITY_CONST",
    "BROTLI_DEFAULT_QUALITY, $SYMBOL_BROTLI_DEFAULT_QUALITY, $BROTLI_DEFAULT_QUALITY_CONST",
    "BROTLI_PARAM_LGWIN, $SYMBOL_BROTLI_PARAM_LGWIN, $BROTLI_PARAM_LGWIN_CONST",
    "BROTLI_MIN_WINDOW_BITS, $SYMBOL_BROTLI_MIN_WINDOW_BITS, $BROTLI_MIN_WINDOW_BITS_CONST",
    "BROTLI_MAX_WINDOW_BITS, $SYMBOL_BROTLI_MAX_WINDOW_BITS, $BROTLI_MAX_WINDOW_BITS_CONST",
    "BROTLI_LARGE_MAX_WINDOW_BITS, $SYMBOL_BROTLI_LARGE_MAX_WINDOW_BITS, $BROTLI_LARGE_MAX_WINDOW_BITS_CONST",
    "BROTLI_DEFAULT_WINDOW, $SYMBOL_BROTLI_DEFAULT_WINDOW, $BROTLI_DEFAULT_WINDOW_CONST",
    "BROTLI_PARAM_LGBLOCK, $SYMBOL_BROTLI_PARAM_LGBLOCK, $BROTLI_PARAM_LGBLOCK_CONST",
    "BROTLI_MIN_INPUT_BLOCK_BITS, $SYMBOL_BROTLI_MIN_INPUT_BLOCK_BITS, $BROTLI_MIN_INPUT_BLOCK_BITS_CONST",
    "BROTLI_MAX_INPUT_BLOCK_BITS, $SYMBOL_BROTLI_MAX_INPUT_BLOCK_BITS, $BROTLI_MAX_INPUT_BLOCK_BITS_CONST",
    "BROTLI_PARAM_DISABLE_LITERAL_CONTEXT_MODELING, $SYMBOL_BROTLI_PARAM_DISABLE_LITERAL_CONTEXT_MODELING, $BROTLI_PARAM_DISABLE_LITERAL_CONTEXT_MODELING_CONST",
    "BROTLI_PARAM_SIZE_HINT, $SYMBOL_BROTLI_PARAM_SIZE_HINT, $BROTLI_PARAM_SIZE_HINT_CONST",
    "BROTLI_PARAM_LARGE_WINDOW, $SYMBOL_BROTLI_PARAM_LARGE_WINDOW, $BROTLI_PARAM_LARGE_WINDOW_CONST",
    "BROTLI_PARAM_NPOSTFIX, $SYMBOL_BROTLI_PARAM_NPOSTFIX, $BROTLI_PARAM_NPOSTFIX_CONST",
    "BROTLI_PARAM_NDIRECT, $SYMBOL_BROTLI_PARAM_NDIRECT, $BROTLI_PARAM_NDIRECT_CONST",
    "BROTLI_DECODER_RESULT_ERROR, $SYMBOL_BROTLI_DECODER_RESULT_ERROR, $BROTLI_DECODER_RESULT_ERROR_CONST",
    "BROTLI_DECODER_RESULT_SUCCESS, $SYMBOL_BROTLI_DECODER_RESULT_SUCCESS, $BROTLI_DECODER_RESULT_SUCCESS_CONST",
    "BROTLI_DECODER_RESULT_NEEDS_MORE_INPUT, $SYMBOL_BROTLI_DECODER_RESULT_NEEDS_MORE_INPUT, $BROTLI_DECODER_RESULT_NEEDS_MORE_INPUT_CONST",
    "BROTLI_DECODER_RESULT_NEEDS_MORE_OUTPUT, $SYMBOL_BROTLI_DECODER_RESULT_NEEDS_MORE_OUTPUT, $BROTLI_DECODER_RESULT_NEEDS_MORE_OUTPUT_CONST",
    "BROTLI_DECODER_PARAM_DISABLE_RING_BUFFER_REALLOCATION, $SYMBOL_BROTLI_DECODER_PARAM_DISABLE_RING_BUFFER_REALLOCATION, $BROTLI_DECODER_PARAM_DISABLE_RING_BUFFER_REALLOCATION_CONST",
    "BROTLI_DECODER_PARAM_LARGE_WINDOW, $SYMBOL_BROTLI_DECODER_PARAM_LARGE_WINDOW, $BROTLI_DECODER_PARAM_LARGE_WINDOW_CONST",
    "BROTLI_DECODER_NO_ERROR, $SYMBOL_BROTLI_DECODER_NO_ERROR, $BROTLI_DECODER_NO_ERROR_CONST",
    "BROTLI_DECODER_SUCCESS, $SYMBOL_BROTLI_DECODER_SUCCESS, $BROTLI_DECODER_SUCCESS_CONST",
    "BROTLI_DECODER_NEEDS_MORE_INPUT, $SYMBOL_BROTLI_DECODER_NEEDS_MORE_INPUT, $BROTLI_DECODER_NEEDS_MORE_INPUT_CONST",
    "BROTLI_DECODER_NEEDS_MORE_OUTPUT, $SYMBOL_BROTLI_DECODER_NEEDS_MORE_OUTPUT, $BROTLI_DECODER_NEEDS_MORE_OUTPUT_CONST",
    "BROTLI_DECODER_ERROR_FORMAT_EXUBERANT_NIBBLE, $SYMBOL_BROTLI_DECODER_ERROR_FORMAT_EXUBERANT_NIBBLE, $BROTLI_DECODER_ERROR_FORMAT_EXUBERANT_NIBBLE_CONST",
    "BROTLI_DECODER_ERROR_FORMAT_RESERVED, $SYMBOL_BROTLI_DECODER_ERROR_FORMAT_RESERVED, $BROTLI_DECODER_ERROR_FORMAT_RESERVED_CONST",
    "BROTLI_DECODER_ERROR_FORMAT_EXUBERANT_META_NIBBLE, $SYMBOL_BROTLI_DECODER_ERROR_FORMAT_EXUBERANT_META_NIBBLE, $BROTLI_DECODER_ERROR_FORMAT_EXUBERANT_META_NIBBLE_CONST",
    "BROTLI_DECODER_ERROR_FORMAT_SIMPLE_HUFFMAN_ALPHABET, $SYMBOL_BROTLI_DECODER_ERROR_FORMAT_SIMPLE_HUFFMAN_ALPHABET, $BROTLI_DECODER_ERROR_FORMAT_SIMPLE_HUFFMAN_ALPHABET_CONST",
    "BROTLI_DECODER_ERROR_FORMAT_SIMPLE_HUFFMAN_SAME, $SYMBOL_BROTLI_DECODER_ERROR_FORMAT_SIMPLE_HUFFMAN_SAME, $BROTLI_DECODER_ERROR_FORMAT_SIMPLE_HUFFMAN_SAME_CONST",
    "BROTLI_DECODER_ERROR_FORMAT_CL_SPACE, $SYMBOL_BROTLI_DECODER_ERROR_FORMAT_CL_SPACE, $BROTLI_DECODER_ERROR_FORMAT_CL_SPACE_CONST",
    "BROTLI_DECODER_ERROR_FORMAT_HUFFMAN_SPACE, $SYMBOL_BROTLI_DECODER_ERROR_FORMAT_HUFFMAN_SPACE, $BROTLI_DECODER_ERROR_FORMAT_HUFFMAN_SPACE_CONST",
    "BROTLI_DECODER_ERROR_FORMAT_CONTEXT_MAP_REPEAT, $SYMBOL_BROTLI_DECODER_ERROR_FORMAT_CONTEXT_MAP_REPEAT, $BROTLI_DECODER_ERROR_FORMAT_CONTEXT_MAP_REPEAT_CONST",
    "BROTLI_DECODER_ERROR_FORMAT_BLOCK_LENGTH_1, $SYMBOL_BROTLI_DECODER_ERROR_FORMAT_BLOCK_LENGTH_1, $BROTLI_DECODER_ERROR_FORMAT_BLOCK_LENGTH_1_CONST",
    "BROTLI_DECODER_ERROR_FORMAT_BLOCK_LENGTH_2, $SYMBOL_BROTLI_DECODER_ERROR_FORMAT_BLOCK_LENGTH_2, $BROTLI_DECODER_ERROR_FORMAT_BLOCK_LENGTH_2_CONST",
    "BROTLI_DECODER_ERROR_FORMAT_TRANSFORM, $SYMBOL_BROTLI_DECODER_ERROR_FORMAT_TRANSFORM, $BROTLI_DECODER_ERROR_FORMAT_TRANSFORM_CONST",
    "BROTLI_DECODER_ERROR_FORMAT_DICTIONARY, $SYMBOL_BROTLI_DECODER_ERROR_FORMAT_DICTIONARY, $BROTLI_DECODER_ERROR_FORMAT_DICTIONARY_CONST",
    "BROTLI_DECODER_ERROR_FORMAT_WINDOW_BITS, $SYMBOL_BROTLI_DECODER_ERROR_FORMAT_WINDOW_BITS, $BROTLI_DECODER_ERROR_FORMAT_WINDOW_BITS_CONST",
    "BROTLI_DECODER_ERROR_FORMAT_PADDING_1, $SYMBOL_BROTLI_DECODER_ERROR_FORMAT_PADDING_1, $BROTLI_DECODER_ERROR_FORMAT_PADDING_1_CONST",
    "BROTLI_DECODER_ERROR_FORMAT_PADDING_2, $SYMBOL_BROTLI_DECODER_ERROR_FORMAT_PADDING_2, $BROTLI_DECODER_ERROR_FORMAT_PADDING_2_CONST",
    "BROTLI_DECODER_ERROR_FORMAT_DISTANCE, $SYMBOL_BROTLI_DECODER_ERROR_FORMAT_DISTANCE, $BROTLI_DECODER_ERROR_FORMAT_DISTANCE_CONST",
    "BROTLI_DECODER_ERROR_DICTIONARY_NOT_SET, $SYMBOL_BROTLI_DECODER_ERROR_DICTIONARY_NOT_SET, $BROTLI_DECODER_ERROR_DICTIONARY_NOT_SET_CONST",
    "BROTLI_DECODER_ERROR_INVALID_ARGUMENTS, $SYMBOL_BROTLI_DECODER_ERROR_INVALID_ARGUMENTS, $BROTLI_DECODER_ERROR_INVALID_ARGUMENTS_CONST",
    "BROTLI_DECODER_ERROR_ALLOC_CONTEXT_MODES, $SYMBOL_BROTLI_DECODER_ERROR_ALLOC_CONTEXT_MODES, $BROTLI_DECODER_ERROR_ALLOC_CONTEXT_MODES_CONST",
    "BROTLI_DECODER_ERROR_ALLOC_TREE_GROUPS, $SYMBOL_BROTLI_DECODER_ERROR_ALLOC_TREE_GROUPS, $BROTLI_DECODER_ERROR_ALLOC_TREE_GROUPS_CONST",
    "BROTLI_DECODER_ERROR_ALLOC_CONTEXT_MAP, $SYMBOL_BROTLI_DECODER_ERROR_ALLOC_CONTEXT_MAP, $BROTLI_DECODER_ERROR_ALLOC_CONTEXT_MAP_CONST",
    "BROTLI_DECODER_ERROR_ALLOC_RING_BUFFER_1, $SYMBOL_BROTLI_DECODER_ERROR_ALLOC_RING_BUFFER_1, $BROTLI_DECODER_ERROR_ALLOC_RING_BUFFER_1_CONST",
    "BROTLI_DECODER_ERROR_ALLOC_RING_BUFFER_2, $SYMBOL_BROTLI_DECODER_ERROR_ALLOC_RING_BUFFER_2, $BROTLI_DECODER_ERROR_ALLOC_RING_BUFFER_2_CONST",
    "BROTLI_DECODER_ERROR_ALLOC_BLOCK_TYPE_TREES, $SYMBOL_BROTLI_DECODER_ERROR_ALLOC_BLOCK_TYPE_TREES, $BROTLI_DECODER_ERROR_ALLOC_BLOCK_TYPE_TREES_CONST",
    "BROTLI_DECODER_ERROR_UNREACHABLE, $SYMBOL_BROTLI_DECODER_ERROR_UNREACHABLE, $BROTLI_DECODER_ERROR_UNREACHABLE_CONST"
  )
  @ParameterizedTest
  fun `brotli constants - brotli symbol alignment`(expectedSymbol: String, symbol: String, expectedValue: Int) {
    val constants = assertNotNull(zlib.provide().constants)
    assertEquals(expectedSymbol, symbol)
    assertIs<NodeZlibConstants>(constants)
    assertTrue(constants.hasMember(symbol))
    assertNotNull(constants.getMember(symbol))
    assertIs<Int>(assertNotNull(constants.getMember(symbol)))
    assertEquals(expectedValue, assertNotNull(constants.getMember(symbol)))
  }

  @Test fun `zlib options - create defaults`() {
    assertNotNull(ImmutableZlibOptions.defaults())
    assertSame(ImmutableZlibOptions.defaults(), ImmutableZlibOptions.defaults())
    assertNotNull(MutableZlibOptions.defaults())
    assertNotSame(MutableZlibOptions.defaults(), MutableZlibOptions.defaults())
  }

  @Test fun `zlib - crc32 string`() = dual {
    assertEquals(1486392595, assertNotNull(obtain().crc32("hello, world!".toByteArray(StandardCharsets.UTF_8))))
    assertEquals(3196917353, assertNotNull(obtain().crc32("elide test".toByteArray(StandardCharsets.UTF_8))))
  }.guest {
    // language=javascript
    """
      const { crc32 } = require("zlib");
      const { ok, equal } = require("assert");
      ok(!!crc32);
      ok(typeof crc32 === "function");
      equal(crc32("hello, world!"), 1486392595);
      equal(crc32("elide test"), 3196917353);
    """
  }

  @Test fun `zlib - crc32 bytearray`() = dual {
    assertEquals(1486392595, assertNotNull(obtain().crc32("hello, world!".toByteArray(StandardCharsets.UTF_8))))
    assertEquals(3196917353, assertNotNull(obtain().crc32("elide test".toByteArray(StandardCharsets.UTF_8))))
  }.guest {
    // language=javascript
    """
      const { crc32 } = require("zlib");
      const { ok, equal } = require("assert");
      ok(!!crc32);
      ok(typeof crc32 === "function");
      const sample1 = "hello, world!"
      const test1 = Uint8Array.from(Array.from(sample1).map(letter => letter.charCodeAt(0)));
      const sample2 = "elide test"
      const test2 = Uint8Array.from(Array.from(sample2).map(letter => letter.charCodeAt(0)));
      equal(crc32(test1), 1486392595);
      equal(crc32(test2), 3196917353);
    """
  }

  @Test fun `zlib - crc32 buffer`() = dual {
    assertEquals(1486392595, assertNotNull(obtain().crc32("hello, world!".toByteArray(StandardCharsets.UTF_8))))
    assertEquals(3196917353, assertNotNull(obtain().crc32("elide test".toByteArray(StandardCharsets.UTF_8))))
  }.guest {
    // language=javascript
    """
      const { crc32 } = require("zlib");
      const { ok, equal } = require("assert");
      ok(!!crc32);
      ok(typeof crc32 === "function");
      const sample1 = "hello, world!"
      const test1 = Buffer.from(sample1);
      const sample2 = "elide test"
      const test2 = Buffer.from(sample2);
      equal(crc32(test1), 1486392595);
      equal(crc32(test2), 3196917353);
    """
  }

  @Test fun `zlib - createDeflate`() = dual {
    assertNotNull(obtain().createDeflate())
    assertNotNull(obtain().createDeflate(ImmutableZlibOptions.defaults()))
  }.guest {
    // language=javascript
    """
      const { createDeflate } = require("zlib");
      const { ok } = require("assert");
      ok(!!createDeflate);
      ok(typeof createDeflate === "function");
      ok(!!createDeflate());
      ok(!!createDeflate({}));
    """
  }

  @Test fun `zlib - createInflate`() = dual {
    assertNotNull(obtain().createInflate())
    assertNotNull(obtain().createInflate(ImmutableZlibOptions.defaults()))
  }.guest {
    // language=javascript
    """
      const { createInflate } = require("zlib");
      const { ok } = require("assert");
      ok(!!createInflate);
      ok(typeof createInflate === "function");
      ok(!!createInflate());
      ok(!!createInflate({}));
    """
  }

  @Test fun `zlib - gzipSync`() = dual {
    val sample = "hello, hello, hello, hello, hello"
    val input = ByteBuffer.wrap(sample.toByteArray(StandardCharsets.UTF_8))
    val compressed = assertNotNull(obtain().gzipSync(input))
    val decompressed = assertNotNull(obtain().gunzipSync(compressed))
    val right = decompressed.array()
    assertNotEquals(input, compressed)
    assertEquals(sample, String(right, StandardCharsets.UTF_8))
  }.guest {
    // language=javascript
    """
      const { gzipSync, gunzipSync } = require("zlib");
      const { ok, equal } = require("assert");
      const sample = "hello, hello, hello, hello, hello";
      const input = Buffer.from(sample);
      const compressed = gzipSync(input);
      const decompressed = Buffer.from(gunzipSync(compressed));
      const right = decompressed.toString();
      ok(!!compressed);
      ok(!!decompressed);
      ok(!!right);
      equal(right, sample);
    """
  }

  @Test fun `zlib - deflateSync`() = dual {
    val sample = "hello, hello, hello, hello, hello"
    val input = ByteBuffer.wrap(sample.toByteArray(StandardCharsets.UTF_8))
    val compressed = assertNotNull(obtain().deflateSync(input))
    val decompressed = assertNotNull(obtain().inflateSync(compressed))
    val right = decompressed.array()
    assertNotEquals(input, compressed)
    assertEquals(sample, String(right, StandardCharsets.UTF_8))
  }.guest {
    // language=javascript
    """
      const { deflateSync, inflateSync } = require("zlib");
      const { ok, equal } = require("assert");
      const sample = "hello, hello, hello, hello, hello";
      const input = Buffer.from(sample);
      const compressed = deflateSync(input);
      const decompressed = Buffer.from(inflateSync(compressed));
      const right = decompressed.toString();
      ok(!!compressed);
      ok(!!decompressed);
      ok(!!right);
      equal(right, sample);
    """
  }

  @Test fun `zlib - unzipSync`() = dual {
    val sample = "hello, hello, hello, hello, hello"
    val input = ByteBuffer.wrap(sample.toByteArray(StandardCharsets.UTF_8))
    val compressed = assertNotNull(obtain().gzipSync(input))
    val decompressed = assertNotNull(obtain().unzipSync(compressed))
    val right = decompressed.array()
    assertNotEquals(input, compressed)
    assertEquals(sample, String(right, StandardCharsets.UTF_8))
  }.guest {
    // language=javascript
    """
      const { gzipSync, unzipSync } = require("zlib");
      const { ok, equal } = require("assert");
      const sample = "hello, hello, hello, hello, hello";
      const input = Buffer.from(sample);
      const compressed = gzipSync(input);
      const decompressed = Buffer.from(unzipSync(compressed));
      const right = decompressed.toString();
      ok(!!compressed);
      ok(!!decompressed);
      ok(!!right);
      equal(right, sample);
    """
  }

  @Test fun `zlib - brotliCompressSync`() = dual {
    val sample = "hello, hello, hello, hello, hello"
    val input = ByteBuffer.wrap(sample.toByteArray(StandardCharsets.UTF_8))
    assertNotNull(obtain().brotliCompressSync(input))
  }.guest {
    // language=javascript
    """
      const { brotliCompressSync } = require("zlib");
      const { ok } = require("assert");
      const sample = "hello, hello, hello, hello, hello";
      const input = Buffer.from(sample);
      const compressed = brotliCompressSync(input);
      ok(!!compressed);
    """
  }

  @Test fun `zlib - brotliDecompressSync`() = dual {
    val sample = "hello, hello, hello, hello, hello"
    val input = ByteBuffer.wrap(sample.toByteArray(StandardCharsets.UTF_8))
    val compressed = assertNotNull(obtain().brotliCompressSync(input))
    val decompressed = assertNotNull(obtain().brotliDecompressSync(compressed))
    val right = decompressed.array()
    assertNotEquals(input, compressed)
    assertEquals(sample, String(right, StandardCharsets.UTF_8))
  }.guest {
    // language=javascript
    """
      const { brotliCompressSync, brotliDecompressSync } = require("zlib");
      const { ok, equal } = require("assert");
      const sample = "hello, hello, hello, hello, hello";
      const input = Buffer.from(sample);
      const compressed = brotliCompressSync(input);
      const decompressed = Buffer.from(brotliDecompressSync(compressed));
      const right = decompressed.toString();
      ok(!!compressed);
      ok(!!decompressed);
      ok(!!right);
      equal(right, sample);
    """
  }

  @Test fun `zlib - deflate`() {
    val sample = "hello, hello, hello, hello, hello"
    val input = ByteBuffer.wrap(sample.toByteArray(StandardCharsets.UTF_8))
    val didExec = AtomicBoolean(false)

    // simple deflate with callback
    obtain().deflate(input) { err, result ->
      assertFalse(didExec.get())
      didExec.compareAndSet(false, true)
      assertNull(err)
      assertNotNull(result)
      val decompressed = obtain().inflateSync(result)
      val right = decompressed.array()
      assertEquals(sample, String(right, StandardCharsets.UTF_8))
    }
    assertTrue(didExec.get())
  }

  @Test fun `zlib - inflate`() {
    val sample = "hello, hello, hello, hello, hello"
    val input = ByteBuffer.wrap(sample.toByteArray(StandardCharsets.UTF_8))
    val didExec = AtomicBoolean(false)

    // simple deflate with callback
    val compressed = obtain().deflateSync(input)
    obtain().inflate(compressed) { err, result ->
      assertFalse(didExec.get())
      didExec.compareAndSet(false, true)
      assertNull(err)
      assertNotNull(result)
      val right = result.array()
      assertEquals(sample, String(right, StandardCharsets.UTF_8))
    }
    assertTrue(didExec.get())
  }

  @Test fun `zlib - gzip`() {
    val sample = "hello, hello, hello, hello, hello"
    val input = ByteBuffer.wrap(sample.toByteArray(StandardCharsets.UTF_8))
    val didExec = AtomicBoolean(false)

    // simple deflate with callback
    obtain().gzip(input) { err, result ->
      assertFalse(didExec.get())
      didExec.compareAndSet(false, true)
      assertNull(err)
      assertNotNull(result)
      val decompressed = obtain().gunzipSync(result)
      val right = decompressed.array()
      assertEquals(sample, String(right, StandardCharsets.UTF_8))
    }
    assertTrue(didExec.get())
  }

  @Test fun `zlib - gunzip`() {
    val sample = "hello, hello, hello, hello, hello"
    val input = ByteBuffer.wrap(sample.toByteArray(StandardCharsets.UTF_8))
    val didExec = AtomicBoolean(false)

    // simple deflate with callback
    val compressed = obtain().gzipSync(input)
    obtain().gunzip(compressed) { err, result ->
      assertFalse(didExec.get())
      didExec.compareAndSet(false, true)
      assertNull(err)
      assertNotNull(result)
      val right = result.array()
      assertEquals(sample, String(right, StandardCharsets.UTF_8))
    }
    assertTrue(didExec.get())
  }

  @Test fun `zlib - unzip`() {
    val sample = "hello, hello, hello, hello, hello"
    val input = ByteBuffer.wrap(sample.toByteArray(StandardCharsets.UTF_8))
    val didExec = AtomicBoolean(false)

    // simple deflate with callback
    val compressed = obtain().gzipSync(input)
    obtain().unzip(compressed) { err, result ->
      assertFalse(didExec.get())
      didExec.compareAndSet(false, true)
      assertNull(err)
      assertNotNull(result)
      val right = result.array()
      assertEquals(sample, String(right, StandardCharsets.UTF_8))
    }
    assertTrue(didExec.get())
  }

  @Test fun `zlib - brotliCompress`() {
    val sample = "hello, hello, hello, hello, hello"
    val input = ByteBuffer.wrap(sample.toByteArray(StandardCharsets.UTF_8))
    val didExec = AtomicBoolean(false)

    // simple deflate with callback
    obtain().brotliCompress(input) { err, result ->
      assertFalse(didExec.get())
      didExec.compareAndSet(false, true)
      assertNull(err)
      assertNotNull(result)
      val decompressed = obtain().brotliDecompressSync(result)
      val right = decompressed.array()
      assertEquals(sample, String(right, StandardCharsets.UTF_8))
    }
    assertTrue(didExec.get())
  }

  @Test fun `zlib - brotliDecompress`() {
    val sample = "hello, hello, hello, hello, hello"
    val input = ByteBuffer.wrap(sample.toByteArray(StandardCharsets.UTF_8))
    val didExec = AtomicBoolean(false)

    // simple deflate with callback
    val compressed = obtain().brotliCompressSync(input)
    obtain().brotliDecompress(compressed) { err, result ->
      assertFalse(didExec.get())
      didExec.compareAndSet(false, true)
      assertNull(err)
      assertNotNull(result)
      val right = result.array()
      assertEquals(sample, String(right, StandardCharsets.UTF_8))
    }
    assertTrue(didExec.get())
  }
}

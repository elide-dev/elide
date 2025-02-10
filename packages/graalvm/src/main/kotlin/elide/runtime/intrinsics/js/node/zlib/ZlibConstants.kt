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
@file:Suppress("PropertyName", "VariableNaming")

package elide.runtime.intrinsics.js.node.zlib

import org.graalvm.polyglot.proxy.ProxyObject
import elide.annotations.API
import elide.vm.annotations.Polyglot

/** Symbolic constants for `Z_NO_FLUSH`. */
public const val SYMBOL_Z_NO_FLUSH: String = "Z_NO_FLUSH"
public const val Z_NO_FLUSH_CONST: Int = 0

/** Symbolic constants for `Z_PARTIAL_FLUSH`. */
public const val SYMBOL_Z_PARTIAL_FLUSH: String = "Z_PARTIAL_FLUSH"
public const val Z_PARTIAL_FLUSH_CONST: Int = 1

/** Symbolic constants for `Z_SYNC_FLUSH`. */
public const val SYMBOL_Z_SYNC_FLUSH: String = "Z_SYNC_FLUSH"
public const val Z_SYNC_FLUSH_CONST: Int = 2

/** Symbolic constants for `Z_FULL_FLUSH`. */
public const val SYMBOL_Z_FULL_FLUSH: String = "Z_FULL_FLUSH"
public const val Z_FULL_FLUSH_CONST: Int = 3

/** Symbolic constants for `Z_FINISH`. */
public const val SYMBOL_Z_FINISH: String = "Z_FINISH"
public const val Z_FINISH_CONST: Int = 4

/** Symbolic constants for `Z_BLOCK`. */
public const val SYMBOL_Z_BLOCK: String = "Z_BLOCK"
public const val Z_BLOCK_CONST: Int = 5

/** Symbolic constants for `Z_OK`. */
public const val SYMBOL_Z_OK: String = "Z_OK"
public const val Z_OK_CONST: Int = 0

/** Symbolic constants for `Z_STREAM_END`. */
public const val SYMBOL_Z_STREAM_END: String = "Z_STREAM_END"
public const val Z_STREAM_END_CONST: Int = 1

/** Symbolic constants for `Z_NEED_DICT`. */
public const val SYMBOL_Z_NEED_DICT: String = "Z_NEED_DICT"
public const val Z_NEED_DICT_CONST: Int = 2

/** Symbolic constants for `Z_ERRNO`. */
public const val SYMBOL_Z_ERRNO: String = "Z_ERRNO"
public const val Z_ERRNO_CONST: Int = -1

/** Symbolic constants for `Z_STREAM_ERROR`. */
public const val SYMBOL_Z_STREAM_ERROR: String = "Z_STREAM_ERROR"
public const val Z_STREAM_ERROR_CONST: Int = -2

/** Symbolic constants for `Z_DATA_ERROR`. */
public const val SYMBOL_Z_DATA_ERROR: String = "Z_DATA_ERROR"
public const val Z_DATA_ERROR_CONST: Int = -3

/** Symbolic constants for `Z_MEM_ERROR`. */
public const val SYMBOL_Z_MEM_ERROR: String = "Z_MEM_ERROR"
public const val Z_MEM_ERROR_CONST: Int = -4

/** Symbolic constants for `Z_BUF_ERROR`. */
public const val SYMBOL_Z_BUF_ERROR: String = "Z_BUF_ERROR"
public const val Z_BUF_ERROR_CONST: Int = -5

/** Symbolic constants for `Z_VERSION_ERROR`. */
public const val SYMBOL_Z_VERSION_ERROR: String = "Z_VERSION_ERROR"
public const val Z_VERSION_ERROR_CONST: Int = -6

/** Symbolic constants for `Z_NO_COMPRESSION`. */
public const val SYMBOL_Z_NO_COMPRESSION: String = "Z_NO_COMPRESSION"
public const val Z_NO_COMPRESSION_CONST: Int = 0

/** Symbolic constants for `Z_BEST_SPEED`. */
public const val SYMBOL_Z_BEST_SPEED: String = "Z_BEST_SPEED"
public const val Z_BEST_SPEED_CONST: Int = 1

/** Symbolic constants for `Z_BEST_COMPRESSION`. */
public const val SYMBOL_Z_BEST_COMPRESSION: String = "Z_BEST_COMPRESSION"
public const val Z_BEST_COMPRESSION_CONST: Int = 9

/** Symbolic constants for `Z_DEFAULT_COMPRESSION`. */
public const val SYMBOL_Z_DEFAULT_COMPRESSION: String = "Z_DEFAULT_COMPRESSION"
public const val Z_DEFAULT_COMPRESSION_CONST: Int = -1

/** Symbolic constants for `Z_FILTERED`. */
public const val SYMBOL_Z_FILTERED: String = "Z_FILTERED"
public const val Z_FILTERED_CONST: Int = 1

/** Symbolic constants for `Z_HUFFMAN_ONLY`. */
public const val SYMBOL_Z_HUFFMAN_ONLY: String = "Z_HUFFMAN_ONLY"
public const val Z_HUFFMAN_ONLY_CONST: Int = 2

/** Symbolic constants for `Z_RLE`. */
public const val SYMBOL_Z_RLE: String = "Z_RLE"
public const val Z_RLE_CONST: Int = 3

/** Symbolic constants for `Z_FIXED`. */
public const val SYMBOL_Z_FIXED: String = "Z_FIXED"
public const val Z_FIXED_CONST: Int = 4

/** Symbolic constants for `Z_DEFAULT_STRATEGY`. */
public const val SYMBOL_Z_DEFAULT_STRATEGY: String = "Z_DEFAULT_STRATEGY"
public const val Z_DEFAULT_STRATEGY_CONST: Int = 0

/** Symbolic constants for `ZLIB_VERNUM`. */
public const val SYMBOL_ZLIB_VERNUM: String = "ZLIB_VERNUM"
public const val ZLIB_VERNUM_CONST: Int = 4865

/** Symbolic constants for `DEFLATE`. */
public const val SYMBOL_DEFLATE: String = "DEFLATE"
public const val DEFLATE_CONST: Int = 1

/** Symbolic constants for `INFLATE`. */
public const val SYMBOL_INFLATE: String = "INFLATE"
public const val INFLATE_CONST: Int = 2

/** Symbolic constants for `GZIP`. */
public const val SYMBOL_GZIP: String = "GZIP"
public const val GZIP_CONST: Int = 3

/** Symbolic constants for `GUNZIP`. */
public const val SYMBOL_GUNZIP: String = "GUNZIP"
public const val GUNZIP_CONST: Int = 4

/** Symbolic constants for `DEFLATERAW`. */
public const val SYMBOL_DEFLATERAW: String = "DEFLATERAW"
public const val DEFLATERAW_CONST: Int = 5

/** Symbolic constants for `INFLATERAW`. */
public const val SYMBOL_INFLATERAW: String = "INFLATERAW"
public const val INFLATERAW_CONST: Int = 6

/** Symbolic constants for `UNZIP`. */
public const val SYMBOL_UNZIP: String = "UNZIP"
public const val UNZIP_CONST: Int = 7

/** Symbolic constants for `BROTLI_OPERATION_PROCESS`. */
public const val SYMBOL_BROTLI_OPERATION_PROCESS: String = "BROTLI_OPERATION_PROCESS"
public const val BROTLI_OPERATION_PROCESS_CONST: Int = 0

/** Symbolic constants for `BROTLI_OPERATION_FLUSH`. */
public const val SYMBOL_BROTLI_OPERATION_FLUSH: String = "BROTLI_OPERATION_FLUSH"
public const val BROTLI_OPERATION_FLUSH_CONST: Int = 1

/** Symbolic constants for `BROTLI_OPERATION_FINISH`. */
public const val SYMBOL_BROTLI_OPERATION_FINISH: String = "BROTLI_OPERATION_FINISH"
public const val BROTLI_OPERATION_FINISH_CONST: Int = 2

/** Symbolic constants for `BROTLI_OPERATION_EMIT_METADATA`. */
public const val SYMBOL_BROTLI_OPERATION_EMIT_METADATA: String = "BROTLI_OPERATION_EMIT_METADATA"
public const val BROTLI_OPERATION_EMIT_METADATA_CONST: Int = 3

/** Symbolic constants for `BROTLI_PARAM_MODE`. */
public const val SYMBOL_BROTLI_PARAM_MODE: String = "BROTLI_PARAM_MODE"
public const val BROTLI_PARAM_MODE_CONST: Int = 0

/** Symbolic constants for `BROTLI_MODE_GENERIC`. */
public const val SYMBOL_BROTLI_MODE_GENERIC: String = "BROTLI_MODE_GENERIC"
public const val BROTLI_MODE_GENERIC_CONST: Int = 0

/** Symbolic constants for `BROTLI_MODE_TEXT`. */
public const val SYMBOL_BROTLI_MODE_TEXT: String = "BROTLI_MODE_TEXT"
public const val BROTLI_MODE_TEXT_CONST: Int = 1

/** Symbolic constants for `BROTLI_MODE_FONT`. */
public const val SYMBOL_BROTLI_MODE_FONT: String = "BROTLI_MODE_FONT"
public const val BROTLI_MODE_FONT_CONST: Int = 2

/** Symbolic constants for `BROTLI_DEFAULT_MODE`. */
public const val SYMBOL_BROTLI_DEFAULT_MODE: String = "BROTLI_DEFAULT_MODE"
public const val BROTLI_DEFAULT_MODE_CONST: Int = 0

/** Symbolic constants for `BROTLI_PARAM_QUALITY`. */
public const val SYMBOL_BROTLI_PARAM_QUALITY: String = "BROTLI_PARAM_QUALITY"
public const val BROTLI_PARAM_QUALITY_CONST: Int = 1

/** Symbolic constants for `BROTLI_MIN_QUALITY`. */
public const val SYMBOL_BROTLI_MIN_QUALITY: String = "BROTLI_MIN_QUALITY"
public const val BROTLI_MIN_QUALITY_CONST: Int = 0

/** Symbolic constants for `BROTLI_MAX_QUALITY`. */
public const val SYMBOL_BROTLI_MAX_QUALITY: String = "BROTLI_MAX_QUALITY"
public const val BROTLI_MAX_QUALITY_CONST: Int = 11

/** Symbolic constants for `BROTLI_DEFAULT_QUALITY`. */
public const val SYMBOL_BROTLI_DEFAULT_QUALITY: String = "BROTLI_DEFAULT_QUALITY"
public const val BROTLI_DEFAULT_QUALITY_CONST: Int = 11

/** Symbolic constants for `BROTLI_PARAM_LGWIN`. */
public const val SYMBOL_BROTLI_PARAM_LGWIN: String = "BROTLI_PARAM_LGWIN"
public const val BROTLI_PARAM_LGWIN_CONST: Int = 2

/** Symbolic constants for `BROTLI_MIN_WINDOW_BITS`. */
public const val SYMBOL_BROTLI_MIN_WINDOW_BITS: String = "BROTLI_MIN_WINDOW_BITS"
public const val BROTLI_MIN_WINDOW_BITS_CONST: Int = 10

/** Symbolic constants for `BROTLI_MAX_WINDOW_BITS`. */
public const val SYMBOL_BROTLI_MAX_WINDOW_BITS: String = "BROTLI_MAX_WINDOW_BITS"
public const val BROTLI_MAX_WINDOW_BITS_CONST: Int = 24

/** Symbolic constants for `BROTLI_LARGE_MAX_WINDOW_BITS`. */
public const val SYMBOL_BROTLI_LARGE_MAX_WINDOW_BITS: String = "BROTLI_LARGE_MAX_WINDOW_BITS"
public const val BROTLI_LARGE_MAX_WINDOW_BITS_CONST: Int = 30

/** Symbolic constants for `BROTLI_DEFAULT_WINDOW`. */
public const val SYMBOL_BROTLI_DEFAULT_WINDOW: String = "BROTLI_DEFAULT_WINDOW"
public const val BROTLI_DEFAULT_WINDOW_CONST: Int = 22

/** Symbolic constants for `BROTLI_DECODE`. */
public const val SYMBOL_BROTLI_DECODE: String = "BROTLI_DECODE"
public const val BROTLI_DECODE_CONST: Int = 8

/** Symbolic constants for `BROTLI_ENCODE`. */
public const val SYMBOL_BROTLI_ENCODE: String = "BROTLI_ENCODE"
public const val BROTLI_ENCODE_CONST: Int = 9

/** Symbolic constants for `Z_MIN_WINDOWBITS`. */
public const val SYMBOL_Z_MIN_WINDOWBITS: String = "Z_MIN_WINDOWBITS"
public const val Z_MIN_WINDOWBITS_CONST: Int = 8

/** Symbolic constants for `Z_MAX_WINDOWBITS`. */
public const val SYMBOL_Z_MAX_WINDOWBITS: String = "Z_MAX_WINDOWBITS"
public const val Z_MAX_WINDOWBITS_CONST: Int = 15

/** Symbolic constants for `Z_DEFAULT_WINDOWBITS`. */
public const val SYMBOL_Z_DEFAULT_WINDOWBITS: String = "Z_DEFAULT_WINDOWBITS"
public const val Z_DEFAULT_WINDOWBITS_CONST: Int = 15

/** Symbolic constants for `Z_MIN_CHUNK`. */
public const val SYMBOL_Z_MIN_CHUNK: String = "Z_MIN_CHUNK"
public const val Z_MIN_CHUNK_CONST: Int = 64

/** Symbolic constants for `Z_MAX_CHUNK`. */
public const val SYMBOL_Z_MAX_CHUNK: String = "Z_MAX_CHUNK"
public const val Z_MAX_CHUNK_CONST: Int = 0

/** Symbolic constants for `Z_DEFAULT_CHUNK`. */
public const val SYMBOL_Z_DEFAULT_CHUNK: String = "Z_DEFAULT_CHUNK"
public const val Z_DEFAULT_CHUNK_CONST: Int = 16384

/** Symbolic constants for `Z_MIN_MEMLEVEL`. */
public const val SYMBOL_Z_MIN_MEMLEVEL: String = "Z_MIN_MEMLEVEL"
public const val Z_MIN_MEMLEVEL_CONST: Int = 1

/** Symbolic constants for `Z_MAX_MEMLEVEL`. */
public const val SYMBOL_Z_MAX_MEMLEVEL: String = "Z_MAX_MEMLEVEL"
public const val Z_MAX_MEMLEVEL_CONST: Int = 9

/** Symbolic constants for `Z_DEFAULT_MEMLEVEL`. */
public const val SYMBOL_Z_DEFAULT_MEMLEVEL: String = "Z_DEFAULT_MEMLEVEL"
public const val Z_DEFAULT_MEMLEVEL_CONST: Int = 8

/** Symbolic constants for `Z_MIN_LEVEL`. */
public const val SYMBOL_Z_MIN_LEVEL: String = "Z_MIN_LEVEL"
public const val Z_MIN_LEVEL_CONST: Int = -1

/** Symbolic constants for `Z_MAX_LEVEL`. */
public const val SYMBOL_Z_MAX_LEVEL: String = "Z_MAX_LEVEL"
public const val Z_MAX_LEVEL_CONST: Int = 9

/** Symbolic constants for `Z_DEFAULT_LEVEL`. */
public const val SYMBOL_Z_DEFAULT_LEVEL: String = "Z_DEFAULT_LEVEL"
public const val Z_DEFAULT_LEVEL_CONST: Int = -1

/** Symbolic constants for `BROTLI_PARAM_LGBLOCK`. */
public const val SYMBOL_BROTLI_PARAM_LGBLOCK: String = "BROTLI_PARAM_LGBLOCK"
public const val BROTLI_PARAM_LGBLOCK_CONST: Int = 3

/** Symbolic constants for `BROTLI_MIN_INPUT_BLOCK_BITS`. */
public const val SYMBOL_BROTLI_MIN_INPUT_BLOCK_BITS: String = "BROTLI_MIN_INPUT_BLOCK_BITS"
public const val BROTLI_MIN_INPUT_BLOCK_BITS_CONST: Int = 16

/** Symbolic constants for `BROTLI_MAX_INPUT_BLOCK_BITS`. */
public const val SYMBOL_BROTLI_MAX_INPUT_BLOCK_BITS: String = "BROTLI_MAX_INPUT_BLOCK_BITS"
public const val BROTLI_MAX_INPUT_BLOCK_BITS_CONST: Int = 24

/** Symbolic constants for `BROTLI_PARAM_DISABLE_LITERAL_CONTEXT_MODELING`. */
public const val SYMBOL_BROTLI_PARAM_DISABLE_LITERAL_CONTEXT_MODELING: String =
  "BROTLI_PARAM_DISABLE_LITERAL_CONTEXT_MODELING"
public const val BROTLI_PARAM_DISABLE_LITERAL_CONTEXT_MODELING_CONST: Int = 4

/** Symbolic constants for `BROTLI_PARAM_SIZE_HINT`. */
public const val SYMBOL_BROTLI_PARAM_SIZE_HINT: String = "BROTLI_PARAM_SIZE_HINT"
public const val BROTLI_PARAM_SIZE_HINT_CONST: Int = 5

/** Symbolic constants for `BROTLI_PARAM_LARGE_WINDOW`. */
public const val SYMBOL_BROTLI_PARAM_LARGE_WINDOW: String = "BROTLI_PARAM_LARGE_WINDOW"
public const val BROTLI_PARAM_LARGE_WINDOW_CONST: Int = 6

/** Symbolic constants for `BROTLI_PARAM_NPOSTFIX`. */
public const val SYMBOL_BROTLI_PARAM_NPOSTFIX: String = "BROTLI_PARAM_NPOSTFIX"
public const val BROTLI_PARAM_NPOSTFIX_CONST: Int = 7

/** Symbolic constants for `BROTLI_PARAM_NDIRECT`. */
public const val SYMBOL_BROTLI_PARAM_NDIRECT: String = "BROTLI_PARAM_NDIRECT"
public const val BROTLI_PARAM_NDIRECT_CONST: Int = 8

/** Symbolic constants for `BROTLI_DECODER_RESULT_ERROR`. */
public const val SYMBOL_BROTLI_DECODER_RESULT_ERROR: String = "BROTLI_DECODER_RESULT_ERROR"
public const val BROTLI_DECODER_RESULT_ERROR_CONST: Int = 0

/** Symbolic constants for `BROTLI_DECODER_RESULT_SUCCESS`. */
public const val SYMBOL_BROTLI_DECODER_RESULT_SUCCESS: String = "BROTLI_DECODER_RESULT_SUCCESS"
public const val BROTLI_DECODER_RESULT_SUCCESS_CONST: Int = 1

/** Symbolic constants for `BROTLI_DECODER_RESULT_NEEDS_MORE_INPUT`. */
public const val SYMBOL_BROTLI_DECODER_RESULT_NEEDS_MORE_INPUT: String = "BROTLI_DECODER_RESULT_NEEDS_MORE_INPUT"
public const val BROTLI_DECODER_RESULT_NEEDS_MORE_INPUT_CONST: Int = 2

/** Symbolic constants for `BROTLI_DECODER_RESULT_NEEDS_MORE_OUTPUT`. */
public const val SYMBOL_BROTLI_DECODER_RESULT_NEEDS_MORE_OUTPUT: String = "BROTLI_DECODER_RESULT_NEEDS_MORE_OUTPUT"
public const val BROTLI_DECODER_RESULT_NEEDS_MORE_OUTPUT_CONST: Int = 3

/** Symbolic constants for `BROTLI_DECODER_PARAM_DISABLE_RING_BUFFER_REALLOCATION`. */
public const val SYMBOL_BROTLI_DECODER_PARAM_DISABLE_RING_BUFFER_REALLOCATION: String =
  "BROTLI_DECODER_PARAM_DISABLE_RING_BUFFER_REALLOCATION"
public const val BROTLI_DECODER_PARAM_DISABLE_RING_BUFFER_REALLOCATION_CONST: Int = 0

/** Symbolic constants for `BROTLI_DECODER_PARAM_LARGE_WINDOW`. */
public const val SYMBOL_BROTLI_DECODER_PARAM_LARGE_WINDOW: String = "BROTLI_DECODER_PARAM_LARGE_WINDOW"
public const val BROTLI_DECODER_PARAM_LARGE_WINDOW_CONST: Int = 1

/** Symbolic constants for `BROTLI_DECODER_NO_ERROR`. */
public const val SYMBOL_BROTLI_DECODER_NO_ERROR: String = "BROTLI_DECODER_NO_ERROR"
public const val BROTLI_DECODER_NO_ERROR_CONST: Int = 0

/** Symbolic constants for `BROTLI_DECODER_SUCCESS`. */
public const val SYMBOL_BROTLI_DECODER_SUCCESS: String = "BROTLI_DECODER_SUCCESS"
public const val BROTLI_DECODER_SUCCESS_CONST: Int = 1

/** Symbolic constants for `BROTLI_DECODER_NEEDS_MORE_INPUT`. */
public const val SYMBOL_BROTLI_DECODER_NEEDS_MORE_INPUT: String = "BROTLI_DECODER_NEEDS_MORE_INPUT"
public const val BROTLI_DECODER_NEEDS_MORE_INPUT_CONST: Int = 2

/** Symbolic constants for `BROTLI_DECODER_NEEDS_MORE_OUTPUT`. */
public const val SYMBOL_BROTLI_DECODER_NEEDS_MORE_OUTPUT: String = "BROTLI_DECODER_NEEDS_MORE_OUTPUT"
public const val BROTLI_DECODER_NEEDS_MORE_OUTPUT_CONST: Int = 3

/** Symbolic constants for `BROTLI_DECODER_ERROR_FORMAT_EXUBERANT_NIBBLE`. */
public const val SYMBOL_BROTLI_DECODER_ERROR_FORMAT_EXUBERANT_NIBBLE: String =
  "BROTLI_DECODER_ERROR_FORMAT_EXUBERANT_NIBBLE"
public const val BROTLI_DECODER_ERROR_FORMAT_EXUBERANT_NIBBLE_CONST: Int = -1

/** Symbolic constants for `BROTLI_DECODER_ERROR_FORMAT_RESERVED`. */
public const val SYMBOL_BROTLI_DECODER_ERROR_FORMAT_RESERVED: String = "BROTLI_DECODER_ERROR_FORMAT_RESERVED"
public const val BROTLI_DECODER_ERROR_FORMAT_RESERVED_CONST: Int = -2

/** Symbolic constants for `BROTLI_DECODER_ERROR_FORMAT_EXUBERANT_META_NIBBLE`. */
public const val SYMBOL_BROTLI_DECODER_ERROR_FORMAT_EXUBERANT_META_NIBBLE: String =
  "BROTLI_DECODER_ERROR_FORMAT_EXUBERANT_META_NIBBLE"
public const val BROTLI_DECODER_ERROR_FORMAT_EXUBERANT_META_NIBBLE_CONST: Int = -3

/** Symbolic constants for `BROTLI_DECODER_ERROR_FORMAT_SIMPLE_HUFFMAN_ALPHABET`. */
public const val SYMBOL_BROTLI_DECODER_ERROR_FORMAT_SIMPLE_HUFFMAN_ALPHABET: String =
  "BROTLI_DECODER_ERROR_FORMAT_SIMPLE_HUFFMAN_ALPHABET"
public const val BROTLI_DECODER_ERROR_FORMAT_SIMPLE_HUFFMAN_ALPHABET_CONST: Int = -4

/** Symbolic constants for `BROTLI_DECODER_ERROR_FORMAT_SIMPLE_HUFFMAN_SAME`. */
public const val SYMBOL_BROTLI_DECODER_ERROR_FORMAT_SIMPLE_HUFFMAN_SAME: String =
  "BROTLI_DECODER_ERROR_FORMAT_SIMPLE_HUFFMAN_SAME"
public const val BROTLI_DECODER_ERROR_FORMAT_SIMPLE_HUFFMAN_SAME_CONST: Int = -5

/** Symbolic constants for `BROTLI_DECODER_ERROR_FORMAT_CL_SPACE`. */
public const val SYMBOL_BROTLI_DECODER_ERROR_FORMAT_CL_SPACE: String = "BROTLI_DECODER_ERROR_FORMAT_CL_SPACE"
public const val BROTLI_DECODER_ERROR_FORMAT_CL_SPACE_CONST: Int = -6

/** Symbolic constants for `BROTLI_DECODER_ERROR_FORMAT_HUFFMAN_SPACE`. */
public const val SYMBOL_BROTLI_DECODER_ERROR_FORMAT_HUFFMAN_SPACE: String = "BROTLI_DECODER_ERROR_FORMAT_HUFFMAN_SPACE"
public const val BROTLI_DECODER_ERROR_FORMAT_HUFFMAN_SPACE_CONST: Int = -7

/** Symbolic constants for `BROTLI_DECODER_ERROR_FORMAT_CONTEXT_MAP_REPEAT`. */
public const val SYMBOL_BROTLI_DECODER_ERROR_FORMAT_CONTEXT_MAP_REPEAT: String =
  "BROTLI_DECODER_ERROR_FORMAT_CONTEXT_MAP_REPEAT"
public const val BROTLI_DECODER_ERROR_FORMAT_CONTEXT_MAP_REPEAT_CONST: Int = -8

/** Symbolic constants for `BROTLI_DECODER_ERROR_FORMAT_BLOCK_LENGTH_1`. */
public const val SYMBOL_BROTLI_DECODER_ERROR_FORMAT_BLOCK_LENGTH_1: String =
  "BROTLI_DECODER_ERROR_FORMAT_BLOCK_LENGTH_1"
public const val BROTLI_DECODER_ERROR_FORMAT_BLOCK_LENGTH_1_CONST: Int = -9

/** Symbolic constants for `BROTLI_DECODER_ERROR_FORMAT_BLOCK_LENGTH_2`. */
public const val SYMBOL_BROTLI_DECODER_ERROR_FORMAT_BLOCK_LENGTH_2: String =
  "BROTLI_DECODER_ERROR_FORMAT_BLOCK_LENGTH_2"
public const val BROTLI_DECODER_ERROR_FORMAT_BLOCK_LENGTH_2_CONST: Int = -10

/** Symbolic constants for `BROTLI_DECODER_ERROR_FORMAT_TRANSFORM`. */
public const val SYMBOL_BROTLI_DECODER_ERROR_FORMAT_TRANSFORM: String = "BROTLI_DECODER_ERROR_FORMAT_TRANSFORM"
public const val BROTLI_DECODER_ERROR_FORMAT_TRANSFORM_CONST: Int = -11

/** Symbolic constants for `BROTLI_DECODER_ERROR_FORMAT_DICTIONARY`. */
public const val SYMBOL_BROTLI_DECODER_ERROR_FORMAT_DICTIONARY: String = "BROTLI_DECODER_ERROR_FORMAT_DICTIONARY"
public const val BROTLI_DECODER_ERROR_FORMAT_DICTIONARY_CONST: Int = -12

/** Symbolic constants for `BROTLI_DECODER_ERROR_FORMAT_WINDOW_BITS`. */
public const val SYMBOL_BROTLI_DECODER_ERROR_FORMAT_WINDOW_BITS: String = "BROTLI_DECODER_ERROR_FORMAT_WINDOW_BITS"
public const val BROTLI_DECODER_ERROR_FORMAT_WINDOW_BITS_CONST: Int = -13

/** Symbolic constants for `BROTLI_DECODER_ERROR_FORMAT_PADDING_1`. */
public const val SYMBOL_BROTLI_DECODER_ERROR_FORMAT_PADDING_1: String = "BROTLI_DECODER_ERROR_FORMAT_PADDING_1"
public const val BROTLI_DECODER_ERROR_FORMAT_PADDING_1_CONST: Int = -14

/** Symbolic constants for `BROTLI_DECODER_ERROR_FORMAT_PADDING_2`. */
public const val SYMBOL_BROTLI_DECODER_ERROR_FORMAT_PADDING_2: String = "BROTLI_DECODER_ERROR_FORMAT_PADDING_2"
public const val BROTLI_DECODER_ERROR_FORMAT_PADDING_2_CONST: Int = -15

/** Symbolic constants for `BROTLI_DECODER_ERROR_FORMAT_DISTANCE`. */
public const val SYMBOL_BROTLI_DECODER_ERROR_FORMAT_DISTANCE: String = "BROTLI_DECODER_ERROR_FORMAT_DISTANCE"
public const val BROTLI_DECODER_ERROR_FORMAT_DISTANCE_CONST: Int = -16

/** Symbolic constants for `BROTLI_DECODER_ERROR_DICTIONARY_NOT_SET`. */
public const val SYMBOL_BROTLI_DECODER_ERROR_DICTIONARY_NOT_SET: String = "BROTLI_DECODER_ERROR_DICTIONARY_NOT_SET"
public const val BROTLI_DECODER_ERROR_DICTIONARY_NOT_SET_CONST: Int = -19

/** Symbolic constants for `BROTLI_DECODER_ERROR_INVALID_ARGUMENTS`. */
public const val SYMBOL_BROTLI_DECODER_ERROR_INVALID_ARGUMENTS: String = "BROTLI_DECODER_ERROR_INVALID_ARGUMENTS"
public const val BROTLI_DECODER_ERROR_INVALID_ARGUMENTS_CONST: Int = -20

/** Symbolic constants for `BROTLI_DECODER_ERROR_ALLOC_CONTEXT_MODES`. */
public const val SYMBOL_BROTLI_DECODER_ERROR_ALLOC_CONTEXT_MODES: String = "BROTLI_DECODER_ERROR_ALLOC_CONTEXT_MODES"
public const val BROTLI_DECODER_ERROR_ALLOC_CONTEXT_MODES_CONST: Int = -21

/** Symbolic constants for `BROTLI_DECODER_ERROR_ALLOC_TREE_GROUPS`. */
public const val SYMBOL_BROTLI_DECODER_ERROR_ALLOC_TREE_GROUPS: String = "BROTLI_DECODER_ERROR_ALLOC_TREE_GROUPS"
public const val BROTLI_DECODER_ERROR_ALLOC_TREE_GROUPS_CONST: Int = -22

/** Symbolic constants for `BROTLI_DECODER_ERROR_ALLOC_CONTEXT_MAP`. */
public const val SYMBOL_BROTLI_DECODER_ERROR_ALLOC_CONTEXT_MAP: String = "BROTLI_DECODER_ERROR_ALLOC_CONTEXT_MAP"
public const val BROTLI_DECODER_ERROR_ALLOC_CONTEXT_MAP_CONST: Int = -25

/** Symbolic constants for `BROTLI_DECODER_ERROR_ALLOC_RING_BUFFER_1`. */
public const val SYMBOL_BROTLI_DECODER_ERROR_ALLOC_RING_BUFFER_1: String = "BROTLI_DECODER_ERROR_ALLOC_RING_BUFFER_1"
public const val BROTLI_DECODER_ERROR_ALLOC_RING_BUFFER_1_CONST: Int = -26

/** Symbolic constants for `BROTLI_DECODER_ERROR_ALLOC_RING_BUFFER_2`. */
public const val SYMBOL_BROTLI_DECODER_ERROR_ALLOC_RING_BUFFER_2: String = "BROTLI_DECODER_ERROR_ALLOC_RING_BUFFER_2"
public const val BROTLI_DECODER_ERROR_ALLOC_RING_BUFFER_2_CONST: Int = -27

/** Symbolic constants for `BROTLI_DECODER_ERROR_ALLOC_BLOCK_TYPE_TREES`. */
public const val SYMBOL_BROTLI_DECODER_ERROR_ALLOC_BLOCK_TYPE_TREES: String =
  "BROTLI_DECODER_ERROR_ALLOC_BLOCK_TYPE_TREES"
public const val BROTLI_DECODER_ERROR_ALLOC_BLOCK_TYPE_TREES_CONST: Int = -30

/** Symbolic constants for `BROTLI_DECODER_ERROR_UNREACHABLE`. */
public const val SYMBOL_BROTLI_DECODER_ERROR_UNREACHABLE: String = "BROTLI_DECODER_ERROR_UNREACHABLE"
public const val BROTLI_DECODER_ERROR_UNREACHABLE_CONST: Int = -31

/**
 * ## Zlib Constants
 *
 * Models constants provided by the native `zlib` implementation, and surfaced by the Node Zlib module.
 */
@API public sealed interface ZlibConstants : ProxyObject {
  /** Constant for `Z_NO_FLUSH`. */
  @get:Polyglot public val Z_NO_FLUSH: Int

  /** Constant for `Z_PARTIAL_FLUSH`. */
  @get:Polyglot public val Z_PARTIAL_FLUSH: Int

  /** Constant for `Z_SYNC_FLUSH`. */
  @get:Polyglot public val Z_SYNC_FLUSH: Int

  /** Constant for `Z_FULL_FLUSH`. */
  @get:Polyglot public val Z_FULL_FLUSH: Int

  /** Constant for `Z_FINISH`. */
  @get:Polyglot public val Z_FINISH: Int

  /** Constant for `Z_BLOCK`. */
  @get:Polyglot public val Z_BLOCK: Int

  /** Constant for `Z_OK`. */
  @get:Polyglot public val Z_OK: Int

  /** Constant for `Z_STREAM_END`. */
  @get:Polyglot public val Z_STREAM_END: Int

  /** Constant for `Z_NEED_DICT`. */
  @get:Polyglot public val Z_NEED_DICT: Int

  /** Constant for `Z_ERRNO`. */
  @get:Polyglot public val Z_ERRNO: Int

  /** Constant for `Z_STREAM_ERROR`. */
  @get:Polyglot public val Z_STREAM_ERROR: Int

  /** Constant for `Z_DATA_ERROR`. */
  @get:Polyglot public val Z_DATA_ERROR: Int

  /** Constant for `Z_MEM_ERROR`. */
  @get:Polyglot public val Z_MEM_ERROR: Int

  /** Constant for `Z_BUF_ERROR`. */
  @get:Polyglot public val Z_BUF_ERROR: Int

  /** Constant for `Z_VERSION_ERROR`. */
  @get:Polyglot public val Z_VERSION_ERROR: Int

  /** Constant for `Z_NO_COMPRESSION`. */
  @get:Polyglot public val Z_NO_COMPRESSION: Int

  /** Constant for `Z_BEST_SPEED`. */
  @get:Polyglot public val Z_BEST_SPEED: Int

  /** Constant for `Z_BEST_COMPRESSION`. */
  @get:Polyglot public val Z_BEST_COMPRESSION: Int

  /** Constant for `Z_DEFAULT_COMPRESSION`. */
  @get:Polyglot public val Z_DEFAULT_COMPRESSION: Int

  /** Constant for `Z_FILTERED`. */
  @get:Polyglot public val Z_FILTERED: Int

  /** Constant for `Z_HUFFMAN_ONLY`. */
  @get:Polyglot public val Z_HUFFMAN_ONLY: Int

  /** Constant for `Z_RLE`. */
  @get:Polyglot public val Z_RLE: Int

  /** Constant for `Z_FIXED`. */
  @get:Polyglot public val Z_FIXED: Int

  /** Constant for `Z_DEFAULT_STRATEGY`. */
  @get:Polyglot public val Z_DEFAULT_STRATEGY: Int

  /** Constant for `ZLIB_VERNUM`. */
  @get:Polyglot public val ZLIB_VERNUM: Int

  /** Constant for `DEFLATE`. */
  @get:Polyglot public val DEFLATE: Int

  /** Constant for `INFLATE`. */
  @get:Polyglot public val INFLATE: Int

  /** Constant for `GZIP`. */
  @get:Polyglot public val GZIP: Int

  /** Constant for `GUNZIP`. */
  @get:Polyglot public val GUNZIP: Int

  /** Constant for `DEFLATERAW`. */
  @get:Polyglot public val DEFLATERAW: Int

  /** Constant for `INFLATERAW`. */
  @get:Polyglot public val INFLATERAW: Int

  /** Constant for `UNZIP`. */
  @get:Polyglot public val UNZIP: Int

  /** Constant for `Z_MIN_WINDOWBITS`. */
  @get:Polyglot public val Z_MIN_WINDOWBITS: Int

  /** Constant for `Z_MAX_WINDOWBITS`. */
  @get:Polyglot public val Z_MAX_WINDOWBITS: Int

  /** Constant for `Z_DEFAULT_WINDOWBITS`. */
  @get:Polyglot public val Z_DEFAULT_WINDOWBITS: Int

  /** Constant for `Z_MIN_CHUNK`. */
  @get:Polyglot public val Z_MIN_CHUNK: Int

  /** Constant for `Z_MAX_CHUNK`. */
  @get:Polyglot public val Z_MAX_CHUNK: Int

  /** Constant for `Z_DEFAULT_CHUNK`. */
  @get:Polyglot public val Z_DEFAULT_CHUNK: Int

  /** Constant for `Z_MIN_MEMLEVEL`. */
  @get:Polyglot public val Z_MIN_MEMLEVEL: Int

  /** Constant for `Z_MAX_MEMLEVEL`. */
  @get:Polyglot public val Z_MAX_MEMLEVEL: Int

  /** Constant for `Z_DEFAULT_MEMLEVEL`. */
  @get:Polyglot public val Z_DEFAULT_MEMLEVEL: Int

  /** Constant for `Z_MIN_LEVEL`. */
  @get:Polyglot public val Z_MIN_LEVEL: Int

  /** Constant for `Z_MAX_LEVEL`. */
  @get:Polyglot public val Z_MAX_LEVEL: Int

  /** Constant for `Z_DEFAULT_LEVEL`. */
  @get:Polyglot public val Z_DEFAULT_LEVEL: Int
}

/**
 * ## Brotli Constants
 *
 * Models constants provided by the native `brotli` implementation, and surfaced by the Node Zlib module.
 */
@API public sealed interface BrotliConstants : ProxyObject {
  /** Constant for `BROTLI_OPERATION_PROCESS`. */
  @get:Polyglot public val BROTLI_OPERATION_PROCESS: Int

  /** Constant for `BROTLI_OPERATION_FLUSH`. */
  @get:Polyglot public val BROTLI_OPERATION_FLUSH: Int

  /** Constant for `BROTLI_OPERATION_FINISH`. */
  @get:Polyglot public val BROTLI_OPERATION_FINISH: Int

  /** Constant for `BROTLI_OPERATION_EMIT_METADATA`. */
  @get:Polyglot public val BROTLI_OPERATION_EMIT_METADATA: Int

  /** Constant for `BROTLI_PARAM_MODE`. */
  @get:Polyglot public val BROTLI_PARAM_MODE: Int

  /** Constant for `BROTLI_MODE_GENERIC`. */
  @get:Polyglot public val BROTLI_MODE_GENERIC: Int

  /** Constant for `BROTLI_MODE_TEXT`. */
  @get:Polyglot public val BROTLI_MODE_TEXT: Int

  /** Constant for `BROTLI_MODE_FONT`. */
  @get:Polyglot public val BROTLI_MODE_FONT: Int

  /** Constant for `BROTLI_DEFAULT_MODE`. */
  @get:Polyglot public val BROTLI_DEFAULT_MODE: Int

  /** Constant for `BROTLI_PARAM_QUALITY`. */
  @get:Polyglot public val BROTLI_PARAM_QUALITY: Int

  /** Constant for `BROTLI_MIN_QUALITY`. */
  @get:Polyglot public val BROTLI_MIN_QUALITY: Int

  /** Constant for `BROTLI_MAX_QUALITY`. */
  @get:Polyglot public val BROTLI_MAX_QUALITY: Int

  /** Constant for `BROTLI_DEFAULT_QUALITY`. */
  @get:Polyglot public val BROTLI_DEFAULT_QUALITY: Int

  /** Constant for `BROTLI_PARAM_LGWIN`. */
  @get:Polyglot public val BROTLI_PARAM_LGWIN: Int

  /** Constant for `BROTLI_MIN_WINDOW_BITS`. */
  @get:Polyglot public val BROTLI_MIN_WINDOW_BITS: Int

  /** Constant for `BROTLI_MAX_WINDOW_BITS`. */
  @get:Polyglot public val BROTLI_MAX_WINDOW_BITS: Int

  /** Constant for `BROTLI_LARGE_MAX_WINDOW_BITS`. */
  @get:Polyglot public val BROTLI_LARGE_MAX_WINDOW_BITS: Int

  /** Constant for `BROTLI_DEFAULT_WINDOW`. */
  @get:Polyglot public val BROTLI_DEFAULT_WINDOW: Int

  /** Constant for `BROTLI_PARAM_LGBLOCK`. */
  @get:Polyglot public val BROTLI_PARAM_LGBLOCK: Int

  /** Constant for `BROTLI_MIN_INPUT_BLOCK_BITS`. */
  @get:Polyglot public val BROTLI_MIN_INPUT_BLOCK_BITS: Int

  /** Constant for `BROTLI_MAX_INPUT_BLOCK_BITS`. */
  @get:Polyglot public val BROTLI_MAX_INPUT_BLOCK_BITS: Int

  /** Constant for `BROTLI_PARAM_DISABLE_LITERAL_CONTEXT_MODELING`. */
  @get:Polyglot public val BROTLI_PARAM_DISABLE_LITERAL_CONTEXT_MODELING: Int

  /** Constant for `BROTLI_PARAM_SIZE_HINT`. */
  @get:Polyglot public val BROTLI_PARAM_SIZE_HINT: Int

  /** Constant for `BROTLI_PARAM_LARGE_WINDOW`. */
  @get:Polyglot public val BROTLI_PARAM_LARGE_WINDOW: Int

  /** Constant for `BROTLI_PARAM_NPOSTFIX`. */
  @get:Polyglot public val BROTLI_PARAM_NPOSTFIX: Int

  /** Constant for `BROTLI_PARAM_NDIRECT`. */
  @get:Polyglot public val BROTLI_PARAM_NDIRECT: Int

  /** Constant for `BROTLI_DECODER_RESULT_ERROR`. */
  @get:Polyglot public val BROTLI_DECODER_RESULT_ERROR: Int

  /** Constant for `BROTLI_DECODER_RESULT_SUCCESS`. */
  @get:Polyglot public val BROTLI_DECODER_RESULT_SUCCESS: Int

  /** Constant for `BROTLI_DECODER_RESULT_NEEDS_MORE_INPUT`. */
  @get:Polyglot public val BROTLI_DECODER_RESULT_NEEDS_MORE_INPUT: Int

  /** Constant for `BROTLI_DECODER_RESULT_NEEDS_MORE_OUTPUT`. */
  @get:Polyglot public val BROTLI_DECODER_RESULT_NEEDS_MORE_OUTPUT: Int

  /** Constant for `BROTLI_DECODER_PARAM_DISABLE_RING_BUFFER_REALLOCATION`. */
  @get:Polyglot public val BROTLI_DECODER_PARAM_DISABLE_RING_BUFFER_REALLOCATION: Int

  /** Constant for `BROTLI_DECODER_PARAM_LARGE_WINDOW`. */
  @get:Polyglot public val BROTLI_DECODER_PARAM_LARGE_WINDOW: Int

  /** Constant for `BROTLI_DECODER_NO_ERROR`. */
  @get:Polyglot public val BROTLI_DECODER_NO_ERROR: Int

  /** Constant for `BROTLI_DECODER_SUCCESS`. */
  @get:Polyglot public val BROTLI_DECODER_SUCCESS: Int

  /** Constant for `BROTLI_DECODER_NEEDS_MORE_INPUT`. */
  @get:Polyglot public val BROTLI_DECODER_NEEDS_MORE_INPUT: Int

  /** Constant for `BROTLI_DECODER_NEEDS_MORE_OUTPUT`. */
  @get:Polyglot public val BROTLI_DECODER_NEEDS_MORE_OUTPUT: Int

  /** Constant for `BROTLI_DECODER_ERROR_FORMAT_EXUBERANT_NIBBLE`. */
  @get:Polyglot public val BROTLI_DECODER_ERROR_FORMAT_EXUBERANT_NIBBLE: Int

  /** Constant for `BROTLI_DECODER_ERROR_FORMAT_RESERVED`. */
  @get:Polyglot public val BROTLI_DECODER_ERROR_FORMAT_RESERVED: Int

  /** Constant for `BROTLI_DECODER_ERROR_FORMAT_EXUBERANT_META_NIBBLE`. */
  @get:Polyglot public val BROTLI_DECODER_ERROR_FORMAT_EXUBERANT_META_NIBBLE: Int

  /** Constant for `BROTLI_DECODER_ERROR_FORMAT_SIMPLE_HUFFMAN_ALPHABET`. */
  @get:Polyglot public val BROTLI_DECODER_ERROR_FORMAT_SIMPLE_HUFFMAN_ALPHABET: Int

  /** Constant for `BROTLI_DECODER_ERROR_FORMAT_SIMPLE_HUFFMAN_SAME`. */
  @get:Polyglot public val BROTLI_DECODER_ERROR_FORMAT_SIMPLE_HUFFMAN_SAME: Int

  /** Constant for `BROTLI_DECODER_ERROR_FORMAT_CL_SPACE`. */
  @get:Polyglot public val BROTLI_DECODER_ERROR_FORMAT_CL_SPACE: Int

  /** Constant for `BROTLI_DECODER_ERROR_FORMAT_HUFFMAN_SPACE`. */
  @get:Polyglot public val BROTLI_DECODER_ERROR_FORMAT_HUFFMAN_SPACE: Int

  /** Constant for `BROTLI_DECODER_ERROR_FORMAT_CONTEXT_MAP_REPEAT`. */
  @get:Polyglot public val BROTLI_DECODER_ERROR_FORMAT_CONTEXT_MAP_REPEAT: Int

  /** Constant for `BROTLI_DECODER_ERROR_FORMAT_BLOCK_LENGTH_1`. */
  @get:Polyglot public val BROTLI_DECODER_ERROR_FORMAT_BLOCK_LENGTH_1: Int

  /** Constant for `BROTLI_DECODER_ERROR_FORMAT_BLOCK_LENGTH_2`. */
  @get:Polyglot public val BROTLI_DECODER_ERROR_FORMAT_BLOCK_LENGTH_2: Int

  /** Constant for `BROTLI_DECODER_ERROR_FORMAT_TRANSFORM`. */
  @get:Polyglot public val BROTLI_DECODER_ERROR_FORMAT_TRANSFORM: Int

  /** Constant for `BROTLI_DECODER_ERROR_FORMAT_DICTIONARY`. */
  @get:Polyglot public val BROTLI_DECODER_ERROR_FORMAT_DICTIONARY: Int

  /** Constant for `BROTLI_DECODER_ERROR_FORMAT_WINDOW_BITS`. */
  @get:Polyglot public val BROTLI_DECODER_ERROR_FORMAT_WINDOW_BITS: Int

  /** Constant for `BROTLI_DECODER_ERROR_FORMAT_PADDING_1`. */
  @get:Polyglot public val BROTLI_DECODER_ERROR_FORMAT_PADDING_1: Int

  /** Constant for `BROTLI_DECODER_ERROR_FORMAT_PADDING_2`. */
  @get:Polyglot public val BROTLI_DECODER_ERROR_FORMAT_PADDING_2: Int

  /** Constant for `BROTLI_DECODER_ERROR_FORMAT_DISTANCE`. */
  @get:Polyglot public val BROTLI_DECODER_ERROR_FORMAT_DISTANCE: Int

  /** Constant for `BROTLI_DECODER_ERROR_DICTIONARY_NOT_SET`. */
  @get:Polyglot public val BROTLI_DECODER_ERROR_DICTIONARY_NOT_SET: Int

  /** Constant for `BROTLI_DECODER_ERROR_INVALID_ARGUMENTS`. */
  @get:Polyglot public val BROTLI_DECODER_ERROR_INVALID_ARGUMENTS: Int

  /** Constant for `BROTLI_DECODER_ERROR_ALLOC_CONTEXT_MODES`. */
  @get:Polyglot public val BROTLI_DECODER_ERROR_ALLOC_CONTEXT_MODES: Int

  /** Constant for `BROTLI_DECODER_ERROR_ALLOC_TREE_GROUPS`. */
  @get:Polyglot public val BROTLI_DECODER_ERROR_ALLOC_TREE_GROUPS: Int

  /** Constant for `BROTLI_DECODER_ERROR_ALLOC_CONTEXT_MAP`. */
  @get:Polyglot public val BROTLI_DECODER_ERROR_ALLOC_CONTEXT_MAP: Int

  /** Constant for `BROTLI_DECODER_ERROR_ALLOC_RING_BUFFER_1`. */
  @get:Polyglot public val BROTLI_DECODER_ERROR_ALLOC_RING_BUFFER_1: Int

  /** Constant for `BROTLI_DECODER_ERROR_ALLOC_RING_BUFFER_2`. */
  @get:Polyglot public val BROTLI_DECODER_ERROR_ALLOC_RING_BUFFER_2: Int

  /** Constant for `BROTLI_DECODER_ERROR_ALLOC_BLOCK_TYPE_TREES`. */
  @get:Polyglot public val BROTLI_DECODER_ERROR_ALLOC_BLOCK_TYPE_TREES: Int

  /** Constant for `BROTLI_DECODER_ERROR_UNREACHABLE`. */
  @get:Polyglot public val BROTLI_DECODER_ERROR_UNREACHABLE: Int
}

/**
 * ## Node Zlib Constants
 *
 * Describes the combined constants for [ZlibConstants] and [BrotliOptions], which are provided by the Node Zlib
 * module.
 */
@API public interface NodeZlibConstants : ZlibConstants, BrotliConstants

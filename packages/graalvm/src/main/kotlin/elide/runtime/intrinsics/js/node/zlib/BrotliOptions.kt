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

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyInstantiable
import elide.annotations.API
import elide.runtime.gvm.js.JsError
import elide.runtime.node.zlib.ModernNodeZlibConstants
import elide.vm.annotations.Polyglot

/**
 * ## Brotli Options
 *
 * Represents the interface fulfilled by an object that specified options for Brotli compression or decompression;
 * modeled by the `zlib.BrotliOptions` object in the Node.js API.
 */
@API public interface BrotliOptions {
  /**
   * `flush` <integer> Default: `zlib.constants.BROTLI_OPERATION_PROCESS`
   */
  @get:Polyglot public val flush: Int

  /**
   * `finishFlush` <integer> Default: `zlib.constants.BROTLI_OPERATION_FINISH`
   */
  @get:Polyglot public val finishFlush: Int

  /**
   * `chunkSize` <integer> Default: `16 * 1024`
   */
  @get:Polyglot public val chunkSize: Int

  /**
   * `params` <integer> Default: `zlib.constants.BROTLI_DEFAULT_PARAMS`
   */
  @get:Polyglot public val params: Int

  /**
   * `maxOutputLength` <integer> Default: `buffer.kMaxLength`
   */
  @get:Polyglot public val maxOutputLength: Int
}

/**
 * ## Brotli Options (Mutable)
 *
 * Represents the interface fulfilled by an object that specified options for Brotli compression or decompression;
 * modeled by the `zlib.BrotliOptions` object in the Node.js API. This is the mutable form of [BrotliOptions].
 */
@API public interface MutableBrotliOptions {
  /**
   * `flush` <integer> Default: `zlib.constants.BROTLI_OPERATION_PROCESS`
   */
  @get:Polyglot @set:Polyglot public var flush: Int

  /**
   * `finishFlush` <integer> Default: `zlib.constants.BROTLI_OPERATION_FINISH`
   */
  @get:Polyglot @set:Polyglot public var finishFlush: Int

  /**
   * `chunkSize` <integer> Default: `16 * 1024`
   */
  @get:Polyglot @set:Polyglot public var chunkSize: Int

  /**
   * `params` <integer> Default: `zlib.constants.BROTLI_DEFAULT_PARAMS`
   */
  @get:Polyglot @set:Polyglot public var params: Int

  /**
   * `maxOutputLength` <integer> Default: `buffer.kMaxLength`
   */
  @get:Polyglot @set:Polyglot public var maxOutputLength: Int
}

/**
 * ## Brotli Options (Immutable)
 *
 * Implements an immutable Brotli options object.
 */
@JvmRecord public data class ImmutableBrotliOptions internal constructor (
  @get:Polyglot override val flush: Int = ModernNodeZlibConstants.BROTLI_OPERATION_PROCESS,
  @get:Polyglot override val finishFlush: Int = ModernNodeZlibConstants.BROTLI_OPERATION_FINISH,
  @get:Polyglot override val chunkSize: Int = ModernNodeZlibConstants.Z_DEFAULT_CHUNK,
  @get:Polyglot override val params: Int = 0,
  @get:Polyglot override val maxOutputLength: Int = 0,
) : BrotliOptions, BrotliOptionsDefaults {
  /** Constructor definitions for [ImmutableBrotliOptions]. */
  public companion object Factory : ProxyInstantiable {
    // Defaults instance.
    private val DEFAULTS = ImmutableBrotliOptions()

    override fun newInstance(vararg arguments: Value?): Any {
      TODO("Not yet implemented")
    }

    /**
     * Create a new [ImmutableBrotliOptions] from a [Value].
     *
     * @param value The value to convert to [ImmutableBrotliOptions].
     * @return The [ImmutableBrotliOptions] instance.
     */
    @JvmStatic public fun fromValue(value: Value?): ImmutableBrotliOptions = when {
      value == null || value.isNull -> DEFAULTS

      value.isHostObject -> when (val asHost = value.asHostObject<BrotliOptions>()) {
        is ImmutableBrotliOptions -> asHost
        is elide.runtime.node.zlib.MutableBrotliOptions -> asHost.toImmutable()
        else -> JsError.error("Cannot convert value to BrotliOptions (unfamiliar host object)")
      }

      value.hasMembers() -> DEFAULTS.copy(
        flush = value.getMember("flush")?.asInt() ?: ModernNodeZlibConstants.Z_NO_FLUSH,
        finishFlush = value.getMember("finishFlush")?.asInt() ?: ModernNodeZlibConstants.Z_FINISH,
        chunkSize = value.getMember("chunkSize")?.asInt() ?: ModernNodeZlibConstants.Z_DEFAULT_CHUNK,
        params = value.getMember("params")?.asInt() ?: 0,
        maxOutputLength = value.getMember("maxOutputLength")?.asInt() ?: 0,
      )

      value.hasHashEntries() -> DEFAULTS.copy(
        flush = value.getHashValue("flush")?.asInt() ?: ModernNodeZlibConstants.Z_NO_FLUSH,
        finishFlush = value.getHashValue("finishFlush")?.asInt() ?: ModernNodeZlibConstants.Z_FINISH,
        chunkSize = value.getHashValue("chunkSize")?.asInt() ?: ModernNodeZlibConstants.Z_DEFAULT_CHUNK,
        params = value.getHashValue("params")?.asInt() ?: 0,
        maxOutputLength = value.getHashValue("maxOutputLength")?.asInt() ?: 0,
      )

      else -> JsError.error("Cannot convert value to BrotliOptions")
    }

    /**
     * Create a new [ImmutableBrotliOptions] with defaults set.
     */
    @JvmStatic public fun defaults(): BrotliOptions = DEFAULTS
  }
}

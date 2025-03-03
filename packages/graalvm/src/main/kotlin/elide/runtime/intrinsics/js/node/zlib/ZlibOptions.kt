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
 * ## Zlib Options
 *
 * Represents the interface fulfilled by an object that specified options for zlib compression or decompression; modeled
 * by the `zlib.Options` object in the Node.js API.
 */
@API public interface ZlibOptions {
  /**
   * `flush` <integer> Default: `zlib.constants.Z_NO_FLUSH`
   */
  @get:Polyglot public val flush: Int

  /**
   * `finishFlush` <integer> Default: `zlib.constants.Z_FINISH`
   */
  @get:Polyglot public val finishFlush: Int

  /**
   * `chunkSize` <integer> Default: `16 * 1024`
   */
  @get:Polyglot public val chunkSize: Int

  /**
   * `windowBits` <integer> Default: `zlib.constants.Z_DEFAULT_WINDOWBITS`
   */
  @get:Polyglot public val windowBits: Int

  /**
   * `level` <integer> Default: `zlib.constants.Z_DEFAULT_COMPRESSION` (compression only)
   */
  @get:Polyglot public val level: Int?

  /**
   * `memLevel` <integer> Default: `zlib.constants.Z_DEFAULT_MEMLEVEL` (compression only)
   */
  @get:Polyglot public val memLevel: Int?

  /**
   * `strategy` <integer> Default: `zlib.constants.Z_DEFAULT_STRATEGY` (compression only)
   */
  @get:Polyglot public val strategy: Int?

  /**
   * `dictionary` <Buffer> | <TypedArray> | <DataView> | <ArrayBuffer> Default: `null` (compression only)
   */
  @get:Polyglot public val dictionary: Any?

  /**
   * `info` <boolean> Default: `false`
   */
  @get:Polyglot public val info: Boolean?

  /**
   * `maxOutputLength` <integer> Default: `buffer.kMaxLength`
   */
  @get:Polyglot public val maxOutputLength: Int
}

/**
 * ## Zlib Options (Mutable)
 *
 * Represents the interface fulfilled by an object that specified options for zlib compression or decompression; modeled
 * by the `zlib.Options` object in the Node.js API, as a mutable version of [ZlibOptions].
 */
@API public interface MutableZlibOptions : ZlibOptions {
  /**
   * `flush` <integer> Default: `zlib.constants.Z_NO_FLUSH`
   */
  @get:Polyglot @set:Polyglot override var flush: Int

  /**
   * `finishFlush` <integer> Default: `zlib.constants.Z_FINISH`
   */
  @get:Polyglot @set:Polyglot override var finishFlush: Int

  /**
   * `chunkSize` <integer> Default: `16 * 1024`
   */
  @get:Polyglot @set:Polyglot override var chunkSize: Int

  /**
   * `windowBits` <integer> Default: `zlib.constants.Z_DEFAULT_WINDOWBITS`
   */
  @get:Polyglot @set:Polyglot override var windowBits: Int

  /**
   * `level` <integer> Default: `zlib.constants.Z_DEFAULT_COMPRESSION` (compression only)
   */
  @get:Polyglot @set:Polyglot override var level: Int?

  /**
   * `memLevel` <integer> Default: `zlib.constants.Z_DEFAULT_MEMLEVEL` (compression only)
   */
  @get:Polyglot @set:Polyglot override var memLevel: Int?

  /**
   * `strategy` <integer> Default: `zlib.constants.Z_DEFAULT_STRATEGY` (compression only)
   */
  @get:Polyglot @set:Polyglot override var strategy: Int?

  /**
   * `dictionary` <Buffer> | <TypedArray> | <DataView> | <ArrayBuffer> Default: `null` (compression only)
   */
  @get:Polyglot @set:Polyglot override var dictionary: Any?

  /**
   * `info` <boolean> Default: `false`
   */
  @get:Polyglot @set:Polyglot override var info: Boolean

  /**
   * `maxOutputLength` <integer> Default: `buffer.kMaxLength`
   */
  @get:Polyglot @set:Polyglot override var maxOutputLength: Int
}

/**
 * ## Zlib Options (Immutable)
 *
 * Implements an immutable Zlib options object.
 */
@JvmRecord
public data class ImmutableZlibOptions internal constructor (
  @get:Polyglot override val flush: Int = ModernNodeZlibConstants.Z_NO_FLUSH,
  @get:Polyglot override val finishFlush: Int = ModernNodeZlibConstants.Z_FINISH,
  @get:Polyglot override val chunkSize: Int = ModernNodeZlibConstants.Z_DEFAULT_CHUNK,
  @get:Polyglot override val windowBits: Int = ModernNodeZlibConstants.Z_DEFAULT_WINDOWBITS,
  @get:Polyglot override val level: Int = ModernNodeZlibConstants.Z_DEFAULT_LEVEL,
  @get:Polyglot override val memLevel: Int = ModernNodeZlibConstants.Z_DEFAULT_MEMLEVEL,
  @get:Polyglot override val strategy: Int = ModernNodeZlibConstants.Z_DEFAULT_STRATEGY,
  @get:Polyglot override val dictionary: Any? = null,
  @get:Polyglot override val info: Boolean = false,
  @get:Polyglot override val maxOutputLength: Int = 0,
) : ZlibOptions, ZlibOptionsDefaults {
  /** Constructor definitions for [ImmutableZlibOptions]. */
  public companion object Factory : ProxyInstantiable {
    // Defaults instance.
    private val DEFAULTS = ImmutableZlibOptions()

    /**
     * Create a new [ImmutableZlibOptions] from a [Value].
     *
     * @param value The value to convert to [ImmutableZlibOptions].
     * @return The [ImmutableZlibOptions] instance.
     */
    @JvmStatic public fun fromValue(value: Value?): ImmutableZlibOptions = when {
      value == null || value.isNull -> DEFAULTS

      value.isHostObject -> when (val asHost = value.asHostObject<ZlibOptions>()) {
        is ImmutableZlibOptions -> asHost
        is elide.runtime.node.zlib.MutableZlibOptions -> asHost.toImmutable()
        else -> JsError.error("Cannot convert value to ZlibOptions (unfamiliar host object)")
      }

      value.hasMembers() -> DEFAULTS.copy(
        flush = value.getMember("flush")?.asInt() ?: ModernNodeZlibConstants.Z_NO_FLUSH,
        finishFlush = value.getMember("finishFlush")?.asInt() ?: ModernNodeZlibConstants.Z_FINISH,
        chunkSize = value.getMember("chunkSize")?.asInt() ?: ModernNodeZlibConstants.Z_DEFAULT_CHUNK,
        windowBits = value.getMember("windowBits")?.asInt() ?: ModernNodeZlibConstants.Z_DEFAULT_WINDOWBITS,
        level = value.getMember("level")?.asInt() ?: ModernNodeZlibConstants.Z_DEFAULT_LEVEL,
        memLevel = value.getMember("memLevel")?.asInt() ?: ModernNodeZlibConstants.Z_DEFAULT_MEMLEVEL,
        strategy = value.getMember("strategy")?.asInt() ?: ModernNodeZlibConstants.Z_DEFAULT_STRATEGY,
        dictionary = value.getMember("dictionary"),
        info = value.getMember("info")?.asBoolean() ?: false,
        maxOutputLength = value.getMember("maxOutputLength")?.asInt() ?: 0,
      )

      value.hasHashEntries() -> DEFAULTS.copy(
        flush = value.getHashValue("flush")?.asInt() ?: ModernNodeZlibConstants.Z_NO_FLUSH,
        finishFlush = value.getHashValue("finishFlush")?.asInt() ?: ModernNodeZlibConstants.Z_FINISH,
        chunkSize = value.getHashValue("chunkSize")?.asInt() ?: ModernNodeZlibConstants.Z_DEFAULT_CHUNK,
        windowBits = value.getHashValue("windowBits")?.asInt() ?: ModernNodeZlibConstants.Z_DEFAULT_WINDOWBITS,
        level = value.getHashValue("level")?.asInt() ?: ModernNodeZlibConstants.Z_DEFAULT_LEVEL,
        memLevel = value.getHashValue("memLevel")?.asInt() ?: ModernNodeZlibConstants.Z_DEFAULT_MEMLEVEL,
        strategy = value.getHashValue("strategy")?.asInt() ?: ModernNodeZlibConstants.Z_DEFAULT_STRATEGY,
        dictionary = value.getHashValue("dictionary"),
        info = value.getHashValue("info")?.asBoolean() ?: false,
        maxOutputLength = value.getHashValue("maxOutputLength")?.asInt() ?: 0,
      )

      else -> JsError.error("Cannot convert value to ZlibOptions")
    }

    /**
     * Create a new [ImmutableZlibOptions] with defaults set.
     */
    @JvmStatic public fun defaults(): ZlibOptions = DEFAULTS

    override fun newInstance(vararg arguments: Value?): Any {
      require(arguments.isEmpty()) { "Constructor for Zlib options does not take arguments" }
      return defaults()
    }
  }

  /**
   * Create a mutable copy of this [ImmutableZlibOptions].
   *
   * @return A mutable copy of this [ImmutableZlibOptions].
   */
  public fun toMutable(): MutableZlibOptions = elide.runtime.node.zlib.MutableZlibOptions(
    flush = flush,
    finishFlush = finishFlush,
    chunkSize = chunkSize,
    windowBits = windowBits,
    level = level,
    memLevel = memLevel,
    strategy = strategy,
    dictionary = dictionary,
    info = info,
    maxOutputLength = maxOutputLength,
  )
}

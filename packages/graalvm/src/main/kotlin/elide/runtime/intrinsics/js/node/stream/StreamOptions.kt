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
package elide.runtime.intrinsics.js.node.stream

import org.graalvm.polyglot.Value
import elide.annotations.API
import elide.runtime.intrinsics.js.node.AbortSignal
import elide.vm.annotations.Polyglot

/**
 * ## Readable Pipe Options
 *
 * Defines the structure of options which relate to the `Readable.pipe` method.
 */
@API public data class ReadablePipeOptions(
  /**
   * End the writer when the reader ends. Default: `true`.
   */
  @get:Polyglot public var end: Boolean = true,
) {
  /**
   * ## Readable Pipe Options
   *
   * Default options for the `Readable.pipe` method.
   */
  public companion object {
    public val DEFAULTS: ReadablePipeOptions = ReadablePipeOptions()

    @JvmStatic public fun fromGuest(value: Value): ReadablePipeOptions = ReadablePipeOptions(
      end = if (value.hasMembers()) {
        value.getMember("end")?.asBoolean() ?: false
      } else if (value.hasHashEntries()) {
        value.getMember("end")?.asBoolean() ?: false
      } else {
        false
      }
    )
  }
}

/**
 * ## Readable Compose Options
 *
 * Defines the structure of options which relate to the `Readable.compose` method.
 */
@API public data class ReadableComposeOptions(
  /**
   * Abort signal to use for the operation.
   */
  @get:Polyglot public var signal: AbortSignal? = null,
)

/**
 * ## Readable Iterator Options
 *
 * Defines the structure of options which relate to the `Readable.iterator` method.
 */
@API public data class ReadableIteratorOptions(
  /**
   * When set to `false`, calling `return` on the async iterator, or exiting a `for await...of` iteration using a
   * `break`, `return`, or `throw` will not destroy the stream.
   *
   * Default: `true`.
   */
  @get:Polyglot public var destroyOnReturn: Boolean = true,
)

/**
 * ## Readable Map Options
 *
 * Defines the structure of options which relate to the `Readable.map` method.
 */
@API public data class ReadableMapOptions(
  /**
   * The maximum number of concurrent map operations to perform.
   *
   * Default: `1`.
   */
  @get:Polyglot public var concurrency: Int = 1,

  /**
   * The maximum number of items to buffer before applying backpressure.
   *
   * Default: `concurrency * 2 - 1`.
   */
  @get:Polyglot public var highWaterMark: Int = concurrency * 2 - 1,

  /**
   * Abort signal to use for the operation.
   */
  @get:Polyglot public var signal: AbortSignal? = null,
)

/**
 * ## Readable For-each Options
 *
 * Defines the structure of options which relate to the `Readable.forEach` method.
 */
@API public data class ReadableForEachOptions(
  /**
   * The maximum number of concurrent map operations to perform.
   *
   * Default: `1`.
   */
  @get:Polyglot public var concurrency: Int = 1,

  /**
   * Abort signal to use for the operation.
   */
  @get:Polyglot public var signal: AbortSignal? = null,
)

/**
 * ## Readable To Array Options
 *
 * Defines the structure of options which relate to the `Readable.toArray` method.
 */
@API public data class ReadableToArrayOptions(
  /**
   * Abort signal to use for the operation.
   */
  @get:Polyglot public var signal: AbortSignal? = null,
)

/**
 * ## Readable Some Options
 *
 * Defines the structure of options which relate to the `Readable.some` method.
 */
@API public data class ReadableSomeOptions(
  /**
   * The maximum number of concurrent map operations to perform.
   *
   * Default: `1`.
   */
  @get:Polyglot public var concurrency: Int = 1,

  /**
   * Abort signal to use for the operation.
   */
  @get:Polyglot public var signal: AbortSignal? = null,
)

/**
 * ## Readable Find Options
 *
 * Defines the structure of options which relate to the `Readable.find` method.
 */
@API public data class ReadableFindOptions(
  /**
   * The maximum number of concurrent map operations to perform.
   *
   * Default: `1`.
   */
  @get:Polyglot public var concurrency: Int = 1,

  /**
   * Abort signal to use for the operation.
   */
  @get:Polyglot public var signal: AbortSignal? = null,
)

/**
 * ## Readable Every Options
 *
 * Defines the structure of options which relate to the `Readable.every` method.
 */
@API public data class ReadableEveryOptions(
  /**
   * The maximum number of concurrent map operations to perform.
   *
   * Default: `1`.
   */
  @get:Polyglot public var concurrency: Int = 1,

  /**
   * Abort signal to use for the operation.
   */
  @get:Polyglot public var signal: AbortSignal? = null,
)

/**
 * ## Readable Flat-map Options
 *
 * Defines the structure of options which relate to the `Readable.every` method.
 */
@API public data class ReadableFlatMapOptions(
  /**
   * The maximum number of concurrent map operations to perform.
   *
   * Default: `1`.
   */
  @get:Polyglot public var concurrency: Int = 1,

  /**
   * Abort signal to use for the operation.
   */
  @get:Polyglot public var signal: AbortSignal? = null,
)

/**
 * ## Readable Drop Options
 *
 * Defines the structure of options which relate to the `Readable.drop` method.
 */
@API public data class ReadableDropOptions(
  /**
   * Abort signal to use for the operation.
   */
  @get:Polyglot public var signal: AbortSignal? = null,
)

/**
 * ## Readable Take Options
 *
 * Defines the structure of options which relate to the `Readable.take` method.
 */
@API public data class ReadableTakeOptions(
  /**
   * Abort signal to use for the operation.
   */
  @get:Polyglot public var signal: AbortSignal? = null,
)

/**
 * ## Readable Reduce Options
 *
 * Defines the structure of options which relate to the `Readable.reduce` method.
 */
@API public data class ReadableReduceOptions(
  /**
   * Abort signal to use for the operation.
   */
  @get:Polyglot public var signal: AbortSignal? = null,
)

/**
 * ## Readable From Options
 *
 * Defines the structure of options which relate to the `Readable.from` static method.
 */
@API public data class ReadableFromOptions(
  /**
   * Abort signal to use for the operation.
   */
  @get:Polyglot public var signal: AbortSignal? = null,
)

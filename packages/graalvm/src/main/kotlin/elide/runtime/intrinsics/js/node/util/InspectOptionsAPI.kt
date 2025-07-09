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
package elide.runtime.intrinsics.js.node.util

import org.graalvm.polyglot.Value
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.node.util.InspectStyling

/**
 * ## Node Util: Inspect Options
 *
 * Describes available options which may be passed to the `util.inspect` method of the Node.js `util` built-in module,
 * and, as a consequence, to `util.formatWithOptions` or other depending methods. Inspect options govern how the
 * `inspect` method builds the string
 */
public interface InspectOptionsAPI : ReadOnlyProxyObject {
  /**
   * If `true`, object's non-enumerable symbols and properties are included in the formatted result. `<WeakMap>` and
   * `<WeakSet>` entries are also included as well as user defined prototype properties (excluding method properties).
   *
   * Default: `false`.
   */
  public val showHidden: Boolean

  /**
   * Specifies the number of times to recurse while formatting `object`. This is useful for inspecting large objects.
   * To recurse up to the maximum call stack size pass `Infinity` or `null`. Default: `2`.
   */
  public val depth: Int

  /**
   * If `true`, the output is styled with ANSI color codes. Colors are customizable.
   *
   * Default: `false`.
   */
  public val colors: Boolean

  /**
   * If `false`, `[util.inspect.custom](depth, opts, inspect)` functions are not invoked.
   *
   * Default: `true`.
   */
  public val customInspect: Boolean

  /**
   * If `true`, Proxy inspection includes the target and handler objects.
   *
   * Default: `false`.
   */
  public val showProxy: Boolean

  /**
   * Specifies the maximum number of `Array`, `<TypedArray>`, `<Map>`, `<WeakMap>`, and `<WeakSet>` elements to include
   * when formatting. Set to `null` or `Infinity` to show all elements. Set to `0` or negative to show no elements.
   *
   * Default: `100`.
   */
  public val maxArrayLength: Int

  /**
   * Specifies the maximum number of characters to include when formatting. Set to `null` or `Infinity` to show all
   * elements. Set to `0` or negative to show no characters.
   *
   * Default: `10000`.
   */
  public val maxStringLength: Int

  /**
   * The length at which input values are split across multiple lines. Set to `Infinity` to format the input as a single
   * line (in combination with `compact` set to `true` or any number >= `1`).
   *
   * Default: `80`.
   */
  public val breakLength: Int

  /**
   * Setting this to `false` causes each object key to be displayed on a new line. It will break on new lines in text
   * that is longer than `breakLength`. If set to a number, the most `n` inner elements are united on a single line as
   * long as all properties fit into `breakLength`. Short array elements are also grouped together.
   *
   * Default: `3`.
   */
  public val compact: Value?

  /**
   * If set to `true` or a function, all properties of an object, and `Set` and `Map` entries are sorted in the
   * resulting string. If set to `true` the default sort is used. If set to a function, it is used as a
   * compare function.
   *
   * Default: `false`.
   */
  public val sorted: Value?

  /**
   * If set to `true`, getters are inspected. If set to 'get', only getters without a corresponding setter are
   * inspected. If set to `'set'`, only getters with a corresponding setter are inspected. This might cause side effects
   * depending on the getter function.
   *
   * Default: `false`.
   */
  public val getters: Value?

  /**
   * If set to `true`, an underscore is used to separate every three digits in all big-ints and numbers.
   *
   * Default: `false`.
   */
  public val numericSeparator: Boolean

  /**
   * Suite of compiled style information to use when rendering with `colors` enabled.
   *
   * Default: `InspectStyling.default()`.
   */
  public val styles: InspectStyling get() = InspectStyling.default()
}

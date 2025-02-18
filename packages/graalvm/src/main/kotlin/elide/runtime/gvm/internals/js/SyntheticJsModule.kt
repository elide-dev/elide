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
package elide.runtime.gvm.internals.js

import elide.runtime.node.asserts.ExportKind

/**
 * ## Exported Module Symbol
 */
@JvmRecord public data class ExportedSymbol(
  val name: String,
  val kind: ExportKind,
  val from: String? = null,
) {
  public companion object {
    @JvmStatic public fun of(name: String, kind: ExportKind = ExportKind.PROPERTY, from: String? = null): ExportedSymbol
      = ExportedSymbol(name, kind, from)

    @JvmStatic public fun default(name: String, from: String? = null): ExportedSymbol =
      of(name, ExportKind.DEFAULT, from)

    @JvmStatic public fun method(name: String, from: String? = null): ExportedSymbol =
      of(name, ExportKind.METHOD, from)

    @JvmStatic public fun cls(name: String, from: String? = null): ExportedSymbol =
      of(name, ExportKind.CLASS, from)
  }
}

/**
 * ## Synthetic JavaScript Module
 */
public interface SyntheticJsModule {
  /**
   * Name of the intrinsic which will be loaded and mapped.
   */
  public val intrinsic: String

  /**
   * Whether to enable the default module export.
   */
  public val enableDefaultExport: Boolean get() = true

  /**
   * Whether to emit the initial primordial symbol load.
   */
  public val emitDefaultLoad: Boolean get() = true

  /**
   * Name to assign to the intrinsic symbol used as the module.
   */
  public val intrinsicLocalName: String get() = "intrinsic"

  /**
   * Symbol to load the intrinsic from.
   */
  public val intrinsicLoadFrom: String get() = "primordials"

  /**
   * Exported symbols provided by this module.
   *
   * @return Array of exported symbols.
   */
  public fun exports(): Array<ExportedSymbol> = emptyArray()

  /**
   * Rendered module facade.
   *
   * @return Rendered JavaScript code for this module's facade.
   */
  @Suppress("JSUnresolvedReference", "JSUnusedLocalSymbols")
  public fun facade(): String =
    // language=JavaScript
    StringBuilder().apply {
      val alias = intrinsicLocalName
      val primordials = intrinsicLoadFrom

      if (emitDefaultLoad) {
        appendLine("const { $intrinsic: $alias } = $primordials;")
      }
      var didExportDefault = false
      exports().forEach { symbol ->
        // use `from` symbol if defined (for aliasing), or pluck from the intrinsic
        val target = symbol.from ?: "$alias.${symbol.name}"

        when (symbol.kind) {
          ExportKind.DEFAULT -> appendLine("export default $target;").also { didExportDefault = true }
          ExportKind.CLASS,
          ExportKind.METHOD,
          ExportKind.PROPERTY -> appendLine("export const ${symbol.name} = ${target};")
        }
      }
      if (!didExportDefault && enableDefaultExport) {
        appendLine("export default $alias;")
      }
    }.toString()
}

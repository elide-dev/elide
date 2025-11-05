/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

@file:Suppress("LongParameterList")

package elide.tool.cli

import elide.runtime.lang.typescript.TypeScriptLanguage

/** Specifies languages supported for REPL access. */
enum class GuestLanguage (
  internal val id: String,
  override val engine: String = id,
  internal val formalName: String,
  internal val onByDefault: Boolean = false,
  internal val experimental: Boolean = false,
  internal val suppressExperimentalWarning: Boolean = false,
  internal val extensions: Set<String> = emptySet(),
  internal val mimeTypes: Set<String> = emptySet(),
  internal val dependsOn: List<GuestLanguage> = emptyList(),
  internal val executionMode: ExecutionMode = ExecutionMode.SOURCE_DIRECT,
  internal val secondary: Boolean = dependsOn.isNotEmpty(),
) : elide.runtime.gvm.GuestLanguage, elide.runtime.core.GuestLanguage {
  /** Interactive JavaScript VM. */
  JS (
    id = ENGINE_JS,
    formalName = "JavaScript",
    experimental = false,
    onByDefault = true,
    extensions = sortedSetOf("js", "cjs", "mjs"),
    mimeTypes = sortedSetOf("application/javascript", "application/javascript+module", "application/ecmascript"),
  ),

  /** JavaScript VM enabled with TypeScript support. */
  TYPESCRIPT (
    id = "ts",
    engine = ENGINE_JS,
    formalName = "TypeScript",
    experimental = false,
    onByDefault = true,
    extensions = sortedSetOf(
      TypeScriptLanguage.EXTENSION_TS,
      TypeScriptLanguage.EXTENSION_CTS,
      TypeScriptLanguage.EXTENSION_MTS,
      TypeScriptLanguage.EXTENSION_TSX,
      TypeScriptLanguage.EXTENSION_JSX,
    ),
    mimeTypes = sortedSetOf("application/typescript", "application/x-typescript", "text/typescript"),
  ),

  /** Interactive nested JVM. */
  LLVM (
    id = ENGINE_LLVM,
    formalName = "LLVM",
    experimental = true,
    executionMode = ExecutionMode.SOURCE_COMPILED,
    extensions = sortedSetOf("bc"),
  ),

  /** Interactive Python VM. */
  PYTHON (
    id = ENGINE_PYTHON,
    formalName = "Python",
    experimental = false,
    onByDefault = true,
    extensions = sortedSetOf("py"),
    mimeTypes = sortedSetOf("application/x-python-code", "text/x-python")
  ),

  /** Interactive Ruby VM. */
  RUBY (
    id = ENGINE_RUBY,
    formalName = "Ruby",
    experimental = true,
    onByDefault = true,
    extensions = sortedSetOf("rb"),
  ),

  /** Interactive PHP VM. */
  PHP (
    id = ENGINE_PHP,
    formalName = "PHP",
    experimental = true,
    onByDefault = true,
    extensions = sortedSetOf("php"),
  ),

  /** Interactive nested JVM. */
  JVM (
    id = "jvm",
    engine = ENGINE_JVM,
    formalName = "JVM",
    experimental = true,
    executionMode = ExecutionMode.SOURCE_COMPILED,
    extensions = sortedSetOf("class"),
  ),

  /** Interactive nested Java. */
  JAVA (
    id = "java",
    formalName = "JVM",
    experimental = true,
    executionMode = ExecutionMode.SOURCE_COMPILED,
    extensions = sortedSetOf("java"),
  ),

  /** Interactive nested JVM with Kotlin support. */
  KOTLIN (
    id = "kt",
    formalName = "Kotlin",
    engine = ENGINE_JVM,
    experimental = true,
    executionMode = ExecutionMode.SOURCE_COMPILED,
    extensions = sortedSetOf("kt", "kts"),
    dependsOn = listOf(JVM),
  ),

  /** Interactive nested JVM with Groovy support. */
  GROOVY (
    id = "groovy",
    engine = ENGINE_JVM,
    formalName = "Groovy",
    experimental = true,
    extensions = sortedSetOf("groovy"),
    executionMode = ExecutionMode.SOURCE_COMPILED,
    dependsOn = listOf(JVM),
  ),

  /** Interactive nested JVM with Scala support. */
  SCALA (
    id = "scala",
    engine = ENGINE_JVM,
    formalName = "Scala",
    experimental = true,
    extensions = sortedSetOf("scala"),
    executionMode = ExecutionMode.SOURCE_COMPILED,
    dependsOn = listOf(JVM),
  ),

  /** WebAssembly. */
  WASM (
    id = ENGINE_WASM,
    formalName = "WASM",
    experimental = true,
    suppressExperimentalWarning = true,
    extensions = sortedSetOf("wasm"),
    mimeTypes = sortedSetOf("application/wasm"),
  ),

  /** Apple Pkl. */
  PKL (
    id = ENGINE_PKL,
    formalName = "Pkl",
    experimental = true,
    suppressExperimentalWarning = true,
    extensions = sortedSetOf("pkl", "pcl"),
    mimeTypes = sortedSetOf("application/pkl", "text/pkl"),
  );

  companion object {
    /** @return Language based on the provided ID, or `null`. */
    internal fun resolveFromEngine(id: String): GuestLanguage? = when (id) {
      JS.engine -> JS
      TYPESCRIPT.engine -> TYPESCRIPT
      PYTHON.engine -> PYTHON
      RUBY.engine -> RUBY
      JVM.engine -> JVM
      WASM.engine -> WASM
      LLVM.engine -> LLVM
      PKL.engine -> PKL
      PHP.engine -> PHP
      else -> null
    }

    /** @return Language based on the provided ID, or `null`. */
    internal fun resolveFromId(id: String): GuestLanguage? = when (id) {
      JS.id -> JS
      PYTHON.id -> PYTHON
      RUBY.id -> RUBY
      JVM.id -> JVM
      WASM.id -> WASM
      LLVM.id -> LLVM
      TYPESCRIPT.id -> TYPESCRIPT
      PKL.id -> PKL
      PHP.id -> PHP

      // JVM extension guests
      KOTLIN.id -> KOTLIN
      JAVA.id -> JAVA
      GROOVY.id -> GROOVY
      else -> null
    }
  }

  override val symbol: String get() = id

  override val label: String get() = formalName

  override val languageId: String get() = engine
}

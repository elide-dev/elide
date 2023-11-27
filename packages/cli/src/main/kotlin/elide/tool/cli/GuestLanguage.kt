/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.tool.cli

/** Specifies languages supported for REPL access. */
enum class GuestLanguage (
  internal val id: String,
  override val engine: String = id,
  internal val formalName: String,
  internal val experimental: Boolean = false,
  internal val unimplemented: Boolean = false,
  internal val suppressExperimentalWarning: Boolean = false,
  internal val extensions: List<String> = emptyList(),
  internal val mimeTypes: List<String> = emptyList(),
  internal val dependsOn: List<GuestLanguage> = emptyList(),
  internal val secondary: Boolean = dependsOn.isNotEmpty(),
) : elide.runtime.gvm.GuestLanguage {
  /** Interactive JavaScript VM. */
  JS (
    id = ENGINE_JS,
    formalName = "JavaScript",
    experimental = false,
    extensions = listOf("js", "cjs", "mjs"),
    mimeTypes = listOf("application/javascript", "application/ecmascript"),
  ),

  /** Interactive nested JVM. */
  LLVM (
    id = ENGINE_LLVM,
    formalName = "LLVM",
    experimental = true,
    extensions = listOf("bc"),
  ),

  /** Interactive Python VM. */
  PYTHON (
    id = ENGINE_PYTHON,
    formalName = "Python",
    experimental = true,
    unimplemented = true,
    extensions = listOf("py"),
    dependsOn = listOf(LLVM),
  ),

  /** Interactive Python VM. */
  RUBY (
    id = ENGINE_RUBY,
    formalName = "Ruby",
    experimental = true,
    unimplemented = true,
    extensions = listOf("rb"),
  ),

  /** Interactive nested JVM. */
  JVM (
    id = "jvm",
    engine = ENGINE_JVM,
    formalName = "JVM",
    experimental = true,
    unimplemented = true,
    extensions = listOf("java"),
    mimeTypes = emptyList(),
  ),

  /** Interactive nested JVM with Kotlin support. */
  KOTLIN (
    id = "kt",
    formalName = "Kotlin",
    experimental = true,
    unimplemented = true,
    extensions = listOf("kt", "kts"),
    dependsOn = listOf(JVM),
  ),

  /** Interactive nested JVM with Groovy support. */
  GROOVY (
    id = "groovy",
    engine = ENGINE_JVM,
    formalName = "Groovy",
    experimental = true,
    unimplemented = true,
    extensions = listOf("groovy"),
    dependsOn = listOf(JVM),
  ),

  /** Interactive nested JVM with Scala support. */
  SCALA (
    id = "scala",
    engine = ENGINE_JVM,
    formalName = "Scala",
    experimental = true,
    unimplemented = true,
    extensions = listOf("scala"),
    dependsOn = listOf(JVM),
  ),

  /** Interactive nested JVM. */
  WASM (
    id = ENGINE_WASM,
    engine = "wasm",
    formalName = "WASM",
    experimental = true,
    suppressExperimentalWarning = true,
    extensions = listOf("wasm"),
    mimeTypes = listOf("application/wasm"),
  );

  companion object {
    /** @return Language based on the provided ID, or `null`. */
    internal fun resolveFromEngine(id: String): GuestLanguage? = when (id) {
      JS.engine -> JS
      PYTHON.engine -> PYTHON
      RUBY.engine -> RUBY
      JVM.engine -> JVM
      WASM.engine -> WASM
      LLVM.engine -> LLVM
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

      // JVM extension guests
      KOTLIN.id -> KOTLIN
      GROOVY.id -> GROOVY
      else -> null
    }
  }

  override val symbol: String get() = id

  override val label: String get() = formalName
}

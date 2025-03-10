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

package elide.tool.cli.cmd.repl

import com.google.common.collect.Sets
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine.Model.CommandSpec
import picocli.CommandLine.Option
import java.util.EnumSet
import elide.tool.cli.GuestLanguage
import elide.tool.cli.GuestLanguage.JS
import elide.tool.cli.GuestLanguage.JVM
import elide.tool.cli.GuestLanguage.KOTLIN
import elide.tool.cli.GuestLanguage.PYTHON
import elide.tool.cli.GuestLanguage.RUBY
import elide.tool.cli.GuestLanguage.TYPESCRIPT
import elide.tool.cli.GuestLanguage.WASM
import elide.tool.cli.cmd.repl.ToolShellCommand.Companion.languageAliasToEngineId

/** Allows selecting a language by name. */
@Introspected @ReflectiveAccess class LanguageSelector {
  /** Specifies the guest language(s) to support. */
  @Option(
    names = ["--language", "-l"],
    description = ["Specify language by name. Options: \${COMPLETION-CANDIDATES}."],
  )
  internal var language: EnumSet<GuestLanguage>? = null

  /** Flag for a JavaScript VM. */
  @Option(
    names = ["--js", "--javascript", "-js"],
    description = ["Equivalent to passing '--language=JS'."],
  )
  internal var javascript: Boolean = false

  /** Flag for JavaScript with TypeScript support/ */
  @Option(
    names = ["--ts", "--typescript", "-ts"],
    description = ["Equivalent to passing '--language=TYPESCRIPT'."],
  )
  internal var typescript: Boolean = false

  /** Flag for JVM support. */
  @Option(
    names = ["--jvm", "--java", "-java"],
    description = ["Equivalent to passing '--language=JVM'."],
  )
  internal var jvm: Boolean = false

  /** Flag for Kotlin support. */
  @Option(
    names = ["--kotlin", "--kt", "-kt"],
    description = ["Equivalent to passing '--language=KOTLIN'."],
  )
  internal var kotlin: Boolean = jvm

  /** Flag for Ruby support. */
  @Option(
    names = ["--ruby", "--rb", "-rb"],
    description = ["Equivalent to passing '--language=RUBY'."],
  )
  internal var ruby: Boolean = false

  /** Flag for Python support. */
  @Option(
    names = ["--python", "--py", "-py"],
    description = ["Equivalent to passing '--language=PYTHON'."],
  )
  internal var python: Boolean = false

  /** Flag for WebAssembly support. */
  @Option(
    names = ["--wasm"],
    description = ["Equivalent to passing '--language=WASM'."],
  )
  internal var wasm: Boolean = false

  /** Flag for LLVM support. */
  @Option(
    names = ["--llvm"],
    description = ["Equivalent to passing '--language=LLVM'."],
  )
  internal var llvm: Boolean = false

  private fun maybeMatchLanguagesByAlias(
    first: String?,
    second: String?,
    langs: EnumSet<GuestLanguage>,
  ): GuestLanguage? {
    val maybeResolvedFirst = first?.ifBlank { null }?.let {
      languageAliasToEngineId[it.trim().lowercase()]
    }
    val maybeResolvedSecond = second?.ifBlank { null }?.let {
      languageAliasToEngineId[it.trim().lowercase()]
    }
    return langs.firstOrNull {
      it.id == maybeResolvedFirst || it.id == maybeResolvedSecond
    }
  }

  // Resolve the primary interactive language.
  internal fun primary(
    spec: CommandSpec?,
    langs: EnumSet<GuestLanguage>,
    languageHint: String?,
  ): GuestLanguage {
    // languages by flags
    val explicitlySelectedLanguagesByBoolean = listOf(
      JS to javascript,
      TYPESCRIPT to typescript,
      RUBY to ruby,
      PYTHON to python,
      JVM to jvm,
      KOTLIN to kotlin,
      WASM to wasm,
    ).filter {
      langs.contains(it.first)  // is it supported?
    }.filter {
      it.second  // was it requested?
    }.map {
      it.first
    }.toSet()

    // languages by name
    val explicitlySelectedLanguagesBySet = Sets.intersection(language ?: emptySet(), langs)

    // language by alias
    val candidateArgs = spec?.commandLine()?.parseResult?.originalArgs()
    val languageHintMatch = langs.firstOrNull { it.id == languageHint }
    val candidateByName = languageHintMatch ?: maybeMatchLanguagesByAlias(
      candidateArgs?.firstOrNull(),
      candidateArgs?.getOrNull(1),
      langs,
    )

    val selected = (
            // `elide python` et al take maximum precedence
            candidateByName ?:

            // then languages specified via the `--languages` flag ("named")
            explicitlySelectedLanguagesBySet.firstOrNull() ?:

            // then languages specified via boolean flags like `--python`
            explicitlySelectedLanguagesByBoolean.firstOrNull()
            )
    return when {
      // if there is an explicitly selected language, and it is supported, use it
      selected != null && langs.contains(selected) -> selected

      // otherwise, we default to javascript
      else -> JS
    }
  }
}

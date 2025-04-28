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

package elide.tool.cli.options

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine.Parameters
import picocli.CommandLine.ScopeType.INHERIT
import elide.tool.cli.GuestLanguage

/**
 * TBD.
 */
@Introspected @ReflectiveAccess open class LanguagePositionals : OptionsMixin<LanguagePositionals> {
  @Suppress("EnumEntryName", "unused", "EnumNaming")
  enum class RunnerAlias (val language: GuestLanguage? = null, val action: Boolean = false) {
    js(GuestLanguage.JS),
    node(GuestLanguage.JS),
    deno(GuestLanguage.JS),
    javascript(GuestLanguage.JS),
    py(GuestLanguage.PYTHON),
    python(GuestLanguage.PYTHON),
    rb(GuestLanguage.RUBY),
    ruby(GuestLanguage.RUBY),
    java(GuestLanguage.JAVA),
    kt(GuestLanguage.KOTLIN),
    kotlin(GuestLanguage.KOTLIN),
  }

  /**
   * Language selector as positional.
   */
  @Parameters(
    index = "0",
    description = ["Selects a language or operating mode. See", "`elide run --help` for more information."],
    arity = "0..N",
    scope = INHERIT,
    paramLabel = "CMD_OR_LANGUAGE",
  )
  internal lateinit var language: List<RunnerAlias>

  override fun merge(other: LanguagePositionals?): LanguagePositionals {
    val positionals = LanguagePositionals()
    positionals.language = language.plus(other?.language ?: emptyList())
    return positionals
  }
}

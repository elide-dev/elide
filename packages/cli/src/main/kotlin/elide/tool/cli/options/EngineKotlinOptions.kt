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

package elide.tool.cli.options

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine.Option
import elide.tool.cli.GuestLanguage

/** Kotlin engine options. */
@Introspected @ReflectiveAccess class EngineKotlinOptions : AbstractEngineOptions() {
  override val engine: GuestLanguage get() = GuestLanguage.KOTLIN

  /** Whether to activate JVM debug mode. */
  @Option(
    names = ["--kt:debug"],
    description = ["Enable JVM debug mode"],
    defaultValue = "false",
  )
  override var debug: Boolean = false
}

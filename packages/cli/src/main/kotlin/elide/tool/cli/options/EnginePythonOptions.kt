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
import elide.runtime.gvm.GuestLanguage
import elide.runtime.plugins.python.PythonConfig

/** Python engine options. */
@Introspected @ReflectiveAccess class EnginePythonOptions : AbstractEngineOptions() {
  override val engine: GuestLanguage get() = GuestLanguage.PYTHON

  /** Whether to activate Python debug mode. */
  @Option(
    names = ["--python:debug"],
    description = ["Enable Python debug mode"],
    defaultValue = "false",
  )
  override var debug: Boolean = false

  /** Sets the Python engine. */
  @Option(
    names = ["--python:engine"],
    description = ["Python engine; one of 'default' or 'native'"],
    defaultValue = "default",
  )
  internal var pythonEngine: String = "default"

  internal fun apply(cfg: PythonConfig) {
    cfg.pythonEngine = pythonEngine
  }
}

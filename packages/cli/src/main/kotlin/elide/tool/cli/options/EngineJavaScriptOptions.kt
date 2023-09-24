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

package elide.tool.cli.options

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine.Option
import tools.elide.assets.EmbeddedScriptMetadata.JsScriptMetadata.JsLanguageLevel
import elide.runtime.gvm.GuestLanguage
import elide.runtime.plugins.js.JavaScriptConfig
import elide.runtime.plugins.js.JavaScriptVersion

/** JavaScript engine options. */
@Introspected @ReflectiveAccess class EngineJavaScriptOptions : AbstractEngineOptions() {
  override val engine: GuestLanguage get() = GuestLanguage.JAVASCRIPT

  /** Whether to activate JavaScript debug mode. */
  @Option(
    names = ["--js:debug"],
    description = ["Activate JavaScript debug mode"],
    defaultValue = "false",
  )
  override var debug: Boolean = EngineDefaults.DEBUG

  /** Whether to activate JS strict mode. */
  @Option(
    names = ["--js:strict"],
    description = ["Activate JavaScript strict mode"],
    defaultValue = "true",
  )
  internal var strict: Boolean = true

  /** Whether to activate JS strict mode. */
  @Option(
    names = ["--js:ecma"],
    description = ["ECMA standard to use for JavaScript."],
    defaultValue = "ES2022",
  )
  internal var ecma: JsLanguageLevel = JsLanguageLevel.ES2022

  /** Whether to activate NPM support. */
  @Option(
    names = ["--js:npm"],
    description = ["Whether to enable NPM support. Experimental."],
    defaultValue = "false",
  )
  internal var nodeModules: Boolean = false

  /** Whether to activate NPM support. */
  @Option(
    names = ["--js:esm"],
    description = ["Whether to enable ESM support. Experimental."],
    defaultValue = "false",
  )
  internal var esm: Boolean = false

  /** Whether to activate WASM support. */
  @Option(
    names = ["--js:wasm"],
    description = ["Whether to enable WebAssembly support. Experimental."],
    defaultValue = "false",
  )
  internal var wasm: Boolean = false

  /** Apply these settings to the configuration for the JavaScript runtime plugin. */
  internal fun apply(config: JavaScriptConfig) {
    config.language = JavaScriptVersion.valueOf(ecma.name)
    config.wasm = wasm
  }
}

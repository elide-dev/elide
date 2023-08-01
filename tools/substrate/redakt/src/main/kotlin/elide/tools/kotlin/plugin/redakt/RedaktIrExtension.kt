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

package elide.tools.kotlin.plugin.redakt

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.name.FqName

/**
 * # Redakt: IR Extension
 *
 * Extends the IR compiler with logic to handle detection of `Sensitive` annotations (by default,
 * [elide.annotations.data.Sensitive]). When found, the plugin will splice in replacement `toString` and related
 * methods to redact the value from logs.
 */
internal class RedaktIrExtension (
  private val messageCollector: MessageCollector,
  private val maskString: String,
  private val sensitiveAnnotation: String
) : IrGenerationExtension {
  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    val transformer = RedaktIrVisitor(
      pluginContext,
      FqName(sensitiveAnnotation),
      maskString,
      messageCollector,
    )
    moduleFragment.transform(
      transformer,
      null,
    )
  }
}

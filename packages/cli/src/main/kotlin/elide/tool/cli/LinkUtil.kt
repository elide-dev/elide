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

package elide.tool.cli

import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptConfirm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("DEPRECATION")
internal suspend fun CommandContext.promptForLink(
  redirectTarget: String,
  forThing: String,
  promptMessage: String = "Open link",
  force: Boolean = false,
) {
  val printLink: () -> Unit = {
    println("$promptMessage: $redirectTarget")
  }
  val openLink = force || KInquirer.promptConfirm(
    "Open the link for $forThing? 'No' will print it in the console",
    default = false,
  )
  if (openLink) withContext(Dispatchers.IO) {
    val os = System.getProperty("os.name", "unknown").lowercase()
    when {
      os.contains("windows") -> {
        Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler $redirectTarget")
      }
      os.contains("mac") || os.contains("darwin") || os.contains("linux") -> {
        Runtime.getRuntime().exec("open $redirectTarget")
      }
      else -> printLink.invoke()
    }
  } else printLink.invoke()
}

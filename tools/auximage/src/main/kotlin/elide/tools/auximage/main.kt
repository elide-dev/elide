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
package elide.tools.auximage

import kotlin.system.exitProcess

/** Entrypoint for the aux-image generator tool. */
suspend fun main(args: Array<String>) {
  if (args.isEmpty()) {
    println("Usage: auximage <lang> <output> <sources...>")
    exitProcess(1)
  } else AuxImageGenerator.generate(AuxImageParams.fromArgs(args)).let {
    when (it) {
      is AuxImageResult.Success -> {
        println("Successfully generated auxiliary image at ${it.path}")
      }

      is AuxImageResult.Failure -> {
        println("Failed to generate auxiliary image: ${it.message}")
        exitProcess(1)
      }
    }
  }
}

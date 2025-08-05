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
package dev.elide.secrets.impl

import dev.elide.secrets.Console
import elide.annotations.Singleton

/**
 * Implementation of [Console].
 *
 * @author Lauri Heino <datafox>
 */
@Singleton
internal class ConsoleImpl internal constructor() : Console {
  override fun print(string: String) {
    kotlin.io.print(string)
  }

  override fun println(string: String) {
    kotlin.io.println(string)
  }

  override fun readln(): String {
    return kotlin.io.readln()
  }

  override fun readPassword(): String {
    return System.console()?.readPassword()?.concatToString() ?: readln()
  }
}

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
package elide.tool.cli.progress

import kotlinx.serialization.json.Json

/**
 * Abstract base for progress tests.
 *
 * @author Lauri Heino <datafox>
 */
abstract class ProgressTestBase {
  val mockTerminal by lazy { MockTerminal(80) }
  val json by lazy { Json(Json.Default) { prettyPrint = true } }

  inline fun <reified T> readJson(fileName: String): T =
    requireNotNull(javaClass.getResourceAsStream(fileName))
      .bufferedReader()
      .use { it.readText() }
      .let { json.decodeFromString(it) }
}

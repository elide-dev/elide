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
@file:OptIn(DelicateElideApi::class)

package elide.runtime.plugins.js

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotEngine
import elide.runtime.plugins.vfs.Vfs

@OptIn(DelicateElideApi::class)
internal class JsCoreModulesTest {
  @ParameterizedTest
  @ValueSource(
    strings = ["buffer", "util", "fs", "express", "path", "assert"],
  )
  fun testCoreModulePresentCjs(module: String) {
    PolyglotEngine {
      install(JavaScript)
      install(Vfs)
    }.acquire().javascript(
      // language=js
      """require("$module");"""
    )
  }

  @ParameterizedTest
  @ValueSource(
    strings = ["node:buffer", "node:util", "node:fs", "node:express", "node:path", "node:assert"],
  )
  fun testCoreModulePresentNodePrefixCjs(module: String) {
    PolyglotEngine {
      install(JavaScript)
      install(Vfs)
    }.acquire().javascript(
      // language=js
      """require("$module");"""
    )
  }

  @ParameterizedTest
  @ValueSource(
    strings = ["buffer", "util", "fs", "express", "path", "assert"],
  )
  fun testCoreModulePresentEsm(module: String) {
    PolyglotEngine {
      install(JavaScript)
      install(Vfs)
    }.acquire().javascript(
      // language=js
      """
        import $module from "$module";
        $module;
      """.trimIndent(),
      esm = true,
    )
  }

  @ParameterizedTest
  @ValueSource(
    strings = ["node:buffer", "node:util", "node:fs", "node:express", "node:path", "node:assert"],
  )
  fun testCoreModulePresentNodePrefixEsm(module: String) {
    val modname = module.split(":").last()
    PolyglotEngine {
      install(JavaScript)
      install(Vfs)
    }.acquire().javascript(
      // language=js
      """
        import $modname from "$module";
        $modname;
      """.trimIndent(),
      esm = true,
    )
  }
}

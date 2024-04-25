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

@file:Suppress("unused", "unused_parameter", "all")
@file:OptIn(DelicateElideApi::class)

package dev.elide.runtime.tooling.tsc

import org.graalvm.polyglot.*
import java.nio.charset.StandardCharsets
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotEngine
import elide.runtime.plugins.js.JavaScript
import elide.runtime.plugins.js.javascript
import elide.runtime.plugins.vfs.Vfs

/**
 *
 */
object TypeScriptCompiler {
  private const val entry = "entry.mjs"

  /**
   *
   */
  @JvmStatic public fun tsc(vararg args: Array<String>) {
    // shared engine
    val engine = PolyglotEngine {
      // install plugins and configure engine
      install(Vfs)
      install(JavaScript)
    }

    // vm context
    val context = engine.acquire()

    val entrySrc = requireNotNull(TypeScriptCompiler::class.java.getResource(
      "/META-INF/elide/embedded/tools/tsc/$entry"
    )) {
      "Failed to resolve `$entry` script; required for proper operation of TypeScript compiler"
    }.readText()

    val entry = Source.newBuilder("js", entrySrc, entry)
      .internal(true)
      .cached(true)
      .encoding(StandardCharsets.UTF_8)
      .interactive(false)
      .build()

    val entrypoint = context.javascript(
      entry
    )

    context.enter()
    val result = try {
      entrypoint.getMember("default").execute(args)
    } finally {
      context.leave()
    }

    // profit
    println("Result: ${result.asInt()}")
  }

  @JvmStatic fun main(args: Array<String>) {
    tsc(args)
  }
}

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

package dev.elide.runtime.tooling.tsc

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.Source
import java.nio.charset.StandardCharsets

/**
 *
 */
object TypeScriptCompiler {
  private const val entry = "entry.mjs"

  /**
   *
   */
  @JvmStatic public fun tsc(vararg args: Array<String>) {
//    val zipPath = requireNotNull(TypeScriptCompiler::class.java.getResource(
//      "/META-INF/elide/embedded/tools/tsc/typescript.zip"
//    )) {
//      "Failed to resolve `typescript.zip` bundle; required for proper operation of TypeScript compiler"
//    }.toURI().toPath()

    val engine = Engine.newBuilder("js")
      .build()

//    val vfs = OverlayVFS.zipOverlay(zipPath)
    val ctx = Context.newBuilder("js")
      .engine(engine)
      .allowAllAccess(true)
      .allowExperimentalOptions(true)
      .option("js.ecmascript-version", "2023")
//      .option("js.commonjs-require", "true")
//      .option("js.commonjs-require-cwd", "/")
//      .option("js.esm-bare-specifier-relative-lookup", "true")
//      .option("js.esm-eval-returns-exports", "true")
//      .allowIO(IOAccess.newBuilder()
//                 .fileSystem(vfs)
//                 .build())
      .build()

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

    ctx.enter()
    val entrypoint = try {
      ctx.eval(entry)
    } finally {
      ctx.leave()
    }

    ctx.enter()
    val result = try {
      entrypoint.getMember("default").execute(args)
    } finally {
      ctx.leave()
    }

    println("Result: ${result.asInt()}")
  }

  @JvmStatic fun main(args: Array<String>) {
    tsc(args)
  }
}

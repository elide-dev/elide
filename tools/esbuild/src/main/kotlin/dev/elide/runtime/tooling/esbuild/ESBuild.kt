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

package dev.elide.runtime.tooling.esbuild

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.io.ByteSequence
import java.nio.charset.StandardCharsets
import java.util.function.Consumer

/**
 *
 */
public object ESBuild {
  private val callable: Value by lazy {
    val ctx = Context.newBuilder("js", "wasm")
      .allowAllAccess(true)
      .allowExperimentalOptions(true)
      .option("js.esm-bare-specifier-relative-lookup", "true")
      .option("js.esm-eval-returns-exports", "true")
      .option("js.webassembly", "true")
      .build()

    val srcs = listOf(
      "crypto.js" to ("crypto.js" to "js"),
      "polyfills.js" to ("polyfills.js" to "js"),
    ).map {
      readSourcefile(it.second.second, it.first, it.second.first)
    }

// "esbuild.wasm" to ("esbuild.wasm" to "wasm"),
// "wasm_exec.js" to ("wasm_exec.js" to "js"),
// "wasm_exec_node.js" to ("wasm_exec_node.js" to "js"),
// "mod.min.js" to ("mod.js" to "js"),

    val entry = Source.newBuilder(
      "js",
      requireNotNull(
        ESBuild::class.java.getResourceAsStream("/META-INF/elide/embedded/tools/esbuild/entry.mjs"),
      ).readBytes().inputStream().reader(StandardCharsets.UTF_8),
      "entry.mjs",
    ).build()

    ctx.enter()
    val evaluated = srcs.map { ctx.eval(it) }
    val esbuildWasm = evaluated[2]
    // val wasmEntry = ctx.getBindings("wasm").getMember("esbuild.wasm")
    ctx.getBindings("js").putMember("ESBUILD", esbuildWasm)
    val symbols = ctx.eval(entry)
    ctx.leave()
    val setup: Value = symbols.getMember("setup")
    setup.execute()
    val main: Value = symbols.getMember("main")
    main
  }

  private fun readSourcefile(
lang: String,
 srcfile: String,
 name: String
): Source {
    val bytes = ESBuild::class.java.getResourceAsStream(
      "/META-INF/elide/embedded/tools/esbuild/$srcfile"
    )?.readBytes()

    requireNotNull(bytes) { "Failed to load $name" }

    return if (lang == "wasm") {
      Source.newBuilder(lang, ByteSequence.create(bytes), name).build()
    } else {
      Source.newBuilder(lang, bytes.inputStream().reader(StandardCharsets.UTF_8), name).build()
    }
  }

  /**
   *
   */
  public fun run(args: Array<String>) {
    val result = callable.execute(args)
    val javaThen: Consumer<Any> = Consumer<Any> {
      // nothing at this time
    }
    result.invokeMember("then", javaThen)
  }

  @JvmStatic public fun main(args: Array<String>) {
    run(args)
  }
}

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

package elide.runtime.ruby

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.Source
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.system.exitProcess
import elide.runtime.Logging

/**
 * # Elide Ruby
 *
 * Coming soon.
 */
public object ElideRuby {
  /**
   * Public entrypoint when run as a binary.
   */
  @JvmStatic public fun main(args: Array<String>) {
    val engine = Engine.newBuilder("ruby")
      .build()

    val ctx = Context.newBuilder("ruby")
      .engine(engine)
      .build()

    when (val target = args.firstOrNull()?.ifBlank { null }) {
      null -> exitErr("Must provide script (usage: `ruby <file>`")
      else -> Path(target).let { asPath ->
        when {
          !Files.exists(asPath) -> exitErr("Specified path does not exist")
          !Files.isReadable(asPath) -> exitErr("Specified path is not readable")
          else -> asPath.toUri()
        }
      }
    }.let { srcUri ->
      Source.newBuilder("ruby", srcUri.toURL())
        .cached(true)
        .internal(false)
        .interactive(true)
        .uri(srcUri)
        .encoding(StandardCharsets.UTF_8)
        .build()
    }.let { src ->
      ctx.use {
        ctx.enter()
        try {
          ctx.eval(src)
        } catch (err: Throwable) {
          exitErr("Error from Elide Ruby: ${err.message} (type: ${err::class.java.simpleName})")
        } finally {
          ctx.leave()
        }
      }
    }
  }
}

private fun exitErr(message: String, code: Int = 1): Nothing {
  Logging.root().error("Elide Ruby failed: $message")
  exitProcess(code)
}

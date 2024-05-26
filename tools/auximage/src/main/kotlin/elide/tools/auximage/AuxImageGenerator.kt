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

import org.graalvm.polyglot.*
import org.graalvm.polyglot.SandboxPolicy.TRUSTED
import org.graalvm.polyglot.io.IOAccess
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.io.path.name
import elide.runtime.Logging

/**
 * # Auxiliary Image Generator
 *
 * Tool which generates an auxiliary image for use with GraalVM/Elide; accepts a suite of source files, each of which
 * is evaluated within a given context with aggressive JIT, and then exported as an image to a given output path.
 */
object AuxImageGenerator {
  // Private logging pipe.
  private val logging by lazy { Logging.of(AuxImageGenerator::class) }

  // prepare engine
  private val engine = Engine.newBuilder()
    .allowExperimentalOptions(true)
    .sandbox(TRUSTED)
    .useSystemProperties(true)
    .option("engine.CacheCompile", "hot")
    .option("engine.TraceCache", "true")
    .option("engine.Cache", "./aux-img.bin")
    .build()

  // prepare context
  private val ctx: Context = Context.newBuilder()
    .engine(engine)
    .allowPolyglotAccess(PolyglotAccess.ALL)
    .allowNativeAccess(true)
    .allowCreateThread(true)
    .allowCreateProcess(false)
    .allowInnerContextOptions(true)
    .allowEnvironmentAccess(EnvironmentAccess.NONE)
    .allowIO(IOAccess.NONE)
    .build()

  /**
   * ## Generate Image
   *
   * Generate an auxiliary image from the provided suite of [params].
   *
   * @param params Parameters to treat as inputs to the auxiliary image builder
   * @return Resulting auxiliary image structure, describing either an error or successful image build
   */
  suspend fun generate(params: AuxImageParams): AuxImageResult = withContext(Dispatchers.IO) {
    // prepare sources
    val sources = params.sources.asSequence().map {
      when {
        !Files.exists(it) -> throw AuxImageResult.Failure("Source file does not exist: $it")
        !Files.isReadable(it) -> throw AuxImageResult.Failure("Source file is not readable: $it")
        else -> it to Files.readString(it)
      }
    }.map { (path, contents) ->
      Source.newBuilder(params.lang, contents, path.name)
        .encoding(StandardCharsets.UTF_8)
        .internal(true)
        .cached(true)
        .interactive(false)
        .build()
    }

    // use the context to build the image
    engine.use {
      ctx.use {
        ctx.enter()
        try {
          sources.forEach(ctx::eval)
        } finally {
          ctx.leave()
        }
      }
    }

    // image writes to disk in background, we can safely finish now
    AuxImageResult.Success(params.output.toString())
  }
}

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
  private lateinit var engine: Engine

  // prepare context
  private lateinit var ctx: Context

  @JvmStatic internal fun initialize(params: AuxImageParams) {
    initialize(
      params.lang,
      params.engine,
      params.context,
    )
  }

  @JvmStatic internal fun initialize(
    language: String,
    engineOptions: Map<String, String?>,
    contextOptions: Map<String, String?>,
  ) {
    logging.info("Initializing auxiliary image generator for language: $language")

    engine = Engine.newBuilder(language)
      .allowExperimentalOptions(true)
      .sandbox(TRUSTED)
      .useSystemProperties(true)
      .option("engine.CacheCompile", "executed")
      .option("engine.TraceCache", "true")
      .option("engine.Cache", "./aux-img.bin")
      .option("engine.Mode", "latency")
      .option("engine.Compilation", "true")
      .option("engine.MultiTier", "true")
      .option("engine.Splitting", "true")
      .option("engine.UpdatePolicy", "always")
      .option("engine.OSR", "true")
      .option("compiler.FirstTierUseEconomy", "false")
      .apply {
        // apply CLI-provided options
        engineOptions.keys.forEach {
          logging.debug("- Setting engine option: $it=${engineOptions[it]}")
          option(it, engineOptions[it])
        }
      }
      .build()

    logging.info("Engine initialized successfully. Initializing context...")
    ctx = Context.newBuilder(language)
      .engine(engine)
      .allowPolyglotAccess(PolyglotAccess.ALL)
      .allowNativeAccess(true)
      .allowCreateThread(true)
      .allowCreateProcess(false)
      .allowInnerContextOptions(true)
      .allowEnvironmentAccess(EnvironmentAccess.NONE)
      .allowIO(IOAccess.NONE)
      .apply {
        // apply CLI-provided options
        contextOptions.keys.forEach {
          logging.debug("- Setting context option: $it=${contextOptions[it]}")
          option(it, contextOptions[it])
        }
      }
      .build()

    logging.info("Context initialized successfully.")
  }

  // Prepare a context with bindings, intrinsics, and other initialization steps.
  private fun Context.prepare() {
    // nothing yet
  }

  /**
   * ## Generate Image
   *
   * Generate an auxiliary image from the provided suite of [params].
   *
   * @param params Parameters to treat as inputs to the auxiliary image builder
   * @return Resulting auxiliary image structure, describing either an error or successful image build
   */
  suspend fun generate(params: AuxImageParams): AuxImageResult = withContext(Dispatchers.IO) {
    logging.info("Generating auxiliary image for ${params.sources.size} sources...")
    logging.debug("Aux image params: $params")

    // prepare sources
    val sources = params.sources.asSequence().map {
      when {
        !Files.exists(it) -> throw AuxImageResult.Failure("Source file does not exist: $it")
        !Files.isReadable(it) -> throw AuxImageResult.Failure("Source file is not readable: $it")
        else -> it to Files.readString(it)
      }
    }.map { (path, contents) ->
      logging.debug("- Creating source at name '${path.name}'")
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
        // "prepare" the context with intrinsics and other initialization steps
        ctx.prepare()

        // obtain a lock on the context
        ctx.enter()
        try {
          // evaluate all sources
          sources.forEach(ctx::eval)
        } finally {
          // leave the context
          ctx.leave()
        }
      }
    }

    // image writes to disk in background, we can safely finish now
    AuxImageResult.Success(params.output.toString())
  }

  /**
   * ## Check Image
   *
   * Generate an auxiliary image from the provided suite of [params].
   *
   * @param params Parameters to treat as inputs to the auxiliary image builder
   * @return Resulting auxiliary image structure, describing either an error or successful image build
   */
  suspend fun check(params: AuxImageParams): AuxImageResult = withContext(Dispatchers.IO) {
    logging.info("Image checking is not implemented yet.")

    // we return a success response if the image is valid
    AuxImageResult.Success(params.output.toString())
  }
}

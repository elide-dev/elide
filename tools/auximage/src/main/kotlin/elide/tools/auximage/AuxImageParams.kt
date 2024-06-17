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
@file:Suppress("DataClassPrivateConstructor")

package elide.tools.auximage

import java.nio.file.Path
import kotlinx.serialization.Serializable

/**
 * # Auxiliary Image Parameters
 *
 * @param sources The source files to include in the image; evaluated in order
 * @param output Path to write the image to*
 * @param action Action to perform with the auxiliary image
 * @param silent Whether to squelch most output
 * @param verbose Whether to activate debug logging; wins over `silent`
 * @param lang Language to specify for the image; defaults to `js`
 * @param engine Options to apply to the executing engine
 * @param context Options to apply to the executing context
 */
@Serializable
@JvmRecord data class AuxImageParams private constructor (
  val sources: Collection<Path>,
  val output: Path,
  val action: AuxImageAction = AuxImageAction.BUILD,
  val silent: Boolean = DEFAULT_SILENT,
  val verbose: Boolean = DEFAULT_VERBOSE,
  val lang: String = DEFAULT_LANG,
  val engine: Map<String, String?> = emptyMap(),
  val context: Map<String, String?> = emptyMap(),
) {
  companion object {
    private const val DEFAULT_SILENT = false
    private const val DEFAULT_VERBOSE = false
    private const val DEFAULT_LANG = "js"
    private const val DEFAULT_OUTPUT = "image.${DEFAULT_LANG}.bin"

    /** Build auxiliary image params from CLI arguments; only the <output> and <sources...> are supported. */
    @JvmStatic fun fromArgs(args: Array<String>): AuxImageParams {
      val action = AuxImageAction.resolve(
        args.getOrNull(0) ?: error("Please specify an action; valid actions are `build` and `check`")
      )
      val lang = args.getOrNull(1)
      val outputPath = args.getOrNull(2)
      if (lang.isNullOrEmpty()) error("Cannot run with no language specified. Please specify language.")
      if (outputPath == null) error("Cannot run with no output path specified. Please specify an output path.")
      val output = Path.of(outputPath)
      val sources = args.drop(3).map { Path.of(it) }

      // `build` requires sources
      if (action == AuxImageAction.BUILD)
        require(sources.isNotEmpty()) { "Cannot generate aux image from empty source set" }

      return of(action = action, sources = sources, output = output)
    }

    /** Build auxiliary image params from scratch. */
    @JvmStatic fun of(
      sources: Collection<Path>,
      action: AuxImageAction = AuxImageAction.BUILD,
      silent: Boolean = DEFAULT_SILENT,
      verbose: Boolean = DEFAULT_VERBOSE,
      lang: String = DEFAULT_LANG,
      output: Path = Path.of(DEFAULT_OUTPUT),
      engine: Map<String, String?> = emptyMap(),
      context: Map<String, String?> = emptyMap(),
    ): AuxImageParams = AuxImageParams(
      action = action,
      silent = silent,
      verbose = verbose,
      lang = lang,
      sources = sources,
      output = output,
      engine = engine,
      context = context,
    )
  }
}

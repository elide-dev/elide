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
 * @param silent Whether to squelch most output
 * @param verbose Whether to activate debug logging; wins over `silent`
 * @param lang Language to specify for the image; defaults to `js`
 */
@Serializable
@JvmRecord data class AuxImageParams private constructor (
  val sources: Collection<Path>,
  val output: Path,
  val silent: Boolean = DEFAULT_SILENT,
  val verbose: Boolean = DEFAULT_VERBOSE,
  val lang: String = DEFAULT_LANG,
) {
  companion object {
    private const val DEFAULT_SILENT = false
    private const val DEFAULT_VERBOSE = false
    private const val DEFAULT_LANG = "js"
    private const val DEFAULT_OUTPUT = "image.${DEFAULT_LANG}.bin"

    /** Build auxiliary image params from CLI arguments; only the <output> and <sources...> are supported. */
    @JvmStatic fun fromArgs(args: Array<String>): AuxImageParams {
      val lang = args.getOrNull(0)
      val outputPath = args.getOrNull(1)
      if (lang.isNullOrEmpty()) error("Cannot run with no language specified. Please specify language.")
      if (outputPath == null) error("Cannot run with no output path specified. Please specify an output path.")

      val output = Path.of(outputPath)
      val sources = args.drop(2).map { Path.of(it) }
      return of(
        sources = sources,
        output = output,
      )
    }

    /** Build auxiliary image params from scratch. */
    @JvmStatic fun of(
      sources: Collection<Path>,
      silent: Boolean = DEFAULT_SILENT,
      verbose: Boolean = DEFAULT_VERBOSE,
      lang: String = DEFAULT_LANG,
      output: Path = Path.of(DEFAULT_OUTPUT),
    ): AuxImageParams = AuxImageParams(
      silent = silent,
      verbose = verbose,
      lang = lang,
      sources = sources,
      output = output,
    )
  }
}

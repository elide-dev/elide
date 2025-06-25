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
package elide.tooling.containers

import java.net.URI
import java.nio.file.Path
import kotlinx.serialization.Serializable
import elide.runtime.Logging
import elide.tooling.Arguments
import elide.tooling.Environment
import elide.tooling.Inputs
import elide.tooling.MutableArguments
import elide.tooling.Outputs
import elide.tooling.Tool
import elide.tooling.AbstractTool

// Internal debug logs.
private const val JIB_DEBUG_LOGGING = false

// Name of the Jib tool.
public const val JIB: String = "jib"

// Jib version.
public const val JIB_VERSION: String = "3.4.5"

// Description to show.
public const val JIB_DESCRIPTION: String =
  "Jib can build container images from JVM or native apps."

private const val JIB_URL: String = "https://cloud.google.com/java/getting-started/jib"

// Tool description.
public val jib: Tool.CommandLineTool = Tool.describe(
  name = JIB,
  label = "Jib",
  version = JIB_VERSION,
  docs = URI.create(JIB_URL),
  description = JIB_DESCRIPTION,
  registered = true,
  helpText = """
Usage: elide jib -- [-hV] [@<filename>...] COMMAND
A tool for creating container images
      [@<filename>...]   One or more argument files containing options.
  -h, --help             Show this help message and exit.
  -V, --version          Print version information and exit.

Commands:
  build  Build a container
  jar    Containerize a jar
  war    Containerize a war

Build (`jib build`):

  Usage: elide jib build -- [-hV] [--allow-insecure-registries]
                   [--send-credentials-over-http] [-b=<build-file>]
                   [--base-image-cache=<cache-directory>] [-c=<project-root>]
                   [--console=<type>] [--image-metadata-out=<path-to-json>]
                   [--name=<image-reference>] [--project-cache=<cache-directory>]
                   -t=<target-image> [--verbosity=<level>]
                   [--additional-tags=<tag>[,<tag>...]]... [-p=<name>=<value>]...
                   [--credential-helper=<credential-helper> |
                   [--username=<username> --password[=<password>]] |
                   [[--to-credential-helper=<credential-helper> |
                   [--to-username=<username> --to-password[=<password>]]]
                   [--from-credential-helper=<credential-helper> |
                   [--from-username=<username> --from-password[=<password>]]]]]
                   [@<filename>...]
  Build a container
        [@<filename>...]      One or more argument files containing options.
        --additional-tags=<tag>[,<tag>...]
                              Additional tags for target image
        --allow-insecure-registries
                              Allow jib to communicate with registries over http
                                (insecure)
    -b, --build-file=<build-file>
                              The path to the build file (ex: path/to/other-jib.
                                yaml)
        --base-image-cache=<cache-directory>
                              A path to a base image cache
    -c, --context=<project-root>
                              The context root directory of the build (ex:
                                path/to/my/build/things)
        --console=<type>      set console output type, candidates: auto, rich,
                                plain, default: auto
        --credential-helper=<credential-helper>
                              credential helper for communicating with both
                                target and base image registries, either a path
                                to the helper, or a suffix for an executable
                                named `docker-credential-<suffix>`
        --from-credential-helper=<credential-helper>
                              credential helper for communicating with base image
                                registry, either a path to the helper, or a
                                suffix for an executable named
                                `docker-credential-<suffix>`
        --from-password[=<password>]
                              password for communicating with base image registry
        --from-username=<username>
                              username for communicating with base image registry
    -h, --help                Show this help message and exit.
        --image-metadata-out=<path-to-json>
                              path to the json file that should contain image
                                metadata (for example, digest, id and tags) after
                                build iscomplete
        --name=<image-reference>
                              The image reference to inject into the tar
                                configuration (required when using --target tar:
                                //...)
    -p, --parameter=<name>=<value>
                              templating parameter to inject into build file,
                                replace ${'$'}{<name>} with <value> (repeatable)
        --password[=<password>]
                              password for communicating with both target and
                                base image registries
        --project-cache=<cache-directory>
                              A path to the project cache
        --send-credentials-over-http
                              Allow jib to send credentials over http (very
                                insecure)
    -t, --target=<target-image>
                              The destination image reference or jib style url,
                              examples:
                               gcr.io/project/image,
                               registry://image-ref,
                               docker://image,
                               tar://path
        --to-credential-helper=<credential-helper>
                              credential helper for communicating with target
                                registry, either a path to the helper, or a
                                suffix for an executable named
                                `docker-credential-<suffix>`
        --to-password[=<password>]
                              password for communicating with target image
                                registry
        --to-username=<username>
                              username for communicating with target image
                                registry
        --username=<username> username for communicating with both target and
                                base image registries
    -V, --version             Print version information and exit.
        --verbosity=<level>   set logging verbosity, candidates: quiet, error,
                                warn, lifecycle, info, debug, default: lifecycle

  For more information about Jib, see $JIB_URL
  For usage within Elide, see https://docs.elide.dev
"""
)

/**
 * # Jib Driver
 *
 * Implements an [AbstractTool] adapter to `jib`, a container image builder tool; Jib accepts a base image, target image
 * coordinates, and inputs as JAR files, bundles, and resources, and then assembles a compliant OCI or Docker container
 * image from those.
 *
 * Jib is capable of fetching the base image layers to build from, and then assembling the image itself, without calling
 * into Docker or any other tools.
 */
public class JibDriver (
  args: Arguments,
  env: Environment,
  public val inputs: JibInputs,
  public val outputs: JibOutputs,
  private val configurator: JibConfigurator.() -> Unit = {},
  private val projectRoot: Path? = null,
) : AbstractTool(info = jib.extend(
  args,
  env,
).using(
  inputs = inputs,
  outputs = outputs.flatten(),
)) {
  /**
   * ## Jib Configurator
   *
   * Function which has a chance to change Jib's build configuration before the build is executed.
   */
  public interface JibConfigurator {
    /**
     * Arguments to pass to the Jib CLI.
     */
    public val args: MutableArguments
  }

  /**
   * ## Jib Inputs
   *
   * Models inputs for Jib container image builds.
   */
  public sealed interface JibInputs : Inputs.Files {
    /**
     * Input to Jib as a Jar.
     *
     * @property path Path to the JAR to build from.
     */
    @Serializable public data class Jar internal constructor (public val path: Path): JibInputs, Inputs.Files

    /**
     * Base container configuration for Jib.
     *
     * @property coordinate Base container coordinate.
     */
    @Serializable public data class BaseContainer internal constructor (
      public val coordinate: ContainerCoordinate,
    ): JibInputs, Inputs.None

    /**
     * Target container configuration for Jib.
     *
     * @property coordinate Target container coordinate.
     */
    @Serializable public data class TargetContainer internal constructor (
      public val coordinate: ContainerCoordinate,
    ): JibInputs, Inputs.None

    /**
     * Suite of diverse inputs for Jib.
     *
     * @property constituents Inputs part of this compound input.
     */
    @Serializable public data class Compound internal constructor (
      public val constituents: Collection<JibInputs> = emptyList(),
    ): JibInputs, Inputs.Compound

    /** No structured inputs. */
    @Serializable public data object NoInputs : JibInputs, Inputs.None
  }

  /**
   * ## Jib Outputs
   *
   * Models output options for Jib container image builds.
   */
  @Serializable public sealed interface JibOutputs {
    /**
     * Flatten into an [Outputs] type.
     *
     * @return Outputs value.
     */
    public fun flatten(): Outputs

    /**
     * Outputs a Jib build directly to Docker's local image store.
     *
     * @property tag Coordinate for the target container image.
     */
    @Serializable public data class OutputToDocker internal constructor (
      public val tag: ContainerCoordinate,
    ): JibOutputs, Outputs.None {
      override fun flatten(): Outputs = this
    }

    /**
     * Outputs a Jib build to an image tarball.
     *
     * @property path Path to the output tarball.
     */
    @Serializable public data class Tarball internal constructor (
      public val path: Path,
    ): JibOutputs, Outputs.Disk.File {
      override fun flatten(): Outputs = this
    }

    /** No structured outputs. */
    @Serializable public data object NoOutputs : JibOutputs, Outputs.None {
      override fun flatten(): Outputs = this
    }
  }

  @Suppress("TooGenericExceptionCaught")
  override suspend fun invoke(state: EmbeddedToolState): Tool.Result {
    debugLog("Preparing Jib invocation")
    val mut = Arguments.of(info.args.asArgumentSequence()).toMutable().apply {
      // arg mutations here
    }
    val argList = mut.asArgumentStrings().toList()
    return try {
      debugLog("Finalized Jib arguments: ${argList.joinToString(" ")}")
      com.google.cloud.tools.jib.cli.JibCli.main(
        argList.toTypedArray(),
      )
      Tool.Result.Success
    } catch (err: Throwable) {
      logging.error("Jib build failed", err)
      Tool.Result.UnspecifiedFailure
    }
  }

  /** Factories for configuring and obtaining instances of [JibDriver]. */
  public companion object {
    /**
     * Create outputs for Jib which place an image in Docker's local image store.
     *
     * @param tag Coordinate for the target container image.
     * @return Tool outputs.
     */
    @JvmStatic public fun dockerOutput(tag: ContainerCoordinate): JibOutputs = JibOutputs.OutputToDocker(tag)

    /**
     * Create outputs for Jib which place an image in a tarball.
     *
     * @param path Path to the output tarball.
     * @return Tool outputs.
     */
    @JvmStatic public fun tarballOutput(path: Path): JibOutputs = JibOutputs.Tarball(path)

    /**
     * Create a base-container specification for Jib.
     *
     * @param at Coordinate for the base container to build from.
     * @return Jib inputs.
     */
    @JvmStatic public fun baseContainer(at: ContainerCoordinate): JibInputs = JibInputs.BaseContainer(at)

    /**
     * Create a target-container specification for Jib.
     *
     * @param at Coordinate for the base container to build from.
     * @return Jib inputs.
     */
    @JvmStatic public fun targetContainer(at: ContainerCoordinate): JibInputs = JibInputs.BaseContainer(at)

    /**
     * Create a JAR input type for Jib.
     *
     * @param path Path to the JAR to build from.
     * @return Jib inputs.
     */
    @JvmStatic public fun jarInput(path: Path): JibInputs = JibInputs.Jar(path)

    /**
     * Create a suite of inputs for Jib (from arguments).
     *
     * @param inputs Collection of inputs to include in the compound input.
     */
    @JvmStatic public fun jibInputs(vararg inputs: JibInputs): JibInputs = jibInputs(inputs.toList())

    /**
     * Create a suite of inputs for Jib (from a collection).
     *
     * @param inputs Collection of inputs to include in the compound input.
     */
    @JvmStatic public fun jibInputs(inputs: Collection<JibInputs>): JibInputs = JibInputs.Compound(inputs)

    // Logging.
    @JvmStatic private val logging by lazy { Logging.of(JibDriver::class) }

    @JvmStatic private fun debugLog(message: String) {
      if (JIB_DEBUG_LOGGING) {
        System.err.println("[jib:debug] $message")
      }
    }
  }
}

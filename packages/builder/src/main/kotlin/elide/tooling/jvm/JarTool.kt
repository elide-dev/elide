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
@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package elide.tooling.jvm

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import sun.tools.jar.JarToolProvider
import java.io.PrintWriter
import java.net.URI
import java.nio.file.Path
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import elide.runtime.Logger
import elide.runtime.Logging
import elide.tooling.Arguments
import elide.tooling.Environment
import elide.tooling.Inputs
import elide.tooling.MutableArguments
import elide.tooling.Outputs
import elide.tooling.Tool
import elide.tooling.GenericTool

// Name of the `jar` tool.
public const val JAR: String = "jar"

// Description to show for `jar`.
public const val JARTOOL_DESCRIPTION: String = "Builds and manages Java Archive files (JARs)."

// Tool description.
public val jartool: Tool.CommandLineTool = Tool.describe(
  name = JAR,
  label = "JAR Tool",
  version = System.getProperty("java.version"),
  docs = URI.create("https://docs.oracle.com/javase/8/docs/technotes/guides/jar/index.html"),
  description = JARTOOL_DESCRIPTION,
  registered = true,
  helpText = """
    Usage: elide jar <elide option...> -- [OPTION...] [ [--release VERSION] [-C dir] files] ...
    Java: ${System.getProperty("java.version")}

    jar creates an archive for classes and resources, and can manipulate or
    restore individual classes or resources from an archive.

    where possible elide options include:
      (None at this time.)

    Examples:
     # Create an archive called classes.jar with two class files:
     elide jar --create --file classes.jar Foo.class Bar.class

     # Create an archive using an existing manifest, with all the files in foo/:
     elide jar --create --file classes.jar --manifest mymanifest -C foo/ .

     # Create a modular jar archive, where the module descriptor is located in
     # classes/module-info.class:
     elide jar --create --file foo.jar --main-class com.foo.Main --module-version 1.0
         -C foo/ classes resources

     # Update an existing non-modular jar to a modular jar:
     elide jar --update --file foo.jar --main-class com.foo.Main --module-version 1.0
         -C foo/ module-info.class

     # Create a multi-release jar, placing some files in the META-INF/versions/9 directory:
     elide jar --create --file mr.jar -C foo classes --release 9 -C foo9 classes

    To shorten or simplify the jar command, you can specify arguments in a separate
    text file and pass it to the jar command with the at sign (@) as a prefix.

    Examples:
     # Read additional options and list of class files from the file classes.list
     jar --create --file my.jar @classes.list

    Main operation mode:

      -c, --create               Create the archive. When the archive file name specified
                                 by -f or --file contains a path, missing parent directories
                                 will also be created
      -i, --generate-index=FILE  Generate index information for the specified jar
                                 archives. This option is deprecated and may be
                                 removed in a future release.
      -t, --list                 List the table of contents for the archive
      -u, --update               Update an existing jar archive
      -x, --extract              Extract named (or all) files from the archive.
                                 If a file with the same name appears more than once in
                                 the archive, each copy will be extracted, with later copies
                                 overwriting (replacing) earlier copies unless -k is specified.
      -d, --describe-module      Print the module descriptor, or automatic module name
          --validate             Validate the contents of the jar archive. This option
                                 will validate that the API exported by a multi-release
                                 jar archive is consistent across all different release
                                 versions.

    Operation modifiers valid in any mode:
    
      -C DIR                     Change to the specified directory and include the
                                 following file. When used in extract mode, extracts
                                 the jar to the specified directory
      -f, --file=FILE            The archive file name. When omitted, either stdin or
                                 stdout is used based on the operation
          --release VERSION      Places all following files in a versioned directory
                                 of the jar (i.e. META-INF/versions/VERSION/)
      -v, --verbose              Generate verbose output on standard output

    Operation modifiers valid only in create and update mode:

      -e, --main-class=CLASSNAME The application entry point for stand-alone
                                 applications bundled into a modular, or executable,
                                 jar archive
      -m, --manifest=FILE        Include the manifest information from the given
                                 manifest file
      -M, --no-manifest          Do not create a manifest file for the entries
          --module-version=VERSION    The module version, when creating a modular
                                 jar, or updating a non-modular jar
          --hash-modules=PATTERN Compute and record the hashes of modules
                                 matched by the given pattern and that depend upon
                                 directly or indirectly on a modular jar being
                                 created or a non-modular jar being updated
      -p, --module-path          Location of module dependence for generating
                                 the hash

    Operation modifiers valid only in create, update, and generate-index mode:
    
      -0, --no-compress          Store only; use no ZIP compression
          --date=TIMESTAMP       The timestamp in ISO-8601 extended offset date-time with
                                 optional time-zone format, to use for the timestamps of
                                 entries, e.g. "2022-02-12T12:30:00-05:00"

    Operation modifiers valid only in extract mode:

      -k, --keep-old-files       Do not overwrite existing files.
                                 If a Jar file entry with the same name exists in the target
                                 directory, the existing file will not be overwritten.
                                 As a result, if a file appears more than once in an
                                 archive, later copies will not overwrite earlier copies.
                                 Also note that some file system can be case insensitive.
      --dir                    Directory into which the jar will be extracted

    Other options:

      -?, -h, --help[:compat]    Give this, or optionally the compatibility, help
          --help-extra           Give help on extra options
          --version              Print program version

    An archive is a modular jar if a module descriptor, 'module-info.class', is
    located in the root of the given directories, or the root of the jar archive
    itself. The following operations are only valid when creating a modular jar,
    or updating an existing non-modular jar: '--module-version',
    '--hash-modules', and '--module-path'.

    Mandatory or optional arguments to long options are also mandatory or optional
    for any corresponding short options.
"""
)

/**
 * # JAR Tool
 *
 * Implements a [GenericTool] adapter to `jar`. Arguments are passed to the tool verbatim from the command-line.
 */
@ReflectiveAccess @Introspected public class JarTool (
  args: Arguments,
  env: Environment,
  override val inputs: JarToolInputs,
  override val outputs: JarToolOutputs,
) : GenericTool<JarToolProvider, JarTool.JarToolInputs, JarTool.JarToolOutputs>(info = jartool.extend(
  args,
  env,
).using(
  inputs = inputs,
  outputs = outputs.flatten(),
)) {
  // Jar tool.
  private val systemJarTool by lazy { JarToolProvider() }

  // Logging.
  private val jarLogger by lazy { Logging.of(JarTool::class) }

  override val toolLogger: Logger get() = jarLogger
  override val taskDescription: String get() = "Jar assembly"

  // Use the system JAR tool.
  override fun createTool(): JarToolProvider = systemJarTool
  override fun toolRun(out: PrintWriter, err: PrintWriter, vararg args: String): Int = createTool().run(out, err, *args)

  /**
   * JAR tool inputs.
   *
   * Implements understanding of inputs for `jar`.
   */
  public sealed interface JarToolInputs : Inputs.Files {
    /**
     * Provided when no inputs are available.
     */
    public data object NoInputs : JarToolInputs

    /**
     * Describes a sequence of input files to include in the JAR.
     */
    @JvmInline public value class InputFiles(public val files: PersistentList<Path>) : JarToolInputs
  }

  /**
   * JAR tool outputs.
   *
   * Implements understanding of JAR tool outputs -- typically JARs.
   */
  public sealed interface JarToolOutputs {
    /**
     * Provided when no inputs are available.
     */
    public data object NoOutputs : JarToolOutputs, Outputs.None {
      override fun flatten(): Outputs = this
    }

    /**
     * Flatten into an [Outputs] type.
     *
     * @return Outputs value.
     */
    public fun flatten(): Outputs

    /** Defines a [path] to an output JAR. */
    @JvmInline public value class Jar(internal val location: Pair<Path?, Path>) : JarToolOutputs, Outputs.Disk.File {
      public val path: Path get() = location.second
      public val directory: Path? get() = location.first
      override fun flatten(): Outputs = this
    }
  }

  // Amend JAR tool args with additional options that were passed in, or which enable Elide features.
  @Suppress("UNUSED_PARAMETER") override fun amendArgs(args: MutableArguments) {
    // Nothing at this time.
  }

  /** Factories for configuring and obtaining instances of [JarTool]. */
  @Suppress("unused") public companion object {
    /**
     * Create inputs for the Jar tool.
     *
     * @param sequence Sequence of paths to include.
     * @return Jar tool inputs.
     */
    @JvmStatic public fun jarFiles(sequence: Sequence<Path>): JarToolInputs = JarToolInputs.InputFiles(
      sequence.toList().toPersistentList().also {
        if (it.isEmpty()) embeddedToolError(jartool, "No input files provided")
      }
    )

    /**
     * Create outputs for the Jar tool which specify a created jar.
     *
     * @param at Expected path to the built jar.
     * @param dir Optional directory to place the jar in.
     * @return Jar tool outputs.
     */
    @JvmStatic public fun outputJar(at: Path, dir: Path? = null): JarToolOutputs = JarToolOutputs.Jar(dir to at)

    /**
     * Create a Jar tool instance from the provided inputs.
     *
     * @param args Arguments to the tool.
     * @param inputs Inputs to the tool.
     * @param outputs Outputs from the tool.
     * @param env Environment for the tool; defaults to the host environment.
     */
    @JvmStatic
    public fun create(args: Arguments, env: Environment, inputs: JarToolInputs, outputs: JarToolOutputs): JarTool {
      return JarTool(
        args = args,
        env = env,
        inputs = inputs,
        outputs = outputs,
      )
    }
  }
}

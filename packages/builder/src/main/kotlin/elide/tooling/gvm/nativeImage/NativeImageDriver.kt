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
package elide.tooling.gvm.nativeImage

import com.oracle.svm.driver.NativeImage
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlinx.collections.immutable.toPersistentList
import kotlin.io.path.absolutePathString
import elide.runtime.Logging
import elide.tooling.Argument
import elide.tooling.ArgumentContext
import elide.tooling.Arguments
import elide.tooling.Environment
import elide.tooling.Inputs
import elide.tooling.MutableArguments
import elide.tooling.Outputs
import elide.tooling.Tool
import elide.tooling.cli.Statics
import elide.tooling.AbstractTool
import elide.tooling.jvm.JavaCompiler
import elide.tooling.project.ElideProject

// Internal debug logs.
private const val NI_DEBUG_LOGGING = false

// Name of the Native Image Compiler.
public const val NATIVE_IMAGE: String = "native-image"

// GraalVM version.
public const val GRAALVM_VERSION: String = "24.0.1"

// Embedded JVM/JDK version.
public const val EMBEDDED_JDK_VERSION: String = "24"

// Build-time SVM support flag.
public val svmIsSupported: Boolean = System.getProperty("elide.svm") == "true"

// Description to show.
public const val NI_COMPILER_DESCRIPTION: String =
  "The Native Image compiler ($NATIVE_IMAGE) is a command-line tool that compiles native binaries."

// Tool description.
public val nativeImage: Tool.CommandLineTool = Tool.describe(
  name = NATIVE_IMAGE,
  label = "Native Image",
  version = GRAALVM_VERSION,
  docs = URI.create("https://graalvm.org/"),
  description = NI_COMPILER_DESCRIPTION,
  registered = true,
  helpText = """
GraalVM Native Image, via Elide

This tool can ahead-of-time compile code to native executables.

Usage: elide native-image <elide-options> -- [options] class [imagename] [options]
           (to build an image for a class)
   or  elide native-image <elide-options> -- [options] -jar jarfile [imagename] [options]
           (to build an image for a jar file)
   or  elide native-image <elide-options> -- [options] -m <module>[/<mainclass>] [options]
       elide native-image <elide-options> -- [options] --module <module>[/<mainclass>] [options]
           (to build an image for a module)

where options include:

    @argument files       one or more argument files containing options
    -cp <class search path of directories and zip/jar files>
    -classpath <class search path of directories and zip/jar files>
    --class-path <class search path of directories and zip/jar files>
                          A : separated list of directories, JAR archives,
                          and ZIP archives to search for class files.
    -p <module path>
    --module-path <module path>...
                          A : separated list of directories, each directory
                          is a directory of modules.
    --add-modules <module name>[,<module name>...]
                          root modules to resolve in addition to the initial module.
                          <module name> can also be ALL-DEFAULT, ALL-SYSTEM,
                          ALL-MODULE-PATH.
    -D<name>=<value>      set a system property
    -J<flag>              pass <flag> directly to the JVM running the image generator
    --diagnostics-mode    enable diagnostics output: class initialization, substitutions, etc.
    --enable-preview      allow classes to depend on preview features of this release
    --verbose             enable verbose output
    --version             print product version and exit
    --help                print this help message
    --help-extra          print help on non-standard options

    --auto-fallback       build stand-alone image if possible
    --color               color build output ('always', 'never', or 'auto')
    --emit                emit additional data as a result of the build. Use 'build-report'
                          to emit a detailed Build Report, for example: '--emit
                          build-report' or '--emit build-report=/tmp/report.html'
    --enable-all-security-services
                          add all security service classes to the generated image.
    --enable-http         enable http support in the generated image
    --enable-https        enable https support in the generated image
    --enable-monitoring   enable monitoring features that allow the VM to be inspected at
                          run time. Comma-separated list can contain 'heapdump', 'jfr',
                          'jvmstat', 'jmxserver' (experimental), 'jmxclient'
                          (experimental), 'threaddump', 'nmt' (experimental), 'jcmd'
                          (experimental), or 'all' (deprecated behavior: defaults to 'all'
                          if no argument is provided). For example:
                          '--enable-monitoring=heapdump,jfr'.
    --enable-native-access
                          a comma-separated list of modules that are permitted to perform
                          restricted native operations. The module name can also be
                          ALL-UNNAMED.
    --enable-sbom         assemble a Software Bill of Materials (SBOM) for the executable or
                          shared library based on the results from the static analysis.
                          Comma-separated list can contain 'embed' to store the SBOM in
                          data sections of the binary, 'export' to save the SBOM in the
                          output directory, 'classpath' to include the SBOM as a Java
                          resource on the classpath at 'META-INF/native-image/sbom.json',
                          'strict' to abort the build if any class cannot be matched to a
                          library in the SBOM, 'cyclonedx' (the only format currently
                          supported), and 'class-level' to include class-level metadata.
                          Defaults to --enable-sbom=embed,cyclonedx. For example:
                          '--enable-sbom=embed,export,strict'.
    --enable-url-protocols
                          list of comma separated URL protocols to enable.
    --exact-reachability-metadata
                          enables exact and user-friendly handling of reflection, resources,
                          JNI, and serialization.
    --exact-reachability-metadata-path
                          trigger exact handling of reflection, resources, JNI, and
                          serialization from all types in the given class-path or
                          module-path entries.
    --features            a comma-separated list of fully qualified Feature implementation
                          classes
    --force-fallback      force building of fallback image
    --gc=<value>          select native-image garbage collector implementation. Allowed
                          options for <value>:
                          'G1': G1 garbage collector
                          'epsilon': Epsilon garbage collector
                          'serial': Serial garbage collector (default)
    --initialize-at-build-time
                          a comma-separated list of packages and classes (and implicitly all
                          of their superclasses) that are initialized during image
                          generation. An empty string designates all packages.
    --initialize-at-run-time
                          a comma-separated list of packages and classes (and implicitly all
                          of their subclasses) that must be initialized at runtime and not
                          during image building. An empty string is currently not
                          supported.
    --install-exit-handlers
                          provide java.lang.Terminator exit handlers
    --libc                selects the libc implementation to use. Available implementations:
                          glibc, musl, bionic
    --link-at-build-time  require types to be fully defined at image build-time. If used
                          without args, all classes in scope of the option are required to
                          be fully defined.
    --link-at-build-time-paths
                          require all types in given class or module-path entries to be
                          fully defined at image build-time.
    --list-cpu-features   show CPU features specific to the target platform and exit.
    --list-modules        list observable modules and exit.
    --native-compiler-options
                          provide custom C compiler option used for query code compilation.
    --native-compiler-path
                          provide custom path to C compiler used for query code compilation
                          and linking.
    --native-image-info   show native-toolchain information and image-build settings
    --no-fallback         build stand-alone image or report failure
    --parallelism         the maximum number of threads to use concurrently during native
                          image generation.
    --pgo                 a comma-separated list of files from which to read the data
                          collected for profile-guided optimization of AOT compiled code
                          (reads from default.iprof if nothing is specified). Each file
                          must contain a single PGOProfiles object, serialized in JSON
                          format, optionally compressed by gzip.
    --pgo-instrument      instrument AOT compiled code to collect data for profile-guided
                          optimization into default.iprof file
    --pgo-sampling        perform profiling by sampling the AOT compiled code to collect
                          data for profile-guided optimization.
    --shared              build shared library
    --silent              silence build output
    --static              build statically linked executable (requires static libc and zlib)
    --static-nolibc       build statically linked executable with libc dynamically linked
    --target              selects native-image compilation target (in <OS>-<architecture>
                          format). Defaults to host's OS-architecture pair.
    --trace-object-instantiation
                          comma-separated list of fully-qualified class names that object
                          instantiation is traced for.
    -O                    control code optimizations: b - optimize for fastest build time, s
                          - optimize for size, 0 - no optimizations, 1 - basic
                          optimizations, 2 - advanced optimizations, 3 - all optimizations
                          for best performance.
    -da                   also -da[:[packagename]...|:classname] or
                          -disableassertions[:[packagename]...|:classname]. Disable
                          assertions with specified granularity at run time.
    -dsa                  also -disablesystemassertions. Disables assertions in all system
                          classes at run time.
    -ea                   also -ea[:[packagename]...|:classname] or
                          -enableassertions[:[packagename]...|:classname]. Enable
                          assertions with specified granularity at run time.
    -esa                  also -enablesystemassertions. Enables assertions in all system
                          classes at run time.
    -g                    generate debugging information
    -march                generate instructions for a specific machine type. Defaults to
                          'x86-64-v3' on AMD64 and 'armv8.1-a' on AArch64. Use
                          -march=compatibility for best compatibility, or -march=native for
                          best performance if the native executable is deployed on the same
                          machine or on a machine with the same CPU features. To list all
                          available machine types, use -march=list.
    -o                    name of the output file to be generated

  For more information about GraalVM and Native Image, see https://graalvm.org/
  For usage within Elide, see https://docs.elide.dev
"""
)

/**
 * # Native Image Compiler
 *
 * Implements an [AbstractTool] adapter to `native-image`, the GraalVM Native Image compiler. Arguments are passed to
 * the compiler verbatim from the command-line.
 */
@ReflectiveAccess @Introspected public class NativeImageDriver (
  args: Arguments,
  env: Environment,
  public val inputs: NativeImageInputs,
  public val outputs: NativeImageOutputs,
  private val configurator: NativeImageConfigurator.() -> Unit = {},
  private val projectRoot: Path,
) : AbstractTool(info = nativeImage.extend(
  args,
  env,
).using(
  inputs = inputs,
  outputs = outputs.flatten(),
)) {
  /**
   * Native Image compiler configurator.
   */
  public interface NativeImageConfigurator {
    /**
     * Arguments to pass to the Native Image compiler.
     */
    public val args: MutableArguments
  }

  /**
   * Native Image inputs.
   *
   * Implements understanding of Native Image inputs as flags.
   */
  public sealed interface NativeImageInputs : Inputs.Files {
    /**
     * Holds a suite of paths which are expected to exist on-disk.
     *
     * @property paths Paths to include in the inputs.
     */
    public class Paths internal constructor(public val paths: List<Path>) : NativeImageInputs
  }

  /**
   * Native Image outputs.
   *
   * Implements understanding of Native Image compiler outputs -- namely, native binaries and shared libraries.
   */
  public sealed interface NativeImageOutputs {
    /**
     * Flatten into an [Outputs] type.
     *
     * @return Outputs value.
     */
    public fun flatten(): Outputs

    /**
     * Specifies a Native Image binary output.
     *
     * @property path Expected path to the binary.
     */
    public class NativeBinary internal constructor(public val path: Path): NativeImageOutputs, Outputs.Disk.Directory {
      override fun flatten(): Outputs = this
    }

    /**
     * Specifies a Native Image shared library output.
     *
     * @property path Expected path to the shared library.
     */
    public class SharedLibrary internal constructor(public val path: Path): NativeImageOutputs, Outputs.Disk.Directory {
      override fun flatten(): Outputs = this
    }
  }

  private fun amendArgs(
    args: MutableArguments,
    javaToolchainHome: Path,
    inputs: NativeImageInputs.Paths,
    project: ElideProject?,
  ) {
    val baseArgs = args.asArgumentList()
    val targetName = project?.manifest?.name ?: "app"
    val targetOuts = projectRoot.resolve(".dev").resolve("artifacts").resolve("native-image").resolve(targetName)

    if (!Statics.noColor) {
      // @TODO sobbing with no contains check here
      if ("--color" !in baseArgs) {
        args.add(Argument.of("--color" to "always"))
      }
      // use a sensible default name for the output binary if none is provided
      if ("-o" !in baseArgs && "-H:Name" !in baseArgs) {
        args.add(Argument.of("-H:Name" to (project?.manifest?.name ?: "app")))
      }
      // use a sensible default location for the outputs if none is provided
      if ("-H:Path" !in baseArgs) {
        args.add(Argument.of("-H:Path" to targetOuts.absolutePathString()))

        // make sure those exist
        Files.createDirectories(
          targetOuts
        )
      }
    }

    // Apply any custom logic.
    val cfg = object: NativeImageConfigurator {
      override val args: MutableArguments = args
    }
    cfg.configurator()
  }

  @Suppress("TooGenericExceptionCaught")
  override suspend fun invoke(state: EmbeddedToolState): Tool.Result {
    debugLog("Preparing Kotlin Compiler invocation")
    val projectInfo = state.project
    val javaToolchainHome = JavaCompiler.resolveJavaToolchain(nativeImage)
    debugLog("Java toolchain home: $javaToolchainHome")
    val renderCtx = ArgumentContext.of(
      argSeparator = ' ',
      kvToken = '=',
    )
    debugLog("Arg render context ready for args: ${info.args}")
    val mut = Arguments.of(info.args.asArgumentSequence()).toMutable().apply {
      add("-H:+UnlockExperimentalVMOptions")
    }

    debugLog("Amending arguments")
    amendArgs(
      mut,
      javaToolchainHome,
      inputs as NativeImageInputs.Paths,
      projectInfo,
    )
    mut.add("-H:-UnlockExperimentalVMOptions")  // lock it back up
    val argList = mut.asArgumentStrings(renderCtx).toList()
    debugLog("Arg list ready of size=${argList.size}")

    return try {
      debugLog("Finalized arguments: ${argList.joinToString(" ")}")

      @Suppress("SwallowedException")
      try {
        NativeImage.buildImage(argList.toTypedArray(), /* exit = */ false)
      } catch (result: NativeImageResult) {
        when (val err = result.error) {
          null -> Tool.Result.Success
          else -> throw err
        }
      }
      Tool.Result.Success
    } catch (err: Throwable) {
      logging.error("Native Image compilation failed", err)
      Tool.Result.UnspecifiedFailure
    }
  }

  /** Factories for configuring and obtaining instances of [NativeImageDriver]. */
  public companion object {
    /**
     * Create outputs for the Native Image tool which specify an image binary.
     *
     * @param at Expected path to the binary.
     * @return Tool outputs.
     */
    @JvmStatic public fun outputBinary(at: Path): NativeImageOutputs = NativeImageOutputs.NativeBinary(at)

    /**
     * Create outputs for the Native Image tool which specify a shared library.
     *
     * @param at Expected path to the binary.
     * @return Tool outputs.
     */
    @JvmStatic public fun sharedLibrary(at: Path): NativeImageOutputs = NativeImageOutputs.SharedLibrary(at)

    /**
     * Create inputs for the Native Image tool which include the provided paths.
     *
     * @param sequence Sequence of paths to include.
     * @return Tool inputs.
     */
    @JvmStatic public fun nativeImageInputs(sequence: Sequence<Path>): NativeImageInputs = NativeImageInputs.Paths(
      sequence.toList().toPersistentList().also {
        if (it.isEmpty()) embeddedToolError(nativeImage, "No inputs provided")
      }
    )

    // Logging.
    @JvmStatic private val logging by lazy { Logging.of(NativeImageDriver::class) }

    @JvmStatic private fun debugLog(message: String) {
      if (NI_DEBUG_LOGGING) {
        System.err.println("[nativeImage:debug] $message")
      }
    }

    /**
     * Create a Native Image driver instance from the provided inputs.
     *
     * @param args Arguments to the driver.
     * @param inputs Inputs to the driver.
     * @param outputs Outputs from the driver.
     * @param env Environment for the driver; defaults to the host environment.
     * @param projectRoot Project root path; defaults to the current working directory.
     * @param configurator Optional configurator for the driver, which can be used to amend arguments.
     */
    @JvmStatic public fun create(
      args: Arguments,
      env: Environment,
      inputs: NativeImageInputs,
      outputs: NativeImageOutputs,
      projectRoot: Path = Paths.get(System.getProperty("user.dir")),
      configurator: NativeImageConfigurator.() -> Unit = {},
    ): NativeImageDriver = NativeImageDriver(
      args = args,
      env = env,
      inputs = inputs,
      outputs = outputs,
      projectRoot = projectRoot,
      configurator = configurator,
    )
  }
}


/** Static library configuration values. */
object Elide {
  /** Name of the library. */
  const val name = "elide"

  /** Major release version for Elide. */
  const val track = "v3"

  /** Major library version. */
  const val majorVersion = "1.0"

  /** Major library version tag. */
  const val versionTag = "alpha1"

  /** Revision value for the library. */
  const val revision = 32

  /** Version string for the library. */
  const val version = "$majorVersion-$track-$versionTag-rc$revision"

  /** Latest plugin version. */
  const val pluginVersion = "1.0.0-beta9"

  /** Maven group shared by Elide artifacts. */
  const val group = "dev.elide"

  /** Compiler args to include in all Kotlin targets. */
  val compilerArgs = listOf(
    "-progressive",
    "-Xcontext-receivers",
  )

  /** Compiler args to include in Kotlin JVM targets. */
  val jvmCompilerArgs = compilerArgs.plus(listOf(
    "-no-stdlib",
    "-Xjvm-default=all",
  ))

  /** Compiler args to include in Kotlin JS targets. */
  val jsCompilerArgs = compilerArgs.plus(listOf(
    "-Xgenerate-dts",
  ))

  /** Compiler args to include in Kotlin MPP targets. */
  val mppCompilerArgs = compilerArgs

  /** Compiler args to include in Kotlin JVM targets which use `kapt`. */
  val kaptCompilerArgs = compilerArgs.plus(listOf(
    "-no-stdlib",
    "-Xallow-unstable-dependencies",
    "-Xemit-jvm-type-annotations",
    "-Xjvm-default=all",
  ))

  /** Kotlin SDK and platform version. */
  const val kotlinSdk = "1.7.21"

  /** Kotlin language version. */
  const val kotlinLanguage = "1.7"

  /** Sample code modules. */
  val samplesList = listOf(
    ":samples:server:hellocss",
    ":samples:server:helloworld",
    ":samples:fullstack:basic:server",
    ":samples:fullstack:react:server",
    ":samples:fullstack:ssr:server",
    ":samples:fullstack:react-ssr:server",
  )

  /** Kotlin MPP modules. */
  val multiplatformModules = listOf(
    "base",
    "model",
    "test",
  )

  /** Server-side only modules. */
  val serverModules = listOf(
    "graalvm",
    "server",
    "rpc-jvm",
    "ssg",
  )

  /** Browser-side only modules. */
  val frontendModules = listOf(
    "frontend",
    "graalvm-js",
    "graalvm-react",
    "rpc-js",
  )

  /** Modules which should not be reported on for testing.. */
  val noTestModules = listOf(
    "bom",
    "platform",
    "proto",
    "packages",
    "processor",
    "reports",
    "bundler",
    "samples",
    "site",
    "ssg",
    "docs",
    "model",
    "benchmarks",
    "frontend",
    "graalvm-js",
    "graalvm-react",
    "rpc-js",
    "test",
  )
}

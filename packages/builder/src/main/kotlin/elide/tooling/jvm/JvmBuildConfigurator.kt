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
@file:Suppress("UnstableApiUsage")

package elide.tooling.jvm

import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.RepositorySystem
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.incremental.classpathAsList
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.isWritable
import elide.exec.ActionScope
import elide.exec.Task
import elide.exec.Task.Companion.fn
import elide.exec.taskDependencies
import elide.runtime.Logging
import elide.runtime.exec.asExecResult
import elide.runtime.gvm.kotlin.KotlinCompilerConfig
import elide.runtime.gvm.kotlin.KotlinLanguage
import elide.tooling.AbstractTool
import elide.tooling.Argument
import elide.tooling.Arguments
import elide.tooling.Classpath
import elide.tooling.ClasspathSpec
import elide.tooling.Environment
import elide.tooling.MultiPathUsage
import elide.tooling.MutableArguments
import elide.tooling.MutableClasspath
import elide.tooling.Tool
import elide.tooling.config.BuildConfigurator
import elide.tooling.config.BuildConfigurator.ElideBuildState
import elide.tooling.deps.DependencyResolver
import elide.tooling.jvm.resolver.AetherProjectProvider
import elide.tooling.jvm.resolver.MavenAetherResolver
import elide.tooling.jvm.resolver.RepositorySystemFactory
import elide.tooling.kotlin.KotlinCompiler
import elide.tooling.project.ElideProject
import elide.tooling.project.SourceSet
import elide.tooling.project.SourceSetLanguage.Java
import elide.tooling.project.SourceSetLanguage.Kotlin
import elide.tooling.project.SourceSetType
import elide.tooling.project.manifest.ElidePackageManifest
import elide.tooling.project.manifest.ElidePackageManifest.KotlinJvmCompilerOptions
import elide.tooling.archive.ArchiveBuilder
import elide.tooling.project.codecs.AssemblyDescriptorParser
import elide.tooling.runner.ProcessRunner

private fun srcSetTaskName(srcSet: SourceSet, name: String): String = buildString {
  append(name)
  append(srcSet.name[0].uppercase())
  append(srcSet.name.slice(1..srcSet.name.lastIndex))
}

/**
 * ## JVM Build Configurator
 */
internal class JvmBuildConfigurator : BuildConfigurator {
  private companion object {
    @JvmStatic private val logging by lazy { Logging.of(JvmBuildConfigurator::class) }
  }

  private fun jarNameFor(coordinate: String, version: String): String {
    if (coordinate.startsWith("dev.elide")) {
      return JvmLibraries.elideJarNameFor(coordinate, version)
    }
    return JvmLibraries.jarNameFor(coordinate, version)
  }

  private fun builtinKotlinJarPath(state: ElideBuildState, dependency: String, version: String): Path {
    return System.getenv("KOTLIN_HOME")?.let { kotlinHomeByEnv ->
      Path.of(kotlinHomeByEnv)
        .resolve("lib")
        .resolve(jarNameFor(dependency, version))
    } ?: state.resourcesPath
      .resolve("kotlin")
      .resolve(KotlinLanguage.VERSION)
      .resolve("lib")
      .resolve(jarNameFor(dependency, version))
  }

  private fun builtinJavaJarPath(state: ElideBuildState, dependency: String, version: String): Path {
    return builtinKotlinJarPath(state, dependency, version)
  }

  private fun calculateCommonSourceRoot(sources: JavaCompiler.JavaCompilerInputs.SourceFiles): String? {
    val paths = sources.files
    if (paths.size < 2) return null

    // convert all paths to absolute paths, normalize, and split into components
    val pathComponents = paths.map { path ->
      path.toAbsolutePath()
        .normalize()
        .toString()
        .split(File.pathSeparator)
        .filter { it.isNotEmpty() }
    }

    // find the minimum length to avoid index out of bounds
    val minLength = pathComponents.minOf { it.size }

    // find the longest common prefix
    var commonLength = 0
    for (i in 0 until minLength) {
      val component = pathComponents[0][i]
      if (pathComponents.all { it[i] == component }) {
        commonLength++
      } else {
        break
      }
    }

    // if no common components (besides root), return null
    if (commonLength == 0) return null

    // reconstruct the path
    return File.pathSeparator + pathComponents[0].take(commonLength).joinToString(File.pathSeparator)
  }

  @Suppress("LongMethod")
  private fun ActionScope.javac(
    name: String,
    resolver: MavenAetherResolver?,
    state: ElideBuildState,
    config: BuildConfigurator.BuildConfiguration,
    srcSet: SourceSet,
    additionalDeps: Classpath? = null,
    tests: Boolean = false,
    dependencies: List<Task> = emptyList(),
    argsAmender: MutableArguments.() -> Unit = {},
  ) = fn(name, taskDependencies(dependencies)) {
    val compileClasspath = resolver?.classpathProvider(
      object : ClasspathSpec {
        override val usage: MultiPathUsage = if (tests) MultiPathUsage.TestCompile else MultiPathUsage.Compile
      },
    )?.classpath()

    val processorClasspath = resolver?.classpathProvider(
      object : ClasspathSpec {
        override val usage: MultiPathUsage = if (tests) MultiPathUsage.TestProcessors else MultiPathUsage.Processors
      },
    )?.classpath()

    val mainClassesOutput = state.layout.artifacts
      .resolve("jvm") // `.dev/artifacts/jvm/...`
      .resolve("classes") // `.../classes/...`
      .resolve(if (tests) "main" else srcSet.name) // `.../classes/main/...`

    val codegenClassesOutput = state.layout.artifacts
      .resolve("jvm") // `.dev/artifacts/jvm/...`
      .resolve("classes") // `.../classes/...`
      .resolve("codegen") // `.../classes/codegen/...`

    val testClassesOutput = if (!tests) null else state.layout.artifacts
      .resolve("jvm") // `.dev/artifacts/jvm/...`
      .resolve("classes") // `.../classes/...`
      .resolve("test") // `.../classes/main/...`

    val classOutput = if (tests) testClassesOutput!! else mainClassesOutput
    val mountedInputs = buildList {
      add(mainClassesOutput)
      add(codegenClassesOutput)
      if (tests) add(testClassesOutput)
    }

    logging.debug { "Java compiler task dependencies: $dependencies" }
    logging.debug { "Java main classpath: $compileClasspath" }
    logging.debug { "Classes output root: $classOutput" }

    // resolve source/target settings
    val (explicitSourceVersion, sourceVersion) = (
            state.manifest.jvm?.java?.release
              ?: state.manifest.jvm?.java?.source
              ?: state.manifest.jvm?.target
            ).let {
        when (it) {
          null -> false to ElidePackageManifest.JvmTarget.DEFAULT
          else -> true to it
        }
      }
    val (explicitTargetVersion, targetVersion) = (
            state.manifest.jvm?.java?.release
              ?: state.manifest.jvm?.java?.source
              ?: state.manifest.jvm?.target
            ).let {
        when (it) {
          null -> false to ElidePackageManifest.JvmTarget.DEFAULT
          else -> true to it
        }
      }

    // if there is an explicit target or source version, use that pair, otherwise, use `release`.
    val useReleaseFlag = !(explicitSourceVersion || explicitTargetVersion)
    val effectiveReleaseVersion = when (val explicit = state.manifest.jvm?.java?.release) {
      null -> targetVersion
      else -> explicit
    }

    // prepare to inject deps
    val staticDeps = Classpath.empty().toMutable()
    if (config.settings.dependencies && state.manifest.jvm?.features?.testing != false && tests) {
      // add:
      // `org.junit.jupiter:junit-jupiter-engine`
      // `org.junit.jupiter:junit-jupiter-api`
      // `org.junit.jupiter:junit-jupiter-params`
      // `org.junit.platform:junit-platform-engine`
      // `org.junit.platform:junit-platform-commons`
      // `org.junit.platform:junit-platform-console`
      // `org.apiguardian:apiguardian-api`
      staticDeps.add(
        builtinJavaJarPath(
          state,
          JvmLibraries.JUNIT_JUPITER_ENGINE,
          JvmLibraries.EMBEDDED_JUNIT_VERSION,
        ),
      )
      staticDeps.add(
        builtinJavaJarPath(
          state,
          JvmLibraries.JUNIT_JUPITER_API,
          JvmLibraries.EMBEDDED_JUNIT_VERSION,
        ),
      )
      staticDeps.add(
        builtinJavaJarPath(
          state,
          JvmLibraries.JUNIT_JUPITER_PARAMS,
          JvmLibraries.EMBEDDED_JUNIT_VERSION,
        ),
      )
      staticDeps.add(
        builtinJavaJarPath(
          state,
          JvmLibraries.JUNIT_PLATFORM_ENGINE,
          JvmLibraries.EMBEDDED_JUNIT_PLATFORM_VERSION,
        ),
      )
      staticDeps.add(
        builtinJavaJarPath(
          state,
          JvmLibraries.JUNIT_PLATFORM_COMMONS,
          JvmLibraries.EMBEDDED_JUNIT_PLATFORM_VERSION,
        ),
      )
      staticDeps.add(
        builtinJavaJarPath(
          state,
          JvmLibraries.JUNIT_PLATFORM_CONSOLE,
          JvmLibraries.EMBEDDED_JUNIT_PLATFORM_VERSION,
        ),
      )
      staticDeps.add(
        builtinJavaJarPath(
          state,
          JvmLibraries.APIGUARDIAN_API,
          JvmLibraries.EMBEDDED_APIGUARDIAN_VERSION,
        ),
      )
      staticDeps.add(
        builtinJavaJarPath(
          state,
          JvmLibraries.OPENTEST,
          JvmLibraries.EMBEDDED_OPENTEST_VERSION,
        ),
      )
    }

    // prepare compiler configuration
    val env = Environment.host()
    val inputs = JavaCompiler.sources(
      srcSet.paths.filter { it.path.extension == "java" }.map { it.path.absolute() }.asSequence()
    )
    val commonSourceRoot = calculateCommonSourceRoot(inputs)

    val args = Arguments.empty().toMutable().apply {
      // bytecode and source targeting
      if (useReleaseFlag) {
        // @TODO: argument splitting by default
        add(Argument.of("--release"))
        add(Argument.of(effectiveReleaseVersion.argValue))
        logging.debug { "Using `--release`: $effectiveReleaseVersion" }
      } else {
        // @TODO: argument splitting by default
        val src = sourceVersion.argValue
        if (src != "auto") {
          add(Argument.of("--source"))
          add(Argument.of(src))
        }
        val tgt = targetVersion.argValue
        if (tgt != "auto") {
          add(Argument.of("--target"))
          add(Argument.of(tgt))
        }
        logging.debug { "Using `--source`/`--target`: ${sourceVersion}/${targetVersion}" }
      }

      // assemble classpath
      MutableClasspath.empty().apply {
        compileClasspath?.let { add(it) }
        add(staticDeps)
        additionalDeps?.let { add(it) }
        mountedInputs.filterNotNull().filter { it.exists() }.forEach { prepend(it) }
      }.let {
        add(it)
      }

      // activate processors if we have them
      if (processorClasspath != null) {
        add("-proc:full")
        add("--processor-path")
        add(processorClasspath.asArgumentStrings().joinToString(":"))
      }

      // if we have a common source root, specify ig
      if (commonSourceRoot != null) {
        add("--source-path")
        add(commonSourceRoot)
      }

      // prepare outputs
      if (!classOutput.exists()) {
        try {
          Files.createDirectories(classOutput)
        } catch (ioe: IOException) {
          logging.error { "Failed to create class output directory: $classOutput" }
          throw ioe
        }
      } else if (!classOutput.isWritable()) {
        logging.error { "Class output directory is not writable: $classOutput" }
        throw IOException("Class output directory is not writable: $classOutput")
      }
      // @TODO proper splitting of args here by default, instead of manually
      add(Argument.of("-d"))
      add(Argument.of(classOutput.absolutePathString()))

      // if the user specifies extra flags in their project configuration, add them here
      state.manifest.jvm?.java?.compiler?.flags?.let {
        addAllStrings(it)
      }

      // invoker amendments
      argsAmender()
    }.build()

    logging.debug { "Java compiler args: '${args.asArgumentList().joinToString(" ")}'" }

    val outputs = JavaCompiler.classesDir(classOutput)
    val compiler = JavaCompiler.create(
      args = args,
      env = env,
      inputs = inputs,
      outputs = outputs,
    )

    // fire compile job
    val result = compiler.invoke(
      object : AbstractTool.EmbeddedToolState {
        override val resourcesPath: Path get() = state.resourcesPath
        override val project: ElideProject? get() = state.project
      },
    )
    when (result) {
      is Tool.Result.Success -> {
        logging.debug { "Java compilation finished without error" }
      }

      else -> {
        logging.error { "Java compilation failed" }
      }
    }
    result.asExecResult()
    // @TODO error triggering
  }.describedBy {
    val pluralized = if (srcSet.paths.size == 1) "source file" else "sources"
    val suiteTag = if (srcSet.name == "main") "" else " (suite '${srcSet.name}')"
    "Compiling ${srcSet.paths.filter { it.path.extension == "java" }.size} Java $pluralized$suiteTag"
  }.also { javac ->
    config.taskGraph.apply {
      addNode(javac)
      if (dependencies.isNotEmpty()) {
        dependencies.forEach { dependency ->
          putEdge(javac, dependency)
        }
      }
    }
  }

  private fun ActionScope.kotlinc(
    name: String,
    resolver: MavenAetherResolver?,
    state: ElideBuildState,
    config: BuildConfigurator.BuildConfiguration,
    srcSet: SourceSet,
    additionalDeps: Classpath? = null,
    tests: Boolean = false,
    dependencies: List<Task> = emptyList(),
    argsAmender: K2JVMCompilerArguments.() -> Unit = {},
  ) = fn(name, taskDependencies(dependencies)) {
    val compileClasspath = resolver?.classpathProvider(
      object : ClasspathSpec {
        override val usage: MultiPathUsage = if (tests) MultiPathUsage.TestCompile else MultiPathUsage.Compile
      },
    )?.classpath()

    val processorClasspath = resolver?.classpathProvider(
      object : ClasspathSpec {
        override val usage: MultiPathUsage =
          if (tests) MultiPathUsage.TestProcessors else MultiPathUsage.Processors
      },
    )?.classpath()

    val kotlincClassesOutput = state.layout.artifacts
      .resolve("jvm") // `.dev/artifacts/jvm/...`
      .resolve("classes") // `.../classes/...`
      .resolve(srcSet.name) // `.../classes/main/...`

    logging.debug { "Kotlin task dependencies: $dependencies" }
    logging.debug { "Kotlin main classpath: $compileClasspath" }
    logging.debug { "Classes output root: $kotlincClassesOutput" }

    // kotlinx support is enabled by default; this includes kotlin's testing tools
    val staticDeps = Classpath.empty().toMutable()

    if (state.manifest.kotlin?.features?.autoClasspath != false) {
      // add `dev.elide:elide-core`
      staticDeps.add(
        builtinKotlinJarPath(
          state,
          JvmLibraries.ELIDE_CORE,
          JvmLibraries.ELIDE_VERSION,
        )
      )
      // add `dev.elide:elide-base`
      staticDeps.add(
        builtinKotlinJarPath(
          state,
          JvmLibraries.ELIDE_BASE,
          JvmLibraries.ELIDE_VERSION,
        )
      )
      if (tests) {
        // add `dev.elide:elide-test`
        staticDeps.add(
          builtinKotlinJarPath(
            state,
            JvmLibraries.ELIDE_TEST,
            JvmLibraries.ELIDE_VERSION,
          )
        )
      }
    }
    if (state.manifest.kotlin?.features?.kapt != false) {
      staticDeps.add(
        builtinKotlinJarPath(
          state,
          JvmLibraries.KOTLIN_KAPT_RUNTIME,
          KotlinLanguage.VERSION,
        )
      )
    }
    if (state.manifest.kotlin?.features?.kotlinx != false) {
      if (state.manifest.kotlin?.features?.coroutines != false) {
        // add `org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm`
        staticDeps.add(
          builtinKotlinJarPath(
            state,
            JvmLibraries.KOTLINX_COROUTINES,
            JvmLibraries.EMBEDDED_COROUTINES_VERSION,
          ),
        )
      }
      if (state.manifest.kotlin?.features?.serialization != false) {
        // add `org.jetbrains.kotlinx:kotlinx-serialization-core`
        staticDeps.add(
          builtinKotlinJarPath(
            state,
            JvmLibraries.KOTLINX_SERIALIZATION,
            JvmLibraries.EMBEDDED_SERIALIZATION_VERSION,
          ),
        )

        // add `org.jetbrains.kotlinx:kotlinx-serialization-json`
        staticDeps.add(
          builtinKotlinJarPath(
            state,
            JvmLibraries.KOTLINX_SERIALIZATION_JSON,
            JvmLibraries.EMBEDDED_SERIALIZATION_VERSION,
          ),
        )
      }
    }
    if (tests) {
      if (state.manifest.kotlin?.features?.testing != false) {
        // junit5, kotlin testing
        JvmLibraries.testCoordinates.forEach { (testLib, version) ->
          staticDeps.add(builtinKotlinJarPath(state, testLib, version))
        }
      }
      if (state.manifest.kotlin?.features?.kotlinx != false) {
        if (state.manifest.kotlin?.features?.coroutines != false) {
          // add `org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm`
          staticDeps.add(
            builtinKotlinJarPath(
              state,
              JvmLibraries.KOTLINX_COROUTINES,
              JvmLibraries.EMBEDDED_COROUTINES_VERSION,
            ),
          )

          // add `org.jetbrains.kotlinx:kotlinx-coroutines-test-jvm`
          staticDeps.add(
            builtinKotlinJarPath(
              state,
              JvmLibraries.KOTLINX_COROUTINES_TEST,
              JvmLibraries.EMBEDDED_COROUTINES_VERSION,
            ),
          )
        }
        if (state.manifest.kotlin?.features?.serialization == true) {
          // add `org.jetbrains.kotlinx:kotlinx-serialization-core-jvm`
          staticDeps.add(
            builtinKotlinJarPath(
              state,
              JvmLibraries.KOTLINX_SERIALIZATION,
              JvmLibraries.EMBEDDED_SERIALIZATION_VERSION,
            ),
          )

          // add `org.jetbrains.kotlinx:kotlinx-serialization-json-jvm`
          staticDeps.add(
            builtinKotlinJarPath(
              state,
              JvmLibraries.KOTLINX_SERIALIZATION_JSON,
              JvmLibraries.EMBEDDED_SERIALIZATION_VERSION,
            ),
          )
        }
      }
    }

    val finalizedClasspath = (compileClasspath ?: Classpath.empty()).toMutable().apply {
      // injected deps
      addAll(staticDeps.distinct())

      // if additional deps are provided, add them to the classpath
      additionalDeps?.let { add(it) }
    }.toList().let {
      Classpath.from(it.map { item -> item.path })
    }

    val kotlincOpts = state.manifest.kotlin?.compilerOptions ?: KotlinJvmCompilerOptions()
    val args = Arguments.empty().toMutable().apply {
      // @TODO eliminate this
      add("-Xskip-prerelease-check")

      // apply arguments
      addAllStrings(kotlincOpts.collect().toList())

      if (state.manifest.kotlin?.features?.kapt == true && processorClasspath?.isNotEmpty() == true) {
        val kapt3 = "org.jetbrains.kotlin.kapt3"
        val kaptOutBase = state.layout.artifacts.resolve("codegen").resolve("kapt")
        val kaptSources = kaptOutBase.resolve("sources")
        val kaptClasses = kaptOutBase.resolve("classes")
        val kaptStubs = kaptOutBase.resolve("stubs")
        coroutineScope {
          listOf(kaptSources, kaptClasses, kaptStubs).map {
            async(Dispatchers.IO) {
              it.createDirectories()
            }
          }.awaitAll()
        }
        add("-P")
        add("plugin:$kapt3:aptMode=stubsAndApt")
        add("-P")
        add("plugin:$kapt3:correctErrorTypes=true")
        add("-P")
        add("plugin:$kapt3:sources=${kaptSources.absolutePathString()}")
        add("-P")
        add("plugin:$kapt3:classes=${kaptClasses.absolutePathString()}")
        add("-P")
        add("plugin:$kapt3:stubs=${kaptStubs.absolutePathString()}")
        if (state.manifest.kotlin?.compilerOptions?.freeCompilerArgs?.contains("-Xjvm-enable-preview") == true) {
          // preview mode must match for kapt
          add("-P")
          add("plugin:$kapt3:javacOption=--enable-preview=true")
        }
        processorClasspath.forEach { item ->
          add("-P")
          add("plugin:$kapt3:apclasspath=${item.path.absolutePathString()}")
        }
      }
      // add kapt classpath
    }.build()

    logging.debug { "Kotlin compiler args: '${args.asArgumentList().joinToString(" ")}'" }

    val effectiveJvmTarget = (
      state.manifest.kotlin?.compilerOptions?.jvmTarget
      ?: state.manifest.jvm?.target
      ?: ElidePackageManifest.JvmTarget.DEFAULT
    )

    // prepare compiler configuration
    val env = Environment.host()
    val inputs = KotlinCompiler.sources(
      srcSet.paths.map { it.path.absolute() }.asSequence()
    )
    val outputs = KotlinCompiler.classesDir(kotlincClassesOutput)
    val effectiveKnownPlugins: MutableList<KotlinCompilerConfig.KotlinPluginConfig> = LinkedList()
    val compiler = KotlinCompiler.create(args, env, inputs, outputs, projectRoot = state.project.root) {
      // add classpath and let caller amend args as needed
      classpathAsList = finalizedClasspath.paths.filter {
        it.path.isDirectory() || (it.path.isRegularFile() && it.path.extension in sortedSetOf(
          "jar",
          "zip",
          "klib",
          "jmod",
        ))
      }.map { it.path.toFile() }

      incrementalCompilation = state.manifest.kotlin?.features?.incremental != false
      jvmTarget = when (val tgt = effectiveJvmTarget.argValue) {
        "auto" -> ElidePackageManifest.JvmTarget.DEFAULT.argValue
        else -> tgt
      }

      // handle built-in plugins
      if (state.manifest.kotlin?.features?.enableDefaultPlugins != false) {
        effectiveKnownPlugins.addAll(
          KotlinCompiler.configureDefaultPlugins(
            this,
            tests,
            enableSerialization = state.manifest.kotlin?.features?.serialization != false,
          ),
        )
      }
      argsAmender(this)
    }

    // fire compile job
    compiler.invoke(
      object : AbstractTool.EmbeddedToolState {
        override val resourcesPath: Path get() = state.resourcesPath
        override val project: ElideProject? get() = state.project
      },
    ).also { result ->
      when (result) {
        is Tool.Result.Success -> {
          logging.debug { "Kotlin compilation finished without error" }
        }

        else -> {
          logging.debug { "Kotlin compilation failed" }
        }
      }
    }.asExecResult()
  }.describedBy {
    val pluralized = if (srcSet.paths.size == 1) {
      if (tests) "test file" else "source file"
    } else {
      if (tests) "tests" else "sources"
    }
    val suiteTag = if (srcSet.name == "main" || srcSet.name == "test") "" else " (suite '${srcSet.name}')"
    "Compiling ${srcSet.paths.size} Kotlin $pluralized$suiteTag"
  }.also { kotlinc ->
    config.taskGraph.apply {
      addNode(kotlinc)
      if (dependencies.isNotEmpty()) {
        dependencies.forEach { dependency ->
          putEdge(kotlinc, dependency)
        }
      }
    }
  }

  @Suppress("unused", "CyclomaticComplexMethod", "LongMethod")
  override suspend fun contribute(state: ElideBuildState, config: BuildConfigurator.BuildConfiguration) {
    // interpret/load maven dependencies to resolver
    val resolver = if (!state.manifest.dependencies.maven.hasPackages()) {
      null
    } else {
      logging.debug { "Maven dependencies detected; preparing Maven resolver" }
      when (
        val existing = config.resolvers[DependencyResolver.MavenResolver::class]
      ) {
        null -> RepositorySystemFactory().let { systemFactory ->
          // register build state as injectable; must occur before other aether DI usages
          state.beanContext.registerSingleton(
            AetherProjectProvider::class.java,
            object: AetherProjectProvider {
              override fun buildState(): ElideBuildState = state
            }
          )
          MavenAetherResolver(
            config,
            state.events,
            state.beanContext.getBean(RepositorySystem::class.java),
            state.beanContext.getBean(DefaultRepositorySystemSession::class.java).apply {
              setOffline(config.settings.dry)
            },
          ).apply {
            // configure repositories and packages for a resolver from scratch.
            registerPackagesFromManifest(state)
          }
        }

        else -> (existing as MavenAetherResolver).also {
          // existing resolver is already configured; inform it of any additional packages or repositories.
          it.registerPackagesFromManifest(state)
        }
      }.also {
        config.resolvers[DependencyResolver.MavenResolver::class] = it
      }
    }
    logging.debug { "Finished preparing resolver: $resolver" }

    // load jvm source sets
    val javaSrcSet = state.project.sourceSets.find(Java).toList()
    val kotlinSrcSet = state.project.sourceSets.find(Kotlin).toList()

    if (javaSrcSet.isNotEmpty() || kotlinSrcSet.isNotEmpty()) {
      // prepare to configure java and kotlin build tooling
      logging.debug { "Java or Kotlin sources detected; preparing JVM build tooling" }
      config.actionScope.apply {
        config.taskGraph.apply {
          val javacs = if (javaSrcSet.isEmpty() || kotlinSrcSet.isNotEmpty()) {
            // we don't want javac to run before kotlinc because of java/kotlin class inter-dependencies; so, if both
            // are present, we want to run kotlinc instead, passing all sources to it.
            emptyList()
          } else {
            // skip all tests for now (these come last, and depend on mains)
            javaSrcSet.filter { it.paths.isNotEmpty() && it.type != SourceSetType.Tests }.map { srcSet ->
              javac(
                srcSetTaskName(srcSet, "compileJava"),
                resolver,
                state,
                config,
                srcSet,
              )
            }
          }
          val kotlincs = if (kotlinSrcSet.isEmpty()) {
            emptyList()
          } else {
            // kotlin comes after java so they can depend on each other
            kotlinSrcSet.filter { it.paths.isNotEmpty() && it.type != SourceSetType.Tests }.map { srcSet ->
              // `compileKotlinMain` for name `main`
              kotlinc(
                srcSetTaskName(srcSet, "compileKotlin"),
                resolver,
                state,
                config,
                srcSet,
                additionalDeps = buildList {
                  val mainClassesOutput = state.layout.artifacts
                    .resolve("jvm") // `.dev/artifacts/jvm/...`
                    .resolve("classes") // `.../classes/...`
                    .resolve("main") // `.../classes/main/...`

                  if (mainClassesOutput.exists()) add(mainClassesOutput)

                  val codegenClassesOutput = state.layout.artifacts
                    .resolve("jvm") // `.dev/artifacts/jvm/...`
                    .resolve("classes") // `.../classes/...`
                    .resolve("codegen") // `.../classes/codegen/...`

                  if (codegenClassesOutput.exists()) add(codegenClassesOutput)
                }.takeIf { it.isNotEmpty() }?.let {
                  Classpath.from(it)
                },
                dependencies = buildList {
                  addAll(javacs)
                }
              )
            }
          }
          val javacTests = if (javaSrcSet.isEmpty()) {
            emptyList()
          } else {
            // find all tests and depend on both kotlinc and javac output
            javaSrcSet.filter { it.paths.isNotEmpty() && it.type == SourceSetType.Tests }.map { srcSet ->
              javac(
                srcSetTaskName(srcSet, "compileJavaTest"),
                resolver,
                state,
                config,
                srcSet,
                tests = true,
              ).describedBy {
                val pluralized = if (srcSet.paths.size == 1) "source file" else "sources"
                "Compiling ${srcSet.paths.filter { it.path.extension == "java" }.size} Java test $pluralized"
              }.also { javacTest ->
                addNode(javacTest)
                if (javacs.isNotEmpty()) {
                  javacs.forEach { javac ->
                    putEdge(javacTest, javac)
                  }
                }
              }
            }
          }
          if (kotlinSrcSet.isEmpty()) {
            emptyList()
          } else {
            val kotlinTestSrcSets = kotlinSrcSet.filter { it.paths.isNotEmpty() && it.type == SourceSetType.Tests }
            if (kotlinTestSrcSets.isEmpty()) {
              emptyList()
            } else {
              // add main class outputs so they are visible to tests
              val mainClassesOutput = state.layout.artifacts
                .resolve("jvm") // `.dev/artifacts/jvm/...`
                .resolve("classes") // `.../classes/...`
                .resolve("main") // `.../classes/main/...`

              kotlinTestSrcSets.map { srcSet ->
                kotlinc(
                  srcSetTaskName(srcSet, "compileKotlin"),
                  resolver,
                  state,
                  config,
                  srcSet,
                  Classpath.of(mainClassesOutput),
                  tests = true,
                  dependencies = kotlincs + javacs + javacTests,
                )
              }
            } // end kotlin test compiles
          } // end kotlin source sets

          // Configure exec tasks from Maven exec-maven-plugin
          val execTasks = state.manifest.execTasks
          if (execTasks.isNotEmpty()) {
            logging.debug { "Found ${execTasks.size} exec task(s) from Maven configuration" }

            // Compile tasks that exec tasks should depend on
            val compileTasks = javacs + kotlincs

            execTasks.forEach { execTask ->
              val taskName = "exec:${execTask.id}"
              logging.debug { "Configuring exec task: $taskName (type=${execTask.type})" }

              val task = fn(taskName) {
                contributeExecTask(state, config, execTask, resolver).asExecResult()
              }.describedBy {
                when (execTask.type) {
                  ElidePackageManifest.ExecTaskType.JAVA ->
                    "Running ${execTask.mainClass}"
                  ElidePackageManifest.ExecTaskType.EXECUTABLE ->
                    "Executing ${execTask.executable}"
                }
              }

              addNode(task)

              // Exec tasks depend on compilation
              compileTasks.forEach { compileTask ->
                putEdge(task, compileTask)
              }
            }
          }

          // Configure javadoc JAR tasks from maven-javadoc-plugin
          val compileTasks = javacs + kotlincs
          val javadocJarTasks = state.manifest.artifacts.filterValues {
            it is ElidePackageManifest.JavadocJar
          }.map { (name, artifact) ->
            val javadocArtifact = artifact as ElidePackageManifest.JavadocJar
            contributeJavadocJarTask(name, state, config, javadocArtifact, compileTasks)
          }

          // Configure source JAR tasks from maven-source-plugin
          val sourceJarTasks = state.manifest.artifacts.filterValues {
            it is ElidePackageManifest.SourceJar
          }.map { (name, artifact) ->
            val sourceArtifact = artifact as ElidePackageManifest.SourceJar
            contributeSourceJarTask(name, state, config, sourceArtifact, compileTasks)
          }

          // Configure assembly tasks from maven-assembly-plugin
          val allJarTasks = javadocJarTasks + sourceJarTasks
          state.manifest.artifacts.filterValues {
            it is ElidePackageManifest.Assembly
          }.forEach { (name, artifact) ->
            val assembly = artifact as ElidePackageManifest.Assembly
            contributeAssemblyTasks(name, state, config, assembly, allJarTasks + compileTasks)
          }
        } // end task graph scope
      } // end action scope
    } // end kotlin/java presence
  } // end contributions for jvm

  /** Execute a Maven exec-maven-plugin task */
  private suspend fun contributeExecTask(
    state: ElideBuildState,
    config: BuildConfigurator.BuildConfiguration,
    execTask: ElidePackageManifest.ExecTask,
    resolver: MavenAetherResolver?,
  ): Tool.Result {
    val workingDir = execTask.workingDirectory?.let {
      state.project.root.resolve(it)
    } ?: state.project.root

    when (execTask.type) {
      ElidePackageManifest.ExecTaskType.JAVA -> {
        // Build classpath based on scope
        val classpathPaths = buildList {
          // Add compiled classes
          val classesDir = state.layout.artifacts
            .resolve("jvm")
            .resolve("classes")
            .resolve("main")
          if (classesDir.exists()) add(classesDir)

          // Add resolved dependencies via classpath provider
          resolver?.classpathProvider(
            object : ClasspathSpec {
              override val usage: MultiPathUsage = when (execTask.classpathScope) {
                ElidePackageManifest.ClasspathScope.COMPILE -> MultiPathUsage.Compile
                ElidePackageManifest.ClasspathScope.RUNTIME -> MultiPathUsage.Runtime
                ElidePackageManifest.ClasspathScope.TEST -> MultiPathUsage.TestCompile
              }
            },
          )?.classpath()?.paths?.forEach { item ->
            add(item.path)
          }
        }

        val classpathString = classpathPaths.joinToString(File.pathSeparator) { it.absolutePathString() }

        // Build java command
        val javaHome = System.getProperty("java.home")
        val javaExec = Path.of(javaHome, "bin", "java")

        val args = Arguments.empty().toMutable().apply {
          // Classpath
          add("-cp")
          add(classpathString)

          // System properties
          execTask.systemProperties.forEach { (k, v) ->
            add("-D$k=$v")
          }

          // Main class
          add(execTask.mainClass!!)

          // Arguments
          execTask.args.forEach { add(it) }
        }

        val env = Environment.host().extend(execTask.env)

        logging.info { "Executing: java -cp ... ${execTask.mainClass} ${execTask.args.joinToString(" ")}" }

        val task = ProcessRunner.buildFrom(
          javaExec,
          args,
          env,
          options = ProcessRunner.ProcessOptions(
            shell = ProcessRunner.ProcessShell.None,
            workingDirectory = workingDir,
          ),
        )

        val process = task.spawn()
        val status = process.asDeferred().await()

        return when (status) {
          is ProcessRunner.ProcessStatus.Success -> {
            logging.debug { "Exec task '${execTask.id}' completed successfully" }
            Tool.Result.Success
          }
          is ProcessRunner.ProcessStatus.ExitCode -> {
            logging.error { "Exec task '${execTask.id}' failed with exit code ${status.code}" }
            Tool.Result.UnspecifiedFailure
          }
          is ProcessRunner.ProcessStatus.Err -> {
            logging.error { "Exec task '${execTask.id}' failed with error: ${status.err.message}" }
            Tool.Result.UnspecifiedFailure
          }
          else -> {
            logging.error { "Exec task '${execTask.id}' ended with unexpected status: $status" }
            Tool.Result.UnspecifiedFailure
          }
        }
      }

      ElidePackageManifest.ExecTaskType.EXECUTABLE -> {
        val execPath = Path.of(execTask.executable!!)

        val args = Arguments.empty().toMutable().apply {
          execTask.args.forEach { add(it) }
        }

        val env = Environment.host().extend(execTask.env)

        logging.info { "Executing: ${execTask.executable} ${execTask.args.joinToString(" ")}" }

        val task = ProcessRunner.buildFrom(
          execPath,
          args,
          env,
          options = ProcessRunner.ProcessOptions(
            shell = ProcessRunner.ProcessShell.Active,
            workingDirectory = workingDir,
          ),
        )

        val process = task.spawn()
        val status = process.asDeferred().await()

        return when (status) {
          is ProcessRunner.ProcessStatus.Success -> {
            logging.debug { "Exec task '${execTask.id}' completed successfully" }
            Tool.Result.Success
          }
          is ProcessRunner.ProcessStatus.ExitCode -> {
            logging.error { "Exec task '${execTask.id}' failed with exit code ${status.code}" }
            Tool.Result.UnspecifiedFailure
          }
          is ProcessRunner.ProcessStatus.Err -> {
            logging.error { "Exec task '${execTask.id}' failed with error: ${status.err.message}" }
            Tool.Result.UnspecifiedFailure
          }
          else -> {
            logging.error { "Exec task '${execTask.id}' ended with unexpected status: $status" }
            Tool.Result.UnspecifiedFailure
          }
        }
      }
    }
  }

  /** Contribute a javadoc JAR task */
  private fun ActionScope.contributeJavadocJarTask(
    name: String,
    state: ElideBuildState,
    config: BuildConfigurator.BuildConfiguration,
    artifact: ElidePackageManifest.JavadocJar,
    dependencies: List<Task>,
  ): Task {
    val projectName = state.manifest.name ?: "project"
    val projectVersion = state.manifest.version ?: "0.0.0"

    return fn("javadoc:$name", taskDependencies(dependencies)) {
      val javadocOutputDir = state.layout.artifacts.resolve("javadoc")
      val javadocJarPath = state.layout.artifacts.resolve("$projectName-$projectVersion-javadoc.jar")

      // Ensure output dir exists
      if (!javadocOutputDir.exists()) {
        javadocOutputDir.createDirectories()
      }

      // Build javadoc arguments
      val sourceDir = state.project.root.resolve("src/main/java")
      if (!sourceDir.exists()) {
        logging.warn { "Source directory not found for javadoc: $sourceDir" }
        return@fn Tool.Result.Success.asExecResult()
      }

      val args = Arguments.empty().toMutable().apply {
        add("-d")
        add(javadocOutputDir.absolutePathString())
        add("-sourcepath")
        add(sourceDir.absolutePathString())

        // Add groups
        artifact.groups.forEach { (title, packages) ->
          add("-group")
          add(title)
          add(packages.joinToString(":"))
        }

        // Add window title
        artifact.windowTitle?.let {
          add("-windowtitle")
          add(it)
        }

        // Add doc title
        artifact.docTitle?.let {
          add("-doctitle")
          add(it)
        }

        // Add links
        artifact.links.forEach { link ->
          add("-link")
          add(link)
        }

        // Add subpackages to process
        add("-subpackages")
        add(state.manifest.dependencies.maven.coordinates?.group ?: ".")
      }.build()

      // Run javadoc
      val javadocTool = JavadocTool(
        args = args,
        env = Environment.host(),
        inputs = JavadocTool.JavadocInputs.NoInputs,
        outputs = JavadocTool.JavadocOutputs.NoOutputs,
      )

      val javadocResult = javadocTool.invoke(object : AbstractTool.EmbeddedToolState {
        override val resourcesPath: Path get() = state.resourcesPath
        override val project: ElideProject? get() = state.project
      })

      if (javadocResult != Tool.Result.Success) {
        logging.warn { "Javadoc generation had warnings or errors" }
      }

      // Create JAR from javadoc output
      if (javadocOutputDir.exists() && Files.list(javadocOutputDir).findAny().isPresent) {
        val jarArgs = Arguments.empty().toMutable().apply {
          add("--create")
          add("--file")
          add(javadocJarPath.absolutePathString())
          add("-C")
          add(javadocOutputDir.absolutePathString())
          add(".")
        }.build()

        val jarTool = JarTool.create(
          args = jarArgs,
          env = Environment.host(),
          inputs = JarTool.JarToolInputs.NoInputs,
          outputs = JarTool.outputJar(javadocJarPath),
        )

        jarTool.invoke(object : AbstractTool.EmbeddedToolState {
          override val resourcesPath: Path get() = state.resourcesPath
          override val project: ElideProject? get() = state.project
        }).asExecResult()
      } else {
        Tool.Result.Success.asExecResult()
      }
    }.describedBy {
      "Generating Javadoc JAR"
    }.also { task ->
      config.taskGraph.apply {
        addNode(task)
        dependencies.forEach { dep -> putEdge(task, dep) }
      }
    }
  }

  /** Contribute a source JAR task */
  private fun ActionScope.contributeSourceJarTask(
    name: String,
    state: ElideBuildState,
    config: BuildConfigurator.BuildConfiguration,
    artifact: ElidePackageManifest.SourceJar,
    dependencies: List<Task>,
  ): Task {
    val projectName = state.manifest.name ?: "project"
    val projectVersion = state.manifest.version ?: "0.0.0"
    val classifier = artifact.classifier ?: "sources"

    return fn("sourceJar:$name", taskDependencies(dependencies)) {
      val sourceJarPath = state.layout.artifacts.resolve("$projectName-$projectVersion-$classifier.jar")
      val sourceDir = state.project.root.resolve("src/main/java")

      if (!sourceDir.exists()) {
        logging.warn { "Source directory not found for source JAR: $sourceDir" }
        return@fn Tool.Result.Success.asExecResult()
      }

      // Build jar arguments
      val jarArgs = Arguments.empty().toMutable().apply {
        add("--create")
        add("--file")
        add(sourceJarPath.absolutePathString())
        add("-C")
        add(sourceDir.absolutePathString())
        add(".")
      }.build()

      val jarTool = JarTool.create(
        args = jarArgs,
        env = Environment.host(),
        inputs = JarTool.JarToolInputs.NoInputs,
        outputs = JarTool.outputJar(sourceJarPath),
      )

      jarTool.invoke(object : AbstractTool.EmbeddedToolState {
        override val resourcesPath: Path get() = state.resourcesPath
        override val project: ElideProject? get() = state.project
      }).asExecResult()
    }.describedBy {
      "Packaging source JAR ($classifier)"
    }.also { task ->
      config.taskGraph.apply {
        addNode(task)
        dependencies.forEach { dep -> putEdge(task, dep) }
      }
    }
  }

  /** Contribute assembly tasks for creating distribution archives */
  private fun ActionScope.contributeAssemblyTasks(
    name: String,
    state: ElideBuildState,
    config: BuildConfigurator.BuildConfiguration,
    assembly: ElidePackageManifest.Assembly,
    dependencies: List<Task>,
  ) {
    val projectName = state.manifest.name ?: "project"
    val projectVersion = state.manifest.version ?: "0.0.0"

    // Parse descriptor if path provided
    val resolvedAssembly = assembly.descriptorPath?.let { descPath ->
      val fullPath = state.project.root.resolve(descPath)
      if (fullPath.exists()) {
        try {
          AssemblyDescriptorParser.parse(fullPath)
        } catch (e: Exception) {
          logging.warn { "Failed to parse assembly descriptor $descPath: ${e.message}" }
          assembly
        }
      } else assembly
    } ?: assembly

    // Resolve base directory with Maven-like variable substitution
    val baseDir = resolvedAssembly.baseDirectory
      ?.replace("\${artifactId}", projectName)
      ?.replace("\${version}", projectVersion)
      ?.replace("\${project.artifactId}", projectName)
      ?.replace("\${project.version}", projectVersion)

    resolvedAssembly.formats.forEach { format ->
      val task = fn("assembly:${assembly.id}:$format", taskDependencies(dependencies)) {
        val outputFile = state.layout.artifacts.resolve("$projectName-${assembly.id}.$format")

        // Collect files based on fileSets
        val files = resolvedAssembly.fileSets.flatMap { fileSet ->
          val sourceDir = fileSet.directory?.let { state.project.root.resolve(it) }
            ?: state.project.root
          val outputDir = fileSet.outputDirectory ?: fileSet.directory ?: ""

          if (!sourceDir.exists()) return@flatMap emptyList<Pair<Path, String>>()

          collectFilesForAssembly(sourceDir, fileSet.includes, fileSet.excludes).map { file ->
            val relativePath = if (sourceDir == state.project.root) {
              file.fileName.toString()
            } else {
              sourceDir.relativize(file).toString()
            }
            val archivePath = if (outputDir.isEmpty()) relativePath else "$outputDir/$relativePath"
            file to archivePath
          }
        }

        if (files.isEmpty()) {
          logging.debug { "No files to include in assembly ${assembly.id}" }
          return@fn Tool.Result.Success.asExecResult()
        }

        when (format) {
          "tar.gz" -> {
            val builder = ArchiveBuilder.tarGzBuilder(outputFile)
            files.forEach { (sourcePath, archivePath) ->
              val finalPath = baseDir?.let { "$it/$archivePath" } ?: archivePath
              builder.packFile(sourcePath, finalPath)
            }
            builder.finalizeArchive()
          }
          "zip" -> {
            val builder = ArchiveBuilder.zipBuilder(outputFile)
            files.forEach { (sourcePath, archivePath) ->
              val finalPath = baseDir?.let { "$it/$archivePath" } ?: archivePath
              builder.packFile(sourcePath, finalPath)
            }
            builder.finalizeArchive()
          }
          else -> {
            logging.warn { "Unsupported archive format: $format" }
          }
        }

        Tool.Result.Success.asExecResult()
      }.describedBy {
        "Creating $format assembly (${assembly.id})"
      }

      config.taskGraph.apply {
        addNode(task)
        dependencies.forEach { dep -> putEdge(task, dep) }
      }
    }
  }

  /** Collect files for assembly based on includes/excludes patterns */
  private fun collectFilesForAssembly(
    sourceDir: Path,
    includes: List<String>,
    excludes: List<String>,
  ): List<Path> {
    if (!sourceDir.exists()) return emptyList()

    return Files.walk(sourceDir)
      .filter { Files.isRegularFile(it) }
      .filter { file ->
        val relativePath = sourceDir.relativize(file).toString()

        // If no includes specified, include all; otherwise check includes
        val included = includes.isEmpty() || includes.any { pattern ->
          matchGlobPattern(relativePath, pattern)
        }

        // Check excludes
        val excluded = excludes.any { pattern ->
          matchGlobPattern(relativePath, pattern)
        }

        included && !excluded
      }
      .toList()
  }

  /** Simple glob pattern matching for assembly file filtering */
  private fun matchGlobPattern(path: String, pattern: String): Boolean {
    // Convert glob to regex
    val regex = pattern
      .replace(".", "\\.")
      .replace("**", ".*")
      .replace("*", "[^/]*")
      .replace("?", ".")
    return path.matches(Regex(regex))
  }
}

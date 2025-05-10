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

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.incremental.classpathAsList
import java.nio.file.Path
import kotlin.io.path.absolute
import elide.exec.ActionScope
import elide.exec.Task
import elide.exec.Task.Companion.fn
import elide.runtime.Logging
import elide.runtime.gvm.kotlin.KotlinLanguage
import elide.tool.Arguments
import elide.tool.Classpath
import elide.tool.ClasspathSpec
import elide.tool.Environment
import elide.tool.MultiPathUsage
import elide.tool.Tool
import elide.tooling.AbstractTool
import elide.tooling.config.BuildConfigurator
import elide.tooling.config.BuildConfigurator.ElideBuildState
import elide.tooling.deps.DependencyResolver
import elide.tooling.jvm.resolver.MavenAetherResolver
import elide.tooling.kotlin.KotlinCompiler
import elide.tooling.project.SourceSet
import elide.tooling.project.SourceSetLanguage.Java
import elide.tooling.project.SourceSetLanguage.Kotlin
import elide.tooling.project.SourceSetType
import elide.tooling.project.manifest.ElidePackageManifest.KotlinJvmCompilerOptions

private fun srcSetTaskName(srcSet: SourceSet, name: String): String {
  return "$name${srcSet.name[0].uppercase()}${srcSet.name.slice(1..srcSet.name.lastIndex)}"
}

/**
 * ## JVM Build Configurator
 */
internal class JvmBuildConfigurator : BuildConfigurator {
  private companion object {
    private const val JUNIT_JUPITER_API = "org.junit.jupiter:junit-jupiter-api"
    private const val JUNIT_JUPITER_ENGINE = "org.junit.jupiter:junit-jupiter-engine"
    private const val JUNIT_PLATFORM_ENGINE = "org.junit.platform:junit-platform-engine"
    private const val JUNIT_PLATFORM_COMMONS = "org.junit.platform:junit-platform-console"
    private const val JUNIT_PLATFORM_CONSOLE = "org.junit.platform:junit-platform-console"
    private const val JUNIT_JUPITER_PARAMS = "org.junit.jupiter:junit-jupiter-params"
    private const val KOTLIN_TEST = "org.jetbrains.kotlin:kotlin-test"
    private const val KOTLIN_TEST_JUNIT5 = "org.jetbrains.kotlin:kotlin-test-junit5"
    private const val KOTLINX_COROUTINES = "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm"
    private const val KOTLINX_COROUTINES_TEST = "org.jetbrains.kotlinx:kotlinx-coroutines-test-jvm"

    private val testCoordinates = arrayOf(
      JUNIT_JUPITER_API,
      JUNIT_JUPITER_ENGINE,
      JUNIT_PLATFORM_ENGINE,
      JUNIT_PLATFORM_COMMONS,
      JUNIT_PLATFORM_CONSOLE,
      JUNIT_JUPITER_PARAMS,
      KOTLIN_TEST,
      KOTLIN_TEST_JUNIT5,
    )

    @JvmStatic private val logging by lazy { Logging.of(JvmBuildConfigurator::class) }
  }

  private fun jarNameFor(coordinate: String): String {
    val parts = coordinate.split(":")
    require(parts.size == 2) { "Invalid built-in coordinate: $coordinate" }
    return "${parts[1]}.jar"
  }

  private fun builtinKotlinJarPath(state: ElideBuildState, dependency: String): Path {
    return System.getenv("KOTLIN_HOME")?.let { kotlinHomeByEnv ->
      Path.of(kotlinHomeByEnv)
        .resolve("lib")
        .resolve(jarNameFor(dependency))
    } ?: state.resourcesPath
      .resolve("kotlin")
      .resolve(KotlinLanguage.VERSION)
      .resolve("lib")
      .resolve(jarNameFor(dependency))
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
  ) = fn(name) {
    val compileClasspath = resolver?.classpathProvider(object : ClasspathSpec {
      override val usage: MultiPathUsage = if (tests) MultiPathUsage.TestCompile else MultiPathUsage.Compile
    })?.classpath()

    val kotlincClassesOutput = state.layout.artifacts
      .resolve("jvm") // `.dev/artifacts/jvm/...`
      .resolve("classes") // `.../classes/...`
      .resolve(srcSet.name) // `.../classes/main/...`

    logging.debug { "Kotlin main classpath: $compileClasspath" }
    logging.debug { "Classes output root: $kotlincClassesOutput" }

    // kotlinx support is enabled by default; this includes kotlin's testing tools
    val staticDeps = Classpath.empty().toMutable()
    if (state.manifest.kotlin?.features?.kotlinx != false) {
      if (state.manifest.kotlin?.features?.coroutines != false) {
        // add `org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm`
        staticDeps.add(builtinKotlinJarPath(state, KOTLINX_COROUTINES))
      }
    }
    if (tests) {
      if (state.manifest.kotlin?.features?.testing != false) {
        // junit5, kotlin testing
        testCoordinates.forEach { testLib ->
          staticDeps.add(builtinKotlinJarPath(state, testLib))
        }
      }
      if (state.manifest.kotlin?.features?.kotlinx != false) {
        if (state.manifest.kotlin?.features?.coroutines != false) {
          // add `org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm`
          staticDeps.add(builtinKotlinJarPath(state, KOTLINX_COROUTINES))

          // add `org.jetbrains.kotlinx:kotlinx-coroutines-test-jvm`
          staticDeps.add(builtinKotlinJarPath(state, KOTLINX_COROUTINES_TEST))
        }
      }
    }

    val finalizedClasspath = (compileClasspath ?: Classpath.empty()).toMutable().apply {
      // injected deps
      add(staticDeps)

      // if additional deps are provided, add them to the classpath
      additionalDeps?.let { add(it) }
    }.toList().let {
      Classpath.from(it.map { it.path })
    }

    val kotlincOpts = state.manifest.kotlin?.compilerOptions ?: KotlinJvmCompilerOptions()
    val args = Arguments.empty().toMutable().apply {
      // apply arguments
      kotlincOpts.amend(this)
    }.build()

    logging.debug { "Kotlin compiler args: '${args.asArgumentList().joinToString(" ")}'" }

    // prepare compiler configuration
    val env = Environment.host()
    val inputs = KotlinCompiler.sources(srcSet.paths.map { it.path.absolute() }.asSequence())
    val outputs = KotlinCompiler.classesDir(kotlincClassesOutput)
    val compiler = KotlinCompiler.create(args, env, inputs, outputs) {
      // add classpath and let caller amend args as needed
      classpathAsList = finalizedClasspath.paths.map { it.path.toFile() }
      argsAmender(this)
    }

    // fire compile job
    compiler.invoke(object : AbstractTool.EmbeddedToolState {
      override val resourcesPath: Path get() = state.resourcesPath
    }).also { result ->
      when (result) {
        is Tool.Result.Success -> {
          logging.debug { "Kotlin compilation finished without error" }
        }
        else -> {
          logging.error { "Kotlin compilation failed" }
        }
      }
    }
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
        null -> state.beanContext.getBean(MavenAetherResolver::class.java).apply {
          // configure repositories and packages for a resolver from scratch.
          registerPackagesFromManifest(state)
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
          val javacs = if (javaSrcSet.isEmpty()) {
            emptyList()
          } else {
            // skip all tests for now (these come last, and depend on mains)
            javaSrcSet.filter { it.paths.isNotEmpty() && it.type != SourceSetType.Tests }.map { srcSet ->
              // `compileJavaMain` for name `main`
              fn(name = srcSetTaskName(srcSet, "compileJava")) {
                val compileClasspath = resolver?.classpathProvider(object : ClasspathSpec {
                  override val usage: MultiPathUsage = MultiPathUsage.Compile
                })?.classpath()

                val javacClassesOutput = state.layout.artifacts
                  .resolve("jvm") // `.dev/artifacts/jvm/...`
                  .resolve("classes") // `.../classes/...`
                  .resolve(srcSet.name) // `.../classes/main/...`

                logging.debug { "Kotlin main classpath: $compileClasspath" }
                logging.debug { "Classes output root: $javacClassesOutput" }

                val args = Arguments.empty().toMutable().apply {
                  compileClasspath?.let { add(it) }
                }.build()

                logging.debug { "Java compiler args: '${args.asArgumentList().joinToString(" ")}'" }

                // prepare compiler configuration
                val env = Environment.host()
                val inputs = JavaCompiler.sources(srcSet.paths.map { it.path.absolute() }.asSequence())
                val outputs = JavaCompiler.classesDir(javacClassesOutput)
                val compiler = JavaCompiler.create(
                  args = args,
                  env = env,
                  inputs = inputs,
                  outputs = outputs,
                )

                // fire compile job
                val result = compiler.invoke(object : AbstractTool.EmbeddedToolState {
                  override val resourcesPath: Path get() = state.resourcesPath
                })
                when (result) {
                  is Tool.Result.Success -> {
                    logging.debug { "Java compilation finished without error" }
                  }
                  else -> {
                    logging.error { "Java compilation failed" }
                  }
                }
                // @TODO error triggering
              }.describedBy {
                val pluralized = if (srcSet.paths.size == 1) "source file" else "sources"
                val suiteTag = if (srcSet.name == "main") "" else " (suite '${srcSet.name}')"
                "Compiling ${srcSet.paths.size} Java $pluralized$suiteTag"
              }.also { javac ->
                addNode(javac)
              }
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
              )
            }
          }
          val javacTests = if (javaSrcSet.isEmpty()) {
            emptyList()
          } else {
            val testCompileClasspath = resolver?.classpathProvider(object : ClasspathSpec {
              override val usage: MultiPathUsage = MultiPathUsage.TestCompile
            })?.classpath()

            // find all tests and depend on both kotlinc and javac output
            javaSrcSet.filter { it.paths.isNotEmpty() && it.type == SourceSetType.Tests }.map { srcSet ->
              // `compileJavaTest` for name `test`
              fn(name = srcSetTaskName(srcSet, "compileJava")) {
                val compileClasspath = resolver?.classpathProvider(object : ClasspathSpec {
                  override val usage: MultiPathUsage = MultiPathUsage.Compile
                })?.classpath()

                val javacMainClassesOutput = state.layout.artifacts
                  .resolve("jvm") // `.dev/artifacts/jvm/...`
                  .resolve("classes") // `.../classes/...`
                  .resolve("main") // `.../classes/main/...`

                val javacClassesOutput = state.layout.artifacts
                  .resolve("jvm") // `.dev/artifacts/jvm/...`
                  .resolve("classes") // `.../classes/...`
                  .resolve(srcSet.name) // `.../classes/test/...`

                logging.debug { "Java main classpath: $compileClasspath" }
                logging.debug { "Java output root: $javacClassesOutput" }

                val args = Arguments.empty().toMutable().apply {
                  (compileClasspath ?: Classpath.empty()).toMutable().apply {
                    // user's test deps go before all compile-time and static libs
                    testCompileClasspath?.let { prepend(it) }

                    // class deps go before all other deps
                    prepend(javacMainClassesOutput)
                  }.also {
                    add(it)
                  }
                }.build()

                // prepare compiler configuration
                val env = Environment.host()
                val inputs = JavaCompiler.sources(srcSet.paths.map { it.path.absolute() }.asSequence())
                val outputs = JavaCompiler.classesDir(javacClassesOutput)
                val compiler = JavaCompiler.create(
                  args = args,
                  env = env,
                  inputs = inputs,
                  outputs = outputs,
                )

                // fire compile job
                val result = compiler.invoke(object : AbstractTool.EmbeddedToolState {
                  override val resourcesPath: Path get() = state.resourcesPath
                })
                when (result) {
                  is Tool.Result.Success -> {
                    logging.debug { "Java test compilation finished without error" }
                  }
                  else -> {
                    logging.error { "Java test compilation failed" }
                  }
                }
              }.describedBy {
                val pluralized = if (srcSet.paths.size == 1) "source file" else "sources"
                "Compiling ${srcSet.paths.size} Java test $pluralized"
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
        } // end task graph scope
      } // end action scope
    } // end kotlin/java presence
  } // end contributions for jvm
}

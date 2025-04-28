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

import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import elide.exec.Task.Companion.fn
import elide.runtime.Logging
import elide.tool.ClasspathSpec
import elide.tool.MultiPathUsage
import elide.tooling.config.BuildConfigurator
import elide.tooling.config.BuildConfigurator.ElideBuildState
import elide.tooling.deps.DependencyResolver
import elide.tooling.jvm.resolver.MavenAetherResolver
import elide.tooling.project.SourceSetLanguage.Java
import elide.tooling.project.SourceSetLanguage.Kotlin
import elide.tooling.project.SourceSetType

internal class JvmBuildConfigurator : BuildConfigurator {
  private companion object {
    @JvmStatic private val logging by lazy { Logging.of(JvmBuildConfigurator::class) }
  }

  @Suppress("unused", "UnusedVariable")
  override fun contribute(state: ElideBuildState, config: BuildConfigurator.BuildConfiguration) {
    // interpret/load maven dependencies to resolver
    val resolver = if (state.manifest.dependencies.maven.packages.isEmpty()) {
      null
    } else {
      logging.debug { "Maven dependencies detected; preparing Maven resolver" }
      when (
        val existing = config.resolvers[DependencyResolver.MavenResolver::class]
      ) {
        null -> MavenAetherResolver().apply {
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
        val javacs = if (javaSrcSet.isEmpty()) {
          emptyList()
        } else {
          // skip all tests for now (these come last, and depend on mains)
          javaSrcSet.filter { it.type != SourceSetType.Tests }.map {
            // `compileJavaMain` for name `main`
            fn(name = "compileJava${it.name[0].uppercase()}${it.name.slice(1..it.name.lastIndex)}") {
              // would compile
              val compileClasspath = resolver?.classpathProvider(object : ClasspathSpec {
                override val usage: MultiPathUsage = MultiPathUsage.Compile
              })?.classpath()

              val javacClassesOutput = state.layout.artifacts
                .resolve("jvm") // `.dev/artifacts/jvm/...`
                .resolve("classes") // `.../classes/...`
                .resolve(it.name) // `.../classes/main/...`

              logging.debug { "Kotlin main classpath: $compileClasspath" }
              logging.debug { "Classes output root: $javacClassesOutput" }

              delay(5.seconds)
            }.describedBy {
              val pluralized = if (it.paths.size == 1) "source file" else "sources"
              val suiteTag = if (it.name == "main") "" else " (suite '${it.name}')"
              "Compiling ${it.paths.size} Java $pluralized$suiteTag"
            }
          }
        }
        config.taskGraph.apply {
          val kotlincs = if (kotlinSrcSet.isEmpty()) {
            emptyList()
          } else {
            // kotlin comes after java so they can depend on each other
            kotlinSrcSet.filter { it.type != SourceSetType.Tests }.map {
              // `compileKotlinMain` for name `main`
              fn(name = "compileKotlin${it.name[0].uppercase()}${it.name.slice(1..it.name.lastIndex)}") {
                // would compile
                val compileClasspath = resolver?.classpathProvider(object : ClasspathSpec {
                  override val usage: MultiPathUsage = MultiPathUsage.Compile
                })?.classpath()

                val kotlincClassesOutput = state.layout.artifacts
                  .resolve("jvm") // `.dev/artifacts/jvm/...`
                  .resolve("classes") // `.../classes/...`
                  .resolve(it.name) // `.../classes/main/...`

                logging.debug { "Kotlin main classpath: $compileClasspath" }
                logging.debug { "Classes output root: $kotlincClassesOutput" }

                delay(5.seconds)
              }.describedBy {
                val pluralized = if (it.paths.size == 1) "source file" else "sources"
                val suiteTag = if (it.name == "main") "" else " (suite '${it.name}')"
                "Compiling ${it.paths.size} Kotlin $pluralized$suiteTag"
              }.also { kotlinc ->
                if (javacs.isNotEmpty()) {
                  javacs.forEach { javac ->
                    putEdge(kotlinc, javac)
                  }
                }
              }
            }
          }
          val javacTests = if (javaSrcSet.isEmpty()) {
            emptyList()
          } else {
            // find all tests and depend on both kotlinc and javac output
            javaSrcSet.filter { it.type == SourceSetType.Tests }.map {
              // `compileJavaTest` for name `test`
              fn(name = "compileJava${it.name[0].uppercase()}${it.name.slice(1..it.name.lastIndex)}") {
                // would compile
                val compileClasspath = resolver?.classpathProvider(object : ClasspathSpec {
                  override val usage: MultiPathUsage = MultiPathUsage.TestCompile
                })?.classpath()

                val javacMainClassesOutput = state.layout.artifacts
                  .resolve("jvm") // `.dev/artifacts/jvm/...`
                  .resolve("classes") // `.../classes/...`
                  .resolve("main") // `.../classes/main/...`

                val javacClassesOutput = state.layout.artifacts
                  .resolve("jvm") // `.dev/artifacts/jvm/...`
                  .resolve("classes") // `.../classes/...`
                  .resolve(it.name) // `.../classes/test/...`

                logging.debug { "Java main classpath: $compileClasspath" }
                logging.debug { "Java output root: $javacClassesOutput" }

                delay(5.seconds)
              }.describedBy {
                val pluralized = if (it.paths.size == 1) "source file" else "sources"
                "Compiling ${it.paths.size} Java test $pluralized"
              }.also { javacTest ->
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
            // finally, kotlinc tests depend on everything else
            kotlinSrcSet.filter { it.type == SourceSetType.Tests }.map {
              // `compileKotlinTest` for name `test`
              fn(name = "compileKotlin${it.name[0].uppercase()}${it.name.slice(1..it.name.lastIndex)}") {
                // would compile
                val compileClasspath = resolver?.classpathProvider(object : ClasspathSpec {
                  override val usage: MultiPathUsage = MultiPathUsage.TestCompile
                })?.classpath()

                val javacMainClassesOutput = state.layout.artifacts
                  .resolve("jvm") // `.dev/artifacts/jvm/...`
                  .resolve("classes") // `.../classes/...`
                  .resolve("main") // `.../classes/main/...`

                val kotlincClassesOutput = state.layout.artifacts
                  .resolve("jvm") // `.dev/artifacts/jvm/...`
                  .resolve("classes") // `.../classes/...`
                  .resolve(it.name) // `.../classes/test/...`

                logging.debug { "Kotlin test classpath: $compileClasspath" }
                logging.debug { "Kotlin output root: $kotlincClassesOutput" }

                delay(5.seconds)
              }.describedBy {
                val pluralized = if (it.paths.size == 1) "source file" else "sources"
                "Compiling ${it.paths.size} Kotlin test $pluralized"
              }.also { kotlincTest ->
                if (javacs.isNotEmpty()) {
                  javacs.forEach { javac ->
                    putEdge(kotlincTest, javac)
                  }
                }
                if (javacTests.isNotEmpty()) {
                  javacTests.forEach { javac ->
                    putEdge(kotlincTest, javac)
                  }
                }
                if (kotlincs.isNotEmpty()) {
                  kotlincs.forEach { kotlinc ->
                    putEdge(kotlincTest, kotlinc)
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

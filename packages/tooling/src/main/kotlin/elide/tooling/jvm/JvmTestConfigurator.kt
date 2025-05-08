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
@file:OptIn(DelicateElideApi::class)

package elide.tooling.jvm

import io.github.classgraph.ClassInfo
import io.github.classgraph.MethodInfo
import java.util.stream.Stream
import kotlin.io.path.exists
import elide.runtime.Logging
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.jvm.GuestClassgraph
import elide.runtime.intrinsics.testing.TestingRegistrar
import elide.tool.Classpath
import elide.tool.ClasspathSpec
import elide.tool.MultiPathUsage
import elide.tooling.config.TestConfigurator
import elide.tooling.config.TestConfigurator.*
import elide.tooling.deps.DependencyResolver
import elide.tooling.jvm.resolver.MavenAetherResolver

// Scans classpaths for test discovery.
internal class JvmTestConfigurator : TestConfigurator {
  private companion object {
    private val logging by lazy { Logging.of(JvmTestConfigurator::class) }
    private val eligibleClassAnnotations = arrayOf(
      "io.micronaut.test.extensions.junit5.annotation.MicronautTest",
    )
    private val eligibleMethodAnnotations = arrayOf(
      "org.junit.jupiter.api.Test",
      // (The following are aliases for the first entry, but held here in case they become full annotation types.)
      // "kotlin.test.Test",
      // "elide.testing.Test",
    )
  }

  // Match/register a candidate test class, if it matches criteria; yield a stream of methods to process.
  private fun matchCandidateClass(registry: TestingRegistrar, cls: ClassInfo): Stream<MethodInfo> {
    return cls.declaredMethodInfo.parallelStream().also {
      if (eligibleClassAnnotations.any { cls.hasAnnotation(it) }) {
        registry.register(TestingRegistrar.namedScope(
          cls.simpleName,
          cls.name,
        ))
      }
    }
  }

  // Match/register a candidate test method, if it matches criteria.
  private fun matchCandidateMethod(registry: TestingRegistrar, cls: ClassInfo, method: MethodInfo) {
    // if the method is annotated with any eligible known annotation, we register it and defer evaluation.
    if (eligibleMethodAnnotations.any { method.hasAnnotation(it) }) {
      registry.register(TestingRegistrar.deferred(
        label = method.name,
        qualified = "${cls.name}.${method.name}",
      ) { context ->
        TODO("resolve jvm test method")
      }, scope = TestingRegistrar.namedScope(
        cls.simpleName,
        cls.name,
      ))
    }
  }

  override suspend fun contribute(state: ElideTestState, config: TestConfiguration) {
    val javacMainClassesOutput = state.layout.artifacts
      .resolve("jvm") // `.dev/artifacts/jvm/...`
      .resolve("classes") // `.../classes/...`
      .resolve("main") // `.../classes/main/...`

    val javacTestClassesOutput = state.layout.artifacts
      .resolve("jvm") // `.dev/artifacts/jvm/...`
      .resolve("classes") // `.../classes/...`
      .resolve("test") // `.../classes/test/...`

    if (javacMainClassesOutput.exists() && javacTestClassesOutput.exists()) {
      val resolver = config.resolvers[DependencyResolver.MavenResolver::class] as? MavenAetherResolver
      val classpathProvider = resolver?.classpathProvider(object: ClasspathSpec {
        override val usage: MultiPathUsage get() = MultiPathUsage.TestRuntime
      })?.classpath()

      GuestClassgraph.buildFrom(Classpath.from(
        listOf(
          javacTestClassesOutput,
          javacMainClassesOutput,
        ).plus(
          classpathProvider?.asList()?.map { it.path } ?: emptyList()
        )
      )) {
        // we need to scan for full class and method info, so we can catch method and class tests.
        classgraph.enableClassInfo()
        classgraph.enableAnnotationInfo()
        classgraph.enableMethodInfo()
      }.let { graph ->
        when (graph.isEmpty()) {
          // no tests found on classpath
          true -> logging.debug { "No tests found on (empty) classpath." }

          // classpath is non-empty; scan for tests
          false -> graph.scanResult().use { result ->
            result.allClassesAsMap.values.stream().parallel().flatMap { cls ->
              matchCandidateClass(state.registrar, cls).map { mth ->
                cls to mth
              }
            }.forEach { (host, mth) ->
              matchCandidateMethod(state.registrar, host, mth)
            }
          }
        }
      }
    }
  }
}

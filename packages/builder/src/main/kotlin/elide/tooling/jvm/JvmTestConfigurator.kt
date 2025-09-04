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

import kotlin.io.path.absolute
import kotlin.io.path.exists
import elide.runtime.Logging
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.jvm.GuestClassgraph
import elide.runtime.gvm.jvm.GuestClassgraph.Classgraph
import elide.tooling.Classpath
import elide.tooling.ClasspathSpec
import elide.tooling.MultiPathUsage
import elide.tooling.config.TestConfigurator
import elide.tooling.config.TestConfigurator.ElideTestState
import elide.tooling.config.TestConfigurator.TestConfiguration
import elide.tooling.deps.DependencyResolver
import elide.tooling.jvm.resolver.MavenAetherResolver
import elide.tooling.testing.TestDriverRegistry
import elide.tooling.testing.jvm.GuestJvmTestDriver
import elide.tooling.testing.jvm.JvmTestCase
import elide.tooling.testing.TestGroup
import elide.tooling.testing.TestRegistry
import elide.util.UUID

// Scans classpaths for test discovery.
internal class JvmTestConfigurator : TestConfigurator {
  private companion object {
    private val logging by lazy { Logging.of(JvmTestConfigurator::class) }

    // Test registration currently depends on methods, but class-level annotations are tracked here for future use
    // private val eligibleClassAnnotations = arrayOf(
    //   "io.micronaut.test.extensions.junit5.annotation.MicronautTest",
    // )

    private val eligibleMethodAnnotations = arrayOf(
      "org.junit.jupiter.api.Test",
      // (The following are aliases for the first entry, but held here in case they become full annotation types.)
      // "kotlin.test.Test",
      // "elide.testing.Test",
    )

    private fun registerTests(registry: TestRegistry, classgraph: Classgraph) {
      if (classgraph.isEmpty()) {
        logging.debug { "No tests found on (empty) classpath." }
        return
      }

      // for each class with eligible test methods, emit a suite
      classgraph.scanResult().use { result ->
        result.allClasses.forEach { suiteCandidate ->
          // class suite node, registered lazily
          var suiteNode: TestGroup? = null

          // methods must have one of the test annotations
          suiteCandidate.methodInfo.asSequence().filter { candidate ->
            eligibleMethodAnnotations.any(candidate::hasAnnotation)
          }.forEach { testCandidate ->
            if (suiteNode == null) {
              // lazy registration of the parent node (one-time)
              suiteNode = TestGroup(
                id = UUID.random(),
                parent = null, // currently, there is no way to specify test super-groups via annotations
                displayName = "${suiteCandidate.packageName}.${suiteCandidate.simpleName}",
              )

              registry.register(suiteNode)
            }

            registry.register(
              JvmTestCase(
                id = UUID.random(),
                parent = suiteNode.id,
                displayName = "${suiteNode.displayName}.${testCandidate.name}",
                className = testCandidate.className,
                methodName = testCandidate.name,
              ),
            )
          }
        }
      }
    }
  }

  override suspend fun contribute(state: ElideTestState, config: TestConfiguration) {
    // add test drivers (force guest only for now, can be configured later)
    val drivers = state.beanContext.getBean(TestDriverRegistry::class.java)
    drivers.register(GuestJvmTestDriver(state.guestContextProvider))

    val javacMainClassesOutput = state.layout.artifacts
      .resolve("jvm") // `.dev/artifacts/jvm/...`
      .resolve("classes") // `.../classes/...`
      .resolve("main") // `.../classes/main/...`
      .absolute()

    val javacTestClassesOutput = state.layout.artifacts
      .resolve("jvm") // `.dev/artifacts/jvm/...`
      .resolve("classes") // `.../classes/...`
      .resolve("test") // `.../classes/test/...`
      .absolute()

    if (javacMainClassesOutput.exists() && javacTestClassesOutput.exists()) {
      val resolver = config.resolvers[DependencyResolver.MavenResolver::class] as? MavenAetherResolver
      val classpathProvider = resolver?.classpathProvider(
        object : ClasspathSpec {
          override val usage: MultiPathUsage get() = MultiPathUsage.TestRuntime
        },
      )?.classpath()

      val classgraph = GuestClassgraph.buildFrom(
        Classpath.from(
          listOf(
            javacTestClassesOutput,
            javacMainClassesOutput,
          ).plus(
            classpathProvider?.asList()?.map { it.path } ?: emptyList(),
          ),
        ),
        root = config.projectRoot,
      ) {
        // we need to scan for full class and method info, so we can catch method and class tests.
        classgraph.enableClassInfo()
        classgraph.enableAnnotationInfo()
        classgraph.enableMethodInfo()
      }

      registerTests(state.registry, classgraph)
    }
  }
}

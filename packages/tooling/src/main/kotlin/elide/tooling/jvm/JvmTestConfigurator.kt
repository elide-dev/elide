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

import com.oracle.truffle.espresso.impl.Klass
import com.oracle.truffle.espresso.impl.Method
import io.github.classgraph.ClassInfo
import io.github.classgraph.MethodInfo
import org.graalvm.polyglot.proxy.ProxyInstantiable
import java.util.stream.Stream
import kotlin.io.path.absolute
import kotlin.io.path.exists
import elide.runtime.Logging
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotValue
import elide.runtime.gvm.jvm.GuestClassgraph
import elide.runtime.intrinsics.testing.TestingRegistrar
import elide.runtime.plugins.jvm.Jvm
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

  // Obtain an instance of the test class, so a test method can be invoked.
  @Suppress("TooGenericExceptionCaught")
  private fun instantiateTestClass(ctx: PolyglotContext, cls: ClassInfo): Pair<Klass, Any> {
    // @TODO junit semantics
    val guestCls = try {
      requireNotNull(requireNotNull(ctx.bindings(Jvm)) { "Failed to resolve JVM bindings" }.getMember(cls.name)) {
        "Test case class not found: '${cls.name}'"
      }
    } catch (err: Throwable) {
      logging.error("Failed to load test class '${cls.name}'", err)
      throw err
    }
    return try {
      guestCls.`as`(Klass::class.java) to guestCls.newInstance()
    } catch (err: Throwable) {
      logging.error("Failed to instantiate test class '${cls.name}'", err)
      throw err
    }
  }

  // Resolve a test method on a test class, so it can be invoked.
  private fun pluck(guestCls: Klass, method: MethodInfo): Method {
    // @TODO methods with same-name but different sig? forbid registration of identical names?
    return guestCls.declaredMethods.first { mth -> mth.name.toString() == method.name }
  }

  // Resolve a test method on a test class, so it can be invoked.
  private fun wrapTestMethod(context: PolyglotContext, instance: Any, method: Method): PolyglotValue {
    val ctx = context.unwrap()
    val wrappedInstance = context.unwrap().asValue(instance)
    val callable = wrappedInstance.getMember(method.name.toString())
    require(callable.canExecute()) { "Failed to pluck executable instance method: $callable" }

    return ctx.asValue(Runnable {
      callable.executeVoid()
    })
  }

  // Match/register a candidate test method, if it matches criteria.
  private fun matchCandidateMethod(registry: TestingRegistrar, cls: ClassInfo, method: MethodInfo) {
    // if the method is annotated with any eligible known annotation, we register it and defer evaluation.
    if (eligibleMethodAnnotations.any { method.hasAnnotation(it) }) {
      registry.register(TestingRegistrar.deferred(
        label = method.name,
        qualified = "${cls.name}.${method.name}",
      ) { context ->
        instantiateTestClass(context, cls).let { (guestCls, instance) ->
          wrapTestMethod(
            context,
            instance,
            pluck(
              guestCls,
              method,
            ),
          )
        }
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
      .absolute()

    val javacTestClassesOutput = state.layout.artifacts
      .resolve("jvm") // `.dev/artifacts/jvm/...`
      .resolve("classes") // `.../classes/...`
      .resolve("test") // `.../classes/test/...`
      .absolute()

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
      ), root = config.projectRoot) {
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

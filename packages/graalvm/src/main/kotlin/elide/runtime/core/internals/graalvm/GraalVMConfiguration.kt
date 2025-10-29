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
package elide.runtime.core.internals.graalvm

import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import io.micronaut.context.BeanContext
import java.net.URL
import java.util.LinkedList
import java.util.SortedSet
import java.util.TreeSet
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlinx.atomicfu.atomic
import kotlinx.collections.immutable.toImmutableList
import elide.runtime.core.*
import elide.runtime.core.EnginePlugin.InstallationScope
import elide.runtime.core.internals.MutableEngineLifecycle

/**
 * Internal implementation of the [PolyglotEngineConfiguration] abstract class, specialized for the GraalVM engine
 * implementation.
 */
@DelicateElideApi public class GraalVMConfiguration(
  /** A [MutableEngineLifecycle] instance that can be used to emit events to registered plugins. */
  private val lifecycle: MutableEngineLifecycle,
  /** Active injection context. */
  private val beanContextFactory: () -> BeanContext,
) : PolyglotEngineConfiguration() {
  private companion object {
    private const val EXPERIMENTAL_INIT_EXECUTOR = false
    private const val EXPERIMENTAL_MULTITHREAD_INIT = false
    private const val INIT_CORE_POOL_SIZE = 2
  }

  // Initialization executor.
  private lateinit var initExecutor: ListeningExecutorService

  init {
    if (EXPERIMENTAL_INIT_EXECUTOR) {
      initExecutor = MoreExecutors.listeningDecorator(
        when (EXPERIMENTAL_MULTITHREAD_INIT) {
          true -> Executors.newScheduledThreadPool(INIT_CORE_POOL_SIZE)
          false -> Executors.newSingleThreadExecutor()
        }
      )
    }
  }

  // Registered VFS bundles from plugins.
  private val registeredBundles = LinkedList<URL>()

  // Whether plugins have finished their async initialization phase; after this is set to `true`, the engine's readiness
  // latch is assigned and will eventually signal readiness.
  private val initialized = atomic(false)

  // A latch used to signal readiness of the VM. This value is only initialized after all plugins have enqueued their
  // asynchronous setup tasks.
  private val readinessLatch: CountDownLatch = CountDownLatch(1)

  /**
   * Represents an [InstallationScope] used by plugins, binding to this configuration's lifecycle and other required
   * properties.
   */
  private inner class GraalVMInstallationScope(
    val config: GraalVMConfiguration,
    val exec: () -> ListeningExecutorService,
    val beanContextFactory: () -> BeanContext,
  ) : InstallationScope {
    override val configuration: PolyglotEngineConfiguration get() = config
    override val lifecycle: EngineLifecycle get() = config.lifecycle

    override fun registerBundle(resource: URL) {
      registeredBundles.add(resource)
    }

    override val beanContext: BeanContext by lazy {
      beanContextFactory.invoke()
    }

    override fun registeredBundles(): List<URL> =
      registeredBundles.toImmutableList()

    @Suppress("UNCHECKED_CAST", "KotlinConstantConditions")
    override fun <T> deferred(block: () -> T): Future<T> {
      if (!EXPERIMENTAL_INIT_EXECUTOR) {
        return Futures.immediateFuture(requireNotNull(block.invoke()))
      }
      val fut = exec.invoke().submit<T> { block.invoke() }
      inFlight.computeIfAbsent("", { LinkedList() }).add(fut)
      return fut
    }
  }

  /** Suite of seen plugin IDs. */
  private val seenPlugins: SortedSet<String> = TreeSet()

  /** Suite of in-flight initialization tasks. */
  private val inFlight: ConcurrentSkipListMap<String, LinkedList<ListenableFuture<*>>> = ConcurrentSkipListMap()

  /** Internal map holding plugin instances that can be retrieved during engine configuration. */
  private val plugins: MutableMap<String, Any?> = mutableMapOf()

  /** Internal mutable set of enabled languages. */
  private val langs: MutableSet<GuestLanguage> = mutableSetOf()

  /** All main entrypoint arguments. */
  private val entrypointArgs = atomic<Array<String>?>(null)

  /** A set of languages enabled for use in the engine. */
  internal val languages: Set<GuestLanguage> get() = langs

  /** Runtime info, resolved from GraalVM static properties. */
  override val hostRuntime: HostRuntime = GraalVMRuntime()

  /** Installation scope for plugins. */
  private val cachedScope by lazy { GraalVMInstallationScope(this, { initExecutor }, beanContextFactory) }

  /** Arguments to provide to guest code. */
  public val arguments: Array<out String> get() = entrypointArgs.value ?: emptyArray()

  @Deprecated("Use installLazy instead", replaceWith = ReplaceWith("installLazy(plugin, configure)"))
  override fun <C : Any, I : Any> install(plugin: EnginePlugin<C, I>, configure: C.() -> Unit): I {
    assert(plugin.key.id !in plugins) { "A plugin with the provided key is already registered" }
    val instance = plugin.install(GraalVMInstallationScope(this, { initExecutor }, beanContextFactory), configure)
    plugins[plugin.key.id] = instance
    return instance
  }

  @Suppress("DEPRECATION")
  override fun <C : Any, I : Any> configure(plugin: EnginePlugin<C, I>, configure: C.() -> Unit) {
    assert(!initialized.value) { "Cannot configure engine plugins after initialization is complete" }
    if (plugin.key.id in seenPlugins) {
      // Plugin already installed; no-op
      return
    }
    seenPlugins.add(plugin.key.id)

    if (EXPERIMENTAL_INIT_EXECUTOR) {
      val (assign, futSet) = if (plugin.key.id in inFlight) {
        false to inFlight[plugin.key.id]!!
      } else {
        true to LinkedList()
      }
      futSet.add(initExecutor.submit {
        val instance = plugin.install(cachedScope, configure)
        plugins[plugin.key.id] = instance
      })
      if (assign) {
        inFlight[plugin.key.id] = futSet
      }
    } else {
      install(plugin, configure)
    }
  }

  override fun <T> plugin(key: EnginePlugin.Key<T>): T? {
    @Suppress("unchecked_cast")
    return plugins[key.id] as? T
  }

  override fun enableLanguage(language: GuestLanguage) {
    langs.add(language)
  }

  override fun args(args: Array<String>) {
    entrypointArgs.value = args
  }

  override fun registeredBundles(): List<URL> = registeredBundles.toImmutableList()

  override fun blockUntilReady() {
    if (EXPERIMENTAL_INIT_EXECUTOR) {
      if (initialized.value) {
        return  // already initialized; no-op
      }
      initialized.compareAndSet(expect = false, update = true)
      Futures.whenAllComplete<Any?>(inFlight.values.flatten()).call(
        Callable {
          readinessLatch.countDown()
        },
        initExecutor,
      )
      readinessLatch.await(1, TimeUnit.SECONDS)
    } else {
      initialized.value = true
    }
  }
}

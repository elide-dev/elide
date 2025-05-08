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
package elide.runtime.gvm.jvm

import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import io.github.classgraph.ClassGraph
import io.github.classgraph.ScanResult
import java.nio.file.Path
import java.util.TreeSet
import java.util.concurrent.Executors
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.io.path.absolutePathString
import elide.runtime.gvm.jvm.GuestClassgraph.ClassgraphBuilderApi
import elide.tool.Classpath

/**
 * Function which can customize how a class-graph is built.
 */
public typealias ClassgraphConfigurator = (ClassgraphBuilderApi.() -> Unit)

/**
 * # Guest Classgraph
 *
 * Static utilities for assembling class graphs based on guest classpath scanning.
 */
public object GuestClassgraph {
  /**
   * ## Classgraph
   */
  public sealed interface Classgraph {
    /**
     * Indicate whether this classgraph has contents.
     *
     * @return Emptiness status.
     */
    public fun isEmpty(): Boolean

    /**
     * Export a classgraph from this result.
     *
     * @return Classgraph instance.
     */
    public fun export(): ClassGraph

    /**
     * Provide the classpath used to build this classgraph.
     *
     * @return Classpath instance.
     */
    public fun classpath(): Classpath

    /**
     * Provide the scan result from this classgraph.
     *
     * @return Scan result.
     */
    public fun scanResult(): ScanResult
  }

  /**
   * ## Empty Classgraph
   *
   * Describes a class-graph that is empty because the class-path it was calculated from is empty, with consideration
   * for predicates and other filtering.
   */
  public data object Empty: Classgraph {
    override fun isEmpty(): Boolean = true
    override fun classpath(): Classpath = Classpath.empty()
    override fun export(): ClassGraph = ClassGraph()
    override fun scanResult(): ScanResult = error("No scan result available")
  }

  /**
   * ## Materialized Classgraph
   *
   * Holds data from a class-graph scan, including:
   *
   * - The [classpath] used to build this class-graph
   * - The [graph] instance used to build this class-graph
   * - The [result] of the scan
   *
   * Materialized classpaths can be interrogated for further information.
   */
  public class MaterializedClassgraph internal constructor (
    private val classpath: Classpath,
    private val graph: ClassGraph,
    private val result: ScanResult,
  ): Classgraph {
    internal constructor(classpath: Classpath, pair: Pair<ClassGraph, ScanResult>): this(
      classpath,
      graph = pair.first,
      result = pair.second,
    )

    override fun isEmpty(): Boolean = classpath.isEmpty()
    override fun classpath(): Classpath = classpath
    override fun export(): ClassGraph = graph
    override fun scanResult(): ScanResult = result
  }

  /**
   * ## Classgraph Builder API
   *
   * Allows customization of the class-graph builder before scanning.
   */
  public interface ClassgraphBuilderApi {
    public val packages: MutableSet<String>
    public val classgraph: ClassGraph
  }

  // Builder which manages the preparation of a `classgraph` instance.
  private class ClassgraphBuilder (
    private val rootPath: Path,
    classpath: Classpath,
    packages: Iterable<String> = emptyList(),
  ) :
    ClassgraphBuilderApi {
    private val acceptPackages: MutableSet<String> = TreeSet<String>().also {
      it.addAll(packages)
    }

    // Virtual thread executor.
    private val executor by lazy {
      MoreExecutors.listeningDecorator(
        Executors.newVirtualThreadPerTaskExecutor()
      )
    }

    override val packages: MutableSet<String> get() = acceptPackages

    // Classgraph instance managed by this builder.
    @Suppress("SpreadOperator")
    override val classgraph: ClassGraph = ClassGraph().apply {
      verbose(System.getProperty("elide.classgraph.debug") == "true")
      enableMultiReleaseVersions()
      enableURLScheme("file")
      enableURLScheme("jar")
      enableURLScheme("jimfs")
      enableMemoryMapping()
      enableClassInfo()
      enableMethodInfo()
      enableAnnotationInfo()
      acceptPackages.takeIf { it.isNotEmpty() }?.let {
        acceptPackages(*it.toTypedArray())
      }
      overrideClasspath(classpath.asList().map {
        when (it.path.isAbsolute) {
          true -> it.path
          false -> rootPath.resolve(it.path).absolutePathString()
        }
      })
    }

    // Perform scanning; this concludes our use of the builder.
    suspend fun scan(): Pair<ClassGraph, ScanResult> = withContext(IO) {
      suspendCancellableCoroutine { continuation: CancellableContinuation<ScanResult> ->
        Futures.addCallback<ScanResult>(
          classgraph.scanAsync(executor, Runtime.getRuntime().availableProcessors()) as ListenableFuture<ScanResult>,
          object: FutureCallback<ScanResult> {
            override fun onSuccess(result: ScanResult) {
              continuation.resumeWith(Result.success(result))
            }

            override fun onFailure(t: Throwable) {
              continuation.resumeWith(Result.failure(t))
            }
          },
          executor,
        )
      }.let { scanResult ->
        classgraph to scanResult
      }
    }
  }

  /**
   * Build a classgraph instance from the provided [classpath].
   *
   * @param classpath Guest classpath to build from.
   * @param withConfigurator Optional configurator to apply to the classgraph builder.
   * @return Assembled/materialized classgraph.
   */
  public suspend fun buildFrom(
    classpath: Classpath,
    root: Path? = null,
    withConfigurator: ClassgraphConfigurator? = null,
  ): Classgraph = when (classpath.isEmpty()) {
    true -> Empty
    false -> MaterializedClassgraph(classpath, ClassgraphBuilder(
      root ?: Path.of(System.getProperty("user.dir")
      ), classpath).apply {
      withConfigurator?.invoke(this)
    }.scan())
  }
}

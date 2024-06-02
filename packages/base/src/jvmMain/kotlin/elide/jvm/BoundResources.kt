/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
package elide.jvm

import java.io.Closeable
import kotlinx.collections.immutable.toImmutableSet

/**
 * ## Bound Resource
 *
 * Describes a single "bound resource," which is held within the context of a [LifecycleBoundResources] instance; such
 * resources have a weak reference back to their owner, and are automatically closed when the owner is closed.
 *
 * Bound resources are considered disposable types.
 */
public sealed interface BoundResource: Closeable, AutoCloseable {
  /**
   * Describes a [Closeable] resource as a [BoundResource].
   */
  public class CloseableResource(private val resource: Closeable): BoundResource {
    override fun close(): Unit = resource.close()
  }

  /**
   * Describes an [AutoCloseable] resource as a [BoundResource].
   */
  public class AutoCloseableResource(private val resource: AutoCloseable): BoundResource {
    override fun close(): Unit = resource.close()
  }

  /**
   * Companion utilities for creating [BoundResource] instances.
   */
  public companion object {
    /**
     * Create a [BoundResource] from a [Closeable].
     *
     * @param resource The closeable resource to bind.
     * @return A new [BoundResource] instance.
     */
    public fun of(resource: Closeable): BoundResource = CloseableResource(resource)

    /**
     * Create a [BoundResource] from an [AutoCloseable].
     *
     * @param resource The auto-closeable resource to bind.
     * @return A new [BoundResource] instance.
     */
    public fun of(resource: AutoCloseable): BoundResource = AutoCloseableResource(resource)
  }
}

/**
 * ## Lifecycle-bound Resources
 *
 * Describes a pattern wherein resources ([Closeable] or [AutoCloseable] instances) are bound to a lifecycle, such that
 * they are automatically closed when the lifecycle is closed.
 *
 * Resources are registered via the [register] method, optionally de-registered via the [unregister] method, and always
 * closed when persisted to terminal close.
 *
 * Note that lifecycle-bound resource groups are not necessarily [Closeable] or [AutoCloseable] themselves.
 */
public interface LifecycleBoundResources {
  /**
   * Returns all resources currently bound to this lifecycle.
   */
  public val allResources: Sequence<BoundResource>

  /**
   * Register a [Closeable] resource to this lifecycle.
   *
   * @param resource The resource to register.
   * @return The bound resource instance.
   */
  public fun register(resource: Closeable)

  /**
   * Register an [AutoCloseable] resource to this lifecycle.
   *
   * @param resource The resource to register.
   */
  public fun unregister(resource: Closeable)

  /**
   * Unregister a [Closeable] resource from this lifecycle.
   *
   * @param resource The resource to unregister.
   */
  public fun register(resource: AutoCloseable)

  /**
   * Unregister an [AutoCloseable] resource from this lifecycle.
   *
   * @param resource The resource to unregister.
   */
  public fun unregister(resource: AutoCloseable)
}

/**
 * ## Resource Manager
 *
 * Simple implementation of [LifecycleBoundResources], which propagates [close] calls to resources registered to it.
 * Implementations are intended to use delegation:
 *
 * ```kotlin
 * class MyResourceHolder(
 *   private val manager: LifecycleBoundResources = ResourceManager()
 * ): LifecycleBoundResources by manager, AutoCloseable by manager {
 *   // ...
 *   private fun somethingThatSpawnsAResource() {
 *     val resource = // ...
 *     register(resource)
 *   }
 * }
 *
 * // later ...
 * val instance = MyResourceHolder()
 * instance.use {
 *   // the inner resource will be closed automatically
 * }
 * ```
 */
public open class ResourceManager : LifecycleBoundResources, AutoCloseable, Closeable {
  private val resources: MutableSet<BoundResource> = mutableSetOf()

  override val allResources: Sequence<BoundResource>
    get() = resources.toImmutableSet().asSequence()

  override fun register(resource: Closeable) { resources.add(BoundResource.of(resource)) }
  override fun register(resource: AutoCloseable) { resources.add(BoundResource.of(resource)) }
  override fun unregister(resource: Closeable) { resources.remove(BoundResource.of(resource)) }
  override fun unregister(resource: AutoCloseable) { resources.remove(BoundResource.of(resource)) }

  override fun close() {
    resources.forEach { it.close() }
    resources.clear()
  }
}

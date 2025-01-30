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

@file:Suppress("MemberVisibilityCanBePrivate")

package elide.internal.conventions

import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.ModuleVersionSelector
import org.gradle.api.provider.Provider
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicBoolean

/** Specification API for a dependency of some kind. */
public interface DependencySpec: Comparable<DependencySpec> {
  /** Module group. */
  public val group: String

  /** Module name. */
  public val module: String

  /** Rendered coordinate string. */
  public val coordinate: String

  override fun compareTo(other: DependencySpec): Int {
    return coordinate.compareTo(other.coordinate)
  }
}

/** Enhances a dependency with a version. */
public interface VersionedDependencySpec : DependencySpec {
  /** Module version. */
  public val version: String
}

/** Information about a dependency module. */
@JvmRecord public data class Dependency(override val group: String, override val module: String) : DependencySpec {
  /** Return the combined coordinate. */
  override val coordinate: String get() = "$group:$module"

  override fun equals(other: Any?): Boolean {
    return other is Dependency && other.coordinate == coordinate
  }

  override fun hashCode(): Int {
    return coordinate.hashCode()
  }
}

/** Information about an excluded dependency. */
@JvmRecord public data class DependencyExclusion(
  val dependency: Dependency,
  val reason: String? = null,
) : Comparable<DependencyExclusion> {
  override fun equals(other: Any?): Boolean {
    return other is DependencyExclusion && other.dependency == dependency
  }

  override fun hashCode(): Int {
    return dependency.hashCode()
  }

  override fun compareTo(other: DependencyExclusion): Int {
    return dependency.compareTo(other.dependency)
  }
}

/** Information about a pinned dependency. */
@JvmRecord public data class DependencyPin(
  val dependency: Dependency,
  val version: String,
  val reason: String? = null,
): Comparable<DependencyPin> {
  public companion object {
    /** Create a new [DependencyPin] from a [dependency] and [version]. */
    @JvmStatic public fun of(group: String, module: String, version: String, reason: String? = null): DependencyPin {
      return DependencyPin(Dependency(group, module), version, reason)
    }
  }

  override fun equals(other: Any?): Boolean {
    return other is DependencyExclusion && other.dependency == dependency
  }

  override fun hashCode(): Int {
    return dependency.hashCode()
  }

  override fun compareTo(other: DependencyPin): Int {
    return dependency.compareTo(other.dependency)
  }
}

/**
 * Simple container which can lock and keep track of something.
 *
 * During the "write phase," the container is filled with items which are presumably applicable during later computation
 * phases. Later, a read comes along, triggering the "read phase;" now, the container is locked and cannot be written to
 * anymore.
 */
public abstract class LockableContainer<T>(initial: T) {
  private val locked: AtomicBoolean = AtomicBoolean(false)
  private val tracked: T = initial

  /**
   * Obtain an unlocked form of this container, which is mutable.
   *
   * This method will throw if the container has already been locked.
   *
   * @param op Operation to perform with the obtained value.
   */
  protected fun withUnlocked(op: T.() -> Unit) {
    require(locked.get()) {
      "Dependency suite is locked and cannot be modified."
    }
    op(tracked)
  }

  /**
   * Obtain a locked form of this container, which is hardened against further writes.
   *
   * @param op Operation to perform after making sure the structure is locked; runs in the receivership of the tracked
   *   data type [T].
   * @return Result of the operation.
   */
  protected fun <R> withLocked(op: T.() -> R): R {
    if (!locked.get()) {
      locked.compareAndSet(false, true)
    } else require(locked.get()) {
      "Dependency suite is locked and cannot be modified."
    }
    return op(tracked)
  }
}

/**
 * Represents a JPMS module ID.
 *
 * @param id String ID of the module.
 */
@JvmInline public value class ModuleId private constructor (private val id: String): Comparable<ModuleId> {
  public companion object {
    /** @return New module ID from provided [id] string. */
    @JvmStatic public fun of(id: String): ModuleId = ModuleId(id)
  }

  override fun compareTo(other: ModuleId): Int {
    return id.compareTo(other.id)
  }

  /** Module ID string. */
  public val idString: String get() = id
}

/**
 * Settings for a given JPMS module.
 *
 * @param modularize Whether to modularize this module automatically if needed. Defaults to `true`.
 * @param moduleName Explicit module name to use, if any; if one is needed and none is provided, a name is generated.
 * @param forceClasspath If set, causes the tools to forcibly de-modularize the dependency; this includes removing any
 *   present compiled `/module-info.class`, and any `Automatic-Module-Name` present in the JAR manifest. Without these
 *   attributes, Gradle places it on the classpath instead of the module path. Takes precedence over [modularize].
 */
@ConsistentCopyVisibility
public data class ModuleConfiguration private constructor (
  var modularize: Boolean = true,
  var moduleName: String? = null,
  var forceClasspath: Boolean = false,
) {
  public companion object {
    /** @return Default suite of module configuration settings. */
    @JvmStatic public fun defaults(): ModuleConfiguration = ModuleConfiguration()

    /** Create a new [ModuleConfiguration] with the given [moduleName]. */
    @JvmStatic public fun of(moduleName: String, modularize: Boolean = true): ModuleConfiguration {
      return ModuleConfiguration(
        modularize = modularize,
        moduleName = moduleName
      )
    }

    /** Create and configure a new [ModuleConfiguration] with the given [moduleName] and configuration [op]. */
    @JvmStatic public fun of(moduleName: String, op: ModuleConfiguration.() -> Unit): ModuleConfiguration {
      return of {
        this.moduleName = moduleName
        op()
      }
    }

    /** Create and configure a new [ModuleConfiguration] with the given [moduleName] and configuration [op]. */
    @JvmStatic public fun of(op: ModuleConfiguration.() -> Unit): ModuleConfiguration {
      return ModuleConfiguration().apply(op)
    }
  }
}

/** Context for managing the JPMS transform suite. */
public class ModularContext : LockableContainer<SortedMap<Dependency, ModuleConfiguration>>(ConcurrentSkipListMap()) {
  public fun module(group: String, module: String, op: ModuleConfiguration.() -> Unit = { }): Unit = withUnlocked {
    module(Dependency(group, module), op)
  }

  public fun module(
    dependency: Provider<MinimalExternalModuleDependency>,
    op: ModuleConfiguration.() -> Unit = { },
  ): Unit = withUnlocked {
    module(dependency.get(), op)
  }

  public fun module(
    dependency: MinimalExternalModuleDependency,
    op: ModuleConfiguration.() -> Unit = { },
  ): Unit = withUnlocked {
    module(Dependency(dependency.module.group, dependency.module.name), op)
  }

  public fun module(dependency: Dependency, op: ModuleConfiguration.() -> Unit = { }): Unit = withUnlocked {
    module(dependency, ModuleConfiguration.defaults().apply(op))
  }

  public fun module(dependency: Dependency, configuration: ModuleConfiguration): Unit = withUnlocked {
    this[dependency] = configuration
  }

  internal fun resolve(dependency: Dependency): ModuleConfiguration? = withLocked {
    this[dependency]
  }

  internal fun resolve(dependency: MinimalExternalModuleDependency): ModuleConfiguration? = withLocked {
    this[Dependency(dependency.module.group, dependency.module.name)]
  }

  internal fun resolve(dependency: Provider<MinimalExternalModuleDependency>): ModuleConfiguration? = withLocked {
    this[Dependency(dependency.get().module.group, dependency.get().module.name)]
  }

  internal fun resolve(dependency: ModuleVersionSelector): ModuleConfiguration? = withLocked {
    this[Dependency(dependency.group, dependency.name)]
  }

  internal fun resolve(jar: File): ModuleConfiguration? = withLocked {
    TODO("not yet implemented")
  }

  internal fun resolve(module: ModuleId): ModuleConfiguration? = withLocked {
    values.find {
      it.moduleName == module.idString
    }
  }
}

/** Configures additional dependency exclusions for a given project. */
public class DependencyExclusions : LockableContainer<SortedSet<Dependency>>(ConcurrentSkipListSet()) {
  /** Add an excluded dependency via [group] and [module] strings. */
  public fun exclude(group: String, module: String): Unit = withUnlocked {
    exclude(Dependency(group, module))
  }

  /** Add an excluded [dependency] from a version catalog. */
  public fun exclude(dependency: Provider<MinimalExternalModuleDependency>): Unit = withUnlocked {
    exclude(dependency.get())
  }

  /** Add an excluded [dependency] from a version catalog, but pre-resolved. */
  public fun exclude(dependency: MinimalExternalModuleDependency): Unit = withUnlocked {
    exclude(Dependency(dependency.module.group, dependency.module.name))
  }

  /** Add an excluded dependency descriptor. */
  public fun exclude(dependency: Dependency): Unit = withUnlocked {
    exclude(dependency)
  }

  /** Return the set of all excluded dependencies. */
  internal fun drain(): SortedSet<Dependency> = withLocked { this }
}

/** Configures additional dependency pins for a given project. */
public class DependencyPinning : LockableContainer<SortedMap<Dependency, DependencyPin>>(ConcurrentSkipListMap()) {
  /** Add an excluded dependency via [group] and [module] strings. */
  public fun pin(group: String, module: String, version: String, reason: String? = null): Unit = withUnlocked {
    pin(Dependency(group, module), version, reason)
  }

  /** Add an excluded [dependency] from a version catalog. */
  public fun pin(dependency: Provider<MinimalExternalModuleDependency>, reason: String? = null): Unit = withUnlocked {
    pin(dependency.get(), reason)
  }

  /** Add an excluded [dependency] from a version catalog, but pre-resolved. */
  public fun pin(dependency: MinimalExternalModuleDependency, reason: String? = null): Unit = withUnlocked {
    pin(Dependency(dependency.module.group, dependency.module.name), (
      dependency.version?.ifBlank { null } ?: error("Cannot pin versionless dependency")
    ), reason)
  }

  /** Add an excluded dependency descriptor. */
  public fun pin(dependency: Dependency, version: String, reason: String? = null): Unit = withUnlocked {
    val current = this[dependency]
    if (current != null) {
      // if pins are equal, safe to ignore
      if (current.version == version) return@withUnlocked
      error("Cannot pin $dependency to $version, already pinned to $current")
    }
    this[dependency] = DependencyPin(dependency, version, reason)
  }

  /** Resolve the pinned version, if any, for the provided module [descriptor]; otherwise, return `null`. */
  internal fun resolve(descriptor: ModuleVersionSelector): DependencyPin? = withLocked {
    this[Dependency(descriptor.group, descriptor.name)]
  }
}

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
package elide.tooling

import org.graalvm.nativeimage.ImageInfo
import java.nio.file.Path
import java.util.LinkedList
import java.util.function.Predicate
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.serialization.Serializable
import kotlin.collections.plus
import kotlin.io.path.absolutePathString
import kotlin.reflect.KClass

/**
 * # Multi-Path
 *
 * Describes a special kind of [Argument] which specifies multiple paths, separated with some delimiter; this is common
 * for compiler interfaces which need to accept a suite of dependencies (for example, class paths and module paths).
 *
 * Various implementations exist which are optimized for different use cases, such as Java and Kotlin class and module
 * path assembly.
 *
 * Multi-path containers are guaranteed to preserve certain relevant semantics:
 *
 * - Containers are path-aware
 * - Containers preserve insertion order
 * - Containers emit with preserved order by default
 * - Containers can be expressed safely as command-line arguments
 * - Containers are serializable
 */
@Serializable
public sealed interface MultiPath : Argument {
  /**
   * Produce a mutable form of this container; if this object is already mutable, it may return  itself.
   *
   * @return Mutable multi-path container.
   */
  public fun toMutable(): MutableMultiPath

  /**
   * Produce a sequence of [Entry] (or [Entry] subtypes) from this multi-path container. This method is guaranteed to
   * preserve insertion order.
   *
   * @return An ordered sequence of entries from this multi-path container.
   */
  public fun asSequence(): Sequence<Entry>

  /**
   * Produce a sequence of [Path] entries for this multi-path container. This method is guaranteed to preserve insertion
   * order.
   *
   * @return An ordered sequence of paths from this multi-path container.
   */
  public fun asPathSequence(): Sequence<Path>

  /**
   * Produce a list of [Entry] (or [Entry] subtypes) from this multi-path container. Like [asSequence], this method is
   * guaranteed to preserve insertion order.
   *
   * @return A list of entries from this multi-path container.
   */
  public fun asList(): List<Entry> = asSequence().toList()

  /**
   * Retrieve the positional path entry corresponding to the provided [index].
   *
   * @return The path entry at the specified index.
   * @throws IndexOutOfBoundsException if the index is out of bounds.
   */
  public operator fun get(index: UInt): Entry

  /**
   * Retrieve the positional path entry corresponding to the provided [index].
   *
   * @return The path entry at the specified index.
   * @throws IndexOutOfBoundsException if the index is out of bounds.
   */
  override operator fun get(index: Int): Argument

  /**
   * Check whether this multi-path container contains the provided [str] path.
   *
   * Note that string-based comparison is not aware of path semantics. This method matches paths on a literal basis,
   * and developers should typically prefer to use the contains operator with [Path] instances instead.
   *
   * @param str String path to check for.
   * @return True if this multi-path container contains the provided path string, false otherwise.
   */
  public operator fun contains(str: String): Boolean

  /**
   * Check whether this multi-path container contains the provided [path].
   *
   * @param path Path to check for.
   * @return True if this multi-path container contains the provided path, false otherwise.
   */
  public operator fun contains(path: Path): Boolean

  /**
   * Performs a copy-on-write and yields a new container with the provided [path].
   *
   * @param path Path to add to the container.
   * @return True if the path was added, false if it was already present.
   */
  public operator fun plus(path: Path): MultiPath

  /**
   * Performs a copy-on-write and yields a new container with the provided [other] paths.
   *
   * @param other Paths to add to the container.
   * @return True if the path was added, false if it was already present.
   */
  public operator fun plus(other: Collection<Path>): MultiPath

  /**
   * Performs a copy-on-write and yields a new container with the provided [other] paths.
   *
   * @param other Paths to add to the container.
   * @return True if the path was added, false if it was already present.
   */
  public operator fun plus(other: Sequence<Path>): MultiPath

  /**
   * Performs a copy-on-write and yields a new container with the provided [other] paths.
   *
   * @param other Paths to add to the container.
   * @return True if the path was added, false if it was already present.
   */
  public operator fun plus(other: MultiPath): MultiPath

  /**
   * ## Multi-path Entry
   *
   * Multi-path container entries always specify a [path] at the very least, and may specify additional metadata as
   * relates to their use case.
   */
  public sealed interface Entry {
    /**
     * Path held by this multi-path container entry.
     */
    public val path: Path
  }

  /**
   * ## Multi-path (Mutable)
   *
   * Base interface for mutable multi-path containers. This interface extends the base [MultiPath] interface, and adds
   * methods for mutating the container.
   */
  public sealed interface MutableMultiPath : MultiPath, MutableCollection<Entry> {
    /**
     * Prepend the provided [path] to the current container.
     *
     * @param path Path to prepend to the container.
     */
    public fun prepend(path: Path): Boolean

    /**
     * Prepend the provided [entry] to the current container.
     *
     * @param entry Entry to prepend to the container.
     */
    public fun prepend(entry: Entry): Boolean

    /**
     * Prepend the provided [suite] to the current container.
     *
     * @param suite Path suite to prepend to the container.
     */
    public fun prepend(suite: MultiPath): Boolean

    /**
     * Append the provided [path] to the current container.
     *
     * @param path Path to append to the container.
     */
    public fun add(path: Path): Boolean

    /**
     * Append the provided [suite] to the current container.
     *
     * @param suite Path suite to append to the container.
     */
    public fun add(suite: MultiPath): Boolean
  }

  /**
   * ## Multi-Path Container
   *
   * Defines a persistent (copy-on-write) container which holds a suite of path entries. Path entries are ultimately
   * designed to be emitted as command-line arguments, or as a single command-line argument joined by a delimiter.
   */
  @Serializable
  public sealed interface MultiPathContainer<T : Entry, E : Enum<E>> : MultiPath, Collection<Entry> {
    public val role: E
    public val paths: List<T>
    public val entries: Set<Path>
    override fun asSequence(): Sequence<T> = paths.asSequence()
    override operator fun get(index: UInt): T = paths[index.toInt()]
    override operator fun get(index: Int): Argument = Argument.of(paths[index].path.absolutePathString())
    override operator fun contains(str: String): Boolean = paths.firstOrNull { it.path.toString() == str } != null
    override operator fun contains(path: Path): Boolean = path in entries
    override fun contains(element: Entry): Boolean = element in paths
    override fun containsAll(elements: Collection<Entry>): Boolean = paths.containsAll(elements)
    override fun isEmpty(): Boolean = paths.isEmpty()
    override val size: Int get() = paths.size
    override fun iterator(): Iterator<Entry> = paths.iterator()
    override operator fun plus(path: Path): MultiPathContainer<T, E>
    override operator fun plus(other: Collection<Path>): MultiPathContainer<T, E>
    override operator fun plus(other: Sequence<Path>): MultiPathContainer<T, E>
    override operator fun plus(other: MultiPath): MultiPathContainer<T, E>
  }

  /**
   * ## Multi-path Container (Mutable)
   *
   * Extends the base implementation of a multi-path container with mutability.
   */
  public sealed interface MutableMultiPathContainer<T : Entry, E : Enum<E>> :
    MutableMultiPath, MultiPathContainer<T, E> {
    override fun toMutable(): MutableMultiPathContainer<T, E> = this
    override fun iterator(): MutableIterator<Entry>

    /**
     * Add the specified [path] to this container.
     *
     * @param path Path to add to the container.
     * @return True if the path was added, false if it was already present.
     */
    override fun add(path: Path): Boolean

    /**
     * Set the specified [index] to the provided [path].
     *
     * @param index Index to set.
     * @param path Path to set at the specified index.
     */
    public operator fun set(index: Int, path: Path): Boolean

    /**
     * Add the specified [path] to this container, and return self.
     *
     * @param path Path to add to the container.
     * @return This container.
     */
    override operator fun plus(path: Path): MutableMultiPathContainer<T, E>

    /**
     * Add the specified [other] paths to this container, and return self.
     *
     * @param other Paths to add to the container.
     * @return This container.
     */
    override operator fun plus(other: Collection<Path>): MutableMultiPathContainer<T, E>

    /**
     * Add the specified [other] paths to this container, and return self.
     *
     * @param other Paths to add to the container.
     * @return This container.
     */
    override operator fun plus(other: Sequence<Path>): MutableMultiPathContainer<T, E>

    /**
     * Add the specified [other] paths to this container, and return self.
     *
     * @param other Paths to add to the container.
     * @return This container.
     */
    override operator fun plus(other: MultiPath): MutableMultiPathContainer<T, E>

    /**
     * Produce an immutable form of this container.
     *
     * @return Immutable multi-path container.
     */
    public fun build(): MultiPathContainer<T, E>
  }
}

/**
 * ## JVM Multi-Path Type
 *
 * Type of multi-path expressed by a JVM path container.
 */
public enum class JvmMultiPath (internal val shortFlag: String, internal val longFlag: String) {
  CLASSPATH("-cp", "-classpath"),
  MODULEPATH("-mp", "--module-path");

  internal fun inferEntryType(path: Path): JvmMultiPathEntryType {
    return when {
      path.toString().endsWith(".jmod") -> JvmMultiPathEntryType.JMOD
      path.toString().endsWith(".jar") -> JvmMultiPathEntryType.JAR
      else -> JvmMultiPathEntryType.DIRECTORY
    }
  }
}

/**
 * ## JVM Multi-Path Entry Type
 *
 * Type of entry specified by a path within a JVM path container.
 */
public enum class JvmMultiPathEntryType {
  /** Directory entry with class files. */
  DIRECTORY,

  /** Modular Java JMOD files. */
  JMOD,

  /** Traditional JAR files. */
  JAR
}

/**
 * ## JVM Multi-Path Entry
 *
 * Specifies an entry within a classpath or modulepath.
 */
@JvmRecord @Serializable public data class JvmMultiPathEntry (
  public val type: JvmMultiPathEntryType,
  override val path: Path,
) : MultiPath.Entry

/**
 * ## Abstract JVM Multi-Path Container
 *
 * Provides shared code and a common inheritance point for [JvmMultiPathContainer] and [MutableJvmMultiPathContainer].
 */
@Serializable
public sealed class AbstractJvmMultiPathContainer : MultiPath.MultiPathContainer<JvmMultiPathEntry, JvmMultiPath>

/**
 * ## JVM Multi-Path Container
 *
 * Concrete container type hierarchy for JVM platforms; defines both the concept of a module-path (which can accept
 * JMODs, in addition to JARs and directories), or traditional class-paths.
 */
@Serializable public class JvmMultiPathContainer(
  override val role: JvmMultiPath,
  override val paths: PersistentList<JvmMultiPathEntry> = persistentListOf(),
  override val entries: PersistentSet<Path> = paths.map { it.path }.toPersistentSet(),
) : AbstractJvmMultiPathContainer() {
  override fun asArgumentSequence(): Sequence<Argument> = sequenceOf(Argument.of(role.longFlag to asArgumentString()))
  override fun asPathSequence(): Sequence<Path> = paths.asSequence().map { it.path }
  override fun ArgumentContext.asArgumentString(): String = paths.joinToString(":") { it.path.absolutePathString() }
  override fun plus(other: Collection<Path>): AbstractJvmMultiPathContainer = plus(other.asSequence())

  override fun plus(other: MultiPath): JvmMultiPathContainer = when (other) {
    is JvmMultiPathContainer -> other.paths.filter { it in this }.let { added ->
      JvmMultiPathContainer(
        role = role,
        paths = other.paths.plus(added),
        entries = entries.plus(added.map { it.path }),
      )
    }

    else -> JvmMultiPathContainer(
      role = role,
      paths = paths.addAll(other.asSequence().map { it as JvmMultiPathEntry }.toList()),
      entries = entries.addAll(other.asSequence().map { it.path }.toList()),
    )
  }

  override fun plus(path: Path): JvmMultiPathContainer = JvmMultiPathContainer(
    role = role,
    paths = paths.add(JvmMultiPathEntry(
      type = role.inferEntryType(path),
      path = path,
    )),
    entries = entries.add(path),
  )

  override fun plus(other: Sequence<Path>): JvmMultiPathContainer {
    var mutated = false
    var result = paths
    var newEntries = entries

    for (path in other) {
      if (path in entries) {
        continue
      }
      mutated = true
      result = result.add(JvmMultiPathEntry(
        type = role.inferEntryType(path),
        path = path,
      ))
      newEntries = newEntries.add(path)
    }
    return if (!mutated) {
      this
    } else {
      JvmMultiPathContainer(
        role = role,
        paths = result,
        entries = newEntries,
      )
    }
  }

  override fun toMutable(): MultiPath.MutableMultiPath {
    return when (role) {
      JvmMultiPath.CLASSPATH -> MutableClasspath.from(
        paths.map { it.path }.toList()
      )
      JvmMultiPath.MODULEPATH -> MutableModulepath.from(
        paths.map { it.path }.toList()
      )
    }
  }

  // Internal factories used by `Classpath` and `Modulepath` wrapper types.
  internal companion object {
    // Create an empty multi-path container.
    @JvmStatic fun empty(): JvmMultiPathContainer = JvmMultiPathContainer(role = JvmMultiPath.CLASSPATH)

    // Create a multi-path container from the provided paths.
    @JvmStatic fun of(role: JvmMultiPath, first: Path, vararg addl: Path): JvmMultiPathContainer =
      from(role, listOf(first).plus(addl))

    // Create a multi-path container from the provided collection of paths.
    @JvmStatic fun from(role: JvmMultiPath, collection: Collection<Path>): JvmMultiPathContainer {
      return collection.map {
        JvmMultiPathEntry(
          type = role.inferEntryType(it),
          path = it,
        )
      }.toPersistentList().let { built ->
        JvmMultiPathContainer(
          role = role,
          paths = built,
        )
      }
    }
  }
}

/**
 * ## JVM Multi-Path Container (Mutable)
 *
 * Concrete container type hierarchy for JVM platforms; defines both the concept of a module-path (which can accept
 * JMODs, in addition to JARs and directories), or traditional class-paths.
 */
@Serializable public class MutableJvmMultiPathContainer(
  override val role: JvmMultiPath,
  override var paths: MutableList<JvmMultiPathEntry> = LinkedList(),
  override var entries: MutableSet<Path> = paths.map { it.path }.toMutableSet(),
) : MultiPath.MutableMultiPathContainer<JvmMultiPathEntry, JvmMultiPath> {
  override fun toMutable(): MutableJvmMultiPathContainer = this
  override fun asPathSequence(): Sequence<Path> = paths.asSequence().map { it.path }
  override fun iterator(): MutableIterator<MultiPath.Entry> = paths.toMutableList().iterator()

  override fun asArgumentSequence(): Sequence<Argument> = sequenceOf(
    Argument.of(role.longFlag to asArgumentString())
  )

  override fun ArgumentContext.asArgumentString(): String =
    paths.joinToString(separator = ":") { it.path.absolutePathString() }

  override operator fun set(index: Int, path: Path): Boolean {
    if (path in entries) {
      return false
    }
    paths[index] = JvmMultiPathEntry(
      type = role.inferEntryType(path),
      path = path,
    )
    entries.add(path)
    return true
  }

  override operator fun plus(other: Collection<Path>): MutableJvmMultiPathContainer = apply {
    for (path in other) {
      if (path in entries) {
        continue
      }
      paths.add(JvmMultiPathEntry(
        type = role.inferEntryType(path),
        path = path,
      ))
      entries.add(path)
    }
  }

  override operator fun plus(other: MultiPath): MutableJvmMultiPathContainer = apply {
    TODO("Not yet implemented")
  }

  override operator fun plus(other: Sequence<Path>): MutableJvmMultiPathContainer = apply {
    TODO("Not yet implemented")
  }

  override operator fun plus(path: Path): MutableJvmMultiPathContainer = apply {
    TODO("Not yet implemented")
  }

  override fun prepend(path: Path): Boolean {
    val entry = JvmMultiPathEntry(
      type = role.inferEntryType(path),
      path = path,
    )
    if (path in entries) {
      if (paths.first().path == path) {
        // already at the front
        return false
      }
      paths.remove(entry) // need to move it to the front
    }
    paths.add(0, entry)
    return true
  }

  override fun prepend(entry: MultiPath.Entry): Boolean {
    require(entry is JvmMultiPathEntry) { "Invalid entry type" }
    if (entry.path in entries) {
      if (paths.first() == entry) {
        // already at the front
        return false
      }
      paths.remove(entry) // need to move it to the front
    }
    paths.add(0, entry)
    return true
  }

  override fun prepend(suite: MultiPath): Boolean {
    var mutated = false

    // we need to iterate in reverse to maintain order of other suite
    for (entry in suite.asSequence().toList().reversed()) {
      val entryObj = when (entry) {
        is JvmMultiPathEntry -> entry
      }
      if (entryObj.path in entries) {
        if (paths.first() == entryObj) {
          // already at the front
          continue
        }
        paths.remove(entryObj) // need to move it to the front
      }
      mutated = true
      paths.add(0, entryObj)
    }
    return mutated
  }

  override fun add(suite: MultiPath): Boolean {
    var mutated = false

    // we need to iterate in reverse to maintain order of other suite
    for (entry in suite.asSequence()) {
      val entryObj = when (entry) {
        is JvmMultiPathEntry -> entry
      }
      if (entryObj.path in entries) {
        continue
      }
      mutated = true
      paths.add(entryObj)
      entries.add(entryObj.path)
    }
    return mutated
  }

  override fun add(path: Path): Boolean {
    if (path in entries) {
      return false
    }
    paths.add(JvmMultiPathEntry(
      type = role.inferEntryType(path),
      path = path,
    ))
    entries.add(path)
    return true
  }

  override fun add(element: MultiPath.Entry): Boolean {
    if (element.path in entries) {
      return false
    }
    paths.add(element as JvmMultiPathEntry)
    entries.add(element.path)
    return true
  }

  override fun addAll(elements: Collection<MultiPath.Entry>): Boolean {
    var mutated = false
    for (element in elements) {
      if (element.path in entries) {
        continue
      }
      paths.add(element as JvmMultiPathEntry)
      entries.add(element.path)
      mutated = true
    }
    return mutated
  }

  override fun remove(element: MultiPath.Entry): Boolean {
    TODO("Not yet implemented")
  }

  override fun removeAll(elements: Collection<MultiPath.Entry>): Boolean {
    TODO("Not yet implemented")
  }

  override fun retainAll(elements: Collection<MultiPath.Entry>): Boolean {
    TODO("Not yet implemented")
  }

  override fun clear() {
    TODO("Not yet implemented")
  }

  override fun build(): MultiPath.MultiPathContainer<JvmMultiPathEntry, JvmMultiPath> {
    TODO("Not yet implemented")
  }

  // Internal factories used by `Classpath` and `Modulepath` wrapper types.
  internal companion object {
    // Create an empty mutable multi-path container.
    @JvmStatic fun empty(): MutableJvmMultiPathContainer = MutableJvmMultiPathContainer(role = JvmMultiPath.CLASSPATH)

    // Create an empty mutable multi-path container.
    @JvmStatic fun of(role: JvmMultiPath, first: Path, vararg addl: Path): MutableJvmMultiPathContainer {
      return from(role, listOf(first).plus(addl))
    }

    // Create a mutable multi-path container from the provided collection of paths.
    @JvmStatic fun from(role: JvmMultiPath, collection: Collection<Path>): MutableJvmMultiPathContainer {
      return collection.map {
        JvmMultiPathEntry(
          type = role.inferEntryType(it),
          path = it,
        )
      }.toMutableList().let { built ->
        MutableJvmMultiPathContainer(
          role = role,
          paths = built,
        )
      }
    }
  }
}

/**
 * ## Classpath
 *
 * Concrete implementation of a [MultiPath] container for a JVM classpath; this container is aware of the entries within
 * the classpath, and enforces validity of such entries. Classpath entries may be directories or JARs.
 *
 * @property classpath Multi-path container managed for this classpath.
 */
public class Classpath private constructor (
  private val classpath: JvmMultiPathContainer = JvmMultiPathContainer(role = JvmMultiPath.CLASSPATH),
) : MultiPath.MultiPathContainer<JvmMultiPathEntry, JvmMultiPath> by classpath {
  /** Factories for creating or obtaining [Classpath] instances. */
  public companion object {
    /** Create an empty classpath container. */
    @JvmStatic public fun empty(): Classpath = Classpath()

    /** Create an empty multi-path container from the provided [first] (and optionally [additional]) paths. */
    @JvmStatic public fun of(first: Path, vararg additional: Path): Classpath = Classpath(
      JvmMultiPathContainer.of(JvmMultiPath.CLASSPATH, first, *additional)
    )

    /** Create a [collection] of paths. */
    @JvmStatic public fun from(collection: Collection<Path>): Classpath = Classpath(
      JvmMultiPathContainer.from(JvmMultiPath.CLASSPATH, collection)
    )

    /** Create a [Classpath] from the current classpath; only usable on JVM. */
    @JvmStatic public fun fromCurrent(): Classpath = require(!ImageInfo.inImageCode()) {
      "Cannot build current-classpath info from native context"
    }.let {
      when (val classpath: String? = System.getProperty("java.class.path")?.ifBlank { null }) {
        null -> empty()
        else -> from(classpath.split(":").map { Path.of(it) })
      }
    }

    /** Create a [Classpath] from the JAR hosting the given class. */
    @JvmStatic public fun fromOriginOf(cls: KClass<*>): Classpath? = require(!ImageInfo.inImageCode()) {
      "Cannot build current-classpath info from native context"
    }.let {
      cls.java.protectionDomain?.codeSource?.location?.let {
        from(listOf(Path.of(it.toURI())))
      }
    }
  }
}

/**
 * ## Classpath Provider
 *
 * Defers calculations to assemble a classpath until the classpath is needed. Expected to provide a fully assembled
 * [Classpath] as a result.
 */
public fun interface ClasspathProvider {
  /**
   * Provide the [Classpath] associated with this object, or provide this object as a [Classpath].
   *
   * @return Classpath instance.
   */
  public suspend fun classpath(): Classpath
}

/**
 * ## Modulepath Provider
 *
 * Defers calculations to assemble a modulepath until the modulepath is needed. Expected to provide a fully assembled
 * [Modulepath] as a result.
 */
public fun interface ModulepathProvider {
  /**
   * Provide the [Modulepath] associated with this object, or provide this object as a [Modulepath].
   *
   * @return Classpath instance.
   */
  public suspend fun modulepath(): Modulepath
}

/**
 * ## Multi-path Usage
 *
 * Describes usage types of a classpath or modulepath.
 */
public sealed interface MultiPathUsage : Comparable<MultiPathUsage> {
  public val apiSensitive: Boolean get() = true
  public val internalOnly: Boolean get() = false
  public val runtimeOnly: Boolean get() = false
  public val testOnly: Boolean get() = false
  public val scope: String
  public val order: UInt

  public fun expand(): List<MultiPathUsage> = listOf(this).plus(includes()).distinct()
  public fun includes(): List<MultiPathUsage> = listOf(this)

  public data object Compile : MultiPathUsage {
    override val order: UInt get() = 0u
    override val scope: String get() = "compile"
  }
  public data object Runtime : MultiPathUsage {
    override val order: UInt get() = 1u
    override fun includes(): List<MultiPathUsage> = listOf(Compile)
    override val scope: String get() = "runtime"
  }
  public data object Processors : MultiPathUsage {
    override val order: UInt get() = 1u
    override fun includes(): List<MultiPathUsage> = listOf()
    override val scope: String get() = "proc"
  }
  public data object TestProcessors : MultiPathUsage {
    override val order: UInt get() = 1u
    override fun includes(): List<MultiPathUsage> = listOf()
    override val scope: String get() = "testproc"
  }
  public data object TestCompile : MultiPathUsage {
    override val order: UInt get() = 2u
    override fun includes(): List<MultiPathUsage> = listOf(Compile)
    override val scope: String get() = "test"
  }
  public data object Dev : MultiPathUsage {
    override val order: UInt get() = 2u
    override fun includes(): List<MultiPathUsage> = listOf(Compile)
    override val scope: String get() = "dev"
  }
  public data object TestRuntime : MultiPathUsage {
    override val order: UInt get() = 3u
    override fun includes(): List<MultiPathUsage> = listOf(Compile, Runtime)
    override val scope: String get() = "test"
  }
  public data object Modules : MultiPathUsage {
    override val order: UInt get() = 0u
    override val scope: String get() = "modules"
  }

  override fun compareTo(other: MultiPathUsage): Int {
    return order.compareTo(other.order)
  }
}

/**
 * ## Module Inclusion
 *
 * Specifies the module inclusion mode for a classpath spec.
 */
public enum class ModuleInclusion {
  NoTransform,
  ExcludeModules,
  ModulesOnly,
}

/**
 * ## Classpath Spec
 *
 * Describes a classpath when requested from consumers; matching classpaths are returned for each specified optional
 * property.
 */
public interface ClasspathSpec : Predicate<ClasspathSpec> {
  public companion object {
    public val CompileAll: ClasspathSpec = object : ClasspathSpec {
      override val usage: MultiPathUsage get() = MultiPathUsage.Compile
      override val moduleInclusion: ModuleInclusion get() = ModuleInclusion.NoTransform
    }

    public val CompileClasspath: ClasspathSpec = object : ClasspathSpec {
      override val usage: MultiPathUsage get() = MultiPathUsage.Compile
      override val moduleInclusion: ModuleInclusion get() = ModuleInclusion.ExcludeModules
    }

    @Deprecated(
      "Use `CompileAll`, `CompileClasspath`, or `CompileModules` instead",
      ReplaceWith("CompileAll")
    )
    public val Compile: ClasspathSpec = CompileAll

    public val CompileModules: ClasspathSpec = object : ClasspathSpec {
      override val usage: MultiPathUsage get() = MultiPathUsage.Compile
      override val moduleInclusion: ModuleInclusion get() = ModuleInclusion.ModulesOnly
    }

    public val Runtime: ClasspathSpec = object : ClasspathSpec {
      override val usage: MultiPathUsage get() = MultiPathUsage.Runtime
    }

    public val TestCompile: ClasspathSpec = object : ClasspathSpec {
      override val usage: MultiPathUsage get() = MultiPathUsage.TestCompile
    }

    public val TestRuntime: ClasspathSpec = object : ClasspathSpec {
      override val usage: MultiPathUsage get() = MultiPathUsage.TestRuntime
    }
  }

  /**
   * Name of the classpath, if applicable.
   */
  public val name: String? get() = null

  /**
   * Handling logic for modules.
   */
  public val moduleInclusion: ModuleInclusion get() = ModuleInclusion.NoTransform

  /**
   * Usage type for the classpath.
   */
  public val usage: MultiPathUsage? get() = null

  override fun test(t: ClasspathSpec): Boolean {
    return when {
      name != null && t.name != null && name != t.name -> false
      else -> when (val usages = usage?.expand()) {
        null -> true
        else -> usages.contains(t.usage)
      }
    }
  }
}

/**
 * ## Classpaths Provider
 *
 * Defers calculations to assemble a classpath until the classpath is needed. Expected to provide a fully assembled
 * [Classpath] as a result of a request via a [ClasspathSpec].
 */
public fun interface ClasspathsProvider {
  /**
   * Provide the [Classpath] resolving from the provided [spec].
   *
   * @param spec Specifies the filter to apply; if not provided (`null`), all visible paths are returned as one
   *   classpath provider.
   * @return Classpath provider instance.
   */
  public suspend fun classpathProvider(spec: ClasspathSpec?): ClasspathProvider?
}

/**
 * ## Modulepaths Provider
 *
 * Defers calculations to assemble a module path until the path is needed. Expected to provide a fully assembled
 * [Modulepath] as a result of a request via a [ClasspathSpec].
 */
public fun interface ModulepathsProvider {
  /**
   * Provide the [Modulepath] resolving from the provided [spec].
   *
   * @param spec Specifies the filter to apply; if not provided (`null`), all visible paths are returned as one
   *   modulepath provider.
   * @return Modulepath provider instance.
   */
  public suspend fun modulepathProvider(spec: ClasspathSpec?): ModulepathProvider?
}

/**
 * ## Classpath (Mutable)
 *
 * Extends the base implementation of a [Classpath] container with mutability.
 *
 * @property classpath Multi-path container managed for this classpath.
 */
public class MutableClasspath private constructor (
  private val classpath: MutableJvmMultiPathContainer = MutableJvmMultiPathContainer(role = JvmMultiPath.CLASSPATH),
) : MultiPath.MutableMultiPathContainer<JvmMultiPathEntry, JvmMultiPath> by classpath {
  override val size: Int get() = classpath.size
  override fun asSequence(): Sequence<JvmMultiPathEntry> = classpath.asSequence()
  override fun isEmpty(): Boolean = classpath.isEmpty()
  override fun contains(path: Path): Boolean = classpath.contains(path)
  override fun contains(element: MultiPath.Entry): Boolean = classpath.contains(element)
  override fun contains(str: String): Boolean = classpath.contains(str)
  override fun containsAll(elements: Collection<MultiPath.Entry>): Boolean = classpath.containsAll(elements)
  override fun iterator(): MutableIterator<MultiPath.Entry> = classpath.iterator()
  override fun add(element: MultiPath.Entry): Boolean = classpath.add(element)
  override fun addAll(elements: Collection<MultiPath.Entry>): Boolean = classpath.addAll(elements)
  override fun remove(element: MultiPath.Entry): Boolean = classpath.remove(element)
  override fun get(index: Int): Argument = classpath[index]
  override fun get(index: UInt): JvmMultiPathEntry = classpath[index]

  /** Factories for creating or obtaining [Classpath] instances. */
  public companion object {
    /** Create an empty classpath container. */
    @JvmStatic public fun empty(): MutableClasspath = MutableClasspath()

    /** Create an empty multi-path container from the provided [first] (and optionally [additional]) paths. */
    @JvmStatic public fun of(first: Path, vararg additional: Path): MutableClasspath = MutableClasspath(
      MutableJvmMultiPathContainer.of(JvmMultiPath.CLASSPATH, first, *additional)
    )

    /** Create a [collection] of paths. */
    @JvmStatic public fun from(collection: Collection<Path>): MutableClasspath = MutableClasspath(
      MutableJvmMultiPathContainer.from(JvmMultiPath.CLASSPATH, collection)
    )
  }
}

/**
 * ## Modulepath
 *
 * Concrete implementation of a [MultiPath] container for a JVM modulepath; this container is aware of the entries
 * within the modulepath, and enforces validity of such entries. Modulepath entries may be directories, JARs, or JMODs.
 *
 * @property modulepath Multi-path container managed for this modulepath.
 */
public class Modulepath private constructor (
  private val modulepath: JvmMultiPathContainer = JvmMultiPathContainer(role = JvmMultiPath.MODULEPATH),
) : MultiPath.MultiPathContainer<JvmMultiPathEntry, JvmMultiPath> by modulepath {
  /** Factories for creating or obtaining [Modulepath] instances. */
  public companion object {
    /** Create an empty modulepath container. */
    @JvmStatic public fun empty(): Modulepath = Modulepath()

    /** Create an empty multi-path container from the provided [first] (and optionally [additional]) paths. */
    @JvmStatic public fun of(first: Path, vararg additional: Path): Modulepath = Modulepath(
      JvmMultiPathContainer.of(JvmMultiPath.MODULEPATH, first, *additional)
    )

    /** Create a [collection] of paths. */
    @JvmStatic public fun from(collection: Collection<Path>): Modulepath = Modulepath(
      JvmMultiPathContainer.from(JvmMultiPath.MODULEPATH, collection)
    )
  }
}

/**
 * ## Modulepath (Mutable)
 *
 * Extends the base implementation of a [Classpath] container with mutability.
 *
 * @property modulepath Multi-path container managed for this modulepath.
 */
public class MutableModulepath private constructor (
  private val modulepath: MutableJvmMultiPathContainer = MutableJvmMultiPathContainer(role = JvmMultiPath.MODULEPATH),
) : MultiPath.MutableMultiPathContainer<JvmMultiPathEntry, JvmMultiPath> by modulepath {
  override val size: Int get() = modulepath.size
  override fun asSequence(): Sequence<JvmMultiPathEntry> = modulepath.asSequence()
  override fun isEmpty(): Boolean = modulepath.isEmpty()
  override fun contains(path: Path): Boolean = modulepath.contains(path)
  override fun contains(element: MultiPath.Entry): Boolean = modulepath.contains(element)
  override fun contains(str: String): Boolean = modulepath.contains(str)
  override fun containsAll(elements: Collection<MultiPath.Entry>): Boolean = modulepath.containsAll(elements)
  override fun iterator(): MutableIterator<MultiPath.Entry> = modulepath.iterator()
  override fun add(element: MultiPath.Entry): Boolean = modulepath.add(element)
  override fun addAll(elements: Collection<MultiPath.Entry>): Boolean = modulepath.addAll(elements)
  override fun remove(element: MultiPath.Entry): Boolean = modulepath.remove(element)
  override fun get(index: Int): Argument = modulepath[index]
  override fun get(index: UInt): JvmMultiPathEntry = modulepath[index]

  /** Factories for creating or obtaining [Modulepath] instances. */
  public companion object {
    /** Create an empty mutable modulepath container. */
    @JvmStatic public fun empty(): MutableModulepath = MutableModulepath()

    /** Create an empty multi-path container from the provided [first] (and optionally [additional]) paths. */
    @JvmStatic public fun of(first: Path, vararg additional: Path): MutableModulepath = MutableModulepath(
      MutableJvmMultiPathContainer.of(JvmMultiPath.MODULEPATH, first, *additional)
    )

    /** Create a [collection] of paths. */
    @JvmStatic public fun from(collection: Collection<Path>): MutableModulepath = MutableModulepath(
      MutableJvmMultiPathContainer.from(JvmMultiPath.MODULEPATH, collection)
    )
  }
}

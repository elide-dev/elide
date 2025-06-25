package elide.tooling.project

import java.nio.file.Path
import java.util.*

/** Name of the source set. */
public typealias SourceSetName = String

/** Sorted set of file paths. */
public typealias PathSuite = SortedSet<Path>

/** Sorted set of file paths. */
public typealias SourceTaggedPathSuite = SortedSet<SourceFilePath>

@JvmRecord public data class SourceFilePath(
  public val path: Path,
  public val lang: SourceSetLanguage? = null,
) : Comparable<SourceFilePath> {
  override fun compareTo(other: SourceFilePath): Int = path.compareTo(other.path)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as SourceFilePath
    return path == other.path
  }

  override fun hashCode(): Int {
    return path.hashCode()
  }
}

/** Describes known languages of source sets. */
public sealed interface SourceSetLanguage {
  /** Describes a variant or dialect of a known language. */
  public sealed interface SourceVariant<T> : SourceSetLanguage where T : SourceSetLanguage

  /** The source set specifies Java sources. */
  public data object Java : SourceSetLanguage

  /** The source set specifies Kotlin sources. */
  public data object Kotlin : SourceSetLanguage

  /** The source set specifies Kotlin Script sources. */
  public data object KotlinScript : SourceSetLanguage, SourceVariant<Kotlin>

  /** The source set specifies JavaScript sources. */
  public data object JavaScript : SourceSetLanguage

  /** The source set specifies JSX sources. */
  public data object JSX : SourceSetLanguage, SourceVariant<JavaScript>

  /** The source set specifies TypeScript sources. */
  public data object TypeScript : SourceSetLanguage

  /** The source set specifies JSX sources. */
  public data object TSX : SourceSetLanguage, SourceVariant<TypeScript>

  /** The source set specifies Python sources. */
  public data object Python : SourceSetLanguage

  /** The source set specifies Ruby sources. */
  public data object Ruby : SourceSetLanguage
}

/** Describes types of source sets. */
public sealed interface SourceSetType : Comparable<SourceSetType> {
  /** The source set contains primary source code. */
  public data object Sources : SourceSetType {
    override fun compareTo(other: SourceSetType): Int {
      return when (other) {
        is Sources -> 0
        is Tests -> -1
      }
    }
  }

  /** The source set contains test source code. */
  public data object Tests : SourceSetType {
    override fun compareTo(other: SourceSetType): Int {
      return when (other) {
        is Sources -> 1
        is Tests -> 0
      }
    }
  }
}

/** Describes source sets loaded for a [ElideConfiguredProject]. */
public interface SourceSets {
  /**
   * Indicate whether a source set with the given [name] exists.
   *
   * @return True if a source set with the given [name] exists, false otherwise.
   */
  public operator fun contains(name: SourceSetName): Boolean

  /**
   * Retrieve a source set by [name].
   *
   * @return The source set with the given [name], or null if no such source set exists.
   */
  public operator fun get(name: SourceSetName): SourceSet?

  /**
   * Find all source sets matching the specified [types].
   *
   * @param types Type(s) of source set to find.
   * @return A sequence of source sets matching the specified [types].
   */
  public fun find(vararg types: SourceSetType): Sequence<SourceSet>

  /**
   * Find all source sets matching the specified [langs].
   *
   * @param langs Language(s) of source set to find.
   * @return A sequence of source sets matching the specified [langs].
   */
  public fun find(vararg langs: SourceSetLanguage): Sequence<SourceSet>
}

/** Describes information about a configured source set in a [ElideConfiguredProject]. */
public interface SourceSet {
  /** Name assigned to this source set. */
  public val name: SourceSetName

  /** Type of this source set. */
  public val type: SourceSetType

  /** Languages held by this source set. */
  public val languages: Set<SourceSetLanguage>?

  /** Specified strings for this source set. */
  public val spec: List<String>

  /** Interpreted full suite of source set paths. */
  public val paths: SourceTaggedPathSuite
}

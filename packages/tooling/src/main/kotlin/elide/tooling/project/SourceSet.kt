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
package elide.tooling.project

import java.nio.file.Path
import java.util.*
import kotlin.reflect.KClass
import elide.tooling.project.SourceSetLanguage.SourceVariant

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

public fun SourceSetLanguage.isVariant(): Boolean {
  return this as? SourceVariant<*> != null
}

public inline fun <reified T : SourceSetLanguage> SourceSetLanguage.rootVariant(): KClass<T> {
  return T::class
}

public inline fun <reified T : SourceSetLanguage, reified O: SourceSetLanguage> O.isVariantOf(): Boolean {
  return isVariant() && rootVariant<T>() == rootVariant<O>()
}

/** Describes known languages of source sets. */
public sealed interface SourceSetLanguage {
  /** Extensions associated with this source set language. */
  public fun extensions(): Set<String> = emptySet()

  /** Formal name for this language. */
  public val formalName: String

  /** Describes a variant or dialect of a known language. */
  public sealed interface SourceVariant<T> : SourceSetLanguage where T : SourceSetLanguage

  /** The source set specifies Java sources. */
  public data object Java : SourceSetLanguage {
    override val formalName: String = "Java"
    override fun extensions(): Set<String> = setOf("java")
  }

  /** The source set specifies Kotlin sources. */
  public data object Kotlin : SourceSetLanguage {
    override val formalName: String = "Kotlin"
    override fun extensions(): Set<String> = setOf("kt")
  }

  /** The source set specifies Kotlin Script sources. */
  public data object KotlinScript : SourceSetLanguage, SourceVariant<Kotlin> {
    override val formalName: String = "KotlinScript"
    override fun extensions(): Set<String> = setOf("kts")
  }

  /** The source set specifies JavaScript sources. */
  public data object JavaScript : SourceSetLanguage {
    override val formalName: String = "JavaScript"
    override fun extensions(): Set<String> = setOf("js", "mjs", "cjs")
  }

  /** The source set specifies JSX sources. */
  public data object JSX : SourceSetLanguage, SourceVariant<JavaScript> {
    override val formalName: String = "JSX"
    override fun extensions(): Set<String> = setOf("jsx")
  }

  /** The source set specifies TypeScript sources. */
  public data object TypeScript : SourceSetLanguage {
    override val formalName: String = "TypeScript"
    override fun extensions(): Set<String> = setOf("ts", "cts", "mts")
  }

  /** The source set specifies JSX sources. */
  public data object TSX : SourceSetLanguage, SourceVariant<TypeScript> {
    override val formalName: String = "TSX"
    override fun extensions(): Set<String> = setOf("tsx")
  }

  /** The source set specifies Python sources. */
  public data object Python : SourceSetLanguage {
    override val formalName: String = "Python"
    override fun extensions(): Set<String> = setOf("py")
  }

  /** The source set specifies Ruby sources. */
  public data object Ruby : SourceSetLanguage {
    override val formalName: String = "Ruby"
    override fun extensions(): Set<String> = setOf("rb")
  }

  /** The source set specifies JSON assets. */
  public data object JSON : SourceSetLanguage {
    override val formalName: String = "JSON"
    override fun extensions(): Set<String> = setOf("json")
  }

  /** The source set specifies SVG assets. */
  public data object SVG : SourceSetLanguage {
    override val formalName: String = "SVG"
    override fun extensions(): Set<String> = setOf("svg")
  }

  /** The source set specifies HTML sources. */
  public data object HTML : SourceSetLanguage {
    override val formalName: String = "HTML"
    override fun extensions(): Set<String> = setOf("html")
  }

  /** The source set specifies CSS sources. */
  public data object CSS : SourceSetLanguage {
    override val formalName: String = "CSS"
    override fun extensions(): Set<String> = setOf("css")
  }

  /** The source set specifies SCSS sources. */
  public data object SCSS : SourceSetLanguage, SourceVariant<CSS> {
    override val formalName: String = "SCSS"
    override fun extensions(): Set<String> = setOf("scss", "sass")
  }

  /** The source set specifies Markdown sources. */
  public data object Markdown : SourceSetLanguage {
    override val formalName: String = "Markdown"
    override fun extensions(): Set<String> = setOf("md")
  }

  /** The source set specifies MDX sources. */
  public data object MDX : SourceSetLanguage, SourceVariant<Markdown> {
    override val formalName: String = "MDX"
    override fun extensions(): Set<String> = setOf("mdx")
  }
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

  /** Root path for this source set. */
  public val root: Path
}

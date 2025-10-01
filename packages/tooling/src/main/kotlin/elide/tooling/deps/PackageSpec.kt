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
package elide.tooling.deps

import com.github.packageurl.PackageURL
import org.semver4j.Semver
import java.util.LinkedList
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlin.jvm.Throws
import elide.core.api.Symbolic
import elide.tooling.project.ElideProject
import elide.tooling.deps.DependencyEcosystem.*
import elide.tooling.project.manifest.ElidePackageManifest

/**
 * # Package Spec
 *
 * Describes a universal package/dependency specification, usually originating from a simple string; strings are parsed
 * from multiple formats, with hinting, and when well-formed, ultimately create matching types within this hierarchy.
 *
 * Package specs can be parsed using static methods on this interface. Package spec objects are immutable, and can be
 * interrogated for their parsed values; matching against the type hierarchy allows for type-safe access to ecosystem
 * specific properties.
 *
 * Package specs can be formatted as strings and are fully serializable. Every package spec exposes a [purl] which
 * provides a rendered specification in the Package URL format.
 *
 * ## Supported Formats
 *
 * Package specifications are supported in string form for multiple ecosystems; the universal "purl" format is supported
 * as well:
 *
 * - **Maven** (`maven`): `groupId:artifactId` or `groupId:artifactId:version`
 * - **NPM** (`npm`): `packageName`, `@scope/packageName`, or `packageName@version`, or `@scope/packageName@version`
 * - **PyPI** (`pip`): `packageName` or `packageName==version` (or any other PyPI-supported operator)
 *
 * Ambiguity is possible across package specifications, of course, so there are some ways to address that. The following
 * strategies are consulted in order:
 *
 * - **Prefixing:** Prepending the ecosystem tag uses that ecosystem only; for example, `maven:<...>` or `npm:<...>` or
 *   `pip:<...>`.
 *
 * - **PURL:** The [Package URL](https://github.com/package-url/purl-spec) format is unambiguous
 *
 * - **Searching:** Failing all other options, a search is performed in parallel against each registry; the options are
 *   shown to the developer. If an interactive terminal is not available, an error is thrown.
 *
 * ### Handling Ambiguity
 *
 * The property [ambiguous] is marked as `true` if the parsed package spec is ambiguous in any aspect, including version
 * resolution. Packages which use symbolic version references are always [ambiguous] until resolution. To understand the
 * reasoning for ambiguity, use the [parse] method (rather than [tryParse]), which provides checked-exception access to
 * the reasons an ambiguity failure occurred.
 *
 * Note that some ambiguity failures are errors:
 *
 * - **Unresolved ecosystem:** In the case of ecosystem ambiguity, an error is thrown, or `null` is returned from the
 *   [tryParse] method; exceptions thrown by [parse] specify this reasoning.
 *
 * ## Versions
 *
 * Package specifications may or may not include a version, or a version specifier. Multiple version specifier formats
 * are supported. The token `version` in the section above refers to strings of this format.
 *
 * - **Symbolic:** The special strings `latest`, `stable`, and `snapshot` are supported; these are resolved according to
 *   the target ecosystem's semantics.
 *
 * - **Exact:** `1.2.3` (a semver), or any other valid version string for the target ecosystem; best-effort validation
 *   is performed by Elide.
 *
 * - **Semver Range:** NPM-style semver ranges, such as `^1.2.3`, `~1.2.3`, `>=1.2.3`, etc. Semantic version ranges are
 *   supported for NPM, Maven, and PyPI.
 *
 * If a version is not specified for a package, `latest` is implied.
 *
 * ## Usage
 *
 * [PackageSpec] instances can be obtained by using the [parse] method, and optionally providing an [ElideProject] as
 * context; if a project is provided, package spec parsing prefers ecosystems which are in use by the project already.
 *
 * The methods [parse] and [tryParse] are made available for string parsing:
 *
 * ```kotlin
 * // parse and fall back to `null` on failure
 * val packageSpec = PackageSpec.tryParse("npm:react@18")
 *
 * // parse and throw with reasons
 * val packageSpec = PackageSpec.parse("com.google.guava:guava:latest")
 * ```
 *
 * @see DependencyEcosystem Dependency ecosystems
 * @see ElideProject Elide projects
 */
@Serializable public sealed interface PackageSpec {
  /** Dependency ecosystem relating to the parsed package spec. */
  public val ecosystem: DependencyEcosystem

  /** String-formatted version of the package spec. */
  public val string: String

  /** Namespace of the package (scope for NPM, group ID for Maven, not present for Pip). */
  public val namespace: String?

  /** Name of the module (package for NPM, module for Maven, package for Pip). */
  public val module: String?

  /** Formatted version of this package spec according to its own ecosystem. */
  public val formatted: String

  /** Package URL-formatted version of this package spec. */
  public val purl: PackageURL

  /** Version of the package (or version specifier). */
  public val version: Version

  /** Whether this package spec is "ambiguous" in any manner, including with regard to versioning. */
  public val ambiguous: Boolean

  /**
   * ## Parse Error
   *
   * Root error type for fatal parsing errors of [PackageSpec] instances; this type can be matched against to determine
   * the actual error and its type-safe properties.
   */
  public sealed class ParseError(message: String, cause: Throwable?): Exception(message, cause, true, false)

  /**
   * ### Ambiguous Ecosystem
   *
   * This exception type is thrown when a [DependencyEcosystem] cannot be determined during parsing of a [PackageSpec].
   * If known, the [candidates] property contains a list of possible candidates for the ecosystem.
   */
  public class AmbiguousEcosystem internal constructor (
    public val candidates: List<DependencyEcosystem> = emptyList(),
    message: String = buildString {
      append("Package spec is ambiguous: ")
      if (candidates.isNotEmpty()) {
        append("could be any of ${candidates.joinToString(", ") { it.name }}")
      } else {
        append("could not determine dependency ecosystem")
      }
    },
  ): ParseError(message, null)

  // Package spec parser implementation.
  private object Parser {
    private val NPM_REGEX by lazy { Regex("""^@?([^/]+)/([^@]+)(?:@(.+))?$""") }
    private val MAVEN_REGEX by lazy { Regex("""^([^:]+):([^:]+)(?::(.+))?$""") }
    private val PIP_REGEX by lazy { Regex("""^([^=]+)(?:==(.+))?$""") }

    // All registered matchers, by ecosystem; listed by priority.
    private val matchers by lazy {
      mapOf(
        NPM to LinkedList<Regex>().apply { add(NPM_REGEX) },
        Maven to LinkedList<Regex>().apply { add(MAVEN_REGEX) },
        PyPI to LinkedList<Regex>().apply { PIP_REGEX },
      )
    }

    // All registered parsers, by ecosystem.
    private val parsers by lazy {
      mapOf(
        NPM to NpmPackageSpec.Companion,
        Maven to MavenPackageSpec.Companion,
        PyPI to PipPackageSpec.Companion,
      )
    }

    // Attempt to parse a dependency ecosystem prefix from the provided `subject`.
    @JvmStatic fun parsePrefix(subject: String): DependencyEcosystem? {
      return when (subject.substringBefore(':').lowercase().trim()) {
        PREFIX_MAVEN -> Maven
        PREFIX_NPM -> NPM
        PREFIX_PIP -> PyPI
        else -> null
      }
    }

    // Attempt to match a dependency ecosystem prefix, or otherwise fallback to regex matching.
    @JvmStatic fun detectEcosystem(subject: String): DependencyEcosystem? {
      return when (val explicit = parsePrefix(subject)) {
        // the user did not provide a prefix; attempt to match via regexes.
        null -> matchers.flatMap { (ecosystem, candidates) ->
          candidates.map { regex ->
            ecosystem to regex
          }
        }.firstNotNullOfOrNull { (ecosystem, regex) ->
          if (!regex.matches(subject)) null else {
            ecosystem
          }
        }

        // the user provided an explicit ecosystem prefix.
        else -> explicit
      }
    }

    // Given the detected or preferred ecosystem, parse the provided spec string.
    @JvmStatic fun parseForEcosystem(ecosystem: DependencyEcosystem, subject: String): PackageSpec {
      return requireNotNull(parsers[ecosystem]) { "No parser for ecosystem: $ecosystem" }
        .parse(when (val prefix = parsePrefix(subject)) {
          null -> subject
          else -> subject.removePrefix(prefix.dependencyPrefix)
        })
    }

    // Parse a version specifier, falling back to ecosystem-specific version parsing as appropriate.
    @JvmStatic fun parseVersion(factory: SpecializedPackageSpecParser<*>, version: String): Version = when (version) {
      // symbol versions
      LATEST_VERSION -> SymbolicVersion.LATEST
      STABLE_VERSION -> SymbolicVersion.STABLE
      SNAPSHOT_VERSION -> SymbolicVersion.SNAPSHOT

      // fallback
      else -> runCatching {
        // semver parse
        if (!SEMVER_REGEX.matches(version)) null else {
          SemanticVersion(Semver(version))
        }
      }.getOrNull() ?: runCatching {
        // it could still be an ecosystem-specific version
        factory.parseVersion(version)
      }.getOrNull() ?: VersionString(
        // otherwise, treat it as an opaque string
        version,
      )
    }
  }

  /** Constants, factories, and parsing entrypoint for [PackageSpec]. */
  public companion object {
    /** Symbolic string representing the 'latest' package version. */
    public const val LATEST_VERSION: String = "latest"

    /** Symbolic string representing the 'stable' package version. */
    public const val STABLE_VERSION: String = "stable"

    /** Symbolic string representing the 'snapshot' package version. */
    public const val SNAPSHOT_VERSION: String = "snapshot"

    /** Symbolic string representing the 'maven' ecosystem. */
    public const val PREFIX_MAVEN: String = "maven"

    /** Symbolic string representing the 'npm' ecosystem. */
    public const val PREFIX_NPM: String = "npm"

    /** Symbolic string representing the 'pip' ecosystem. */
    public const val PREFIX_PIP: String = "pip"

    // Regex to use for matching a semantic version string.
    private val SEMVER_REGEX by lazy { Regex("""^(\d+)\.(\d+)\.(\d+)(?:-([a-zA-Z0-9.-]+))?(\+([a-zA-Z0-9.-]+))?$""") }

    /**
     * Attempt to parse a package spec from the provided [subject] string, and with the provided [project] as context
     * (optionally); if no package spec can safely be parsed, `null` is returned.
     *
     * @param subject String to parse as a package spec.
     * @param project Optional project context to use when parsing the package spec.
     * @param ecosystem Optional known ecosystem.
     * @return Package spec, or null if the string could not be parsed.
     */
    @JvmStatic public fun tryParse(
      subject: String,
      project: ElideProject? = null,
      ecosystem: DependencyEcosystem? = null,
    ): PackageSpec? = runCatching { parse(subject, project, ecosystem) }.getOrNull()

    /**
     * Parse a package spec from the provided [subject] string, and with the provided [project] as context (optionally);
     * if no package spec can safely be parsed, an error is thrown.
     *
     * @param subject String to parse as a package spec.
     * @param project Optional project context to use when parsing the package spec.
     * @param ecosystem Optional known ecosystem.
     * @return Package spec.
     * @throws ParseError If the provided string cannot be parsed without fatal ambiguity (for example, an ecosystem
     *   cannot be determined). Specific error types can be matched against to determine the reason for failure.
     */
    @Throws(ParseError::class)
    @JvmStatic public fun parse(
      subject: String,
      @Suppress("UNUSED_PARAMETER") project: ElideProject? = null,
      ecosystem: DependencyEcosystem? = null,
    ): PackageSpec {
      // resolve the ecosystem first...
      val effective = ecosystem ?: Parser.detectEcosystem(subject) ?: throw AmbiguousEcosystem()

      // then parse, after removing any prefix
      return Parser.parseForEcosystem(
        effective,
        subject.removePrefix(effective.dependencyPrefix),
      )
    }
  }

  // -- Package Spec: Versions

  /**
   * ## Package Version
   *
   * Root of a sealed hierarchy for types which specify a package version; package versions are optional, and may be
   * symbolic, or may be parsed from semantic versions or semantic version ranges.
   *
   * @property formatted Formatted version string.
   */
  @Serializable public sealed interface Version {
    public val formatted: String
  }

  /**
   * ## No Version
   *
   * Used when no version is specified within a given [PackageSpec] instance.
   */
  @Serializable public data object NoVersion: Version {
    override val formatted: String get() = ""
  }

  /**
   * ## Symbolic Version
   *
   * Specifies a "symbolic" version, such as the string `latest` or `stable` or `snapshot`; this is modeled as an enum
   * because the suite of symbolic versions understood by Elide is hard-coded. Other string values (which are not sem-
   * ver) are supported, but do not trigger special resolution behavior.
   */
  @Serializable public enum class SymbolicVersion (override val symbol: String): Version, Symbolic<String> {
    /** The latest version is resolved. */
    LATEST(LATEST_VERSION),

    /** The latest release version is resolved. */
    STABLE(STABLE_VERSION),

    /** The latest possible version is resolved. */
    SNAPSHOT(SNAPSHOT_VERSION);

    override val formatted: String get() = symbol

    public companion object: Symbolic.SealedResolver<String, SymbolicVersion> {
      override fun resolve(symbol: String): SymbolicVersion = when (symbol) {
        LATEST_VERSION -> LATEST
        STABLE_VERSION -> STABLE
        SNAPSHOT_VERSION -> SNAPSHOT
        else -> throw unresolved(symbol)
      }
    }
  }

  /**
   * ## String Version
   *
   * Specifies an arbitrary [version] string of some kind (such as 'beta', or 'my-version'), which is not sem-ver, and
   * which is not a [SymbolicVersion] match.
   */
  @Serializable
  @JvmInline public value class VersionString internal constructor (public val version: String): Version {
    override val formatted: String get() = version
  }

  /**
   * ## Exact Semantic Version
   *
   * Specifies a parsed semantic version, such as `1.2.3` or `2.0.0-beta.1`.
   *
   * @property version Parsed semantic version.
   */
  @Serializable
  @JvmInline public value class SemanticVersion internal constructor(@Contextual private val version: Semver): Version {
    override val formatted: String get() = version.version
  }

  /**
   * ## Semantic Version Operators
   *
   * Describes the concept of an operator that places constraints based on a semantic version; such operators are simple
   * in some cases (greater-than, equal-to) and complex in others (up-to-major, less-than-major-minor, etc).
   */
  @Serializable public sealed interface SemverOperator {
    /**
     * Effective operator which is paired with this one; describes how an operator will be treated when it is used in a
     * complex range.
     */
    public val effective: SimpleSemverOperator

    /** Formatted string representation of this operator. */
    public val formatted: String
  }

  /**
   * ## Semantic Version Operators (Simple)
   *
   * Enumerates understood effective operators for semantic version ranges; this is used as a component within a package
   * spec that declares a range for its version.
   */
  @Serializable public enum class SimpleSemverOperator (override val formatted: String)
    : SemverOperator, Symbolic<String> {
    /** Semver operator effecting `==`. */
    EQUALS("=="),

    /** Semver operator effecting `>`. */
    GREATER_THAN(">"),

    /** Semver operator effecting `>=`. */
    GREATER_THAN_OR_EQUALS(">="),

    /** Semver operator effecting `<`. */
    LESS_THAN("<"),

    /** Semver operator effecting `<=`. */
    LESS_THAN_OR_EQUALS("<=");

    override val symbol: String get() = formatted

    // Simple operators are always effective.
    override val effective: SimpleSemverOperator get() = this

    public companion object: Symbolic.SealedResolver<String, SimpleSemverOperator> {
      override fun resolve(symbol: String): SimpleSemverOperator = when (symbol) {
        EQUALS.symbol -> EQUALS
        GREATER_THAN.symbol -> GREATER_THAN
        GREATER_THAN_OR_EQUALS.symbol -> GREATER_THAN_OR_EQUALS
        LESS_THAN.symbol -> LESS_THAN
        LESS_THAN_OR_EQUALS.symbol -> LESS_THAN_OR_EQUALS
        else -> throw unresolved(symbol)
      }
    }
  }

  /**
   * ## NPM Version Operators
   *
   * Enumerates understood NPM-style semver operators; such operators are specified within `package.json` or equivalent
   * Elide manifest blocks.
   */
  @Serializable public enum class NpmSemverOperator (override val formatted: String) : SemverOperator {
    /** NPM operator effecting `^`. */
    CARET("^"),

    /** NPM operator effecting `~`. */
    TILDE("~"),;

    // Simple operators are always effective.
    override val effective: SimpleSemverOperator get() = SimpleSemverOperator.GREATER_THAN_OR_EQUALS
  }

  /**
   * ## Semantic Version Range
   *
   * Pairs a [SemanticVersion] subject with a [SemverOperator], to create a simple range specification; such strings
   * take the form `<operator> <version>`, such as `>= 1.2.3` or `<= 2.0.0-beta.1` (with or without whitespace).
   */
  @Serializable
  @JvmInline public value class SemanticVersionRange internal constructor (
    private val pair: Pair<SemverOperator, SemanticVersion>,
  ): Version {
    override val formatted: String get() = buildString {
      append(pair.first.formatted)
      append(pair.second.formatted)
    }
  }

  // -- Package Spec: Implementations

  /**
   * ### Specialized Package Spec Parser
   *
   * Describes the interface supported by [SpecializedPackageSpec] parsers; each specialized package spec must adhere to
   * this base type via their companion object.
   */
  public sealed class SpecializedPackageSpecParser<T> where T: SpecializedPackageSpec {
    /**
     * Attempt to parse a package spec under the context of the specialized parser [T].
     *
     * @param subject String to parse as a package spec.
     * @return Package spec, or null if the string could not be parsed.
     * @throws ParseError If the provided string cannot be parsed without fatal ambiguity.
     */
    @Throws(ParseError::class)
    public abstract fun parse(subject: String): T

    /**
     * Attempt to parse a version according to the semantics expressed by this package ecosystem; if none can be parsed
     * safely, `null` is returned.
     *
     * @param subject Subject version string to parse.
     * @return Parsed version value, or `null` if none could safely be parsed.
     */
    public open fun parseVersion(subject: String): Version? = null
  }

  /**
   * ### Specialized Package Spec
   *
   * Base type for specialized package spec implementations; such types implement type-safe access to ecosystem-specific
   * parsed metadata, as well as a [SpecializedPackageSpecParser] on their companion object.
   */
  public sealed class SpecializedPackageSpec (
    override val ecosystem: DependencyEcosystem,
    override val version: Version,
    override val string: String,
  ) : PackageSpec {
    override val ambiguous: Boolean get() = version is NoVersion

    override val purl: PackageURL get() =
      PackageURL(ecosystem.purlType, namespace, module, version.formatted, null, null)

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is SpecializedPackageSpec) return false
      if (ecosystem != other.ecosystem) return false
      if (purl != other.purl) return false
      return true
    }

    override fun hashCode(): Int {
      var result = ecosystem.hashCode()
      result = 31 * result + purl.hashCode()
      return result
    }

    override fun toString(): String {
      return "PackageSpec(ecosystem=$ecosystem, purl=$purl)"
    }
  }

  /**
   * ### Maven Package Spec
   *
   * Implements a [SpecializedPackageSpec] for [Maven]; packages are supported in the following formats:
   *
   * - `groupId:artifactId`
   * - `groupId:artifactId@version`
   * - `groupId:artifactId:classifier`
   * - `groupId:artifactId:classifier@version`
   *
   * @property groupId Maven group ID.
   * @property name Maven artifact ID.
   * @property classifier Maven artifact classifier, if any, otherwise, `null`.
   */
  public class MavenPackageSpec internal constructor (
    public val groupId: String,
    public val name: String,
    public val classifier: String?,
    version: Version,
    string: String,
  ) : SpecializedPackageSpec(Maven, version, string) {
    // Parser for Maven package specs.
    public companion object: SpecializedPackageSpecParser<MavenPackageSpec>() {
      // Parse Maven-style versions.
      private fun parseMavenVersion(string: String): Version {
        // @TODO("not yet implemented: Maven-style version parsing")
        return Parser.parseVersion(Companion, string)
      }

      override fun parse(subject: String): MavenPackageSpec {
        val split = subject.split(':')
        val groupId = split.first()
        val versionIfAny = subject.substringAfterLast('@')
        val (name, classifier) = when (split.size) {
          2 -> split[1].split('@').first() to null
          3 -> split[1] to split[2].split('@').first()
          else -> error("Invalid Maven package spec (too many or too few components): $subject")
        }

        val version = versionIfAny
          .takeIf { it.isNotEmpty() && it.isNotBlank() }
          ?.let { parseMavenVersion(it) }

        return MavenPackageSpec(
          groupId,
          name,
          classifier,
          version ?: NoVersion,
          subject,
        )
      }
    }

    override val namespace: String get() = groupId
    override val module: String get() = name
    override val formatted: String get() = buildString {
      append(groupId)
      append(":")
      append(name)
      if (version !is NoVersion) {
        append(":")
        append(version.formatted)
      }
    }
  }

  /**
   * ### NPM Package Spec
   *
   * Implements a [SpecializedPackageSpec] for [NPM]; packages are supported in the following formats:
   *
   * - `packageName`
   * - `@scope/packageName`
   * - `packageName@version`
   * - `@scope/packageName@version`
   *
   * @property scope The parsed NPM package scope, if any; otherwise, `null`.
   * @property name The parsed NPM package name.
   */
  public class NpmPackageSpec internal constructor (
    public val scope: String?,
    public val name: String,
    version: Version,
    string: String,
  ) : SpecializedPackageSpec(NPM, version, string) {
    // Parser for Maven package specs.
    public companion object: SpecializedPackageSpecParser<NpmPackageSpec>() {
      // Parse NPM-style versions.
      private fun parseNpmVersion(string: String): Version {
        // @TODO("not yet implemented: NPM-style version parsing")
        return Parser.parseVersion(Companion, string)
      }

      // Check for emptiness, blankness, and other conditions which should fail a scope or name string.
      private fun checkNpmSpec(subject: String, scope: String?, name: String) {
        check(name.isNotEmpty() && name.isNotBlank()) {
          "NPM package spec name is empty or blank (parsed from: '$subject')"
        }
        scope?.let {
          check(it.isNotEmpty() && it.isNotBlank()) {
            "NPM package spec scope is empty or blank (parsed from: '$subject')"
          }
        }
      }

      override fun parse(subject: String): NpmPackageSpec {
        val (scope, nameAndVersionMaybe) = if (subject.startsWith("@")) {
          subject.substringBefore("/") to subject.substringAfter("/")
        } else {
          null to subject
        }
        val (name, version) = if (!nameAndVersionMaybe.contains('@')) (nameAndVersionMaybe to NoVersion) else {
          val versionStr = nameAndVersionMaybe.substringAfter('@')
          nameAndVersionMaybe.substringBefore('@') to parseNpmVersion(versionStr)
        }
        return NpmPackageSpec(
          scope,
          name,
          version,
          subject,
        ).also {
          checkNpmSpec(subject, scope, name)
        }
      }
    }

    override val namespace: String? get() = scope
    override val module: String get() = name
    override val formatted: String get() = buildString {
      if (scope != null) {
        append("@")
        append(scope)
        append("/")
      }
      append(name)
      if (version !is NoVersion) {
        append("@")
        append(version.formatted)
      }
    }
  }

  /**
   * ## Pip Package Spec
   *
   * Implements a [SpecializedPackageSpec] for [PyPI]; packages are supported in the following formats:
   *
   * - `packageName`
   * - `packageName==version`, where `==` is any Pip-supported operator
   */
  public class PipPackageSpec internal constructor (
    override val module: String,
    version: Version,
    string: String,
  ) : SpecializedPackageSpec(PyPI, version, string) {
    internal fun asPipPackage(): ElidePackageManifest.PipPackage = ElidePackageManifest.PipPackage(
      name = module,
      version = version.formatted,
    )

    // Parser for Maven package specs.
    public companion object: SpecializedPackageSpecParser<PipPackageSpec>() {
      // Parse pip-style versions.
      private fun parsePipVersion(string: String): Version {
        // @TODO("not yet implemented: Pip-style version parsing")
        return Parser.parseVersion(Companion, string)
      }

      override fun parse(subject: String): PipPackageSpec {
        // go until the first non-alphanumeric character
        val module = subject.takeWhile { it in (('a'..'z') + ('A'..'Z') + ('0'..'9')) }
        val versionIfPresent = subject.removePrefix(module)

        val version = versionIfPresent
          .filter { it in ('0'..'9') || it == '.' }
          .takeIf { it.isNotBlank() && it.isNotEmpty() }
          ?.let { parsePipVersion(it) }
          ?: NoVersion

        val operator: SemverOperator? = versionIfPresent
          .takeWhile { it !in ('0'..'9') && it != '.' }
          .takeIf { it.isNotBlank() && it.isNotEmpty() }
          ?.let {
            runCatching { SimpleSemverOperator.resolve(it) }.getOrNull()
          }

        val effectiveVersion = when (operator) {
          null -> version
          else -> if (version is SemanticVersion) {
            SemanticVersionRange(operator to version)
          } else error(
            "Pip package spec version is not a semantic version: $version (parsed from: '$subject')",
          )
        }

        return PipPackageSpec(
          module = module,
          version = effectiveVersion,
          string = subject,
        )
      }
    }

    // Pip modules do not have a namespace value.
    override val namespace: String? get() = null

    // The formatted package reference consists of the module name and version.
    override val formatted: String get() = buildString {
      append(module)
      append(version.formatted)
    }
  }
}

// Resolve a Package URL type for the given ecosystem.
private val DependencyEcosystem.purlType get() = when (this) {
  NPM -> "npm"
  Maven -> "maven"
  PyPI -> "pypi"
  else -> error("Unsupported ecosystem for PURL: $this")
}

// Resolve a Package URL type for the given ecosystem.
private val DependencyEcosystem.dependencyPrefix get() = when (this) {
  NPM -> PackageSpec.PREFIX_NPM + ":"
  Maven -> PackageSpec.PREFIX_MAVEN + ":"
  PyPI -> PackageSpec.PREFIX_PIP + ":"
  else -> error("Unsupported ecosystem for PURL: $this")
}

package elide.runtime.gvm

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import elide.runtime.gvm.ExecutableScript.State

/**
 * # Executable Script
 *
 * Defines the interface which executable scripts must comply with in order to be compatible with Elide's Guest VM layer
 * and associated features. Executable scripts maintain an internal [State] describing their current point in the script
 * execution lifecycle.
 *
 * Elide doesn't care what language an executable script is written in, so long as it is described by a valid language
 * descriptor ([GuestLanguage]) and supported by the guest VM. Each step in the script lifecycle is described below. See
 * ancillary classes for more information.
 *
 * ## Script Lifecycle
 *
 * When a script is first created, it is initialized with a source target, language, and any relevant arguments, state,
 * or environment. As applicable, Elide may elect to prepend or append content before evaluating user code (in order to
 * produce a stable global environment). At this stage, the script is considered [State.UNINITIALIZED].
 *
 * Once the script has been assembled (linked), it is ready to be parsed/evaluated. At this stage, a VM is exclusively
 * acquired to parse and evaluate the script, and, at the successful conclusion of this initial execution, it moves into
 * the [State.PARSED] stage.
 *
 * Parsed scripts are no longer executing from source, but still need to be interrogated for a proper entrypoint. After
 * this stage completes, the script enters [State.EVALUATED] and is ready to be invoked. At this stage, the active VM is
 * released back into the pool, and the executable script is ready.
 *
 * At some point later (potentially immediately), the script is executed, which moves it into the [State.EXECUTED] stage
 * terminally. The script remains in this stage for the duration of the server's lifecycle, and is re-executed from the
 * cache for each invocation.
 *
 * ## Sealed Interface
 *
 * This interface is sealed to prevent direct external implementation; sub-classes are provided which describe common
 * script loading strategies. The developer may opt to extend one of these sub-classes, where needed and supported.
 *
 * @see GuestLanguage for the expected specification adhered to by guest language descriptors.
 * @see State for an exhaustive enumeration of states that a script may inhabit during its lifecycle.
 */
@Suppress("unused")
public sealed interface ExecutableScript {
  /**
   * ## State Lifecycle
   *
   * Enumerates each state that an executable guest script may inhabit during its lifecycle. The process of these states
   * is described in the main [ExecutableScript] docs.
   *
   * - [State.UNINITIALIZED]: The script has just been created and has not yet initialized at all.
   * - [State.PARSED]: The script has been linked and parsed, but has not been evaluated yet.
   * - [State.EVALUATED]: The script has been evaluated but has not been executed yet.
   * - [State.EXECUTED]: The script has executed before and is fully cached, configuration permitting.
   */
  public enum class State {
    /** The script has just been created and has not yet initialized at all. */
    UNINITIALIZED,

    /** The script has been linked and parsed, but has not been evaluated yet. */
    PARSED,

    /** The script has been evaluated but has not been executed yet. */
    EVALUATED,

    /** The script has executed before and is fully cached. */
    EXECUTED,
  }

  /**
   * ## Script Type
   *
   * Specifies the type of code contained in a guest VM script, typically in the form of a MIME type; e.g.
   * `text/javascript`. Alternative symbols are provided as calculated properties from the underlying [spec]/
   */
  @JvmInline public value class ScriptType private constructor(internal val spec: String) {
    internal companion object {
      /** Prefix used when [spec] is a MIME type. */
      private const val MIME_TYPE_PREFIX = "mime"

      /** Specify the script type with a MIME type. */
      @JvmStatic fun fromMime(mime: String): ScriptType = ScriptType("$MIME_TYPE_PREFIX:$mime")
    }

    override fun toString(): String = "ScriptType(${spec.drop(5)})"

    /** Tests equality between two [ScriptType] instances. */
    internal fun `is`(other: ScriptType): Boolean = spec == other.spec

    /** @return Raw MIME type. */
    internal fun asMimeType(): String = spec.drop(MIME_TYPE_PREFIX.length + 1)

    /** @return Expected character set for the script. */
    internal fun charset(): Charset = StandardCharsets.UTF_8  // @TODO(sgammon): make this configurable
  }

  /**
   * ## Script Source
   *
   * Specifies the source code [target] for a guest VM script. This may be a file, a literal string, an embedded asset,
   * or any other source permitted by the interface defined herein.
   */
  @JvmInline public value class ScriptSource private constructor(internal val target: String) {
    internal companion object {
      /** Prefix used when [target] is a file. */
      private const val PROTOCOL_FILE = "file"

      /** Prefix used when [target] is a classpath resource. */
      private const val PROTOCOL_CLASSPATH = "classpath"

      /** Prefix used when [target] is an application-embedded script. */
      private const val PROTOCOL_EMBEDDED = "embedded"

      /** Prefix used when [target] is a literal string. */
      private const val PROTOCOL_LITERAL = "literal"

      /** Constant which refers to a literal script. */
      private val LITERAL_SCRIPT = ScriptSource(PROTOCOL_LITERAL)

      /** @return Script source which references a file [path]. */
      @JvmStatic fun fromFile(path: String) = ScriptSource("$PROTOCOL_FILE://$path")

      /** @return Script source which references a [resource] path. */
      @JvmStatic fun fromResource(resource: String) = ScriptSource("$PROTOCOL_CLASSPATH://$resource")

      /** @return Script source which references an embedded script by [name]. */
      @JvmStatic fun fromEmbedded(name: String) = ScriptSource("$PROTOCOL_EMBEDDED://$name")

      /** Symbol for a literal script. */
      @JvmStatic val LITERAL: ScriptSource = LITERAL_SCRIPT
    }

    /** @return Filename for this script source. */
    internal val filename: String get() = if (target == PROTOCOL_LITERAL) {
      "literal"
    } else target.split("/").last()

    /** @return Path value for this script resource, as applicable (or an empty string). */
    internal val path: String get() = if (target == PROTOCOL_LITERAL) {
      ""
    } else when {
      isEmbedded -> target.drop(PROTOCOL_EMBEDDED.length + 3)
      isResource -> target.drop(PROTOCOL_CLASSPATH.length + 3)
      isFile -> target.drop(PROTOCOL_FILE.length + 3)
      else -> target
    }

    /** @return Extension for this script resource, if present. */
    internal val extension: String? get() = filename.substringAfterLast(".").ifBlank {
      null
    }

    /** @return Indication that this script is a literal value. */
    internal val isLiteral: Boolean get() = target == PROTOCOL_LITERAL

    /** @return Indication that this script is a classpath resource. */
    internal val isResource: Boolean get() = target.startsWith(PROTOCOL_CLASSPATH)

    /** @return Indication that this script is an embedded resource. */
    internal val isEmbedded: Boolean get() = target.startsWith(PROTOCOL_EMBEDDED)

    /** @return Indication that this script is a file resource. */
    internal val isFile: Boolean get() = target.startsWith(PROTOCOL_FILE)
  }

  /**
   * ## Source Map
   *
   * Specifies the source code map [target] for a guest VM script. This may be a file, a literal string, an embedded
   * asset, or any other source permitted by the interface defined herein. This source map is expected to correspond
   * with an associated [ScriptSource].
   */
  @JvmInline public value class SourceMap(private val target: String)

  /**
   * @return The current state of this executable script record.
   */
  public fun state(): State

  /**
   * @return The [InvocationMode]s supported by this script.
   */
  public fun invocation(): EnumSet<InvocationMode>

  /**
   * Indicate the guest language that this script is written in; it is expected that the returned [GuestLanguage] is
   * supported by the active VM, or no execution can take place.
   *
   * @return Guest language associated with this script.
   */
  public fun language(): GuestLanguage

  /**
   * TBD.
   */
  public fun type(): ScriptType

  /**
   * TBD.
   */
  public fun source(): ScriptSource

  /**
   * TBD.
   */
  public fun map(): SourceMap?
}

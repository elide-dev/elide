package elide.runtime.gvm.internals.intrinsics.js.typed

import elide.runtime.intrinsics.js.err.ValueError
import elide.runtime.intrinsics.js.typed.UUID
import elide.vm.annotations.Polyglot
import java.util.UUID as JavaUUID

/**
 * # UUID Value
 *
 * Typed representation of a [UUID]. Under the hood, UUIDs are treated as quasi-strings, much like URLs are. This class
 * holds the implementation details for a wrapped or generated UUID, and behaves as a string from a guest perspective.
 * Parsing and encoding happen at construction time; [UUIDValue] objects never exist in an invalid state.
 *
 * Generating a UUID uses an optimized internal structure, which is only serialized to a string on-demand. If a UUID is
 * generated and never converted to a string, no string encoding occurs.
 *
 * If a string happens to be available (for instance, when a UUID is constructed from a string), it is cached after
 * parsing occurs.
 */
@JvmInline public value class UUIDValue private constructor (private val value: ValidUUID): UUID {
  /** Internal representation of a UUID string and structured value. */
  internal data class ValidUUID(
    /** Structural (parsed and validated) UUID. */
    val type: UUID.UUIDType = UUID.UUIDType.V4,

    /** Structural (parsed and validated) UUID. */
    val uuid: JavaUUID,

    /** Optional cached string, if available. */
    private val cachedString: String? = null,
  ) {
    /** Serialized UUID. */
    val string: String by lazy {
      cachedString ?: uuid.toString()
    }
  }

  /**
   * ## UUID Factory
   *
   * Internal factory implementation which handles the generation and parsing of UUIDs; these are always returned as an
   * instance of [UUIDValue].
   */
  public companion object: UUID.Factory {
    // Length of a V4 UUID.
    internal const val UUID_LENGTH: Int = 36

    override fun random(): UUIDValue = of(
      JavaUUID.randomUUID()
    )

    @Throws(ValueError::class)
    override fun of(value: String): UUIDValue = UUIDValue(ValidUUID(
      cachedString = value,
      uuid = try {
        JavaUUID.fromString(value)
      } catch (iae: IllegalArgumentException) {
        throw ValueError.create("Not a valid UUID string: '$value'", iae)
      },
    ))

    override fun of(value: UUIDValue): UUIDValue = UUIDValue(value.value)

    override fun of(value: JavaUUID): UUIDValue = UUIDValue(ValidUUID(
      uuid = value,
    ))
  }

  @get:Polyglot override val length: Int get() = UUID_LENGTH

  @Polyglot override fun get(index: Int): Char = value.string[index]

  @Polyglot override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
    value.string.subSequence(startIndex, endIndex)

  @Polyglot override fun toString(): String = asString

  override val asString: String get() = value.string
}

package elide.runtime.intrinsics.js.typed

import elide.runtime.gvm.internals.intrinsics.js.typed.UUIDValue
import elide.runtime.intrinsics.js.StringLike
import elide.runtime.intrinsics.js.err.ValueError
import java.util.UUID as JavaUUID

/**
 * # Typed UUID
 *
 * Describes the concept of a [StringLike] typed UUID, which is held in an optimized internal representation. UUIDs
 * behave as native strings just like URLs.
 */
public interface UUID: StringLike {
  /**
   * ## UUID Types
   *
   * Describes types of supported UUIDs.
   */
  public enum class UUIDType {
    /** UUIDv4 (random). */
    V4
  }

  /**
   * ## Typed UUID: Factory
   *
   * Describes the layout of static methods which are available to obtain (generate, parse) UUID values.
   */
  public interface Factory {
    /**
     * Generate a random UUIDv4.
     *
     * @return Generated UUID value (a [UUIDValue] compliant with [UUID]).
     */
    public fun random(): UUIDValue

    /**
     * Wrap an existing UUID from a string [value].
     *
     * The provided [value] is parsed as a UUIDv4; if parsing fails, [ValueError] is thrown, which surfaces to guest
     * code as applicable.
     *
     * @param value UUID string.
     * @return Parsed UUID value (a [UUIDValue] compliant with [UUID]).
     */
    @Throws(ValueError::class)
    public fun of(value: String): UUIDValue

    /**
     * Wrap an existing UUID from an already-structured [value].
     *
     * The provided [value] is re-used with a full copy of internal state, since it can safely be assumed to have been
     * checked during earlier construction.
     *
     * @param value Pre-validated UUID value.
     * @return Parsed UUID value (a [UUIDValue] compliant with [UUID]).
     * @see [UUIDValue.ValidUUID]
     */
    public fun of(value: UUIDValue): UUIDValue

    /**
     * Wrap an existing Java-originating UUID.
     *
     * The provided [value] is re-used with a re-materialized internal state, since Java UUIDs can be assumed to be
     * structurally valid.
     *
     * @param value Java UUID.
     * @return Parsed UUID value (a [UUIDValue] compliant with [UUID]).
     */
    public fun of(value: JavaUUID): UUIDValue
  }

  /**
   * Return the string representation of this UUID.
   */
  public val asString: String
}

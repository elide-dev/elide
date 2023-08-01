@file:Suppress("RedundantVisibilityModifier", "unused")

package elide.proto

import java.util.*
import elide.proto.api.data.DataModelStrategy

/**
 * TBD.
 */
public interface ElideProtocol {
  /** Known model implementation types. */
  public enum class ImplementationLibrary {
    /** Protocol Buffers main library. */
    PROTOBUF,

    /** Protocol Buffers lite library. */
    PROTOBUF_LITE,

    /** Flatbuffers library. */
    FLATBUFFERS,

    /** Pure-Kotlin via KotlinX Serialization. */
    KOTLINX,

    /** Any other implementation. */
    CUSTOM,
  }

  /** Enumerates supported types of serialization dialects. */
  public enum class DialectType {
    /**
     * Type: Internal.
     *
     * Assigned to serialization formats which are internal to their implementation or adapter. Formats which are not
     * designed to cross a network boundary may choose to declare this as their dialect type.
     */
    INTERNAL,

    /**
     * Type: Binary.
     *
     * The dialect uses raw binary data to engage in interchange. When selected, served or received data will be passed
     * to codec tools without modification or decoding steps.
     */
    BINARY,

    /**
     * Type: Text.
     *
     * The dialect uses text data to engage in interchange. By default, the character set is considered to be UTF-8, but
     * this can be adjusted by the implementation.
     */
    TEXT,
  }

  /** Enumerates known serialization dialects across all implementations. */
  public enum class Dialect constructor (private val type: DialectType) {
    /** Dialect: Unspecified or unknown. */
    UNSPECIFIED(type = DialectType.INTERNAL),

    /** Dialect: JSON. */
    JSON(type = DialectType.TEXT),

    /** Dialect: `msgpack`. */
    MSGPACK(type = DialectType.BINARY),

    /** Dialect: Protocol Buffers' binary format. */
    PROTO(type = DialectType.BINARY),

    /** Dialect: Flatbuffers' binary format. */
    FLATBUFFERS(type = DialectType.BINARY),
  }

  /** Describes the API interface expected for implementing adapters to models. */
  public interface ModelAdapterStrategy {
    /**
     * TBD.
     */
    fun model(): DataModelStrategy<*, *, *, *, *, *, *, *>
  }

  /** @return Whether this protocol engine supports reflective access. */
  public val reflection: Boolean get() = false

  /** @return Whether this protocol engine supports compressed data. */
  public val compression: Boolean get() = false

  /**
   * Returns the engine which implements the Elide Protocol for this class structure.
   *
   * @return Library engine.
   */
  public fun engine(): ImplementationLibrary

  /**
   * Returns the set of dialects supported by this engine.
   *
   * @return Supported serialization dialects.
   */
  public fun dialects(): EnumSet<Dialect>

  /**
   * Returns the base model class for this implementation, as applicable.
   *
   * @return Base model implementation.
   */
  public fun base(): Class<*>?

  /**
   * TBD.
   */
  public fun strategy(): ModelAdapterStrategy
}

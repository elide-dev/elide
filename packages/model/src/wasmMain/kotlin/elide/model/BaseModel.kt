@file:OptIn(
  InternalSerializationApi::class,
  ExperimentalSerializationApi::class,
)

package elide.model

import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer

/** Describes the expected interface for wire messages, usually implemented via Protocol Buffers on a given platform. */
public actual open class WireMessage {
  private lateinit var message: Any

  @Suppress("UNCHECKED_CAST")
  private val serializer: KSerializer<Any> = (message)::class.serializer() as KSerializer<Any>

  /**
   * Serialize this [WireMessage] instance into a raw [ByteArray], which is suitable for sending over the wire; formats
   * expressed via this interface must keep schema in sync on both sides.
   *
   * Binary serialization depends on platform but is typically implemented via Protocol Buffer messages. For schemaless
   * serialization, use Proto-JSON.
   *
   * @return Raw bytes of this message, in serialized form.
   */
  @OptIn(ExperimentalSerializationApi::class)
  public actual open fun toSerializedBytes(): ByteArray {
    return ProtoBuf.encodeToByteArray(
      serializer,
      message,
    )
  }

  /**
   * Return this [WireMessage] as a debug-friendly [String] representation, which emits property values and other info
   * descriptive to the current [WireMessage] instance.
   *
   * @return String-formatted [WireMessage] instance.
   */
  public actual open fun toSerializedString(): String {
    return this.message.toString()
  }

  /** @return Underlying [lib.protobuf.Message] object. */
  public fun getProto(): Any = message
}

/** Describes the expected interface for model objects which are reliably serializable into [WireMessage] instances. */
public actual interface AppModel<M : WireMessage> {
  // Nothing yet.
  /**
   * Translate the current [AppModel] into an equivalent [WireMessage] instance [M].
   *
   * @return lib.protobuf.Message instance corresponding to this model.
   */
  public actual fun toMessage(): M
}

/**
 * Describes the expected interface for model objects which are designated as records.
 *
 * Records, within the scope of an Elide application, are [AppModel] objects which comply with an extended set of API
 * guarantees:
 * - Like [AppModel] instances, [AppRecord]s must be paired to a [WireMessage] [M] (usually a protocol buffer message).
 * - Records must export some annotated field as their designated [id], which should resolve to a stable type [K].
 * - Where applicable, records must export their [parentId], which should resolve to a matching key type [K].
 * - Where applicable, records must export an annotated [displayName] field for use in form UIs and so forth.
 *
 * Additional extensions of the [AppRecord] type form supersets of these guarantees:
 * - [StampedRecord] instances carry [StampedRecord.createdAt] and [StampedRecord.updatedAt] timestamps.
 * - [VersionedRecord] instances carry a [VersionedRecord.version] property for optimistic concurrency control.
 *
 * Generally speaking, [AppModel] instances correspond with objects which are serialized and exchanged by an application
 * but are not always addressable or persistent. [AppRecord] objects are expected to be identified (perhaps with type
 * annotations), and typically correspond to database records which need CRUD-like operations.
 */
public actual interface AppRecord<K, M : WireMessage> {
  /** @return Assigned ID (of type [K]) for this record, or `null` if no ID has been assigned at this time. */
  public actual fun id(): K? {
    return null
  }

  /** @return Assigned parent ID (of type [K]) for this record, or `null` if no ID is applicable or assigned. */
  public actual fun parentId(): K? {
    return null
  }

  /** @return Display name for this record, if applicable/available, otherwise, `null`. */
  public actual fun displayName(): String? {
    return null
  }
}

/**
 * Describes the expected interface for model records which carry designated create/update timestamps.
 *
 * Stamped records extend the base [AppRecord] interface with the [createdAt] and [updatedAt] timestamp fields. These
 * fields are typically provided by the database or the application runtime, and don't need to be set explicitly by the
 * developer, although explicitly set values do override automatic values.
 */
public actual interface StampedRecord<K, M : WireMessage> : AppRecord<K, M> {
  /** @return Created-at timestamp for this record, or `null` if the record has not yet been persisted. */
  public actual fun createdAt(): Instant? {
    TODO("not yet implemented")
  }

  /** @return Updated-at timestamp for this record, or `null` if the record has not yet been persisted. */
  public actual fun updatedAt(): Instant? {
    TODO("not yet implemented")
  }
}

/**
 * Describes the expected interface for model records which are versioned.
 *
 * Versioned records extend the base [StampedRecord] interface with a [version] property which increments with each
 * update to the associated entity. The Micronaut Data layer will enforce optimistic concurrency when persisting records
 * which inherit from this interface and provide a valid version value.
 */
public actual interface VersionedRecord<K, M : WireMessage> : StampedRecord<K, M> {
  /** @return Version number assigned to this instance, within the scope of [id], or `-1` if no version is present. */
  public actual fun version(): Long {
    TODO("not yet implemented")
  }
}

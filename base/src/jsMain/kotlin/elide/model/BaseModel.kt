package elide.model

import elide.runtime.js.toByteArray


/** Describes the expected interface for wire messages, usually implemented via Protocol Buffers on a given platform. */
actual open class WireMessage {
  lateinit var message: lib.protobuf.Message

  /**
   * Serialize this [WireMessage] instance into a raw [ByteArray], which is suitable for sending over the wire; formats
   * expressed via this interface must keep schema in sync on both sides.
   *
   * Binary serialization depends on platform but is typically implemented via Protocol Buffer messages. For schemaless
   * serialization, use Proto-JSON.
   *
   * @return Raw bytes of this message, in serialized form.
   */
  actual open fun toSerializedBytes(): ByteArray {
    return this.message.serializeBinary().buffer.toByteArray()
  }

  /**
   * Return this [WireMessage] as a debug-friendly [String] representation, which emits property values and other info
   * descriptive to the current [WireMessage] instance.
   *
   * @return String-formatted [WireMessage] instance.
   */
  actual open fun toSerializedString(): String {
    return this.message.toString()
  }

  /** @return Underlying [lib.protobuf.Message] object. */
  fun getProto(): lib.protobuf.Message {
    return message
  }
}


/** Describes the expected interface for model objects which are reliably serializable into [WireMessage] instances. */
actual interface AppModel<M: WireMessage> {
  // Nothing yet.
  /**
   * Translate the current [AppModel] into an equivalent [WireMessage] instance [M].
   *
   * @return lib.protobuf.Message instance corresponding to this model.
   */
  actual fun toMessage(): M
}

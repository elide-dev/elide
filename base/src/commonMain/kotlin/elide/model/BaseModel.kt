package elide.model


/** Describes the expected interface for wire messages, usually implemented via Protocol Buffers on a given platform. */
expect open class WireMessage {
  /**
   * Serialize this [WireMessage] instance into a raw [ByteArray], which is suitable for sending over the wire; formats
   * expressed via this interface must keep schema in sync on both sides.
   *
   * Binary serialization depends on platform but is typically implemented via Protocol Buffer messages. For schemaless
   * serialization, use Proto-JSON.
   *
   * @return Raw bytes of this message, in serialized form.
   */
  open fun toSerializedBytes(): ByteArray

  /**
   * Return this [WireMessage] as a debug-friendly [String] representation, which emits property values and other info
   * descriptive to the current [WireMessage] instance.
   *
   * @return String-formatted [WireMessage] instance.
   */
  open fun toSerializedString(): String
}


/** Describes the expected interface for model objects which are reliably serializable into [WireMessage] instances. */
expect interface AppModel<M: WireMessage> {
  /**
   * Translate the current [AppModel] into an equivalent [WireMessage] instance [M].
   *
   * @return Message instance corresponding to this model.
   */
  fun toMessage(): M
}

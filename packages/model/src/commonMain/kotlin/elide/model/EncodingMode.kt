package elide.model

/** Supported encoding modes for application models. */
public enum class EncodingMode {
  /** Length-prefixed binary encoding. Efficient but requires stateful schema on both sides. */
  BINARY,

  /** JSON-based encoding, via ProtoJSON on the JVM and regular JSON support in JS. */
  JSON
}

package elide.core.encoding

/** Enumerates supported encodings and binds [Encoder] instances to each. */
public enum class Encoding {
  /** Hex encoding. */
  HEX,

  /** Base64 encoding. */
  BASE64,

  /** Plain UTF-8 encoding. */
  UTF_8,

  /** UTF-16 encoding. */
  UTF_16,

  /** UTF-32 encoding. */
  UTF_32
}

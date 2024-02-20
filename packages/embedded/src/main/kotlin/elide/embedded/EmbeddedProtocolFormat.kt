package elide.embedded

/** The serialization format used for the invocation protocol. */
public enum class EmbeddedProtocolFormat {
  /** Selects Protobuf as the binary exchange format for the invocation protocol. */
  PROTOBUF,

  /** Selects Cap'n'Proto as the binary exchange format for the invocation protocol. */
  CAPNPROTO,
}
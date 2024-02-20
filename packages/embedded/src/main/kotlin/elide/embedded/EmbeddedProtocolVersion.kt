package elide.embedded

/**
 * Enumerates the versions of the invocation protocol and the serialized data structures used for its operations.
 *
 * Note that while the binary exchange [formats][EmbeddedProtocolFormat] used by the runtime typically support
 * backwards-compatibility, using a matching version is recommended to enable the latest runtime features.
 */
public enum class EmbeddedProtocolVersion {
  /** Selects version 1.0 of the dispatch protocol. */
  V1_0,
}
package elide.server.assets

/** Enumerates known kinds of registered application assets. */
public enum class AssetType {
  /** Generic assets which employ custom configuration. */
  GENERIC,

  /** Plain text assets. */
  TEXT,

  /** JavaScript assets. */
  SCRIPT,

  /** Stylesheet assets. */
  STYLESHEET,
}

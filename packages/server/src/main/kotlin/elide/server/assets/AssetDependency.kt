package elide.server.assets

/** Model for a dependency relationship between assets. */
internal data class AssetDependency(
  val optional: Boolean = false,
)

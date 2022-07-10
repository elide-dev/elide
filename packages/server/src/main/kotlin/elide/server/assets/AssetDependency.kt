package elide.server.assets

/** Model for a dependency relationship between assets. */
internal data class AssetDependency(
  val depender: String,
  val dependee: String,
  val optional: Boolean = false,
) {
  init {
    require(depender != dependee) {
      "Asset cannot depend on itself"
    }
  }
}

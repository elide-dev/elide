package elide.server.assets

/**
 * Model for a dependency relationship between assets.
 *
 * @param depender Module which is establishing the dependency.
 * @param dependee Module which is being depended on.
 * @param optional Whether the dependency is optional. Defaults to `false`.
 */
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

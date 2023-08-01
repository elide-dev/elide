package elide.server.http

import elide.server.assets.AssetManager

/** Effective namespace for request context values and objects. */
public object RequestContext {
  /**
   * Defines a known key within the context payload of a request.
   *
   * @param name Name associated with this key.
   */
  public data class Key(
    public val name: String,
  ) {
    /** Keys where request context may be accessed. */
    public companion object {
      /** Key for accessing the [AssetManager]. */
      public val ASSET_MANAGER: Key = Key("elide.assetManager")
    }
  }
}

@file:Suppress("unused")

package elide.server

import java.util.*
import kotlinx.html.*
import elide.server.assets.AssetReference

// DOM type for JavaScript files.
private const val JAVASCRIPT_TYPE = "application/javascript"

/** Generate a stylesheet link from an embedded server [asset], optionally with the provided [media] declaration. */
public fun HEAD.stylesheet(
  asset: AssetReference,
  media: String? = null,
  attrs: SortedMap<String, String>? = null
): Unit = LINK(
  attributesMapOf(
    "rel",
    "stylesheet",
    "href",
    asset.href,
  ).plus(
    attrs ?: emptyMap()
  ),
  consumer
).visit {
  if (media?.isNotBlank() == true) {
    this.media = media
  }
}

/** Generates a CSS link from the provided handler [uri], optionally including the specified [attrs]. */
public fun HEAD.stylesheet(uri: String, media: String? = null, attrs: Map<String, String>? = null): Unit = LINK(
  attributesMapOf(
    "rel",
    "stylesheet",
    "href",
    uri
  ).plus(
    attrs ?: emptyMap()
  ),
  consumer
).visit {
  if (media?.isNotBlank() == true) {
    this.media = media
  }
}

/** Generates a `<head>` script link from the provided [asset], optionally including the specified [attrs], etc. */
public fun HEAD.script(
  asset: AssetReference,
  defer: Boolean = false,
  async: Boolean = false,
  nomodule: Boolean = false,
  type: String = JAVASCRIPT_TYPE,
  attrs: Map<String, String>? = null,
): Unit = SCRIPT(
  attributesMapOf(
    "type",
    type,
    "src",
    asset.href,
  ).plus(
    attrs ?: emptyMap()
  ),
  consumer
).visit {
  if (defer) this.defer = true
  if (async) this.async = true
  if (nomodule) this.attributes["nomodule"] = ""
}

/** Generates a `<head>` script link from the provided handler [uri], optionally including the specified [attrs]. */
public fun HEAD.script(
  uri: String,
  defer: Boolean = false,
  async: Boolean = false,
  type: String = JAVASCRIPT_TYPE,
  attrs: Map<String, String>? = null,
): Unit = SCRIPT(
  attributesMapOf(
    "type",
    type,
    "src",
    uri
  ).plus(
    attrs ?: emptyMap()
  ),
  consumer
).visit {
  if (defer) this.defer = true
  if (async) this.async = true
}

/** Generates a `<body>` script link from the provided handler [uri], optionally including the specified [attrs]. */
public fun BODY.script(
  uri: String,
  defer: Boolean = false,
  async: Boolean = false,
  type: String = JAVASCRIPT_TYPE,
  attrs: Map<String, String>? = null,
): Unit = SCRIPT(
  attributesMapOf(
    "type",
    type,
    "src",
    uri
  ).plus(
    attrs ?: emptyMap()
  ),
  consumer
).visit {
  if (defer) this.defer = true
  if (async) this.async = true
}

/** Generates a `<body>` script link from the provided [asset], optionally including the specified [attrs], etc. */
public fun BODY.script(
  asset: AssetReference,
  defer: Boolean = false,
  async: Boolean = false,
  type: String = JAVASCRIPT_TYPE,
  attrs: Map<String, String>? = null,
): Unit = SCRIPT(
  attributesMapOf(
    "type",
    type,
    "src",
    asset.href,
  ).plus(
    attrs ?: emptyMap()
  ),
  consumer
).visit {
  if (defer) this.defer = true
  if (async) this.async = true
}

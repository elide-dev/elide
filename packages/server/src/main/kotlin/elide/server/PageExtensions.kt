@file:Suppress("NOTHING_TO_INLINE", "unused")

package elide.server

import kotlinx.html.*


/** Generates a CSS link from the provided handler [uri], optionally including the specified [attrs]. */
inline fun HEAD.stylesheet(uri: String, media: String? = null, attrs: Map<String, String>? = null): Unit = LINK(
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

/** Generates a `<head>` script link from the provided handler [uri], optionally including the specified [attrs]. */
inline fun HEAD.script(
  uri: String,
  defer: Boolean = false,
  async: Boolean = false,
  type: String = "application/javascript",
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
inline fun BODY.script(
  uri: String,
  defer: Boolean = false,
  async: Boolean = false,
  type: String = "application/javascript",
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

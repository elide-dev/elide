@file:Suppress("NOTHING_TO_INLINE")

package elide.server

import io.micronaut.http.annotation.Get
import kotlinx.html.*
import kotlin.reflect.KFunction0


fun resolveCallableUri(callable: KFunction0<*>): String {
  return (callable.annotations.find {
    it.annotationClass == Get::class
  } as? Get).let {
    if (it != null) {
      arrayOf(
        it.value,
        it.uri,
        if (it.uris.isNotEmpty()) {
          it.uris.first()
        } else {
          ""
        }
      ).find { subjectUri ->
        @Suppress("SENSELESS_COMPARISON")
        subjectUri != null && subjectUri.isNotBlank() && subjectUri != "/"
      }
    } else {
      throw IllegalStateException(
        "Failed to resolve GET annotation for style endpoint `${callable.name}`"
      )
    }
  }!!
}


/** Generate a CSS link from the provided handler [callable], optionally including the specified [attrs]. */
inline fun HEAD.stylesheet(callable: KFunction0<*>, media: String? = null, attrs: Map<String, String>? = null): Unit =
  LINK(
    attributesMapOf(
      "rel",
      "stylesheet",
      "href",
      resolveCallableUri(callable),
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

/** Generate a `<head>` script link from the provided handler [callable], optionally including the specified [attrs]. */
inline fun HEAD.script(
  callable: KFunction0<*>,
  defer: Boolean = false,
  async: Boolean = false,
  type: String = "application/javascript",
  attrs: Map<String, String>? = null
): Unit = SCRIPT(
    attributesMapOf(
      "type",
      type,
      "src",
      resolveCallableUri(callable),
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

/** Generate a `<body>` script link from the provided handler [callable], optionally including the specified [attrs]. */
inline fun BODY.script(
  callable: KFunction0<*>,
  defer: Boolean = false,
  async: Boolean = false,
  type: String = "application/javascript",
  attrs: Map<String, String>? = null
): Unit = SCRIPT(
  attributesMapOf(
    "type",
    type,
    "src",
    resolveCallableUri(callable),
  ).plus(
    attrs ?: emptyMap()
  ),
  consumer
).visit {
  if (defer) this.defer = true
  if (async) this.async = true
}

@file:Suppress("NOTHING_TO_INLINE")

package elide.server

import io.micronaut.http.annotation.Get
import kotlinx.html.HEAD
import kotlinx.html.LINK
import kotlinx.html.attributesMapOf
import kotlinx.html.visit
import kotlin.reflect.KFunction0


/** Generate a CSS link from the provided handler [callable], optionally including the specified [attrs]. */
inline fun HEAD.stylesheet(callable: KFunction0<*>, attrs: Map<String, String>? = null): Unit =
  LINK(
    attributesMapOf(
      "rel",
      "stylesheet",
      "href",
      (callable.annotations.find {
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
      }
    ).plus(
      attrs ?: emptyMap()
    ),
    consumer
  ).visit {
    // No modifications at this time.
  }

/** Generate a CSS link from the provided handler [uri], optionally including the specified [attrs]. */
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

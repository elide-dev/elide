@file:OptIn(ExperimentalJsExport::class)
@file:Suppress("NON_EXPORTABLE_TYPE")

import elide.runtime.ssr.*
import elide.frontend.ssr.*
import js.core.jso
import elide.site.ui.ElideSite as App
import elide.site.ui.components.ThemeModuleServer
import react.Fragment
import react.create
import react.router.dom.server.StaticRouter
import emotion.react.CacheProvider
import emotion.cache.EmotionCache
import emotion.cache.createCache
import emotion.server.createEmotionServer
import js.core.Object
import org.w3c.fetch.Request
import react.ReactElement
import web.url.URL

const val enableStreaming = true
const val chunkCss = true

// Setup cache.
private fun setupCache(): EmotionCache {
  return createCache(jso {
    key = "css"
  })
}

/** App entrypoint fragment. */
val app: SSRContext<AppProps>.(EmotionCache) -> ReactElement<*> = { emotionCache ->
  Fragment.create {
    StaticRouter {
      val currentPage = state?.page ?: "home"
      val url: URL = when (val url = request?.url) {
        null -> URL("https://elide.dev")
        else -> when {
          url.startsWith("/") -> URL("https://elide.dev$url")
          else -> URL(url)
        }
      }

      // route to requested page
      location = url.pathname.ifBlank { "/" }

      CacheProvider(emotionCache) {
        ThemeModuleServer {
          App {
            page = currentPage
          }
        }
      }
    }
  }
}

val emotionCache: EmotionCache = setupCache()
val emotionServer = createEmotionServer(emotionCache)

/** @return Streaming SSR entrypoint for React. */
@JsExport fun render(request: Request, context: dynamic, responder: RenderCallback): dynamic {
  return SSRContext.typed<AppProps>(context, request).execute {
    try {
      return@execute ApplicationBuffer(app.invoke(this, emotionCache), stream = enableStreaming).execute {
        try {
          if (enableStreaming && chunkCss && (it.status != null && it.status != -1)) {
            // in the final chunk, splice in CSS from Emotion.
            val emotionChunks = emotionServer.extractCriticalToChunks(it.content ?: "")
            val emotionCss = emotionServer.constructStyleTagsFromChunks(emotionChunks)
            responder(jso {
              css = emotionCss
            })
          }
          responder(it)

        } catch (err: Throwable) {
          console.error("Failed to dispatch callback: ", err)
          throw err
        }
      }
    } catch (err: Throwable) {
      console.error("Failed to render stream: ", err)
      throw err
    }
  }
}

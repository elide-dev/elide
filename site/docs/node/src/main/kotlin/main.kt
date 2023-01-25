@file:OptIn(ExperimentalJsExport::class)
@file:Suppress("NON_EXPORTABLE_TYPE")

import elide.runtime.ssr.*
import elide.frontend.ssr.*
import elide.site.pages.Home
import js.core.jso
import elide.site.ui.ElideSite as App
import elide.site.ui.components.ThemeModuleServer
import react.Fragment
import react.create
import react.router.dom.server.StaticRouter
import emotion.react.CacheProvider
import emotion.cache.EmotionCache
import emotion.cache.createCache
import emotion.server.worker.EmotionServer
import emotion.server.worker.createEmotionServer
import js.core.Object
import js.json.JSON
import org.w3c.fetch.Request
import react.ReactElement
import web.url.URL

const val enableStreaming = true
const val chunkCss = true

// Setup Emotion cache.
private fun setupCache(context: dynamic): EmotionCache {
  val statejson = JSON.stringify(context, emptyArray(), 0)
  val cspNonce = context.getNonce()
  console.info("Setting Emotion cache nonce: \"$cspNonce\"")
  return createCache(jso {
    key = "es"
    nonce = cspNonce
  })
}

/** App entrypoint fragment. */
val app: SSRContext<AppProps>.(Request, EmotionCache) -> ReactElement<*> = { _, emotionCache ->
  val url: URL = when (val url = request?.url) {
    null -> URL("https://elide.dev")
    else -> when {
      url.startsWith("/") -> URL("https://elide.dev$url")
      else -> URL(url)
    }
  }

  val currentPageName = state?.page ?: "home"
  val currentPage = elide.site.ElideSite.pages.find {
    it.name == currentPageName || it.path == url.pathname
  }

  Fragment.create {
    CacheProvider(emotionCache) {
      StaticRouter {
        // route to requested page
        location = url.pathname.ifBlank { "/" }

        ThemeModuleServer {
          App {
            page = currentPage?.name
          }
        }
      }
    }
  }
}

var modEmotionCache: EmotionCache? = null
var modEmotionServer: EmotionServer? = null

/** @return Streaming SSR entrypoint for React. */
@JsExport fun render(request: Request, context: dynamic, responder: RenderCallback): dynamic {
  var response = ""

  // initialize emotion cache
  val (emotionServer, emotionCache) = if (modEmotionCache == null) {
    val cache = setupCache(context)
    modEmotionCache = cache
    val server = createEmotionServer(cache)
    modEmotionServer = server
    server to cache
  } else {
    modEmotionServer!! to modEmotionCache!!
  }

  return SSRContext.typed<AppProps>(context, request).execute {
    try {
      return@execute ApplicationBuffer(app.invoke(
        this,
        request,
        emotionCache,
      ), stream = enableStreaming).execute {
        try {
          if (it.hasContent) {
            response += it.content
          }

          if (enableStreaming && chunkCss && it.fin) {
            // in the final chunk, splice in CSS from Emotion.
            val emotionChunks = emotionServer.extractCriticalToChunks(response)
            val emotionCss = emotionServer.constructStyleTagsFromChunks(emotionChunks)
            responder(jso {
              css = emotionCss
            })
          }

          // unfortunately, has to buffer until finish because of emotion >:|
          if (it.fin) {
            responder(jso {
              content = response
              hasContent = true
              fin = true
              status = it.status
              headers = it.headers
            })
          }

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

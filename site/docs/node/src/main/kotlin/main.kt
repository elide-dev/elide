@file:OptIn(ExperimentalJsExport::class)
@file:Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")

import elide.frontend.ssr.*
import js.objects.jso
import elide.site.ui.ElideSite as App
import elide.site.ui.components.ThemeModuleServer
import react.Fragment
import react.create
import react.router.dom.server.StaticRouter
import emotion.react.CacheProvider
import emotion.cache.createCache
import emotion.server.worker.EmotionServer
import emotion.server.worker.createEmotionServer
import emotion.utils.EmotionCache
import org.w3c.fetch.Request
import react.ReactElement
import web.url.URL


// Setup Emotion cache.
private fun setupCache(context: dynamic): EmotionCache {
  val cspNonce = context.getNonce()
  return createCache(jso {
    key = "es"
    nonce = cspNonce
  }) as EmotionCache
}

/** App entrypoint fragment. */
val app: SSRContext<AppProps>.(Request, EmotionCache) -> ReactElement<*> = { _, emotionCache ->
  val url: URL = when (val url = request?.url) {
    null -> URL("https://elide.dev")
    else -> when {
      url.startsWith("/") -> URL("https://elide.dev$url")
      else -> URL("https://elide.dev")  // @TODO(sgammon): relative URL parsing fix
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
      ), stream = true).execute {
        try {
          if (it.hasContent) {
            response += it.content
          }

          if (it.fin) {
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

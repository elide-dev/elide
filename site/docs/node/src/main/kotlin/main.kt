@file:OptIn(ExperimentalJsExport::class)
@file:Suppress("NON_EXPORTABLE_TYPE")

import elide.runtime.ssr.*
import elide.frontend.ssr.*
import js.core.jso
import elide.runtime.gvm.entrypoint
import elide.site.ElideSite
import elide.site.ui.ElideSite as App
import elide.site.ui.components.ThemeModuleServer
import react.Fragment
import react.create
import react.dom.server.rawRenderToString as renderSSR
import react.router.dom.server.StaticRouter
import emotion.react.CacheProvider
import emotion.cache.EmotionCache
import emotion.cache.createCache
import emotion.server.createEmotionServer
import react.ReactElement
import kotlin.js.Promise

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
      val currentPage = state?.page
      val target = path ?: when {
        currentPage?.isNotEmpty() == true -> ElideSite.pages.find {
          it.name == currentPage
        }?.path

        else -> null
      } ?: "/"

      // route to requested page
      location = target

      CacheProvider(emotionCache) {
        ThemeModuleServer {
          App {
            page = state?.page
          }
        }
      }
    }
  }
}

private fun dispatchRaw(callback: RenderCallback, chunk: ServerResponse) {
  val plain: dynamic = jso {
    fin = chunk.fin
    content = chunk.content
    hasContent = chunk.hasContent

    if (chunk.status != null) {
      status = chunk.status
    }
    if (chunk.headers?.isNotEmpty() == true) {
      headers = chunk.headers
    }
  }
  callback.invoke(plain.unsafeCast<ServerResponse>())
}

/** @return Streaming SSR entrypoint for React. */
@JsExport fun renderStream(callback: RenderCallback, context: dynamic = null): Promise<*> = entrypoint {
  SSRContext.typed<AppProps>(context).execute {
    val emotionCache: EmotionCache = setupCache()
    val emotionServer = createEmotionServer(emotionCache)

    try {
      ApplicationBuffer(app.invoke(this, emotionCache)).execute {
        try {
          if (it.status != null && it.status != -1) {
            // in the final chunk, splice in CSS from Emotion.
            val emotionChunks = emotionServer.extractCriticalToChunks(it.content)
            val emotionCss = emotionServer.constructStyleTagsFromChunks(emotionChunks)
            console.log("would emit css: $emotionCss")
          }
          dispatchRaw(callback, it)
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

/** @return String-rendered SSR content from React. */
@OptIn(ExperimentalJsExport::class)
@JsExport fun renderContent(context: dynamic = null): String = entrypoint {
  return@entrypoint SSRContext.typed<AppProps>(context).execute {
    val emotionCache: EmotionCache = setupCache()
    renderSSR(app.invoke(this, emotionCache))
  }
}

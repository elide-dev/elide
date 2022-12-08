@file:OptIn(ExperimentalJsExport::class, DelicateCoroutinesApi::class)

import js.core.jso
import elide.frontend.ssr.SSRContext
import elide.runtime.gvm.entrypoint
import elide.site.ui.ElideSite
import elide.site.ui.components.ThemeModuleServer
import react.Fragment
import react.Props
import react.create
import react.dom.server.rawRenderToString as renderSSR
import react.dom.server.renderToReadableStream as renderSSRStreaming
import react.router.dom.server.StaticRouter
import emotion.react.CacheProvider
import emotion.cache.EmotionCache
import emotion.cache.createCache
import emotion.server.createEmotionServer
import kotlinx.coroutines.*
import react.ReactElement
import kotlin.coroutines.CoroutineContext

/** Props shared with the server. */
external interface AppProps : Props {
  /** @return `page` context value from the server. */
  fun getPage(): String?
}

// Setup cache.
private fun setupCache(): EmotionCache {
  return createCache(jso {
    key = "css"
  })
}

/** App entrypoint fragment. */
val app: (EmotionCache, AppProps?) -> ReactElement<*> = { emotionCache, state ->
  Fragment.create {
    StaticRouter {
      CacheProvider(emotionCache) {
        ThemeModuleServer {
          ElideSite {
            page = state?.getPage()
          }
        }
      }
    }
  }
}

/** @return Streaming SSR entrypoint for React. */
@OptIn(ExperimentalJsExport::class)
@JsExport fun renderStream(context: dynamic = null, callback: RenderCallback): Unit = entrypoint {
  GlobalScope.launch {
    SSRContext.typed<AppProps>(context).execute {
      val emotionCache: EmotionCache = setupCache()
      val emotionServer = createEmotionServer(emotionCache)
      val html = ApplicationBuffer(app.invoke(emotionCache, this.state)).execute().extract()
      val emotionChunks = emotionServer.extractCriticalToChunks(html)
      val emotionCss = emotionServer.constructStyleTagsFromChunks(emotionChunks)

      callback.invoke(RenderedStream(
        status = 200,
        html = html,
        criticalCss = emotionCss,
        styleChunks = emotionChunks.styles.map {
          CssChunk(
            ids = it.ids,
            key = it.key,
            css = it.css,
          )
        }.toTypedArray(),
      ))
    }
  }
}

/** @return String-rendered SSR content from React. */
@OptIn(ExperimentalJsExport::class)
@JsExport fun renderContent(context: dynamic = null): String = entrypoint {
  return@entrypoint SSRContext.typed<AppProps>(context).execute {
    val emotionCache: EmotionCache = setupCache()
    renderSSR(app.invoke(emotionCache, null))
  }
}

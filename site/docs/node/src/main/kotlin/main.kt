
import js.core.jso
import elide.frontend.ssr.SSRContext
import elide.runtime.gvm.entrypoint
import elide.site.ui.ElideSite
import elide.site.ui.components.ThemeModuleServer
import react.Fragment
import react.Props
import react.create
import react.dom.server.rawRenderToString
import react.router.dom.server.StaticRouter
import emotion.react.CacheProvider
import emotion.cache.EmotionCache
import emotion.cache.createCache
import emotion.server.createEmotionServer
import react.FC

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

/** @return String-rendered SSR content from React. */
@OptIn(ExperimentalJsExport::class)
@JsExport fun renderContent(context: dynamic = null): String = entrypoint {
  return@entrypoint SSRContext.typed<AppProps>(context).execute {
    val emotionCache: EmotionCache = setupCache()
    val emotionServer = createEmotionServer(emotionCache)

    val html = rawRenderToString(Fragment.create {
      StaticRouter {
        CacheProvider(emotionCache) {
         ThemeModuleServer {
            ElideSite {
              page = state?.getPage()
            }
          }
        }
      }
    })

    val emotionChunks = emotionServer.extractCriticalToChunks(html)
    val emotionCss = emotionServer.constructStyleTagsFromChunks(emotionChunks)

    print("WOULD RENDER CSS: $emotionCss")  // @TODO(sgammon): multiple return values
    html
  }
}

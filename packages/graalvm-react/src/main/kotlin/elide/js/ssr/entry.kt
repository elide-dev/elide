package elide.js.ssr

import kotlinx.browser.document
import kotlinx.js.jso
import react.Props
import react.ReactElement
import react.dom.client.createRoot
import react.dom.client.hydrateRoot

const val SSR_FLAG = "data-serving-mode"
const val SERVING_MODE_SSR = "ssr"
const val SSR_DATA_ID = "ssr-data"
const val RENDER_ROOT_ID = "root"
const val DEFAULT_EL_TYPE = "div"

/**
 * TBD
 */
private fun <AppProps : Props> renderApp(fragment: ReactElement<*>, props: AppProps?) {
  // resolve the target root and container, or create/mount them
  val target = document.getElementById(RENDER_ROOT_ID)
  val container = if (target == null) {
    val c = document.createElement(DEFAULT_EL_TYPE)
    document.body!!.appendChild(c)
    c
  } else {
    target
  }

  // call into the hydrate or create-root methods based on SSR mode
  if (container.hasAttribute(SSR_FLAG) && container.getAttribute(SSR_FLAG) == SERVING_MODE_SSR) {
    console.log("Hydrating DOM (SSR active)", props)
    hydrateRoot(
      container,
      fragment
    )
  } else {
    console.log("Rendering client-side", props)
    createRoot(
      container
    ).render(
      fragment
    )
  }
}

/**
 * TBD
 */
private fun <AppProps : Props> resolveProps(): AppProps? {
  val dataElement = document.getElementById(SSR_DATA_ID)
  if (dataElement != null) {
    val type = dataElement.getAttribute("type")
    val textContent = dataElement.textContent
    if (type == "application/json" && textContent?.isNotBlank() == true) {
      // decode and return injected application properties
      return JSON.parse(textContent) as AppProps
    }
  }
  return null  // failed to decode, or failed to locate
}

/**
 * TBD
 */
public fun <AppProps : Props> boot(init: (AppProps?) -> ReactElement<*>) {
  // resolve application props, if any
  val appProps = resolveProps<AppProps>()

  // factory the application and render it
  renderApp(init.invoke(appProps), appProps)
}

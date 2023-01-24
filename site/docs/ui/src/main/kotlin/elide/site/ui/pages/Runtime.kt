package elide.site.ui.pages

import elide.site.ui.components.FullbleedPage
import elide.site.ui.pages.runtime.mdx.RuntimeMdx

/** Renders the Runtime page on the Elide website. */
val Runtime = react.FC<react.Props> {
  FullbleedPage {
    heading = "Elide as a Runtime"
    component = RuntimeMdx
  }
}

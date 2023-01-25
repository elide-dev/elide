package elide.site.ui.pages

import elide.site.ui.components.FullbleedPage
import elide.site.ui.pages.architecture.mdx.ArchitectureMdx

/** Renders the top-level Architecture page on the Elide site. */
val Architecture = react.FC<react.Props> {
  FullbleedPage {
    heading = "Architecture"
    component = ArchitectureMdx
  }
}

package elide.site.ui.pages.tooling

import elide.site.ui.components.FullbleedPage
import elide.site.ui.pages.tooling.mdx.GradleMdx

/** Renders the Gradle tooling page on the Elide site. */
val Gradle = react.FC<react.Props> {
  FullbleedPage {
    heading = "Using Elide with Gradle"
    component = GradleMdx
  }
}

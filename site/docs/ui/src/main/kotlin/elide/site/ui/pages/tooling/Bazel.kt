package elide.site.ui.pages.tooling

import elide.site.ui.components.FullbleedPage
import elide.site.ui.pages.tooling.mdx.BazelMdx

/** Renders the Bazel tooling page on the Elide site. */
val Bazel = react.FC<react.Props> {
  FullbleedPage {
    heading = "Using Elide with Bazel"
    component = BazelMdx
  }
}

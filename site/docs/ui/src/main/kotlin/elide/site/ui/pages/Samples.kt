package elide.site.ui.pages

import elide.site.ui.components.FullbleedPage
import elide.site.ui.pages.samples.mdx.SamplesMdx

/** Renders the Code Samples page for the Elide website. */
val Samples = react.FC<react.Props> {
  FullbleedPage {
    heading = "Code Samples"
    component = SamplesMdx
  }
}

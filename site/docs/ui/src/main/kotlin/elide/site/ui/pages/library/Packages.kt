package elide.site.ui.pages.library

import elide.site.ui.components.FullbleedPage
import elide.site.ui.pages.library.mdx.PackagesMdx

/** Renders the library packages page on the Elide site. */
val Packages = react.FC<react.Props> {
  FullbleedPage {
    heading = "Elide as a Framework"
    component = PackagesMdx
  }
}

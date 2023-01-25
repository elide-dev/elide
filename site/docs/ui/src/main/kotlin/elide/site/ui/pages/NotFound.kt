package elide.site.ui.pages

import elide.site.ui.components.FullbleedPage

/** Renders a not-found page. */
val NotFound = react.FC<react.Props> {
  FullbleedPage {
    heading = "Woops!"
    +"That page couldn't be located."
  }
}

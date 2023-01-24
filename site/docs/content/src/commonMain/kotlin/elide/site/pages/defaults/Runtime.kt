package elide.site.pages.defaults

import elide.site.abstract.SitePage

/** Describes defaults for the Elide runtime page. */
object Runtime : SitePage(
  name = "runtime",
  label = "As a Runtime",
  path = "/runtime",
  title = "Elide as a Runtime",
  hidden = true,
)

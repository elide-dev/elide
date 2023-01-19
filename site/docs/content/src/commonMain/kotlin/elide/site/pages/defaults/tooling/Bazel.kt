package elide.site.pages.defaults.tooling

import elide.site.abstract.SitePage
import elide.site.pages.defaults.Tooling

/** Describes the tooling page for Elide's Bazel support. */
object Bazel : SitePage(
  name = "bazel",
  label = "Bazel",
  path = "/tools/bazel",
  title = "Bazel Tooling",
  parent = Tooling,
)

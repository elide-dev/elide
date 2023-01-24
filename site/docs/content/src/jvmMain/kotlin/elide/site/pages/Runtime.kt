package elide.site.pages

import elide.site.I18nPage
import elide.site.abstract.SitePage
import java.net.URI
import java.util.*
import elide.site.pages.defaults.Runtime as Defaults

/** Describes the Elide runtime intro top-level page. */
actual object Runtime : SitePage(
  name = Defaults.name,
  label = Defaults.label,
  path = Defaults.path,
  title = Defaults.title,
  hidden = Defaults.hidden,
), I18nPage {
  // Internal: Internationalized page bundle.
  private val bundle = ResourceBundle.getBundle("ElideSite_Runtime")

  override fun bundle(locale: Locale): ResourceBundle = bundle

  override fun canonical(locale: Locale): URI = URI.create("https://elide.dev/runtime")

  override fun keywords(locale: Locale): Array<String> = arrayOf(
    "elide",
    "runtime",
    "framework",
    "jvm",
    "native",
    "kotlin",
    "cli",
    "tool",
    "server",
    "development",
    "code",
    "multiplatform",
    "react",
    "js",
    "javascript",
    "ssr",
    "node",
    "deno",
    "workers",
  )
}

package elide.site.pages

import elide.site.I18nPage
import elide.site.abstract.SitePage
import java.net.URI
import java.util.*
import elide.site.pages.defaults.Runtime as Defaults

/** Describes the Elide library intro top-level page. */
actual object Library : SitePage(
  name = Defaults.name,
  label = Defaults.label,
  path = Defaults.path,
  title = Defaults.title,
  hidden = Defaults.hidden,
), I18nPage {
  // Internal: Internationalized page bundle.
  private val bundle = ResourceBundle.getBundle("ElideSite_Library")

  override fun canonical(locale: Locale): URI = URI.create("https://elide.dev/library")

  override fun keywords(locale: Locale): Array<String> = arrayOf(
    "elide",
    "library",
    "framework",
    "java",
    "jvm",
    "kotlin",
    "packages",
    "maven",
    "gradle",
    "server",
    "development",
    "code",
    "multiplatform",
    "react",
    "javascript",
    "ssr",
  )

  override fun bundle(locale: Locale): ResourceBundle = bundle
}

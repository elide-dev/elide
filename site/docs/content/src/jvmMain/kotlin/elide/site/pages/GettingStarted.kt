package elide.site.pages

import elide.site.I18nPage
import elide.site.abstract.SitePage
import java.net.URI
import elide.site.pages.defaults.Home as Defaults
import java.util.*

/** Describes the Elide Getting Started page. */
actual object GettingStarted : SitePage(
  name = Defaults.name,
  label = Defaults.label,
  path = Defaults.path,
  title = Defaults.title,
), I18nPage {
  // Internal: Internationalized page bundle.
  private val bundle = ResourceBundle.getBundle("ElideSite_Runtime")

  override fun bundle(locale: Locale): ResourceBundle = bundle

  override fun canonical(locale: Locale): URI = URI.create("https://elide.dev/getting-started")
}

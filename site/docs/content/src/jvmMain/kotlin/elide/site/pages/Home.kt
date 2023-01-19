package elide.site.pages

import elide.site.I18nPage
import elide.site.abstract.SitePage
import java.util.*
import elide.site.pages.defaults.Home as Defaults

/** Describes the Elide site home page. */
actual object Home : SitePage(
  name = Defaults.name,
  label = Defaults.label,
  path = Defaults.path,
  title = Defaults.title,
), I18nPage {
  // Internal: Internationalized page bundle.
  private val bundle = ResourceBundle.getBundle("ElideSite_Home")

  /** @inheritDoc */
  override fun bundle(locale: Locale): ResourceBundle = bundle
}

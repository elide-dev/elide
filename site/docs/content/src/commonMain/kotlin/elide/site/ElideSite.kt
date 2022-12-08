package elide.site

import elide.site.abstract.SitePage
import elide.site.pages.*

/** Main static description of the Elide site. */
object ElideSite {
  /** General site info. */
  sealed interface SiteInfo {
    /** Tail end of all site page titles. */
    val postfix: String

    /** Main title to use for the site. */
    val title: String

    /** Name of the framework. */
    val name: String

    /** Whether to indicate an early release. */
    val prerelease: Boolean

    /** Label to show if [prerelease] is `true`. */
    val prelabel: String

    /** Main heading to use for the navigation bar. */
    val heading: String
  }

  /** Site info in English. */
  object English: SiteInfo {
    /** Tail end of all site page titles. */
    override val postfix = "Rapid development framework for Kotlin"

    /** Main title to use for the site. */
    override val title = "Elide | $postfix"

    /** Name of the framework. */
    override val name = "Elide"

    /** Whether to indicate an early release. */
    override val prerelease = true

    /** Label to show if [prerelease] is `true`. */
    override val prelabel = "αlpha"

    /** Main heading to use for the navigation bar. */
    override val heading = "$name Framework"
  }

  /** Default site info. */
  val defaultInfo = English

  /** All top-level pages on the Elide site. */
  val pages: List<SitePage> = listOf(
    Home,
    GettingStarted,
    Packages,
    Architecture,
    Tooling,
    Samples,
  )
}

package elide.site

/** Common asset references for the Elide site. */
object Assets {
  /** Image assets. */
  object Images {
    /** Gray-only Elide logo (square vector). */
    const val logoGray = "/images/logo-gray.svg"

    /** Color Elide logo (square raster). */
    const val stampColor = "/images/favicon.png"

    /** Color Elide logo (square vector). */
    const val stampDynamic = "/images/favicon.svg"
  }

  /** Style assets. */
  object Styles {
    /** Base stylesheet shared by all Elide sites. */
    const val base = "/assets/base.min.css"
  }

  /** Script assets. */
  object Scripts {
    /** Main UI render script. */
    const val ui = "/ui.js"

    /** Analytics script. */
    const val analytics = "/analytics.min.js"
  }
}

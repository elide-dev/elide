package elide.site.ui.components

import web.cssom.ClassName
import elide.site.ui.MDX
import elide.site.ui.MDXProps
import mui.material.Typography
import mui.material.styles.TypographyVariant
import react.dom.html.ReactHTML.br
import react.dom.html.ReactHTML.header
import react.dom.html.ReactHTML.main
import react.dom.html.ReactHTML.section

/** Props used with a [FullbleedPage] component. */
external interface FullbleedPageProps: react.PropsWithChildren, react.PropsWithClassName {
  /** Page heading. */
  var heading: String

  /** Extra classes to add to the main wrapper. */
  var mainClasses: String?

  /** Extra classes to add to the header. */
  var headerClasses: String?

  /** Extra classes to add to the section content wrapper. */
  var sectionClasses: String?

  /** MDX page content. */
  var component: react.ElementType<MDXProps>?

  /** Whether to omit line-breaks. */
  var omitBreaks: Boolean?
}

// If there are extra `classes`, return them in a string prefixed with a space, otherwise return an empty string.
private fun maybeAddClasses(base: String, classes: String?): String {
  return if (classes.isNullOrBlank()) {
    base
  } else {
    "$base $classes"
  }
}

/** Renders a full-bleed page. */
val FullbleedPage = react.FC<FullbleedPageProps> {
  var injectedChildren = false

  main {
    className = ClassName(maybeAddClasses("elide-site-page narrative", it.mainClasses))

    header {
      className = ClassName(maybeAddClasses("elide-site-page__header", it.headerClasses))

      Typography {
        variant = TypographyVariant.h2
        +(it.heading)
      }
    }

    section {
      className = ClassName(maybeAddClasses("elide-site-page__section-body", it.sectionClasses))

      // if we are given an MDX component, it should inhabit the main container. if not, any children will be rendered
      // within the container.
      when (val component = it.component) {
        null -> when (val children = it.children) {
          null -> {}
          else -> {
            injectedChildren = true
            +(children)
          }
        }

        else -> MDX.render(this, component)
      }
      if (it.omitBreaks != true) br()
    }

    // if there are any present children, add them within the container
    if (!injectedChildren) when (val children = it.children) {
      null -> {}
      else -> +(children)
    }
  }
}

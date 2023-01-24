@file:Suppress("MemberVisibilityCanBePrivate")

package elide.site.ui

import csstype.ClassName
import elide.site.ui.components.CodeSample
import elide.site.ui.components.SyntaxLanguage
import js.core.jso
import mui.icons.material.InfoOutlined
import mui.icons.material.InfoRounded
import mui.material.Box
import mui.material.Link
import mui.material.SvgIconColor
import mui.material.Typography
import mui.material.styles.TypographyVariant
import react.dom.html.ReactHTML.code
import react.dom.html.ReactHTML.blockquote
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.span

external interface MDXComponents {
  /** `a`: Link element. */
  var a: ElementType<*>

  /** `blockquote`: Block-quote element. */
  var blockquote: ElementType<*>

  /** `p`: Paragraph element. */
  var p: ElementType<*>

  /** `code`: Code element. */
  var code: ElementType<*>

  /** `h1`: Heading 1 element. */
  var h1: ElementType<*>

  /** `h2`: Heading 2 element. */
  var h2: ElementType<*>

  /** `h3`: Heading 3 element. */
  var h3: ElementType<*>

  /** `h4`: Heading 4 element. */
  var h4: ElementType<*>

  /** `h5`: Heading 5 element. */
  var h5: ElementType<*>

  /** `h6`: Heading 6 element. */
  var h6: ElementType<*>
}

/** Props supported by all rendered MDX components. */
external interface MDXProps : Props {
  /** Component overrides. */
  var components: MDXComponents
}

/** Combination of props-with-children and props-with-class-name. */
external interface WrappedComponentProps : PropsWithChildren, PropsWithClassName {
  // Nothing at this time.
}

// Wrapped paragraph component.
val wrappedP = FC<WrappedComponentProps> {
  Typography {
    variant = TypographyVariant.body1
    children = it.children
    className = ClassName("elide-mdx__wrapped-p")
  }
}

// Wrapped H1 component.
val wrappedH1 = FC<WrappedComponentProps> {
  Typography {
    variant = TypographyVariant.h1
    children = it.children
  }
}

// Wrapped H2 component.
val wrappedH2 = FC<WrappedComponentProps> {
  Typography {
    variant = TypographyVariant.h2
    children = it.children
  }
}

// Wrapped H3 component.
val wrappedH3 = FC<WrappedComponentProps> {
  Typography {
    variant = TypographyVariant.h3
    children = it.children
  }
}

// Wrapped H4 component.
val wrappedH4 = FC<WrappedComponentProps> {
  Typography {
    variant = TypographyVariant.h4
    children = it.children
  }
}

// Wrapped H5 component.
val wrappedH5 = FC<WrappedComponentProps> {
  Typography {
    variant = TypographyVariant.h5
    children = it.children
  }
}

// Wrapped H6 component.
val wrappedH6 = FC<WrappedComponentProps> {
  Typography {
    variant = TypographyVariant.h6
    children = it.children
  }
}

// Properties for a wrapped code block.
external interface SyntaxHighlighterProps : WrappedComponentProps {
  var language: String?
  var style: dynamic
}

// Wrapped code block.
private val wrappedCode = FC<SyntaxHighlighterProps> {
  val clsName = it.className?.unsafeCast<String>()?.split("-")?.last()
  val languageName = if (it.language.isNullOrBlank() && clsName.isNullOrBlank()) {
    null
  } else if (!clsName.isNullOrBlank()) {
    clsName.split("-").last()
  } else {
    it.language!!
  }
  val syntax = when (languageName) {
    null -> null
    "js", "Javascript" -> SyntaxLanguage.JAVASCRIPT
    "json" -> SyntaxLanguage.JSON
    "bash" -> SyntaxLanguage.BASH
    "kotlin" -> SyntaxLanguage.KOTLIN
    "groovy" -> SyntaxLanguage.GROOVY
    "python", "starlark" -> SyntaxLanguage.PYTHON
    "xml" -> SyntaxLanguage.XML
    else -> {
      console.warn("[elide:site]", "Language not installed:", languageName)
      null
    }
  }

  if (!clsName.isNullOrBlank()) {
    CodeSample {
      language = syntax
      children = it.children
    }
  } else {
    code {
      children = it.children
    }
  }
}

// Wraps a `blockquote` element.
private val wrappedBlockquote = FC<PropsWithChildren> {
  Box {
    component = div
    className = ClassName("elide-mdx__blockquote elide-mdx__note")

    InfoOutlined {
      className = ClassName("elide-mdx__icon-note")
      color = SvgIconColor.info
    }

    +it.children
  }
}

/** Utilities for working with MDX components. */
object MDX {
  /**
   * Render a generic MDX component, optionally with [mui] support.
   *
   * @param P Props type for the element.
   * @param E Element type for the element.
   * @param T Element instance type for the element.
   * @param mui Whether to enable MUI support; defaults to `true`.
   * @param elementType Type of element to render.
   */
  fun <
    P: MDXProps,
    E: ReactElement<P>,
    T: ElementType<P>
  > render(children: ChildrenBuilder, mui: Boolean, elementType: T, block: P.() -> Unit): Unit = if (mui) {
    children.child(Fragment.create {
      elementType {
        components = jso {
          a = Link
          blockquote = wrappedBlockquote
          p = wrappedP
          h1 = wrappedH1
          h2 = wrappedH2
          h3 = wrappedH3
          h4 = wrappedH4
          h5 = wrappedH5
          h6 = wrappedH6
          code = wrappedCode
        }

        block.invoke(this)
      }
    })
  } else {
    children.child(Fragment.create {
      elementType {
        block.invoke(this)
      }
    })
  }

  /**
   * Render a generic MDX component with MUI support.
   *
   * @param P Props type for the element.
   * @param E Element type for the element.
   * @param T Element instance type for the element.
   * @param elementType Type of element to render.
   */
  fun <
    P: MDXProps,
    E: ReactElement<P>,
    T: ElementType<P>
    > render(children: ChildrenBuilder, elementType: T, block: (P.() -> Unit)? = null): Unit = render(
      children = children,
      mui = true,
      elementType = elementType,
      block = block ?: {},
    )
}

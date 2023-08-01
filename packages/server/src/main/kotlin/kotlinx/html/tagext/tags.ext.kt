package kotlinx.html.tagext

import kotlinx.html.*

/**
 * Open a `<body>` tag with support for suspension calls.
 *
 * @Param classes Classes to apply to the body tag in the DOM.
 * @param block Callable block to configure and populate the body tag.
 */
@HtmlTagMarker
public suspend inline fun HTML.body(
  classes : String? = null,
  crossinline block : suspend BODY.() -> Unit
) : Unit = BODY(
  attributesMapOf("class", classes),
  consumer
).visitSuspend(block)


/**
 * Open a `<head>` tag with support for suspension calls.
 *
 * @param block Callable block to configure and populate the body tag.
 */
@HtmlTagMarker
public suspend inline fun HTML.head(
  crossinline block : suspend HEAD.() -> Unit
) : Unit = HEAD(emptyMap, consumer).visitSuspend(
  block
)

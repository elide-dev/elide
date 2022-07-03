package kotlinx.html.tagext

import kotlinx.html.*

/**
 * TBD
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
 * Document head
 */
@HtmlTagMarker
public suspend inline fun HTML.head(
  crossinline block : suspend HEAD.() -> Unit
) : Unit = HEAD(emptyMap, consumer).visitSuspend(
  block
)

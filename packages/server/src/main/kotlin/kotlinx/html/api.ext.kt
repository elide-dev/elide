package kotlinx.html


/**
 * TBD
 */
suspend inline fun <T : Tag> T.visitSuspend(crossinline block: suspend T.() -> Unit) = visitTagSuspend {
  block()
}

suspend inline fun <T : Tag> T.visitTagSuspend(block: T.() -> Unit) {
  consumer.onTagStart(this)
  try {
    this.block()
  } catch (err: Throwable) {
    consumer.onTagError(this, err)
  } finally {
    consumer.onTagEnd(this)
  }
}

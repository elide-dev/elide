package kotlinx.html


/**
 * TBD
 */
public suspend inline fun <T : Tag> T.visitSuspend(crossinline block: suspend T.() -> Unit): Unit = visitTagSuspend {
  block()
}


/**
 * TBD
 */
public suspend inline fun <T : Tag> T.visitTagSuspend(block: T.() -> Unit): Unit {
  consumer.onTagStart(this)
  try {
    this.block()
  } catch (err: Throwable) {
    consumer.onTagError(this, err)
  } finally {
    consumer.onTagEnd(this)
  }
}

package kotlinx.html


// Visitor with suspension support.
public suspend inline fun <T : Tag> T.visitSuspend(crossinline block: suspend T.() -> Unit): Unit = visitTagSuspend {
  block()
}

// Tag visitor with suspension support.
@Suppress("TooGenericExceptionCaught")
public suspend inline fun <T : Tag> T.visitTagSuspend(crossinline block: suspend T.() -> Unit) {
  consumer.onTagStart(this)
  try {
    this.block()
  } catch (err: Throwable) {
    throw err
  } finally {
    consumer.onTagEnd(this)
  }
}

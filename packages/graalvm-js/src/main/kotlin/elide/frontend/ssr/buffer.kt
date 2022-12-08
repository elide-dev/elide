package elide.frontend.ssr

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import react.ReactElement
import kotlin.coroutines.CoroutineContext

/**
 *
 */
class ApplicationBuffer constructor (private val app: ReactElement<*>): CoroutineScope {
  private var job = Job()
  override val coroutineContext: CoroutineContext get() = job

  /**
   *
   */
  private suspend fun render(): ApplicationBuffer {
    val reader = renderSSRStreaming(app).getReader()
    while (true) {
      val state = reader.read().await()
    }
    TODO("")
  }

  /**
   *
   */
  public fun extract(): String {
    TODO("")
  }

  /**
   *
   */
  public fun execute(): ApplicationBuffer {
    val entry = launch {
      render()
    }

    return this
  }
}

@JsExport
public data class CssChunk(
  val ids: Array<String>,
  val key: String,
  val css: String,
)

@JsExport
public data class RenderedStream(
  val status: Int = 200,
  val html: String = "",
  val headers: HeaderMap = emptyMap(),
  val criticalCss: String = "",
  val styleChunks: Array<CssChunk> = emptyArray(),
)

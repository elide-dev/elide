package elide.server.ssr

import elide.server.ResponseRenderer
import java.io.ByteArrayOutputStream


/**
 * Describes supported server-renderer API methods, which are used by the framework to translate result content into an
 * [HttpResponse] instance.
 */
interface ServerRenderer: ResponseRenderer<ByteArrayOutputStream> {
  // Nothing yet.
}

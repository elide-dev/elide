package elide.ssr

import java.io.ByteArrayOutputStream


/**
 * Describes supported server-renderer API methods, which are used by the framework to translate result content from
 * embedded SSR scripts.
 */
public interface ServerRenderer: ResponseRenderer<ByteArrayOutputStream> {
  // Nothing yet.
}

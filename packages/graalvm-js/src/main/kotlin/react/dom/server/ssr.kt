@file:JsModule("react-dom/server.browser")
@file:JsNonModule

package react.dom.server

import react.ReactNode
import web.abort.AbortSignal
import web.streams.ReadableStream
import kotlin.js.Promise

/**
 * TBD.
 */
public external interface ReadableStreamRenderOptions {
  public var identifierPrefix: String?
  public var namespaceURI: String?
  public var nonce: String?
  public var bootstrapScriptContent: String?
  public var bootstrapScripts: List<String>?
  public var bootstrapModules: List<String>?
  public var progressiveChunkSize: Int?
  public var signal: AbortSignal?
  public var onError: (error: dynamic) -> Unit
}

/**
 * TBD.
 */
public external fun renderToReadableStream(
  initialChildren: ReactNode,
  options: ReadableStreamRenderOptions = definedExternally,
): Promise<ReadableStream<ByteArray>>

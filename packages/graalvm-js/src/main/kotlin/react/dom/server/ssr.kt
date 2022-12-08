@file:JsModule("react-dom/server")
@file:JsNonModule
@file:OptIn(ExperimentalJsExport::class)

package react.dom.server

import react.ReactNode
import web.abort.AbortSignal
import web.streams.ReadableStream


/**
 * TBD.
 */
@JsExport
public external fun renderToPipeableStream(
  initialChildren: ReactNode,
): dynamic


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
@JsExport
public external fun renderToReadableStream(
  initialChildren: ReactNode,
  options: ReadableStreamRenderOptions = definedExternally,
): ReadableStream<String>

package elide.runtime.intrinsics.js.http

import elide.annotations.core.Polyglot
import elide.runtime.intrinsics.js.err.Error as JsError

public interface IncomingMessage {
  /**
   * The message.complete property will be true if a complete HTTP message has been received and successfully parsed.
   */
  @get:Polyglot public val complete: Boolean

  /**
   * In case of server request, the HTTP version sent by the client. In the case of client response, the HTTP version
   * of the connected-to server.
   */
  @get:Polyglot public val httpVersion: String

  /**
   * The request method as a read-only string (e.g. `GET`, `DELETE`), Only valid for request obtained from [Server].
   */
  @get:Polyglot public val method: String
  
  /**
   * The request/response headers object. Key-value pairs of header names and values. Header names are lower-cased.
   */
  @get:Polyglot public val headers: HttpHeaders
  
  /**
   * Similar to [headers], but there is no join logic and the values are always arrays of strings, even for headers
   * received just once.
   */
  @get:Polyglot public val headersDistinct: HttpHeaders

  /**
   * The raw request/response headers list exactly as they were received. The keys and values are in the same list, it
   * is not a list of tuples. So, the even-numbered offsets are key values, and the odd-numbered offsets are the
   * associated values. Header names are not lowercased, and duplicates are not merged.
   */
  @get:Polyglot public val rawHeaders: List<String>

  /**
   * The request/response trailers object. Only populated at the 'end' event.
   */
  @get:Polyglot public val trailers: HttpHeaders
  
  /**
   * Similar to [trailers], but there is no join logic and the values are always arrays of strings, even for headers
   * received just once. Only populated at the 'end' event.
   */
  @get:Polyglot public val trailersDistinct: HttpHeaders
  
  /**
   * The raw request/response trailer keys and values exactly as they were received.
   */
  @get:Polyglot public val rawTrailers: List<String>
  
  /**
   * The 3-digit HTTP response status code, e.g. `404`. Only valid for response obtained from [ClientRequest].
   */
  @get:Polyglot public val statusCode: Int
  
  /**
   * The HTTP response status message (reason phrase). e.g. `OK` or `Internal Server Error`. Only valid for response
   * obtained from [ClientRequest].
   */
  @get:Polyglot public val statusMessage: String
  
  /**
   * Request URL string. This contains only the URL that is present in the actual HTTP request.
   */
  @get:Polyglot public val url: String
  
  /**
   * Calls destroy() on the socket that received the IncomingMessage. If [error] is provided, an 'error' event is emitted
   * on the socket and error is passed as an argument to any listeners on the event.
   *
   * @return this [IncomingMessage]
   */
  @Polyglot public fun destroy(error: JsError? = null): IncomingMessage = this
}
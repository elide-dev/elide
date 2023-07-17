package elide.runtime.intrinsics.js.http

import elide.annotations.core.Polyglot
import org.graalvm.polyglot.Value
import elide.runtime.intrinsics.js.err.Error as JsError

public interface OutgoingMessage {
  /**
   * Read-only. true if the headers were sent, otherwise false.
   */
  @get:Polyglot public val headersSent: Boolean
  
  /**
   * Adds HTTP trailers (headers but at the end of the message) to the message.
   *
   * Trailers will only be emitted if the message is chunked encoded. If not, the trailers will be silently discarded.
   * HTTP requires the Trailer header to be sent to emit trailers, with a list of header field names in its value.
   */
  @Polyglot public fun addTrailers(headers: HttpHeaders)

  /**
   * Append a single header value for the header object.
   *
   * If the value is an array, this is equivalent of calling this method multiple times.
   * If there were no previous value for the header, this is equivalent of calling [setHeader] with [name] and [value].
   *
   * Depending of the value of options.uniqueHeaders when the client request or the server were created, this will end
   * up in the header being sent multiple times or a single time with values joined using ';'.
   *
   * @param name the header name
   * @param value the header value
   * @return this [OutgoingMessage]
   */
  @Polyglot public fun appendHeader(name: String, value: String): OutgoingMessage
  
  /**
   * Destroys the message. Once a socket is associated with the message and is connected, that socket will be destroyed
   * as well.
   *
   * @return this [OutgoingMessage]
   */
  @Polyglot public fun destroy(error: JsError? = null): OutgoingMessage
  
  /**
   * Finishes the outgoing message. If any parts of the body are unsent, it will flush them to the underlying system.
   * If the message is chunked, it will send the terminating chunk 0\r\n\r\n, and send the trailers (if any).
   *
   * If chunk is specified, it is equivalent to calling outgoingMessage.write(chunk, encoding), followed by
   * outgoingMessage.end(callback).
   *
   * If callback is provided, it will be called when the message is finished (equivalent to a listener of the 'finish'
   * event).
   */
  @Polyglot public fun end(chunk: Value?, encoding: String?, callback: (() -> Unit)? = null): OutgoingMessage
  
  /** Flushes the message headers. */
  @Polyglot public fun flushHeaders()
  
  /**
   * Gets the value of the HTTP header with the given name. If that header is not set, the returned value will be
   * undefined.
   */
  @Polyglot public fun getHeader(name: String): String?
  
  /**
   * Returns an array containing the unique names of the current outgoing headers. All names are lowercase.
   */
  @Polyglot public fun getHeaderNames(): List<String>
  
  /**
   * Returns a shallow copy of the current outgoing headers.
   *
   * Since a shallow copy is used, array values may be mutated without additional calls to various header-related HTTP
   * module methods. The keys of the returned object are the header names and the values are the respective header
   * values. All header names are lowercase.
   */
  @Polyglot public fun getHeaders(): HttpHeaders
  
  /**
   * Returns true if the header identified by name is currently set in the outgoing headers. The header name is
   * case-insensitive.
   */
  @Polyglot public fun hasHeader(name: String): Boolean
  
  /**
   * Removes a header that is queued for implicit sending.
   */
  @Polyglot public fun removeHeader(name: String)
  
  /**
   * Sets a single header value. If the header already exists in the to-be-sent headers, its value will be replaced.
   * Use an array of strings to send multiple headers with the same name.
   */
  @Polyglot public fun setHeader(name: String, value: Any): OutgoingMessage
  
  /**
   * Returns the response object.
   *
   * Sets multiple header values for implicit headers. headers must be an instance of Headers or Map, if a header
   * already exists in the to-be-sent headers, its value will be replaced.
   */
  @Polyglot public fun setHeaders(headers: HttpHeaders): OutgoingMessage
  
  /**
   * Sends a chunk of the body. This method can be called multiple times.
   *
   * The encoding argument is only relevant when chunk is a string. Defaults to 'utf8'. The callback argument is
   * optional and will be called when this chunk of data is flushed.
   *
   * Returns true if the entire data was flushed successfully to the kernel buffer. Returns false if all or part of
   * the data was queued in the user memory. The 'drain' event will be emitted when the buffer is free again.
   */
  @Polyglot public fun write(chunk: Value, encoding: String?, callback: (() -> Unit)? = null): Boolean
}
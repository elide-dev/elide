package elide.runtime.intrinsics.js.http

import elide.annotations.core.Polyglot

public interface ServerResponse : OutgoingMessage {
  /**
   * A reference to the original HTTP request object.
   */
  @get:Polyglot public val req: IncomingMessage
  
  /**
   * Controls the status code that will be sent to the client when the headers get flushed.
   */
  @get:Polyglot @set:Polyglot public var statusCode: Int
  
  /**
   * The status message that will be sent to the client when the headers get flushed. If this is left as undefined then
   * the standard message for the status code will be used.
   */
  @get:Polyglot @set:Polyglot public var statusMessage: String
}
package elide.runtime.intrinsics.js.http

import elide.annotations.core.Polyglot

public interface Server {
  /**
   * Limit the amount of time the parser will wait to receive the complete HTTP headers.
   *
   * If the timeout expires, the server responds with status 408 without forwarding the request to the request listener
   * and then closes the connection.
   *
   * It must be set to a non-zero value (e.g. 120 seconds) to protect against potential Denial-of-Service attacks in
   * case the server is deployed without a reverse proxy in front.
   */
  @get:Polyglot @set:Polyglot public var headersTimeout: Int
  
  /**
   * Limits maximum incoming headers count. If set to 0, no limit will be applied.
   */
  @get:Polyglot @set:Polyglot public var maxHeadersCount: Int
  
  /**
   * Sets the timeout value in milliseconds for receiving the entire request from the client.
   *
   * If the timeout expires, the server responds with status 408 without forwarding the request to the request listener
   * and then closes the connection.
   * 
   * It must be set to a non-zero value (e.g. 120 seconds) to protect against potential Denial-of-Service attacks in
   * case the server is deployed without a reverse proxy in front.
   */
  @get:Polyglot @set:Polyglot public var requestTimeout: Int
  
  /**
   * The maximum number of requests socket can handle before closing keep alive connection.
   *
   * A value of 0 will disable the limit.
   *
   * When the limit is reached it will set the Connection header value to close, but will not actually close the
   * connection, subsequent requests sent after the limit is reached will get 503 Service Unavailable as a response.
   */
  @get:Polyglot @set:Polyglot public var maxRequestsPerSocket: Int
  
  /**
   * The number of milliseconds of inactivity before a socket is presumed to have timed out.
   *
   * A value of 0 will disable the timeout behavior on incoming connections.
   *
   * The socket timeout logic is set up on connection, so changing this value only affects new connections to the
   * server, not any existing connections.
   */
  @get:Polyglot @set:Polyglot public var timeout: Int
  
  /**
   * The number of milliseconds of inactivity a server needs to wait for additional incoming data, after it has
   * finished writing the last response, before a socket will be destroyed. If the server receives new data before the
   * keep-alive timeout has fired, it will reset the regular inactivity timeout, i.e., server.timeout.
   *
   * A value of 0 will disable the keep-alive timeout behavior on incoming connections. A value of 0 makes the http
   * server behave similarly to Node.js versions prior to 8.0.0, which did not have a keep-alive timeout.
   *
   * The socket timeout logic is set up on connection, so changing this value only affects new connections to the
   * server, not any existing connections.
   */
  @get:Polyglot @set:Polyglot public var keepAliveTimeout: Int
  
  /**
   * Indicates whether or not the server is listening for connections.
   */
  @get:Polyglot public val listening: Boolean
  
  /**
   * Starts the HTTP server listening for connections. This method is identical to server.listen() from net.Server.
   */
  @Polyglot public fun listen(port: Int)
  
  /**
   * Sets the timeout value for sockets, and emits a 'timeout' event on the Server object, passing the socket as an
   * argument, if a timeout occurs.
   *
   * If there is a 'timeout' event listener on the Server object, then it will be called with the timed-out socket as
   * an argument.
   *
   * By default, the Server does not timeout sockets. However, if a callback is assigned to the Server's 'timeout'
   * event, timeouts must be handled explicitly.
   */
  @Polyglot public fun setTimeout(msecs: Long = 0, callback: Any? = null): Server
  
  /**
   * Stops the server from accepting new connections and closes all connections connected to this server which are not
   * sending a request or waiting for a response.
   */
  @Polyglot public fun close(callback: Any?)
  
  /**
   * Closes all connections connected to this server.
   */
  @Polyglot public fun closeAllConnections()
  
  /**
   * Closes all connections connected to this server which are not sending a request or waiting for a response.
   */
  @Polyglot public fun closeIdleConnections()
}
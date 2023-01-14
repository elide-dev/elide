package elide.runtime.gvm

/**
 * TBD.
 */
public fun <R: Any> entrypoint(op: () -> R): R {
  js("""
    var streams = require("web-streams-polyfill/ponyfill");
    globalThis.ReadableStream = streams.ReadableStream;
  """)
  return op.invoke()
}

package webstreams.polyfill

import js.import.import

/** Import the Web Streams polyfill. */
public fun installStreams() {
  import<Any>("web-streams-polyfill/ponyfill")
}

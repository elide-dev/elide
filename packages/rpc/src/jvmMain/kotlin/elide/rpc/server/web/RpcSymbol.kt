package elide.rpc.server.web

/**
 * Enumerates byte symbols which are meaningful in the gRPC-Web Protocol; there are only two, [DATA] and [TRAILER].
 *
 * The [TRAILER] value is used to encode gRPC responses over regular HTTP/1.1-style responses, if needed. The [DATA]
 * symbol is used to demarcate a data frame inside a gRPC Web request or response.
 */
public enum class RpcSymbol constructor(public val value: Byte) {
  /** Symbol indicating a data frame. */
  DATA((0x00).toByte()),

  /** Symbol used to demarcate trailers. */
  TRAILER((0x80).toByte()),
}

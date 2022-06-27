package elide.server.rpc.web

/**
 * Creates frames compliant with the gRPC Web protocol, given a set of input bytes.
 *
 * For more details about how the gRPC Web Protocol works, see this documentation from the gRPC team at Google:
 * https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-WEB.md
 */
internal object MessageFramer {
  /**
   * Prepare an encoded and prefixed value from the provided [input] data, indicating [symbolType] as the use/type of
   * the [input] data.
   *
   * @param input Data to encode and prefix.
   * @param symbolType Type of data we are encoding.
   * @return Packed/encoded data for use in the gRPC Web Protocol.
   */
  @JvmStatic fun getPrefix(input: ByteArray, symbolType: RpcSymbol): ByteArray {
    val len = input.size
    return byteArrayOf(
      symbolType.value,
      (len shr 24 and 0xff).toByte(),
      (len shr 16 and 0xff).toByte(),
      (len shr 8 and 0xff).toByte(),
      (len shr 0 and 0xff).toByte()
    )
  }
}

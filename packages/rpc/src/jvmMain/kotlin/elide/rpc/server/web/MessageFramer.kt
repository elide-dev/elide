/*
 * Copyright (c) 2023 Elide Ventures, LLC.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

package elide.rpc.server.web

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

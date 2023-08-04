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

import com.google.protobuf.Message
import java.lang.reflect.Method

/**
 * Defines the API surface provided for a class which knows how to deserialize gRPC method parameters from raw bytes to
 * inflated [com.google.protobuf.Message] instances.
 */
public interface MessageDeserializer {
  /**
   * Deserialize a generic protocol buffer [Message] from the provided set of [rawData], with the intent of dispatching
   * the provided RPC [method].
   *
   * If the method does not have the expected parameter, [IllegalStateException] is thrown. If the data cannot be
   * decoded, an exception ([IllegalArgumentException]) is thrown. Other errors, such as the parser being missing on the
   * protocol buffer message, are thrown as [IllegalStateException].
   *
   * @param method Method to resolve the proto [Message] type from. The proto is expected to be the first parameter.
   * @param rawData Raw data from the request which should be inflated into the resulting message type.
   * @return Resulting instance of [Message] decoded as a request for the provided [method].
   */
  public fun deserialize(method: Method, rawData: ByteArray): Message
}

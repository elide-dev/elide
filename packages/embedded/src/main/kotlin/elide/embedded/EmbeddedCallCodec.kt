/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

package elide.embedded

import elide.embedded.http.EmbeddedResponse

/** Alias over an unsafe call type received through the native API. */
public typealias UnsafeCall = Any

/** Alias over an unsafe response type sent through the native API. */
public typealias UnsafeResponse = Any

/** A serial codec used to map calls and responses exchanged through the native API. */
public interface EmbeddedCallCodec {
  /** Decode an [unsafe] call provided by the host application. */
  public fun decode(unsafe: UnsafeCall): EmbeddedCall

  /** Encode a [response] into a form compatible with the host application. */
  public fun encode(response: EmbeddedResponse): UnsafeResponse
}

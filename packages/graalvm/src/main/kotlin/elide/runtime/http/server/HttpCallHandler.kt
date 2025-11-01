/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

package elide.runtime.http.server

/**
 * A functional component used to handle each [HttpCall] in a server. Each call corresponds to one request/response
 * pair, and the handler must invoke its lifecycle methods to send the response and end the call as appropriate.
 */
public fun interface HttpCallHandler<C : CallContext> {
  /**
   * Handle an incoming HTTP [call]. Implementations have access to the call's request and response headers, content,
   * and lifecycle.
   *
   * Blocking during this method is *strictly forbidden*, as it is called on the IO event loop; this restriction means
   * handlers can rely on single-threaded invocation within the same call.
   *
   * The call's [context][HttpCall.context] can be used to access implementation-specific state for the call.
   *
   * Implementations *must* call [HttpCall.send] to finalize the response, even if no content producer was attached
   * to the body. See [HttpCall.send] for more details on the operation.
   *
   * Exceptions thrown during handling will interrupt processing and cause an HTTP `500-Internal Server Error` response
   * to be sent to the client if the call's headers have not been sent, or will drop the connection if the response was
   * in progress.
   */
  public fun handle(call: HttpCall<C>)
}

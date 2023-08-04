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

package elide.runtime.gvm

import io.micronaut.http.HttpRequest
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job

/**
 * TBD.
 */
public interface VMFacade {
  /**
   * TBD.
   */
  public fun language(): GuestLanguage

  /**
   * TBD.
   */
  public suspend fun prewarmScript(script: ExecutableScript)

  /**
   * TBD.
   */
  public suspend fun executeStreaming(script: ExecutableScript, args: ExecutionInputs, receiver: StreamingReceiver): Job

  /**
   * TBD.
   */
  public suspend fun executeRender(
    script: ExecutableScript,
    request: HttpRequest<*>,
    context: Any?,
    receiver: StreamingReceiver,
  ): Job

  /**
   * Suspension execution of the provided [script] within an embedded JavaScript VM, by way of GraalVM's runtime engine;
   * de-serialize the result [R] and provide it as the return value.
   *
   * @param script Executable script spec to execute within the embedded JS VM.
   * @return Deferred task which evaluates to the return value [R] when execution finishes.
   */
  public suspend fun <R> execute(script: ExecutableScript, returnType: Class<R>, args: ExecutionInputs?): R?

  /**
   * Asynchronously execute the provided [script] within an embedded JavaScript VM, by way of GraalVM's runtime engine;
   * de-serialize the result [R] and provide it as the return value.
   *
   * @param script Executable script spec to execute within the embedded JS VM.
   * @return Deferred task which evaluates to the return value [R] when execution finishes.
   */
  public suspend fun <R> executeAsync(script: ExecutableScript, returnType: Class<R>, args: ExecutionInputs?):
    Deferred<R?>

  /**
   * Blocking execution of the provided [script] within an embedded JavaScript VM, by way of GraalVM's runtime engine;
   * de-serialize the result [R] and provide it as the return value.
   *
   * @param script Executable script spec to execute within the embedded JS VM.
   * @return Deferred task which evaluates to the return value [R] when execution finishes.
   */
  public fun <R> executeBlocking(script: ExecutableScript, returnType: Class<R>, args: ExecutionInputs?): R?
}

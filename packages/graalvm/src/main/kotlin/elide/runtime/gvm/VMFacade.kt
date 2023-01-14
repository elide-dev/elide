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

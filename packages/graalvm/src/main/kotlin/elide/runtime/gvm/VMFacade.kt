package elide.runtime.gvm

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
  public suspend fun executeStreaming(script: ExecutableScript, vararg args: Any?, receiver: StreamingReceiver): Job

  /**
   * Suspension execution of the provided [script] within an embedded JavaScript VM, by way of GraalVM's runtime engine;
   * de-serialize the result [R] and provide it as the return value.
   *
   * @param script Executable script spec to execute within the embedded JS VM.
   * @return Deferred task which evaluates to the return value [R] when execution finishes.
   */
  public suspend fun <R> execute(script: ExecutableScript, returnType: Class<R>, vararg args: Any?): R?

  /**
   * Asynchronously execute the provided [script] within an embedded JavaScript VM, by way of GraalVM's runtime engine;
   * de-serialize the result [R] and provide it as the return value.
   *
   * @param script Executable script spec to execute within the embedded JS VM.
   * @return Deferred task which evaluates to the return value [R] when execution finishes.
   */
  public suspend fun <R> executeAsync(script: ExecutableScript, returnType: Class<R>, vararg args: Any?):
    Deferred<R?>

  /**
   * Blocking execution of the provided [script] within an embedded JavaScript VM, by way of GraalVM's runtime engine;
   * de-serialize the result [R] and provide it as the return value.
   *
   * @param script Executable script spec to execute within the embedded JS VM.
   * @return Deferred task which evaluates to the return value [R] when execution finishes.
   */
  public fun <R> executeBlocking(script: ExecutableScript, returnType: Class<R>, vararg args: Any?): R?
}

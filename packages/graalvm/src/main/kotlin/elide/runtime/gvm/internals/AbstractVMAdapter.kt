@file:Suppress("RedundantVisibilityModifier")

package elide.runtime.gvm.internals

import org.reactivestreams.Publisher
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.Future
import elide.runtime.gvm.ExecutableScript
import elide.runtime.gvm.ExecutionInputs

/**
 * # VM: Adapter.
 *
 * Binds an invocation [Bindings] object, an executable [Script], and a set of [Inputs]. Given these types and their
 * object representations, the adapter must resolve the appropriate way to dispatch the [Script], with a guest VM
 * binding resolved from [Bindings] for [Script].
 *
 * ## Resolving bindings
 *
 * The [bind] method is the primary entrypoint of the VM adapter. This method's job is to traverse the resolved [Script]
 * [Bindings] to match a script entrypoint which can consume the provided [Inputs]. If no binding can be resolved, an
 * error is thrown.
 *
 * ## Dispatch flow
 *
 * Once a context has been resolved via the [elide.runtime.gvm.internals.context.ContextManager], bindings can be
 * constructed around an evaluated guest [org.graalvm.polyglot.Value]. These bindings are provided to an
 * [AbstractVMAdapter]'s [bind] method, which resolves a VM execution type from the set of available entrypoints.
 *
 * A single VM execution always implements [CompletableFuture] to (1) signify execution completion (and failure), and
 * (2) provide a means to retrieve the final result of an execution. In simple cases, i.e. regular unary execution, a
 * single [Output] may be created from the provided [Inputs].
 *
 * In a more complex case, i.e. server-side SSR (streaming or chunked execution), multiple output chunks may be provided
 * by a reactive Java type of some kind, which augment or replace the primary [CompletableFuture] output. In this case,
 * the final output may be a finalization of the provided concrete output steps, or a simple sigil.
 *
 * @param Output Shape of the output type yielded by a resulting VM execution.
 * @param Bindings Resolved set of entrypoints for a given [Script].
 * @param Script Implementation of a guest-executable script type.
 * @param Inputs Shape of main inputs provided to the [Script] for execution.
 */
internal abstract class AbstractVMAdapter<
  Output,
  Bindings: InvocationBindings,
  Script: ExecutableScript,
  Inputs: ExecutionInputs,
> {
  /**
   * ## Execution: Promise-like.
   *
   * Describes an interface which behaves like a [CompletableFuture], but operates as an interface, so we can easily
   * use instances for delegated access.
   */
  public interface PromiseLike<Output> : Future<Output>, CompletionStage<Output> {
    /**
     * Executes the async operation, producing another promise-like type.
     *
     * @return Promise-like type which concludes when execution concludes.
     */
    fun execute(): PromiseLike<Output>

    /**
     * Indicate whether this promise-like operation has fully completed; when this property returns `true`, backing
     * resources for an operation can safely be freed.
     */
    val done: Boolean
  }

  /**
   * ## Abstract Execution
   *
   * Wires together protected/internal properties, and base logic, for all guest VM execution types (such as
   * [VMUnaryExecution] and [VMStreamingExecution]). VM executions are always tied to an instance of [AbstractVMAdapter]
   * which is itself tied to a configured instance of [AbstractVMEngine].
   */
  internal abstract class AbstractVMExecution<Output> : PromiseLike<Output>

  /**
   * ## Execution: Unary.
   *
   * Accepts a set of [Inputs] and dispatches a [Script], via resolved [Bindings], to produce a single [Output] in a
   * synchronous or buffered manner. When operating in unary execution mode, an [Output] is, by definition, not
   * available until the full completion of the execution.
   *
   * The simplest implementation of a unary execution wraps another [CompletableFuture].
   *
   * @param op Operation which is wrapped by this execution, and expected to produce a result of type [Output].
   */
  internal abstract inner class VMUnaryExecution (
    protected val op: CompletableFuture<Output>,
  ) : AbstractVMExecution<Output>(), PromiseLike<Output>, Future<Output> by op, CompletionStage<Output> by op

  /**
   * ## Execution: Streaming.
   *
   * Accepts a set of [Inputs] and dispatches a [Script], to produce a streaming [Output], via a reactive type. The hot
   * stream is bound to a VM execution which can be cancelled or monitored for completion. When operating in streaming
   * execution mode, one or more [Output] instances (or instances of some other type) may be provided via reactive types
   * with the final [CompletableFuture] resolution signifying an end to the stream.
   *
   * The simplest implementation of a streaming execution wraps a [CompletableFuture]-compatible [Publisher].
   *
   * @param op Publisher which is wrapped by this execution, and expected to produce one or more [Output] results.
   */
  internal abstract inner class VMStreamingExecution (
    protected val op: CompletableFuture<Output>,
  ) : AbstractVMExecution<Output>(), PromiseLike<Output>, Future<Output> by op, CompletionStage<Output> by op

  /**
   * Bind the provided [script] [bindings] to the provided [inputs] to produce a VM execution object.
   *
   * This method is expected to traverse the provided [bindings] to find an entrypoint (for [script]) which can consume
   * the provided [inputs], and produce an appropriate VM execution output. If no matching binding can be resolved, an
   * error is thrown, indicating that the provided [script] has no capable entrypoint for the desired execution.
   *
   * @param script Executable guest script which is the subject of this binding resolution.
   * @param bindings Set of bindings exported by the [script], which may be used to resolve an entrypoint.
   * @param inputs Set of inputs to provide to the [script] during execution.
   * @return VM execution object, which produces a result, or a streaming response, etc.
   */
  abstract fun bind(script: Script, bindings: Bindings, inputs: Inputs): AbstractVMExecution<Output>
}

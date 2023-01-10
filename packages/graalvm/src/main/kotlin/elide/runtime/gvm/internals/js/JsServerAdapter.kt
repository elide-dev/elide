package elide.runtime.gvm.internals.js

import elide.runtime.gvm.internals.AbstractVMAdapter
import io.micronaut.http.HttpResponse
import org.reactivestreams.Publisher
import java.io.InputStream
import java.util.concurrent.CompletableFuture

/** Implements an adapter to [JsRuntime] for server-side use. */
internal class JsServerAdapter : AbstractVMAdapter<
  HttpResponse<InputStream>,
  JsInvocationBindings,
  JsExecutableScript,
  JsMicronautRequestExecutionInputs,
>() {
  /** JavaScript server execution wrapper (for an in-flight VM execution). */
  inner class JsServerExecution (
    stream: Publisher<HttpResponse<InputStream>>,
    op: CompletableFuture<HttpResponse<InputStream>>,
  ) : VMStreamingExecution(
    stream,
    op,
  ) {
    /** @inheritDoc */
    override val done: Boolean get() = op.isDone

    /** @inheritDoc */
    override fun execute(): PromiseLike<HttpResponse<InputStream>> {
      TODO("Not yet implemented")
    }
  }

  /** @inheritDoc */
  override fun bind(
    script: JsExecutableScript,
    bindings: JsInvocationBindings,
    inputs: JsMicronautRequestExecutionInputs
  ): AbstractVMExecution<HttpResponse<InputStream>> {
    TODO("Not yet implemented")
  }
}

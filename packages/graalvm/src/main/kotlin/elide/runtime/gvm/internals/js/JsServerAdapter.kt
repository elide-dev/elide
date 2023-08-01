package elide.runtime.gvm.internals.js

import io.micronaut.http.HttpResponse
import org.reactivestreams.Publisher
import java.io.InputStream
import java.util.concurrent.CompletableFuture
import elide.runtime.gvm.internals.AbstractVMAdapter

/** Implements an adapter to [JsRuntime] for server-side use. */
internal class JsServerAdapter : AbstractVMAdapter<
  HttpResponse<Publisher<InputStream>>,
  JsInvocationBindings,
  JsExecutableScript,
  JsMicronautRequestExecutionInputs,
>() {
  /** JavaScript server execution wrapper (for an in-flight VM execution). */
  inner class JsServerExecution(
    op: CompletableFuture<HttpResponse<Publisher<InputStream>>>,
  ) : VMStreamingExecution(op) {
    /** @inheritDoc */
    override val done: Boolean get() = op.isDone

    /** @inheritDoc */
    override fun execute(): PromiseLike<HttpResponse<Publisher<InputStream>>> {
      TODO("Not yet implemented")
    }
  }

  /** @inheritDoc */
  override fun bind(
    script: JsExecutableScript,
    bindings: JsInvocationBindings,
    inputs: JsMicronautRequestExecutionInputs,
  ): AbstractVMExecution<HttpResponse<Publisher<InputStream>>> {
    TODO("Not yet implemented")
  }
}

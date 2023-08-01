package elide.runtime.gvm

import io.micronaut.http.HttpRequest
import elide.vm.annotations.Polyglot

/**
 * # Execution Inputs: Server Request
 *
 * Implements an [ExecutionInputs] interface for a Micronaut server [HttpRequest], optionally with additional [Data] to
 * include as "execution state."
 */
public interface MicronautRequestExecutionInputs<Data> : RequestExecutionInputs<HttpRequest<Data>> {
  /** @inheritDoc */
  @Polyglot override fun path(): String = request().path
}

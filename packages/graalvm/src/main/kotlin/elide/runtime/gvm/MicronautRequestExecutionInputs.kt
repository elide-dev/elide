package elide.runtime.gvm

import elide.annotations.core.Polyglot
import io.micronaut.http.HttpRequest
import java.util.concurrent.ConcurrentSkipListMap

/**
 * # Execution Inputs: Server Request
 *
 * Implements an [ExecutionInputs] interface for a Micronaut server [HttpRequest], optionally with additional [data] to
 * include as "execution state."
 */
public abstract class MicronautRequestExecutionInputs public constructor (
  public val request: HttpRequest<*>? = null,
  public val data: Map<String, Any?> = ConcurrentSkipListMap(),
) : RequestExecutionInputs<HttpRequest<*>> {
  /** @inheritDoc */
  override fun request(): HttpRequest<*>? {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  @Polyglot override fun path(): String = request?.path ?: RequestExecutionInputs.Defaults.DEFAULT_PATH
}

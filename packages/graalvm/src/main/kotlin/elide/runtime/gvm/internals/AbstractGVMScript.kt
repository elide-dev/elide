package elide.runtime.gvm.internals

import elide.runtime.gvm.EmbeddedScript
import elide.runtime.gvm.ExecutableScript
import elide.runtime.gvm.GuestLanguage
import java.util.concurrent.atomic.AtomicReference

/**
 * Abstract Guest VM script.
 *
 * Implements an [EmbeddedScript] type  for use with GraalVM types, such as [GraalVMGuest] (enumerating a supported
 * language) and related framework types like [ExecutableScript].
 */
internal abstract class AbstractGVMScript protected constructor (
  private val language: GraalVMGuest,
  private val source: ExecutableScript.ScriptSource,
  private val spec: String,
  map: ExecutableScript.SourceMap? = null,
) : EmbeddedScript {
  /** Atomic internal state for this script. */
  private val currentState: AtomicReference<ExecutableScript.State> = AtomicReference(
    ExecutableScript.State.UNINITIALIZED
  )

  /** Atomic reference to the source map for this script, if available. */
  private val sourceMap: AtomicReference<ExecutableScript.SourceMap?> = AtomicReference(map)

  /**
   * Protected access to update the current script state; should only be updated carefully, after exiting any active VM
   * contexts related to the operation which is updating the state.
   *
   * @param state Target state which the current script should now inhabit.
   */
  @Synchronized protected fun updateState(state: ExecutableScript.State) {
    currentState.set(state)
  }

  /** @inheritDoc */
  override fun state(): ExecutableScript.State = currentState.get()

  /** @inheritDoc */
  override fun language(): GuestLanguage = language

  /** @inheritDoc */
  override fun source(): ExecutableScript.ScriptSource = source

  /** @inheritDoc */
  override fun map(): ExecutableScript.SourceMap? = sourceMap.get()
}

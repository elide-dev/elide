package elide.runtime.gvm

import org.graalvm.polyglot.Value

/** Implements a basic arguments list of any type of values. */
internal class BasicExecutionInputs private constructor (
  private val argsList: Array<Value>,
) : ExecutionInputs {
  /** @inheritDoc */
  override fun allInputs(): Array<out Any> = argsList
}

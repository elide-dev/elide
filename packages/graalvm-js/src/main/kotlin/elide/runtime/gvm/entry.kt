package elide.runtime.gvm

/**
 *
 */
public fun <R: Any> entrypoint(op: () -> R): R {
  return op.invoke()
}

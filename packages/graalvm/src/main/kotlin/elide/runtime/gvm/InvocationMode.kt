package elide.runtime.gvm

/**
 * ## Script Invocation
 *
 * Enumerates generic invocation styles that an executable guest script may support, modulo support within the guest
 * language the script is written in.
 */
public enum class InvocationMode {
  /**
   * ### Invocation Mode: Synchronous.
   *
   */
  SYNC,

  /**
   * ### Invocation Mode: Asynchronous.
   *
   */
  ASYNC,

  /**
   * ### Invocation Mode: Streaming
   *
   */
  STREAMING,
}

package elide.frontend.ssr

/**
 * TBD
 */
public external interface SSRStateContainer<State : Any> {
  /**
   * TBD
   */
  public fun state(): State?

  /**
   * TBD
   */
  public fun context(): dynamic

  /**
   * TBD
   */
  public fun path(): String
}

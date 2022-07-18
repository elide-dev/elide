package elide.frontend.ssr

/**
 * TBD
 */
external interface SSRStateContainer<State : Any> {
  /**
   * TBD
   */
  fun state(): State?
}

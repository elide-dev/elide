package elide.proto.api

import elide.proto.ProtocolModel

/**
 * TBD.
 */
interface Record<Model: Record<Model, Builder>, Builder>: ProtocolModel {
  /**
   * TBD.
   */
  interface IBuilder<Type> {
    /**
     * TBD.
     */
    fun build(): Type
  }

  /**
   * TBD.
   */
  interface Factory<Model, Builder> {
    /**
     * TBD.
     */
    fun empty(): Model

    /**
     * TBD.
     */
    fun copy(model: Model): Model

    /**
     * TBD.
     */
    fun defaultInstance(): Model

    /**
     * TBD.
     */
    fun builder(): Builder

    /**
     * TBD.
     */
    fun create(op: Builder.() -> Unit): Model
  }

  /**
   * TBD.
   */
  fun factory(): Factory<out Model, Builder>

  /**
   * TBD.
   */
  fun toBuilder(): Builder
}

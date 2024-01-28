/*
 * Copyright (c) 2023 Elide Ventures, LLC.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

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

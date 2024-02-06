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
public interface Record<Model: Record<Model, Builder>, Builder>: ProtocolModel {
  /**
   * TBD.
   */
  public interface IBuilder<Type> {
    /**
     * TBD.
     */
    public fun build(): Type
  }

  /**
   * TBD.
   */
  public interface Factory<Model, Builder> {
    /**
     * TBD.
     */
    public fun empty(): Model

    /**
     * TBD.
     */
    public fun copy(model: Model): Model

    /**
     * TBD.
     */
    public fun defaultInstance(): Model

    /**
     * TBD.
     */
    public fun builder(): Builder

    /**
     * TBD.
     */
    public fun create(op: Builder.() -> Unit): Model
  }

  /**
   * TBD.
   */
  public fun factory(): Factory<out Model, Builder>

  /**
   * TBD.
   */
  public fun toBuilder(): Builder
}

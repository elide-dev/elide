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

package elide.model.token

import elide.annotations.data.Sensitive
import kotlinx.serialization.Serializable

/** Describes a sensitive token value. */
@Serializable public actual data class Token (
  /** Type of token. */
  public actual val type: TokenType,

  /** Inner token value. */
  @Sensitive public actual val value: TokenValue,
)

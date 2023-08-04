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

package elide.runtime.intrinsics.js.err

import elide.vm.annotations.Polyglot

/** TBD. */
public abstract class Error : AbstractJSException, RuntimeException() {
  /**
   * TBD.
   */
  @get:Polyglot public abstract override val message: String

  /**
   * TBD.
   */
  @get:Polyglot public abstract val name: String

  /**
   * TBD.
   */
  @get:Polyglot public override val cause: Error? get() = null

  /**
   * TBD.
   */
  @get:Polyglot public open val fileName: String? get() = null

  /**
   * TBD.
   */
  @get:Polyglot public open val lineNumber: Int? get() = null

  /**
   * TBD.
   */
  @get:Polyglot public open val columnNumber: Int? get() = null

  /**
   * TBD.
   */
  @get:Polyglot public val stackTrace: Stacktrace get() {
    TODO("not yet implemented")
  }
}

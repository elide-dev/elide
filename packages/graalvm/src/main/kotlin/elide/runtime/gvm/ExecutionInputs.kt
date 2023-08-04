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

package elide.runtime.gvm

/**
 * TBD.
 */
public sealed interface ExecutionInputs {
  /**
   * TBD.
   */
  public fun allInputs(): Array<out Any>

  /**
   * TBD.
   */
  public fun buildArguments(): Array<out Any> = allInputs()

  /** Singleton: Empty execution inputs. */
  public object Empty : ExecutionInputs {
    override fun allInputs(): Array<out Any> = emptyArray()
  }

  /** Factory for instances of [ExecutionInputs]. */
  public companion object {
    /** Singleton representing an empty set of execution inputs. */
    @JvmStatic public val EMPTY: ExecutionInputs = Empty
  }
}

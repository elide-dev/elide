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

package elide.tool.cli.control

import java.util.*
import java.util.function.Supplier
import kotlin.concurrent.Volatile

/**
 * Represents a lazily computed value. Ensures that a single thread runs the computation.
 */
class Lazy<T> private constructor(private val supplier: Supplier<T>?) : Supplier<T?> {
  @Volatile
  private var ref: T? = null

  /**
   * If the supplier returns `null`, [NullPointerException] is thrown. Exceptions
   * thrown by the supplier will be propagated. If the supplier returns a non-null object, it will
   * be cached and the computation is considered finished. The supplier is guaranteed to run on a
   * single thread. A successful computation ([Supplier.get] returns a non-null object) is
   * guaranteed to be executed only once.
   *
   * @return the computed object, guaranteed to be non-null
   */
  override fun get(): T? {
    var localRef = ref
    if (localRef == null) {
      synchronized(this) {
        localRef = ref
        if (localRef == null) {
          localRef = Objects.requireNonNull(supplier!!.get())
          ref = localRef
        }
      }
    }
    return localRef
  }

  companion object {
    /**
     * (Not so) Lazy value that does not run a computation.
     */
    fun <T> value(nonNullValue: T): Lazy<T> {
      val result = Lazy<T>(null)
      result.ref = Objects.requireNonNull(nonNullValue)
      return result
    }

    /**
     * @param supplier if the supplier returns null, [.get] will throw
     * [NullPointerException]
     */
    fun <V> of(supplier: Supplier<V>): Lazy<V> {
      return Lazy(Objects.requireNonNull(supplier))
    }
  }
}

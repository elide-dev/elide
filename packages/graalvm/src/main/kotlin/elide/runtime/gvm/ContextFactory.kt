/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

import org.graalvm.polyglot.Engine
import java.util.stream.Stream
import elide.runtime.gvm.internals.VMProperty

/**
 * TBD.
 */
public interface ContextFactory<Context, Builder> {
  /**
   * TBD.
   */
  public fun configureVM(props: Stream<VMProperty>)

  /**
   * TBD.
   */
  public fun installContextFactory(factory: (Engine) -> Builder)

  /**
   * TBD.
   */
  public fun installContextConfigurator(factory: (Builder) -> Unit)

  /**
   * TBD.
   */
  public fun installContextSpawn(factory: (Builder) -> Context)

  /**
   * TBD.
   */
  public fun activate(start: Boolean = false)

  /**
   * TBD.
   */
  public fun <R> acquire(builder: ((Builder) -> Unit)? = null, operation: Context.() -> R): R

  /**
   * TBD.
   */
  public operator fun <R> invoke(operation: Context.() -> R): R = acquire(null, operation)
}
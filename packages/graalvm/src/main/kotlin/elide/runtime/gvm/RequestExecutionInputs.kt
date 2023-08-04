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

import elide.vm.annotations.Polyglot

/**
 * # Execution Inputs: Server Request
 *
 * Implements [ExecutionInputs] for a server-side request execution. Request executions fulfill SSR-style serving
 * requests via a guest VM execution. This interface is implemented with generic types to allow multiple higher-order
 * server interfaces to be supported via a single VM dispatch system.
 *
 * &nbsp;
 *
 * ## Request State
 *
 * HTTP request state includes such variables as the `path`, `method`, `headers`, and `cookies`, and so on. Each are
 * provided by this interface. See method or property documentation for more information.
 *
 * @param Request concrete request type adapted by an implementor of this interface.
 * @see MicronautRequestExecutionInputs Micronaut implementation of this interface.
 */
public interface RequestExecutionInputs<Request : Any> : ExecutionInputs {
  /** Default values to use for request inputs. */
  public object Defaults {
    /** Default path value to use. */
    public const val DEFAULT_PATH: String = "/"
  }

  /**
   * ## HTTP: Request
   *
   * The underlying request object mapped by this set of execution inputs may be used on the privileged side of the VM
   * border for caching, logging, or other purposes. Implementations are not encouraged to expose this property to the
   * underlying VM.
   *
   * @return HTTP request backing this set of inputs.
   */
  public fun request(): Request

  /**
   * ## HTTP: Path
   *
   * The HTTP path of the request. Should always begin with a `/`. Null values are not allowed; if no value is available
   * the default value of `/` should be used.
   *
   * @return HTTP path value, or `/` as a default if none is available.
   */
  @Polyglot public fun path(): String {
    return Defaults.DEFAULT_PATH
  }

  /** @inheritDoc */
  override fun allInputs(): Array<Any> = arrayOf(request())
}

/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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
package elide.runtime.intrinsics.secrets

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyObject
import elide.annotations.API
import elide.vm.annotations.Polyglot

/**
 * # API for accessing secrets.
 *
 * @author Lauri Heino <datafox>
 */
@API public interface SecretsAPI : ProxyObject {
  /**
   * Returns the secret associated with [name], `null` if none exists or throws an exception if secrets were not
   * initialized properly or [name] is not a string.
   */
  @Polyglot public fun get(
    name: Value,
  ): Value
}

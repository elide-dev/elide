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

package elide.runtime.intrinsics.js.express

import org.graalvm.polyglot.Value
import elide.vm.annotations.Polyglot

/**
 * Represents a very basic binding for a response object passed to an Express handler function.
 * 
 * Note that this interface does not cover many fields and methods expected by regular Express applications, as it is
 * intended only for demonstration purposes.
 */
public interface ExpressResponse {
  /** Sends this response with the given [body]. */
  @Polyglot public fun send(body: Value)
}

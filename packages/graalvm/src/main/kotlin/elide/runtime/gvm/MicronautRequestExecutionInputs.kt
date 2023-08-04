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

import io.micronaut.http.HttpRequest
import elide.vm.annotations.Polyglot

/**
 * # Execution Inputs: Server Request
 *
 * Implements an [ExecutionInputs] interface for a Micronaut server [HttpRequest], optionally with additional [Data] to
 * include as "execution state."
 */
public interface MicronautRequestExecutionInputs<Data> : RequestExecutionInputs<HttpRequest<Data>> {
  /** @inheritDoc */
  @Polyglot override fun path(): String = request().path
}

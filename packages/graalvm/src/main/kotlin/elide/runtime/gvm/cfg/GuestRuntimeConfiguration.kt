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

package elide.runtime.gvm.cfg

import io.micronaut.core.util.Toggleable

/**
 * # Configuration: Guest VMs
 *
 * Defines the surface area of expected configuration for each guest runtime / VM.
 *
 * Note that each guest runtime VM configuration also implements [Toggleable]. If a VM runtime is shut off, it does not
 * load at server startup and is not available to execute user code.
 *
 * @see GuestVMConfiguration for concrete configuration properties.
 */
public interface GuestRuntimeConfiguration : Toggleable {
  // Nothing at this time.
}

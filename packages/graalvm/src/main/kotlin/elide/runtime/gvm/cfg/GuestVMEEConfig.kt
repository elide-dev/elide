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

import io.micronaut.context.annotation.ConfigurationProperties

/**
 * # Guest VM Configuration: GraalVM EE
 *
 * This configuration structure defines options which are only available in GraalVM Enterprise Edition (EE). Options
 * specified within the scope of this object are only applied when running on EE; when running on Community Edition (CE)
 * the options are inert.
 *
 * @see GuestVMEESandboxConfig for sandbox configuration properties.
 */
@Suppress("MemberVisibilityCanBePrivate")
@ConfigurationProperties("elide.gvm.enterprise")
internal interface GuestVMEEConfig {
  /**
   * @return Configuration for VM sandbox and resource limits.
   */
  val sandbox: GuestVMEESandboxConfig? get() = null
}

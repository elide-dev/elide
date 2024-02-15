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

package elide.embedded.api

import tools.elide.call.HostConfiguration
import tools.elide.call.HostConfigurationOrBuilder

/**
 *
 */
public class InstanceConfiguration private constructor (private val config: HostConfiguration) :
  HostConfigurationOrBuilder by config {
  public companion object {
    /**
     *
     */
    @JvmStatic public fun createFrom(host: HostConfiguration): InstanceConfiguration = InstanceConfiguration(host)

    /**
     *
     */
    @JvmStatic public fun loadNative(native: NativeConfiguration): InstanceConfiguration = InstanceConfiguration(
      native.applyTo(HostConfiguration.newBuilder()).build()
    )
  }

  /** @return Host configuration payload. */
  public val host: HostConfiguration get() = config
}

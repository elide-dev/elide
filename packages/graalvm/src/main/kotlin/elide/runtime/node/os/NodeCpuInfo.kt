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
package elide.runtime.node.os

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import elide.runtime.intrinsics.js.node.os.CPUInfo
import elide.runtime.intrinsics.js.node.os.CPUTimes
import elide.vm.annotations.Polyglot

// Implements CPU info as a JVM record.
@JvmRecord @Introspected @ReflectiveAccess internal data class CPUInfoData(
  @get:Polyglot override val model: String,
  @get:Polyglot override val speed: Long,
  @get:Polyglot override val times: CPUTimes
) : CPUInfo

// Implements CPU timings as a JVM record.
@JvmRecord @Introspected @ReflectiveAccess internal data class CPUTimingsData(
  @get:Polyglot override val user: Long,
  @get:Polyglot override val nice: Long,
  @get:Polyglot override val sys: Long,
  @get:Polyglot override val idle: Long,
  @get:Polyglot override val irq: Long
) : CPUTimes

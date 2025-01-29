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
@file:Suppress("MatchingDeclarationName")

package elide.runtime.node.os

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import elide.runtime.intrinsics.js.node.os.NetworkInterfaceInfo
import elide.vm.annotations.Polyglot


// Implements network interface info as a JVM record.
@JvmRecord @Introspected @ReflectiveAccess internal data class NetworkInterfaceInfoData(
  @get:Polyglot override val address: String,
  @get:Polyglot override val netmask: String,
  @get:Polyglot override val family: String,
  @get:Polyglot override val mac: String,
  @get:Polyglot override val internal: Boolean,
  @get:Polyglot override val cidr: String,
  @get:Polyglot override val scopeid: Int?,
) : NetworkInterfaceInfo

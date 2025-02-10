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

import org.graalvm.polyglot.Value
import elide.runtime.intrinsics.js.node.os.Win32SystemConstants

/**
 * Constants for Win32
 *
 * Defines constants which are provided at the `os.constants` module property for Win32-style operating systems.
 */
@Suppress("MemberVisibilityCanBePrivate")
public data object Win32Constants: Win32SystemConstants {
  override fun getMemberKeys(): Array<String> = emptyArray()
  override fun getMember(key: String?): Any? = null
  override fun hasMember(key: String?): Boolean = key != null && key in memberKeys
  override fun putMember(key: String?, value: Value?): Unit = error("Cannot modify `os.constants`")
}

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
import elide.runtime.intrinsics.js.node.os.OperatingSystemConstants.PriorityConstants
import elide.vm.annotations.Polyglot
import elide.runtime.intrinsics.js.node.os.PRIORITY_ABOVE_NORMAL as PRIORITY_ABOVE_NORMAL_CONST
import elide.runtime.intrinsics.js.node.os.PRIORITY_BELOW_NORMAL as PRIORITY_BELOW_NORMAL_CONST
import elide.runtime.intrinsics.js.node.os.PRIORITY_HIGH as PRIORITY_HIGH_CONST
import elide.runtime.intrinsics.js.node.os.PRIORITY_HIGHEST as PRIORITY_HIGHEST_CONST
import elide.runtime.intrinsics.js.node.os.PRIORITY_LOW as PRIORITY_LOW_CONST
import elide.runtime.intrinsics.js.node.os.PRIORITY_NORMAL as PRIORITY_NORMAL_CONST

/**
 * Priority constants; defines constants which relate to OS process priority.
 */
public data object Priority : PriorityConstants {
  @get:Polyglot public override val PRIORITY_LOW: Int = PRIORITY_LOW_CONST
  @get:Polyglot public override val PRIORITY_BELOW_NORMAL: Int = PRIORITY_BELOW_NORMAL_CONST
  @get:Polyglot public override val PRIORITY_NORMAL: Int = PRIORITY_NORMAL_CONST
  @get:Polyglot public override val PRIORITY_ABOVE_NORMAL: Int = PRIORITY_ABOVE_NORMAL_CONST
  @get:Polyglot public override val PRIORITY_HIGH: Int = PRIORITY_HIGH_CONST
  @get:Polyglot public override val PRIORITY_HIGHEST: Int = PRIORITY_HIGHEST_CONST

  override fun getMemberKeys(): Array<String> = arrayOf(
    "PRIORITY_LOW",
    "PRIORITY_BELOW_NORMAL",
    "PRIORITY_NORMAL",
    "PRIORITY_ABOVE_NORMAL",
    "PRIORITY_HIGH",
    "PRIORITY_HIGHEST",
  )

  override fun hasMember(key: String?): Boolean = key != null && key in memberKeys
  override fun putMember(key: String?, value: Value?): Unit = error("Cannot modify `os.constants`")

  override fun getMember(key: String?): Any? = when (key) {
    "PRIORITY_LOW" -> PRIORITY_LOW
    "PRIORITY_HIGH" -> PRIORITY_HIGH
    "PRIORITY_NORMAL" -> PRIORITY_NORMAL
    "PRIORITY_ABOVE_NORMAL" -> PRIORITY_ABOVE_NORMAL
    "PRIORITY_BELOW_NORMAL" -> PRIORITY_BELOW_NORMAL
    "PRIORITY_HIGHEST" -> PRIORITY_HIGHEST
    else -> null
  }
}

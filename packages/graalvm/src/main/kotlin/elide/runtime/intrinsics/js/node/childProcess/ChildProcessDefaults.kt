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
package elide.runtime.intrinsics.js.node.childProcess

import org.graalvm.polyglot.Value
import elide.runtime.gvm.js.JsError

/**
 * ## Child Process: Defaults
 *
 * Statically describes defaults which are used throughout the Node Child Process API; defaults are typically aliased
 * into local classes or modules where needed, and exposed only from this object when shared.
 */
internal object ChildProcessDefaults {
  /** Default encoding token to use. */
  const val ENCODING: String = "buffer"

  /** Default signal to send for killing a process. */
  const val SIGNAL_SIGKILL: String = "SIGKILL"

  /** Default signal to send for terminating a process. */
  const val SIGNAL_SIGTERM: String = "SIGTERM"

  /** Default maximum size for buffered process I/O. */
  const val MAX_BUFFER_DEFAULT: Int = 1024 * 1024

  /** Whether to hide the spawned process on Windows platforms. Inert on other platforms. */
  const val WINDOWS_HIDE: Boolean = false

  // Decode a guest value as an environment map.
  fun decodeEnvMap(value: Value): Map<String, String>? = when {
    value.isNull -> null
    value.hasMembers() -> {
      val members = value.memberKeys
      val envMap = mutableMapOf<String, String>()
      for (key in members) {
        val member = value.getMember(key)
        envMap[key] = member.asString()
      }
      envMap
    }
    value.isHostObject -> value.asHostObject()
    else -> throw JsError.typeError("Invalid type provided as `env` in child process options")
  }
}

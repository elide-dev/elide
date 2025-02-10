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
package elide.runtime.intrinsics.js.node.os

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyObject
import elide.runtime.intrinsics.js.node.fs.StringOrBuffer
import elide.vm.annotations.Polyglot

/**
 * ## Operating System: User Info
 *
 * Describes information provided by the underlying operating system about the active OS user.
 */
public interface UserInfo : ProxyObject {
  /**
   * The username of the user.
   */
  @get:Polyglot public val username: StringOrBuffer

  /**
   * The user's home directory.
   */
  @get:Polyglot public val homedir: StringOrBuffer

  /**
   * The user's shell.
   */
  @get:Polyglot public val shell: StringOrBuffer?

  /**
   * The user's UID.
   */
  @get:Polyglot public val uid: Long

  /**
   * The user's GID.
   */
  @get:Polyglot public val gid: Long

  override fun getMemberKeys(): Array<String> = arrayOf(
    "username",
    "homedir",
    "shell",
    "uid",
    "gid"
  )

  override fun getMember(key: String): Any? = when (key) {
    "username" -> username
    "homedir" -> homedir
    "shell" -> shell
    "uid" -> uid
    "gid" -> gid
    else -> throw IllegalArgumentException("Unknown key: $key")
  }

  override fun hasMember(key: String?): Boolean = key in memberKeys

  override fun putMember(key: String?, value: Value?) {
    throw UnsupportedOperationException("UserInfo is read-only")
  }

  override fun removeMember(key: String?): Boolean {
    throw UnsupportedOperationException("UserInfo is read-only")
  }

  public companion object {
    /**
     * Creates a new instance of [UserInfo].
     */
    @JvmStatic public fun of(
      username: StringOrBuffer,
      homedir: StringOrBuffer,
      shell: StringOrBuffer,
      uid: Long,
      gid: Long
    ): UserInfo = object : UserInfo {
      override val username: StringOrBuffer = username
      override val homedir: StringOrBuffer = homedir
      override val shell: StringOrBuffer = shell
      override val uid: Long = uid
      override val gid: Long = gid
    }
  }
}

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

package elide.tool.engine

/**
 * TBD.
 */
interface NativeLibInfo : Comparable<NativeLib> {
  /** Group which the native library belongs to. */
  val group: String

  /** Name for the native library. */
  val name: String

  /** Full anticipated native library path. */
  val path: String

  /** Static loadable name for the library. */
  val staticName: String

  override fun compareTo(other: NativeLib): Int {
    return name.compareTo(other.name)
  }
}

/**
 * TBD.
 */
@JvmInline value class NativeLib private constructor (private val info: LibInfo) : NativeLibInfo by info {
  /** Managed library info. */
  @JvmRecord data class LibInfo(
    override val group: String,
    override val name: String,
    override val path: String,
    override val staticName: String,
  ): NativeLibInfo

  companion object {
    /**
     * Create native library info.
     *
     * @param group Group which the library belongs to.
     * @param name Name of the library.
     * @param path Full path to the library.
     * @param staticName Static loadable name for the library.
     * @return Native library info.
     */
    @JvmStatic fun of(group: String, name: String, path: String, staticName: String): NativeLib =
      NativeLib(LibInfo(group, name, path, staticName))
  }
}

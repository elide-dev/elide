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

package elide.runtime.feature

import elide.runtime.feature.NativeLibraryFeature.NativeLibType.STATIC

/**
 * TBD.
 */
internal interface NativeLibraryFeature : FrameworkFeature {
  /** Types of linking for native libraries. */
  enum class NativeLibType {
    SHARED,
    STATIC
  }

  /** Native library info used at build-time. */
  @JvmRecord data class NativeLibInfo(
    val name: String,
    val type: NativeLibType,
    val prefix: String = name,
    val registerPrefix: Boolean = false,
    val registerJni: Boolean = true,
  ) {
    companion object {
      @JvmStatic fun of(name: String, type: NativeLibType = STATIC, prefix: Boolean = false, jni: Boolean = true) =
        NativeLibInfo(name, type, registerPrefix = prefix, registerJni = jni)
    }
  }
}

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

package elide.runtime.feature

import java.nio.file.Path

public interface NativeLibraryFeature : FrameworkFeature {
  /** Types of linking for native libraries. */
  public enum class NativeLibType {
    SHARED,
    STATIC,
    DUAL,
  }

  /** Native library info used at build-time. */
  @JvmRecord public data class UnpackedNative(
    val name: String,
    val resource: String,
    val arch: String,
    val path: Path,
  )

  /** Native library info used at build-time. */
  @JvmRecord public data class NativeLibInfo(
    val name: String,
    val prefix: List<String>,
    val type: NativeLibType,
    val linkName: String = name,
    val registerPrefix: Boolean,
    val registerJni: Boolean,
    val builtin: Boolean,
    val eager: Boolean,
    val absolutePath: Path?,
    val absoluteLibs: Pair<Path?, Path?>?,
    val initializer: Boolean,
    val deps: List<String>,
  ) {
    public companion object {
      @Suppress("LongParameterList")
      @JvmStatic public fun of(
        name: String,
        vararg layout: String,
        type: NativeLibType = NativeLibType.STATIC,
        linkName: String = name,
        prefix: Boolean,
        jni: Boolean,
        builtin: Boolean,
        eager: Boolean,
        absolutePath: Path?,
        absoluteLibs: Pair<Path?, Path?>?,
        initializer: Boolean,
        deps: List<String>,
      ): NativeLibInfo = NativeLibInfo(
        name = name,
        prefix = layout.toList(),
        linkName = linkName,
        type = type,
        registerPrefix = prefix,
        registerJni = jni,
        builtin = builtin,
        eager = eager,
        absolutePath = absolutePath,
        absoluteLibs = absoluteLibs,
        initializer = initializer,
        deps = deps,
      )
    }
  }
}

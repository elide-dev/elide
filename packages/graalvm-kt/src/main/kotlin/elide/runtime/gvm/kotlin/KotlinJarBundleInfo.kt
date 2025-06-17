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
@file:OptIn(DelicateElideApi::class)

package elide.runtime.gvm.kotlin

import org.graalvm.polyglot.Value
import java.nio.file.Path
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.precompiler.Precompiler.BundleInfo

public data class KotlinJarBundleInfo(
  override val name: String,
  override val path: Path,
  public val entrypoint: String? = null,
) : BundleInfo, KotlinRunnable {
  override fun apply(context: PolyglotContext): Value? = null // Always null: this represents a JAR on disk.
  override fun toString(): String {
    return "KotlinJarBundleInfo(name='$name', path=$path, entrypoint=${entrypoint ?: "<none>"})"
  }
}

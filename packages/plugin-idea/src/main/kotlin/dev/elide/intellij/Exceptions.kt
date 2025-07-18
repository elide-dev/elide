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
package dev.elide.intellij

import com.intellij.openapi.util.io.toCanonicalPath
import java.nio.file.Path

sealed class ElidePluginException : RuntimeException()

class InvalidElideHomeException(path: String) : ElidePluginException() {
  constructor(path: Path) : this(path.toCanonicalPath())
  override val message: String = "Invalid Elide distribution specified: $path"
}

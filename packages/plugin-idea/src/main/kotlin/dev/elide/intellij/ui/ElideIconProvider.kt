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

package dev.elide.intellij.ui

import com.intellij.openapi.externalSystem.ui.ExternalSystemIconProvider
import dev.elide.intellij.Constants
import javax.swing.Icon

/** Provides UI icons for the project sync reload button and other elements. */
class ElideIconProvider : ExternalSystemIconProvider {
  override val reloadIcon: Icon = Constants.Icons.RELOAD_PROJECT
}

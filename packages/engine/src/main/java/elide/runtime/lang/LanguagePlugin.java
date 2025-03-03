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

package elide.runtime.lang;

import org.jetbrains.annotations.NotNull;

/**
 * Language Plugin
 *
 * <p>Describes the expected interface for language engine plugins; language plugins are loaded by the main engine at
 * runtime. In native circumstances, language plugins are installed on-demand as shared libraries. Plugins are also made
 * available on the classpath via the Service Loader.</p>
 */
public interface LanguagePlugin {
  /**
   * Get this language's string ID; this is a short string identifying the language.
   *
   * @return Language ID string.
   */
  @NotNull Class<? extends Language> getLanguageClass();
}

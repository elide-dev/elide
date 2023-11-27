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

package elide.runtime.plugins.kotlin

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.plugins.AbstractLanguageConfig

/** Configuration for the [Kotlin] plugin. */
@DelicateElideApi public class KotlinConfig internal constructor() : AbstractLanguageConfig() {
  /**
   * Sets the directory where custom classpath entries required for the guest context are located. The plugin will
   * extract the classpath entries if they are not present at the specified path.
   *
   * This path should typically be temporary (such as a /tmp directory on Unix) to avoid unintentional residues of
   * after the application finishes executing. A reasonable platform-specific default value will be used if this
   * property is not explicitly set.
   */
  public var guestClasspathRoot: String = defaultClasspathRoot()

  private companion object {
    /** Resolve a platform-specific temporary directory used to extract guest classpath entries. */
    private fun defaultClasspathRoot(): String {
      return "${System.getProperty("java.io.tmpdir")}/elide-runtime-kt/classpath"
    }
  }
}

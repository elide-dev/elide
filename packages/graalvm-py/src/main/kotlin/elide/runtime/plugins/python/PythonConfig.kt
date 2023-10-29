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

package elide.runtime.plugins.python

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.plugins.AbstractLanguageConfig

@DelicateElideApi public class PythonConfig(fileSystemRoot: String) : AbstractLanguageConfig() {
  /** Path to the Python home directory (GraalVM's python.PythonHome property). */
  internal val pythonHome: String = "$fileSystemRoot$PYTHON_HOME"

  /** Path to the Python standard library (GraalVM's python.StdLibHome property). */
  internal val stdLibHome: String = "$pythonHome$STD_LIB_HOME"

  /** Path to the Python root (GraalVM's python.CoreHome property). */
  internal val coreHome: String = "$pythonHome$CORE_HOME"

  /** Apply init-time settings to a new [context]. */
  internal fun applyTo(context: PolyglotContext) {
    // register intrinsics
    applyBindings(context, Python)
  }

  private companion object {
    private const val STD_LIB_HOME = "/lib/python3.10"
    private const val CORE_HOME = "/lib/graalpy23.1"
    private const val PYTHON_HOME = "python/python-home"
  }
}

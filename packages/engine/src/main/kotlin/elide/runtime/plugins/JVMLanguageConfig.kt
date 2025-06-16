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

package elide.runtime.plugins

import java.nio.file.Path
import java.util.LinkedList
import elide.runtime.core.DelicateElideApi

/**
 * ## JVM Language Configuration
 *
 * Extends the base [AbstractLanguageConfig] with options specific to JVM languages.
 */
public abstract class JVMLanguageConfig : AbstractLanguageConfig() {
  /**
   * Sets the directory where custom classpath entries required for the guest context are located. The plugin will
   * extract the classpath entries if they are not present at the specified path.
   *
   * This path should typically be temporary (such as a /tmp directory on Unix) to avoid unintentional residues of
   * after the application finishes executing. A reasonable platform-specific default value will be used if this
   * property is not explicitly set.
   */
  public var guestClasspathRoots: MutableList<Path> = LinkedList()

  /**
   * Sets the guest-side JAVA_HOME to use, if known.
   */
  public var guestJavaHome: String? = null
}

/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

package elide.runtime.plugins.jvm

import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.pathString
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.plugins.AbstractLanguageConfig

/**
 * Configuration for the [Jvm] plugin.
 *
 * @see classpath
 */
@DelicateElideApi public class JvmConfig internal constructor() : AbstractLanguageConfig() {
  /** Collection of classpath entries passed to Espresso. */
  private val classpathEntries: MutableList<String> = mutableListOf()

  /**
   * Toggle support for multiple threads in the guest context.
   *
   * When using a context where [Jvm] is installed along other languages that don't support threading, this option
   * should be disabled to avoid integration issues.
   */
  public var multithreading: Boolean = true

  /**
   * Set the path to JAVA_HOME that should be used, if known.
   */
  public var guestJavaHome: String? = null

  /** Returns a string representation of the classpath to be used by the guest Espresso JVM. */
  internal fun collectClasspath(): String = classpathEntries.joinToString(CLASSPATH_ENTRY_SEPARATOR)

  /** Add new entries to the guest classpath used by the engine. */
  public fun classpath(vararg paths: String) {
    classpathEntries.addAll(paths)
  }

  /** Add new entries to the guest classpath used by the engine. */
  public fun classpath(paths: Iterable<String>) {
    classpathEntries.addAll(paths)
  }

  /** Add new entries to the guest classpath used by the engine. */
  public fun classpath(paths: Sequence<Path>) {
    classpathEntries.addAll(paths.map { it.absolutePathString() })
  }

  /** Apply init-time settings to a new [context]. */
  @Suppress("UNUSED_PARAMETER") internal fun applyTo(context: PolyglotContext) {
    // Nothing to do at this time.
  }

  private companion object {
    private const val CLASSPATH_ENTRY_SEPARATOR = ":"
  }
}

/**
 * Add new entries to the guest classpath used by the embedded context. Each [Path] will be added in its string
 * representation using [Path.pathString].
 */
@DelicateElideApi public fun JvmConfig.classpath(vararg paths: Path) {
  classpath(paths = Array(paths.size) { paths[it].pathString })
}

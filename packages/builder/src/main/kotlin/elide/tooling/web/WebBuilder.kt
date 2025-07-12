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
package elide.tooling.web

import org.graalvm.nativeimage.ImageInfo
import kotlinx.atomicfu.atomic
import elide.runtime.core.lib.NativeLibraries
import elide.tooling.md.Markdown
import elide.tooling.web.css.CssBuilder

/**
 * # Web Builder
 *
 * Collection of utilities for building web applications and web-related code, such as CSS, HTML, and so on.
 */
public object WebBuilder {
  /**
   * Load the native web builder libraries; if this is not called, then the web builder will not be able to call into
   * native code for the CSS and JS builders.
   *
   * If an error occurs while loading the native library, an exception will be thrown.
   */
  internal fun load() {
    // Load the native library for CSS parsing and building.
    if (!WebBuilderInternals.load()) {
      error("Failed to load native web library")
    }
  }

  /**
   * Obtain tooling for building and manipulating CSS (Cascading Style Sheets) code.
   *
   * @return CSS builder tools.
   */
  public fun css(): CssBuilder = CssBuilder

  /**
   * Obtain tooling for building and manipulating Markdown code in various formats.
   *
   * @return Markdown builder tools.
   */
  public fun markdown(): Markdown = Markdown
}

// Manages the state of loading the native web builder library.
private object WebBuilderInternals {
  private const val NATIVE_WEB_LIB = "web"
  private val initialized = atomic(false)

  // Load the native library for CSS parsing and building.
  @JvmStatic fun load(): Boolean = when (ImageInfo.inImageRuntimeCode()) {
    true -> true // built-in if running in native mode
    else -> when (initialized.value) {
      true -> true // already initialized
      false -> synchronized(this) {
        NativeLibraries.loadLibrary(NATIVE_WEB_LIB).also {
          when (it) {
            true -> initialized.value = true
            else -> error("Failed to load lib$NATIVE_WEB_LIB")
          }
        }
      }
    }
  }
}

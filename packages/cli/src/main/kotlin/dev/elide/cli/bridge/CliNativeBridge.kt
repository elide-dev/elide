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

@file:Suppress("RedundantVisibilityModifier")

package dev.elide.cli.bridge

import org.graalvm.nativeimage.ImageInfo
import elide.exec.Execution
import elide.exec.Tracing

/**
 * # Native Bridge
 *
 * Bridges native methods from Elide's `umbrella` library, via JNI access points.
 */
object CliNativeBridge {
  /** Native platform-agnostic library name for Elide's umbrella library. */
  private const val NATIVE_LIB_NAME = "umbrella"

  /** Whether the native layer has initialized yet. */
  private var initialized: Boolean = false

  /** Initialize the native layer. */
  @Synchronized public fun initialize() {
    if (!initialized && !ImageInfo.inImageCode()) {
      Tracing.ensureLoaded()
      Execution.ensureLoaded()
      System.loadLibrary(NATIVE_LIB_NAME)
      initialized = true
      val init = initializeNative()
      assert(init == 0) { "Failed to initialize native layer; got code $init" }
    }
  }

  /** Initialize the native runtime layer; any non-zero return value indicates an error.  */
  private external fun initializeNative(): Int

  /** Return the tooling protocol version.  */
  external fun apiVersion(): String

  /** Return the library version.  */
  external fun libVersion(): String

  /** Return the suite of reported tool names.  */
  external fun supportedTools(): Array<String>

  /** Return the languages which relate to a given tool.  */
  external fun relatesTo(toolName: String): Array<String>

  /** Return the version string for a tool.  */
  external fun toolVersion(toolName: String): String

  /** Run the Ruff entrypoint.  */
  external fun runRuff(args: Array<String>): Int

  /** Run the Orogene entrypoint.  */
  external fun runOrogene(args: Array<String>): Int

  /** Run the Uv entrypoint.  */
  external fun runUv(args: Array<String>): Int
}

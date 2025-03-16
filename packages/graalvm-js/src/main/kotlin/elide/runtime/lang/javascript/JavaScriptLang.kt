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

package elide.runtime.lang.javascript

import kotlinx.atomicfu.atomic

/**
 * ## JavaScript Language Utilities
 *
 * Static utilities for internal engine use. Manages initialization of key components within Elide's integration with
 * GraalJs; this includes installing the [ElideJsModuleRouter].
 */
public object JavaScriptLang {
  // Whether JavaScript has initialized yet.
  private val initialized = atomic(false)

  /**
   * Obtain an instance of the JavaScript precompiler.
   *
   * @return The JavaScript precompiler.
   */
  public fun precompiler(): JavaScriptPrecompiler = JavaScriptPrecompiler

  /** Initialize the JavaScript language layer. */
  public fun initialize(interop: Boolean = true) {
    if (initialized.compareAndSet(expect = false, update = true)) {
      ElideJsModuleRouter.install(interop)
    }
  }
}

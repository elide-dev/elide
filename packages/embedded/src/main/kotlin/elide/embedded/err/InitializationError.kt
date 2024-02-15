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

package elide.embedded.err

import elide.core.api.Symbolic

/**
 * ## Error: Native Initialization
 *
 * When errors or failures occur early in the embedded boot process, this enumeration is used to describe the error;
 * ultimately, each error is translated into a C-value integer error code or process exit code, depending on context.
 */
public enum class InitializationError : Symbolic<Int> {

}

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
package elide.runtime.exec

import elide.tooling.Tool

public fun Tool.Result.asExecResult(): elide.exec.Result {
  return when (this) {
    is Tool.Result.Success -> elide.exec.Result.Nothing
    is Tool.Result.UnspecifiedFailure -> elide.exec.Result.UnspecifiedFailure
  }
}

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

package elide.embedded

/**
 * Describes the language used by the guest application, which must be supported by the runtime at the time of
 * registration. Source language affects the bindings available at run-time as well as other dispatch features.
 */
public enum class EmbeddedGuestLanguage {
  /** Use JavaScript as guest language. */
  JAVA_SCRIPT,

  /** Use Python as guest language. */
  PYTHON
}

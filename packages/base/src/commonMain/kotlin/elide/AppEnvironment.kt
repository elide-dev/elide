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

package elide

/**
 * Enumerates application environments which can be connected to configuration, secrets, and other behavior.
 *
 * - [SANDBOX]: The application is executing in a development, test, or experimental environment.
 * - [LIVE]: The application is executing in a production environment.
 */
public enum class AppEnvironment {
  /** The application is executing in a development, test, or experimental environment. */
  SANDBOX,

  /** The application is executing in a production environment. */
  LIVE,
}

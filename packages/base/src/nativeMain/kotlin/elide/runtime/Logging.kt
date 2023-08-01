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

package elide.runtime

/** Describes an expected class which is able to produce [Logger] instances as a factory. */
public actual class Logging {
  /**
   * Acquire a [Logger] for the given logger [name], which can be any identifying string; in JVM circumstances, the full
   * class name of the subject which is sending the logs is usually used.
   *
   * @param name Name of the logger to create and return.
   * @return Desired logger.
   */
  public actual fun logger(name: String): Logger {
    TODO("Not yet implemented")
  }

  /**
   * Acquire a root [Logger] which is unnamed, or uses an empty string value (`""`) for the logger name.
   *
   * @return Root logger.
   */
  public actual fun logger(): Logger {
    TODO("Not yet implemented")
  }
}

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

package elide.tool.io

import java.io.File

/**
 * # Working Directory Manager
 *
 * Defines the interface provided by a "working directory manager," which is charged with managing working directory
 * state for temporary space used at runtime; for example, native libraries are unpacked to this location before being
 * loaded.
 */
interface WorkdirManager : AutoCloseable {
  /**
   * Obtain the root temporary working directory for this run.
   *
   * @return Temporary working directory, which may be shared across calls/components; guaranteed to be readable.
   */
  fun temporaryWorkdir(): File

  /**
   * Obtain the directory where native libraries should be unpacked.
   *
   * @return Directory to use for native libraries; guaranteed to be writable and readable.
   */
  fun nativesDirectory(): File

  /**
   * Obtain the directory where temporary data should be written and read.
   *
   * @return Directory to use for temporary data; guaranteed to be writable and readable.
   */
  fun tmpDirectory(): File

  /**
   * Obtain the cache root directory for this run.
   *
   * @return Directory to use for caching; guaranteed to be writable and readable.
   */
  fun cacheDirectory(): File
}

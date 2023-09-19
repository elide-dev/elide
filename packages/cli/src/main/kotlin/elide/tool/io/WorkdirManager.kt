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
import java.nio.file.Path
import kotlin.io.path.absolutePathString

/**
 * # Working Directory Manager
 *
 * Defines the interface provided by a "working directory manager," which is charged with managing working directory
 * state for temporary space used at runtime; for example, native libraries are unpacked to this location before being
 * loaded.
 */
interface WorkdirManager : AutoCloseable {
  /**
   * ## Working Directory: Handle
   *
   * Provided in lieu of a [File], in order to properly manage the lifecycle and lazy-initialization state of a working
   * directory area.
   */
  interface WorkdirHandle {
    /**
     * Whether the working directory described by this handle exists.
     */
    val exists: Boolean

    /**
     * Whether the working directory described by this handle is writable.
     */
    val writable: Boolean

    /**
     * Whether the working directory described by this handle is readable.
     */
    val readable: Boolean

    /**
     * Return the absolute path for this file.
     */
    val absolutePath: String get() = toPath().absolutePathString()

    /**
     * Produce a [File] for this working directory handle.
     *
     * @return File object.
     */
    fun toFile(): File

    /**
     * Produce a [Path] for this working directory handle.
     *
     * @return Path object.
     */
    fun toPath(): Path

    /**
     * @return Whether the file exists.
     */
    fun exists(): Boolean = exists

    /**
     * @return Whether the file can be read.
     */
    fun canRead(): Boolean = readable

    /**
     * @return Whether the file can be written to.
     */
    fun canWrite(): Boolean = writable

    /**
     * @return Resolved file for the provided [path].
     */
    fun resolve(path: String): File = toFile().resolve(path)

    /**
     * @return Resolved file for the provided [file].
     */
    fun resolve(file: File): File = toFile().resolve(file)
  }

  /**
   * Obtain the root temporary working directory for this run.
   *
   * @return Temporary working directory, which may be shared across calls/components; guaranteed to be readable.
   */
  fun workingRoot(): File

  /**
   * Obtain the directory where native libraries should be unpacked.
   *
   * @return Directory to use for native libraries; guaranteed to be writable and readable.
   */
  fun nativesDirectory(): WorkdirHandle

  /**
   * Obtain the directory where errors and flight recorder events should be written.
   *
   * @param create Proactively create the flight recorder directory before returning.
   * @return Directory to use for errors and flight recorder events; guaranteed to be writable and readable.
   */
  fun flightRecorderDirectory(create: Boolean = false): WorkdirHandle

  /**
   * Obtain the directory where temporary data should be written and read.
   *
   * @param create Proactively create the flight recorder directory before returning.
   * @return Directory to use for temporary data; guaranteed to be writable and readable.
   */
  fun tmpDirectory(create: Boolean = false): WorkdirHandle

  /**
   * Obtain the cache root directory for this run.
   *
   * @param create Proactively create the flight recorder directory before returning.
   * @return Directory to use for caching; guaranteed to be writable and readable.
   */
  fun cacheDirectory(create: Boolean = false): WorkdirHandle
}

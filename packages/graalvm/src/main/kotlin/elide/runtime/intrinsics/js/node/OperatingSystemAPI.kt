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
@file:Suppress("VariableNaming")

package elide.runtime.intrinsics.js.node

import org.graalvm.polyglot.Value
import elide.annotations.API
import elide.runtime.intrinsics.js.node.os.*
import elide.vm.annotations.Polyglot

/**
 * # Node API: `os`
 *
 * Describes the API provided by the Node API built-in `os` module, which supplies operating system-related utilities,
 * such as load averages, OS constants, resource usage, and more. Access to these methods is moderated by the active
 * Elide security policy specified by the user.
 *
 * &nbsp;
 *
 * ## Summary
 *
 * The default implementation of the `os` module supplies cross-platform logic, which works on POSIX and Win32-style
 * systems. Operating system APIs are called on-demand. Where access is not granted to such APIs, mock or stubbed values
 * are supplied, or errors are thrown, as applicable.
 */
@API public interface OperatingSystemAPI : NodeAPI {
  /**
   * Type of operating system this instance implements calls for; not exposed to the guest VM.
   */
  public val family: OSType

  /**
   * ## Property: `os.EOL`
   *
   * The end-of-line marker used by the operating system.
   */
  @Suppress("PropertyName")
  @get:Polyglot public val EOL: String

  /**
   * ## Property: `os.constants`
   *
   * Returns an object containing commonly-used operating system constants for error codes, process signals, and more.
   */
  @get:Polyglot public val constants: OperatingSystemConstants

  /**
   * ## Property: `os.devNull`
   *
   * The path to the operating system's null device.
   */
  @get:Polyglot public val devNull: String

  /**
   * ## Method: `os.availableParallelism()`
   *
   * Returns the number of logical CPUs available to the Node.js instance.
   *
   * @return The number of logical CPUs available to the Node.js instance.
   */
  @Polyglot public fun availableParallelism(): Int

  /**
   * ## Method: `os.arch()`
   *
   * Returns the architecture of the operating system.
   *
   * @return The architecture of the operating system.
   */
  @Polyglot public fun arch(): String

  /**
   * ## Method: `os.cpus()`
   *
   * Returns an array of objects containing information about each CPU/core installed on the system.
   *
   * @return An array of objects describing each system CPU.
   */
  @Polyglot public fun cpus(): List<CPUInfo>

  /**
   * ## Method: `os.endianness()`
   *
   * Returns the endianness of the CPU.
   *
   * @return The endianness of the CPU.
   */
  @Polyglot public fun endianness(): String

  /**
   * ## Method: `os.freemem()`
   *
   * Returns the amount of free system memory in bytes.
   *
   * @return The amount of free system memory in bytes.
   */
  @Polyglot public fun freemem(): Long

  /**
   * ## Method: `os.getPriority(pid: Long)`
   *
   * Returns the scheduling priority of a process, specified by its process ID.
   *
   * @param pid The process ID of the process to query.
   * @return The scheduling priority of the process.
   */
  @Polyglot public fun getPriority(pid: Value? = null): Int

  /**
   * ## Method: `os.homedir()`
   *
   * Returns the path to the current user's home directory.
   *
   * @return The path to the current user's home directory.
   */
  @Polyglot public fun homedir(): String

  /**
   * ## Method: `os.hostname()`
   *
   * Returns the hostname of the operating system.
   *
   * @return The hostname.
   */
  @Polyglot public fun hostname(): String

  /**
   * ## Method: `os.loadavg()`
   *
   * Returns an array containing the 1, 5, and 15 minute load averages.
   *
   * @return An array containing the load averages.
   */
  @Polyglot public fun loadavg(): List<Double>

  /**
   * ## Method: `os.networkInterfaces()`
   *
   * Returns an object containing information about the network interfaces on the system.
   *
   * @return An object containing information about each NIC.
   */
  @Polyglot public fun networkInterfaces(): Map<String, List<NetworkInterfaceInfo>>

  /**
   * ## Method: `os.platform()`
   *
   * Returns the operating system platform.
   *
   * @return The operating system platform.
   */
  @Polyglot public fun platform(): String

  /**
   * ## Method: `os.release()`
   *
   * Returns the operating system release.
   *
   * @return The release value.
   */
  @Polyglot public fun release(): String

  /**
   * ## Method: `os.setPriority(pid: Long, priority: Int)`
   *
   * Sets the scheduling priority of a process, specified by its process ID.
   *
   * @param pid The process ID of the process to modify; defaults to `0`.
   * @param priority The new scheduling priority for the process.
   */
  @Polyglot public fun setPriority(pid: Value? = Value.asValue(0), priority: Value? = Value.asValue(PRIORITY_NORMAL))

  /**
   * ## Method: `os.tmpdir()`
   *
   * Returns the operating system's default directory for temporary files.
   *
   * @return The path to the temporary directory.
   */
  @Polyglot public fun tmpdir(): String

  /**
   * ## Method: `os.totalmem()`
   *
   * Returns the total amount of system memory in bytes.
   *
   * @return The total amount of system memory in bytes.
   */
  @Polyglot public fun totalmem(): Long

  /**
   * ## Method: `os.type()`
   *
   * Returns the operating system name.
   *
   * @return The operating system name.
   */
  @Polyglot public fun type(): String

  /**
   * ## Method: `os.uptime()`
   *
   * Returns the system uptime in seconds.
   *
   * @return The system uptime in seconds.
   */
  @Polyglot public fun uptime(): Double

  /**
   * ## Method: `os.userInfo(options: UserInfoOptions)`
   *
   * Returns information about the current user.
   *
   * @param options Options for the user information query.
   */
  @Polyglot public fun userInfo(options: UserInfoOptions? = null): UserInfo?

  /**
   * ## Method: `os.version()`
   *
   * Returns the operating system version.
   *
   * @return The operating system version.
   */
  @Polyglot public fun version(): String
}

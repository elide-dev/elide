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
@file:Suppress("TopLevelPropertyNaming", "MagicNumber")

package elide.runtime.node.os

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import org.jetbrains.annotations.VisibleForTesting
import oshi.SystemInfo
import java.lang.management.ManagementFactory
import java.lang.management.OperatingSystemMXBean
import java.net.InetAddress
import java.nio.ByteOrder
import elide.annotations.Factory
import elide.annotations.Singleton
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.js.JsError
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.OperatingSystemAPI
import elide.runtime.intrinsics.js.node.fs.StringOrBuffer
import elide.runtime.intrinsics.js.node.os.*
import elide.runtime.intrinsics.js.node.os.OSType.POSIX
import elide.runtime.intrinsics.js.node.os.OSType.WIN32
import elide.runtime.lang.javascript.SyntheticJSModule
import elide.runtime.node.childProcess.ChildProcessNative
import elide.vm.annotations.Polyglot

// Name of this module.
internal const val NODE_OS_NAME: String = "os"

// Constants for cross-OS values.
private const val win32EOL: String = "\r\n"
private const val posixEOL: String = "\n"
private const val win32DevNull: String = "\\\\.\\nul"
private const val posixDevNull: String = "/dev/null"

// Defaults for stubbed OS values.
private const val STUBBED_ARCH: String = "amd64"
private const val STUBBED_ENDIANNESS: String = ENDIAN_LITTLE
private const val STUBBED_FREEMEM: Long = /* 1gb */ 1024 * 1024 * 1024
private const val STUBBED_HOMEDIR: String = "/home/user"
private const val STUBBED_HOSTNAME: String = "apphost"
private const val STUBBED_PLATFORM: String = "linux"
private const val STUBBED_RELEASE: String = "5.4.0-80-generic"
private const val STUBBED_TMPDIR: String = "/tmp"
private const val STUBBED_TOTALMEM: Long = /* 4gb */ 4 * STUBBED_FREEMEM
private const val STUBBED_TYPE: String = "Linux"
private const val STUBBED_UPTIME: Double = 12345.6789
private const val STUBBED_PRIORITY: OSPriority = PRIORITY_NORMAL
private const val STUBBED_USERINFO_USERNAME: String = "user"
private const val STUBBED_USERINFO_UID: Long = 1000
private const val STUBBED_USERINFO_GID: Long = 1000
private const val STUBBED_USERINFO_SHELL: String = "/bin/bash"
private const val STUBBED_USERINFO_HOMEDIR: String = STUBBED_HOMEDIR
private const val STUBBED_VERSION: String = "5.4.0"

// Generic 'unknown' symbol.
private const val UNKNOWN: String = "unknown"

// NIC constants.
private const val NIC_FAMILY_IPV6 = "IPv6"
private const val NIC_FAMILY_IPV4 = "IPv4"

// Architecture type constants.
private const val NODE_X86 = "ia32"
private const val NODE_X64 = "x64"
private const val NODE_ARM = "arm"
private const val NODE_ARM64 = "arm64"
private const val JVM_X86 = "x86"
private const val JVM_X86_64 = "x86_64"
private const val JVM_AMD64 = "amd64"
private const val JVM_ARM = "arm"
private const val JVM_ARM64 = "arm64"
private const val JVM_AARCH64 = "aarch64"

// OS type constants.
private const val OS_TYPE_LINUX = "Linux"
private const val OS_TYPE_DARWIN = "Darwin"
private const val OS_TYPE_WINDOWS = "Windows_NT"

// OS name constants.
private const val OS_AIX = "aix"
private const val OS_DARWIN = "darwin"
private const val OS_MAC_OS_X = "mac os x"
private const val OS_FREEBSD = "freebsd"
private const val OS_LINUX = "linux"
private const val OS_OPENBSD = "openbsd"
private const val OS_SUNOS = "sunos"
private const val OS_WIN32 = "win32"
private const val OS_WINDOWS = "windows"

// Single stubbed CPU type.
private val STUBBED_CPU = CPUInfo.of(
  "Intel(R) Core(TM) i7-7700HQ CPU @ 2.80GHz",
  2800,
  CPUTimes.of(0, 0, 0, 0, 0),
)

// Stubbed CPUs.
private val STUBBED_CPUS: List<CPUInfo> = listOf(
  STUBBED_CPU,
  STUBBED_CPU,
)

// Stubbed NICs (network interfaces).
private val STUBBED_NICS: Map<String, List<NetworkInterfaceInfo>> = mapOf(
  "lo" to listOf(
    NetworkInterfaceInfo.of(
      "127.0.0.1",
      "255.0.0.0",
      NIC_FAMILY_IPV4,
      "00:00:00:00:00:00",
      internal = true,
      cidr = "127.0.0.1/8",
    ),
    NetworkInterfaceInfo.of(
      "::1",
      "ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff",
      NIC_FAMILY_IPV6,
      "00:00:00:00:00:00",
      scopeid = 0,
      internal = true,
      cidr = "::1/128",
    ),
  ),
  "eth0" to listOf(
    NetworkInterfaceInfo.of(
      "192.168.0.1",
      "255.255.255.0",
      NIC_FAMILY_IPV4,
      "00:00:00:00:00:00",
      internal = false,
      cidr = "192.168.0.1/24",
    ),
  ),
)

// Stubbed user info.
private val STUBBED_USER: UserInfo = UserInfo.of(
  STUBBED_USERINFO_USERNAME,
  STUBBED_USERINFO_HOMEDIR,
  STUBBED_USERINFO_SHELL,
  STUBBED_USERINFO_UID,
  STUBBED_USERINFO_GID,
)

// Module member names.
private val moduleMembers = arrayOf(
  "EOL",
  "devNull",
  "constants",
  "availableParallelism",
  "arch",
  "cpus",
  "endianness",
  "freemem",
  "getPriority",
  "homedir",
  "hostname",
  "loadavg",
  "networkInterfaces",
  "platform",
  "release",
  "setPriority",
  "tmpdir",
  "totalmem",
  "type",
  "uptime",
  "userInfo",
  "version",
)

// Installs the Node OS module into the intrinsic bindings.
@Intrinsic
@Factory internal class NodeOperatingSystemModule : SyntheticJSModule<OperatingSystemAPI>, AbstractNodeBuiltinModule() {
  // Provide a compliant instance of the OS API to the DI context.
  @Singleton override fun provide(): OperatingSystemAPI = NodeOperatingSystem.obtain()

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NODE_OS_NAME)) {
      ChildProcessNative.initialize()
      NodeOperatingSystem.obtain()
    }
  }
}

/**
 * # Node API: `os`
 */
internal object NodeOperatingSystem {
  internal abstract class ModuleBase : ProxyObject, OperatingSystemAPI {
    override fun getMemberKeys(): Array<String> = moduleMembers
    override fun hasMember(key: String): Boolean = key in moduleMembers
    override fun getMember(key: String?): Any? = when (key) {
      "EOL" -> EOL
      "devNull" -> devNull
      "constants" -> constants
      "availableParallelism" -> ProxyExecutable { availableParallelism() }
      "arch" -> ProxyExecutable { arch() }
      "cpus" -> ProxyExecutable { cpus() }
      "endianness" -> ProxyExecutable { endianness() }
      "freemem" -> ProxyExecutable { freemem() }
      "getPriority" -> ProxyExecutable { args -> getPriority(args.getOrNull(0)) }
      "homedir" -> ProxyExecutable { homedir() }
      "hostname" -> ProxyExecutable { hostname() }
      "loadavg" -> ProxyExecutable { loadavg() }
      "networkInterfaces" -> ProxyExecutable { networkInterfaces() }
      "platform" -> ProxyExecutable { platform() }
      "release" -> ProxyExecutable { release() }
      "setPriority" -> ProxyExecutable { args -> setPriority(args.getOrNull(0), args.getOrNull(1)) }
      "tmpdir" -> ProxyExecutable { tmpdir() }
      "totalmem" -> ProxyExecutable { totalmem() }
      "type" -> ProxyExecutable { type() }
      "uptime" -> ProxyExecutable { uptime() }
      "userInfo" -> ProxyExecutable { args -> userInfo(args.getOrNull(0)?.`as`(UserInfoOptions::class.java)) }
      "version" -> ProxyExecutable { version() }
      else -> null
    }

    override fun putMember(key: String?, value: Value?) {
      throw UnsupportedOperationException("Cannot modify `os` module")
    }
  }

  /**
   * ## Stubbed OS
   *
   * Stubbed implementation of the `os` module for use in simulated or host-restricted environments; returns static,
   * predetermined values for all OS-related calls. Where necessary, chooses POSIX-style values.
   */
  internal class StubbedOs : ModuleBase(), OperatingSystemAPI {
    override val family: OSType get() = POSIX
    override val EOL: String get() = posixEOL
    override val constants: OperatingSystemConstants get() = Posix.constants
    override val devNull: String get() = posixDevNull
    override fun availableParallelism(): Int = STUBBED_CPUS.size * 2
    override fun arch(): String = STUBBED_ARCH
    override fun cpus(): List<CPUInfo> = STUBBED_CPUS
    override fun endianness(): String = STUBBED_ENDIANNESS
    override fun freemem(): Long = STUBBED_FREEMEM
    override fun getPriority(pid: Value?): Int = STUBBED_PRIORITY
    override fun homedir(): String = STUBBED_HOMEDIR
    override fun hostname(): String = STUBBED_HOSTNAME
    override fun loadavg(): List<Double> = listOf(0.0, 0.0, 0.0)
    override fun networkInterfaces(): Map<String, List<NetworkInterfaceInfo>> = STUBBED_NICS
    override fun platform(): String = STUBBED_PLATFORM
    override fun release(): String = STUBBED_RELEASE
    override fun setPriority(pid: Value?, priority: Value?) = Unit
    override fun tmpdir(): String = STUBBED_TMPDIR
    override fun totalmem(): Long = STUBBED_TOTALMEM
    override fun type(): String = STUBBED_TYPE
    override fun uptime(): Double = STUBBED_UPTIME
    override fun userInfo(options: UserInfoOptions?): UserInfo = STUBBED_USER
    override fun version(): String = STUBBED_VERSION
  }

  /**
   * ## Base OS
   *
   * Abstract implementation of basic operating system utilities; Windows and POSIX-style systems are supported, with
   * each type of implementation extending this base.
   *
   * @see Win32 for Win32-style systems
   * @see Posix for POSIX-style systems
   */
  @ReflectiveAccess @Introspected abstract class BaseOS protected constructor(override val family: OSType) :
    ModuleBase(),
    OperatingSystemAPI,
    ProxyObject {
    /** Obtain system info. */
    private val systemInfo: SystemInfo by lazy { SystemInfo() }
    private val osManager: OperatingSystemMXBean by lazy { ManagementFactory.getOperatingSystemMXBean() }

    private fun wrapCleanup(subject: String?): String =
      subject?.trim()?.replace(Regex("[\n\r]"), "") ?: ""

    private fun trimTrailing(subject: String?): String =
      subject?.endsWith("/")?.let { if (it) subject.dropLast(1) else subject } ?: ""

    @VisibleForTesting
    internal fun mapJvmArchToNodeArch(arch: String): String = when (arch.lowercase().trim()) {
      JVM_X86 -> NODE_X86
      JVM_X86_64 -> NODE_X64
      JVM_AMD64 -> NODE_X64
      JVM_ARM -> NODE_ARM
      JVM_AARCH64, JVM_ARM64 -> NODE_ARM64
      else -> UNKNOWN
    }

    @VisibleForTesting
    internal fun mapJvmOsToNodeOs(os: String = System.getProperty("os.name")): String = when (os.lowercase().trim()) {
      OS_AIX -> OS_AIX
      OS_MAC_OS_X -> OS_DARWIN
      OS_FREEBSD -> OS_FREEBSD
      OS_LINUX -> OS_LINUX
      OS_OPENBSD -> OS_OPENBSD
      OS_SUNOS -> OS_SUNOS
      OS_WINDOWS, OS_WIN32 -> OS_WIN32
      else -> UNKNOWN
    }

    @VisibleForTesting
    internal fun typeForOsName(osName: String?): String = when (osName?.lowercase()?.trim()) {
      OS_LINUX -> OS_TYPE_LINUX
      OS_MAC_OS_X -> OS_TYPE_DARWIN
      OS_WINDOWS -> OS_TYPE_WINDOWS
      else -> UNKNOWN
    }

    @Polyglot override fun availableParallelism(): Int = systemInfo.hardware.processor.logicalProcessorCount
    @Polyglot override fun arch(): String = mapJvmArchToNodeArch(System.getProperty("os.arch") ?: UNKNOWN)

    @Polyglot override fun cpus(): List<CPUInfo> {
      val freq = systemInfo.hardware.processor.currentFreq.first()
      return systemInfo.hardware.processor.physicalProcessors.map {
        CPUInfo.of(
          model = it.idString,
          speed = freq,
          times = CPUTimes.of(0, 0, 0, 0, 0),
        )
      }
    }

    @Polyglot override fun endianness(): String = if (ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN)) {
      ENDIAN_BIG
    } else {
      ENDIAN_LITTLE
    }

    @Polyglot override fun freemem(): Long = Runtime.getRuntime().freeMemory()

    @Suppress("TooGenericExceptionCaught")
    @Polyglot override fun getPriority(pid: Value?): Int = try {
      ChildProcessNative.getProcessPriority(when {
        pid == null || pid.isNull -> ProcessHandle.current().pid()
        else -> requireNotNull(pid.asLong())
      })
    } catch (rxe: RuntimeException) {
      JsError.error("Failed to get process priority", rxe)
    }

    @Polyglot override fun homedir(): String = wrapCleanup(System.getProperty("user.home"))
    @Polyglot override fun hostname(): String = wrapCleanup(InetAddress.getLocalHost().hostName)

    @Polyglot override fun loadavg(): List<Double> = osManager.systemLoadAverage.let {
      listOf(it, 0.0, 0.0)  // @TODO: implement 5 and 15 minute averages
    }

    private fun decimalMaskFromSubnetMask(cidrMask: Short): String {
      val bits: Long = (-0x1 xor (1 shl 32 - cidrMask) - 1).toLong()
      return String.format(
        "%d.%d.%d.%d",
        (bits and 0x0000000000ff000000L) shr 24,
        (bits and 0x0000000000ff0000L) shr 16,
        (bits and 0x0000000000ff00L) shr 8, bits and 0xffL,
      )
    }

    private fun prefixLengthFromSubnetMask(cidrMask: Short): String {
      return cidrMask.toString()
    }

    private fun isInternalInterface(name: String, addr: String): Boolean {
      return (
              name.startsWith("lo") ||
                      addr.startsWith("127.") ||
                      addr.startsWith("::1")
              )
    }

    @Polyglot override fun networkInterfaces(): Map<String, List<NetworkInterfaceInfo>> {
      return systemInfo.hardware.networkIFs.associate { nic ->
        val ipv4addr = nic.iPv4addr.zip(nic.subnetMasks).asSequence().map { false to it }
        val ipv6addr = nic.iPv6addr.zip(nic.prefixLengths).asSequence().map { true to it }

        nic.name to ipv4addr.plus(ipv6addr).map { (isIpV6, pair) ->
          val (addr, netmask) = pair

          when (isIpV6) {
            true -> NetworkInterfaceInfo.of(
              address = addr,
              netmask = prefixLengthFromSubnetMask(netmask),
              family = NIC_FAMILY_IPV6,
              mac = nic.macaddr ?: "",
              internal = isInternalInterface(nic.name, addr),
              cidr = "$addr/${netmask ?: ""}",
            )

            else -> NetworkInterfaceInfo.of(
              address = addr,
              netmask = decimalMaskFromSubnetMask(netmask),
              family = NIC_FAMILY_IPV4,
              mac = nic.macaddr ?: "",
              internal = isInternalInterface(nic.name, addr),
              cidr = "$addr/${netmask ?: ""}",
            )
          }
        }.toList()
      }
    }

    @Polyglot override fun platform(): String = mapJvmOsToNodeOs()
    @Polyglot override fun release(): String = systemInfo.operatingSystem.toString()

    @Suppress("TooGenericExceptionCaught")
    @Polyglot override fun setPriority(pid: Value?, priority: Value?) {
      assert(priority != null && !priority.isNull) { "Cannot set `null` as priority for process" }

      val targetPrio = requireNotNull(priority!!.asInt())
      val targetPid = when {
        pid == null || pid.isNull -> ProcessHandle.current().pid()
        else -> requireNotNull(pid.asLong()).let {
          when (it) {
            // `0` is a special case for the current process.
            0L -> ProcessHandle.current().pid()
            else -> it
          }
        }
      }
      try {
        ChildProcessNative.setProcessPriority(
          targetPid,
          targetPrio,
        )
      } catch (rxe: RuntimeException) {
        JsError.error("Failed to set process priority", rxe)
      }
    }

    @Polyglot override fun tmpdir(): String = trimTrailing(wrapCleanup(System.getProperty("java.io.tmpdir")))
    @Polyglot override fun totalmem(): Long = Runtime.getRuntime().totalMemory()

    @Polyglot override fun type(): String = typeForOsName(System.getProperty("os.name"))

    @Polyglot override fun uptime(): Double = systemInfo.operatingSystem.systemUptime.toDouble()

    @Polyglot override fun userInfo(options: UserInfoOptions?): UserInfo = object: UserInfo {
      override val username: String = System.getProperty("user.name")
      override val uid: Long = -1
      override val gid: Long = -1
      override val shell: String = System.getenv("SHELL") ?: ""
      override val homedir: String = System.getProperty("user.home") ?: ""
    }

    @Polyglot override fun version(): String = systemInfo.operatingSystem.versionInfo.version
  }

  // Implements Operating System API calls for Win32-style systems.
  @ReflectiveAccess @Introspected object Win32 : BaseOS(WIN32), OperatingSystemAPI {
    @get:Polyglot override val EOL: String get() = win32EOL
    @get:Polyglot override val devNull: String get() = win32DevNull
    @get:Polyglot override val constants: OperatingSystemConstants get() = Win32Constants
  }

  // Implements Operating System API calls for POSIX-style systems.
  @ReflectiveAccess @Introspected object Posix : BaseOS(POSIX), OperatingSystemAPI {
    private val unixInfo by lazy { com.sun.security.auth.module.UnixSystem() }
    @get:Polyglot override val EOL: String get() = posixEOL
    @get:Polyglot override val devNull: String get() = posixDevNull
    @get:Polyglot override val constants: OperatingSystemConstants get() = PosixConstants
    @Polyglot override fun userInfo(options: UserInfoOptions?): UserInfo = super.userInfo(options).let { base ->
      object: UserInfo {
        override val username: StringOrBuffer = unixInfo.username
        override val uid: Long = unixInfo.uid
        override val gid: Long = unixInfo.gid
        override val shell: StringOrBuffer? = base.shell
        override val homedir: StringOrBuffer = base.homedir
      }
    }
  }

  // Stubbed OS module API singleton.
  private val stubbed: StubbedOs by lazy { StubbedOs() }

  /**
   * ## Node Operating System API: Stubbed
   *
   * Creates a stubbed instance of the [OperatingSystemAPI] for use in simulated or host-restricted environments.
   *
   * @return A stubbed instance of the [OperatingSystemAPI].
   */
  @JvmStatic fun stubbed(): OperatingSystemAPI = stubbed

  /**
   * ## Node Operating System API: Create or Obtain
   *
   * Creates or obtains an instance complying with the [OperatingSystemAPI], based on the provided [family] ([OSType]).
   * If no [family] is provided, the current operating system is detected and used.
   *
   * @param family The operating system family to create or obtain an instance for.
   * @return An instance of the [OperatingSystemAPI] for the specified [family].
   */
  @JvmStatic fun obtain(family: OSType? = null): OperatingSystemAPI = when (family ?: OSType.current()) {
    POSIX -> Posix
    WIN32 -> Win32
  }
}

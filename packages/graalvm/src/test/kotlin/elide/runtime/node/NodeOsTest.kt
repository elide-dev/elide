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
@file:Suppress("NodeCoreCodingAssistance")

package elide.runtime.node

import org.graalvm.nativeimage.ImageInfo
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.stream.Stream
import jakarta.inject.Inject
import kotlin.streams.asStream
import kotlin.test.*
import kotlin.test.Test
import elide.runtime.intrinsics.js.node.OperatingSystemAPI
import elide.runtime.intrinsics.js.node.os.*
import elide.runtime.node.os.NodeOperatingSystem
import elide.runtime.node.os.NodeOperatingSystem.Posix
import elide.runtime.node.os.NodeOperatingSystem.StubbedOs
import elide.runtime.node.os.NodeOperatingSystem.Win32
import elide.runtime.node.os.NodeOperatingSystemModule
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `os` built-in module. */
@TestCase
internal class NodeOsTest : NodeModuleConformanceTest<NodeOperatingSystemModule>() {
  override val moduleName: String get() = "os"
  override fun provide(): NodeOperatingSystemModule = NodeOperatingSystemModule()
  private fun acquire(): OperatingSystemAPI = Posix
  @Inject internal lateinit var os: OperatingSystemAPI

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("EOL")
    yield("constants")
    yield("devNull")
    yield("availableParallelism")
    yield("arch")
    yield("cpus")
    yield("endianness")
    yield("freemem")
    yield("homedir")
    yield("hostname")
    yield("loadavg")
    yield("networkInterfaces")
    yield("platform")
    yield("release")
    yield("tmpdir")
    yield("totalmem")
    yield("type")
    yield("uptime")
    yield("userInfo")
  }

  private val suppressedMembers = emptyList<String>()
  private val suppressedExec = emptyList<String>()
  private val propertyMembers = listOf("EOL", "devNull", "constants")

  @TestFactory fun `api compliance`(): Stream<DynamicTest> {
    return listOf(
      StubbedOs(),
      Posix,
      Win32,
    ).asSequence().flatMap { impl ->
      @Suppress("UNCHECKED_CAST")
      val implMembers = (impl as ProxyObject).memberKeys as Array<String>

      sequence {
        yield(
          dynamicTest("impl '$impl' should enforce read-only object contract") {
            assertDoesNotThrow { impl.memberKeys }
            assertDoesNotThrow { impl.getMember(null) }
            assertThrows<UnsupportedOperationException> { impl.putMember("someKey", Value.asValue("someValue")) }
          },
        )

        requiredMembers().flatMap { member ->
          val isExecutable = member !in propertyMembers
          val execCondition = if (isExecutable) "should be executable" else "should be non-executable"

          listOf(
            dynamicTest("member '$member' should be present") {
              Assumptions.assumeTrue(member !in suppressedMembers, "member '$member' is suppressed")
              assertContains(implMembers, member, "member '$member' should be present in members list")
            },
            dynamicTest("member '$member' should be resolvable") {
              Assumptions.assumeTrue(member !in suppressedMembers, "member '$member' is suppressed")
              assertNotNull(impl.getMember(member), "member '$member' should be resolvable")
            },
            dynamicTest("member '$member' $execCondition") {
              Assumptions.assumeTrue(member !in suppressedMembers, "member '$member' is suppressed")
              when (member !in propertyMembers) {
                true -> {
                  // should get a `ProxyExecutable` when resolved
                  assertIs<ProxyExecutable>(
                    impl.getMember(member),
                    "callable module member '$member' should be `ProxyExecutable`",
                  )
                }

                false -> {
                  // should NOT get a `ProxyExecutable` when resolved
                  assertIsNot<ProxyExecutable>(
                    impl.getMember(member),
                    "non-callable module member '$member' should not be `ProxyExecutable`",
                  )
                }
              }
            },
            dynamicTest("member '$member' should not throw") {
              Assumptions.assumeTrue(
                !ImageInfo.inImageRuntimeCode()
              )
              Assumptions.assumeTrue(
                member !in suppressedMembers,
                "member '$member' is suppressed",
              )
              Assumptions.assumeTrue(
                member !in suppressedExec,
                "member '$member' is suppressed for calling",
              )
              val tgt = assertDoesNotThrow { impl.getMember(member) }
              if (isExecutable) {
                val exec = assertIs<ProxyExecutable>(tgt, "executable member should be `ProxyExecutable`")
                assertDoesNotThrow("callable member '$member' should not throw") {
                  exec.execute()
                }
              }
            },
          )
        }.forEach {
          yield(it)
        }
      }
    }.asStream()
  }

  @Test override fun testInjectable() {
    assertNotNull(os, "should be able to inject instance of OS module")
  }

  @Test fun `os EOL should return expected value for current host`() = conforms {
    assertNotNull(acquire().EOL, "should not get `null` from `os.EOL`")
  }.guest {
    // language=javascript
    """
      const { ok } = require("assert");
      const { EOL } = require("os");
      ok(EOL !== '');
      ok(EOL.length === 1);
    """
  }

  @Test fun `os devNull should return expected value for current host`() = conforms {
    assertNotNull(acquire().devNull, "should not get `null` from `os.devNull`")
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { devNull } = require("os");
      equal(devNull, '${acquire().devNull}');
    """
  }

  @Test fun `os platform() should return expected value for current host`() = conforms {
    assertNotNull(acquire().platform(), "should not get `null` from `os.platform()`")
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { platform } = require("os");
      equal(platform(), '${acquire().platform()}');
    """
  }

  @Test fun `os arch() should return expected value for current host`() = conforms {
    assertNotNull(acquire().arch(), "should not get `null` from `os.arch()`")
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { arch } = require("os");
      equal(arch(), '${acquire().arch()}');
    """
  }

  @Test fun `os availableParallelism() should return expected value for current host`() {
    Assumptions.assumeTrue(!ImageInfo.inImageCode())

    conforms {
      assertNotNull(acquire().availableParallelism(), "should not get `null` from `os.availableParallelism()`")
    }.guest {
      // language=javascript
      """
      const { equal } = require("assert");
      const { availableParallelism } = require("os");
      equal(availableParallelism(), '${acquire().availableParallelism()}');
    """
    }
  }

  @Test fun `os endianness() should return expected value for current host`() = conforms {
    assertNotNull(acquire().endianness(), "should not get `null` from `os.endianness()`")
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { endianness } = require("os");
      equal(endianness(), '${acquire().endianness()}');
    """
  }

  @Test fun `os freemem() should return expected value for current host`() = conforms {
    assertNotNull(acquire().freemem(), "should not get `null` from `os.freemem()`")
  }.guest {
    // language=javascript
    """
      const { ok } = require("assert");
      const { freemem } = require("os");
      ok(typeof freemem() === 'number');
      ok(freemem() > 0);
    """
  }

  @Test fun `os cpus() should return expected value for current host`() {
    Assumptions.assumeTrue(!ImageInfo.inImageCode())

    conforms {
      val cpus = acquire().cpus()
      assertNotNull(cpus, "should not get `null` from `os.cpus()`")
      assertTrue(cpus.isNotEmpty())
      cpus.forEach { cpu ->
        assertNotNull(cpu.model, "should not get `null` from `cpu.model`")
        assertNotNull(cpu.speed, "should not get `null` from `cpu.speed`")
        assertNotNull(cpu.times, "should not get `null` from `cpu.times`")
      }
    }.guest {
      // language=javascript
      """
      const { ok } = require("assert");
      const { cpus } = require("os");
      const allCpus = cpus();
      ok(allCpus.length !== 0);
    """
    }
  }

  @Test fun `os homedir() should return expected value for current host`() = conforms {
    assertNotNull(acquire().homedir(), "should not get `null` from `os.homedir()`")
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { homedir } = require("os");
      equal(homedir(), '${acquire().homedir()}');
    """
  }

  @Test fun `os hostname() should return expected value for current host`() = conforms {
    assertNotNull(acquire().hostname(), "should not get `null` from `os.hostname()`")
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { hostname } = require("os");
      equal(hostname(), '${acquire().hostname()}');
    """
  }

  @Test fun `os release() should return expected value for current host`() = conforms {
    assertNotNull(acquire().release(), "should not get `null` from `os.release()`")
  }.guest {
    // language=javascript
    """
      const { ok } = require("assert");
      const { release } = require("os");
      ok(release());
      ok(typeof release() === 'string');
    """
  }

  @Test fun `os tmpdir() should return expected value for current host`() = conforms {
    assertNotNull(acquire().tmpdir(), "should not get `null` from `os.tmpdir()`")
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { tmpdir } = require("os");
      equal(tmpdir(), '${acquire().tmpdir()}');
    """
  }

  @Test fun `os totalmem() should return expected value for current host`() = conforms {
    assertNotNull(acquire().totalmem(), "should not get `null` from `os.totalmem()`")
  }.guest {
    // language=javascript
    """
      const { ok } = require("assert");
      const { totalmem } = require("os");
      ok(typeof totalmem() === 'number');
      ok(totalmem() > 0);
    """
  }

  @Test fun `os type() should return expected value for current host`() = conforms {
    assertNotNull(acquire().type(), "should not get `null` from `os.type()`")
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { type } = require("os");
      equal(type(), '${acquire().type()}');
    """
  }

  @Test fun `os uptime() should return expected value for current host`() = conforms {
    assertNotNull(acquire().totalmem(), "should not get `null` from `os.uptime()`")
  }.guest {
    // language=javascript
    """
      const { ok } = require("assert");
      const { uptime } = require("os");
      ok(typeof uptime() === 'number');
      ok(uptime() > 0);
    """
  }

  @Test fun `os version() should return expected value for current host`() = conforms {
    assertNotNull(acquire().version(), "should not get `null` from `os.version()`")
  }.guest {
    // language=javascript
    """
      const { ok } = require("assert");
      const { version } = require("os");
      ok(version());
      ok(typeof version() === 'string');
    """
  }

  @Test fun `os networkInterfaces() should return expected value for current host`() {
    Assumptions.assumeTrue(!ImageInfo.inImageCode())

    conforms {
      val nics: Map<String, List<NetworkInterfaceInfo>>? = acquire().networkInterfaces()
      assertNotNull(nics, "should not get `null` from `os.networkInterfaces()`")
      assertTrue(nics.isNotEmpty())
      nics.entries.forEach { entry ->
        assertNotNull(entry.key, "should not get `null` from NIC name as map key")
        assertTrue(entry.key.isNotEmpty(), "NIC name should not be empty")
        entry.value.forEach { nicAddr ->
          assertNotNull(nicAddr.address)
          assertNotNull(nicAddr.netmask)
          assertNotNull(nicAddr.family)
          assertNotNull(nicAddr.mac)
          assertNotNull(nicAddr.internal)
          assertNotNull(nicAddr.cidr)

          assertNotEquals(nicAddr.address, "")
          assertNotEquals(nicAddr.family, "")
          assertNotEquals(nicAddr.mac, "")
          assertNotEquals(nicAddr.netmask, "")
          assertNotEquals(nicAddr.cidr, "")
        }
      }
    }.guest {
      // language=javascript
      """
      const { ok } = require("assert");
      const { networkInterfaces } = require("os");
      const nics = networkInterfaces();
      ok(Object.keys(nics).length !== 0);
    """
    }
  }

  @Test fun `os userInfo() should return expected value for current host`() = conforms {
    val userInfo = acquire().userInfo()
    assertNotNull(userInfo, "should not get `null` from `os.userInfo()`")
    assertNotNull(userInfo.username)
    assertNotNull(userInfo.uid)
    assertNotNull(userInfo.gid)
    assertNotNull(userInfo.shell)
    assertNotNull(userInfo.homedir)
  }.guest {
    // language=javascript
    """
      const { ok } = require("assert");
      const { userInfo } = require("os");
      const info = userInfo();
      ok(!!info);
    """
  }

  @Test fun `os getPriority() should return expected value for current host`() = conforms {
    assertNotNull(acquire().getPriority(), "should not get `null` from `os.getPriority()`")
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { getPriority } = require("os");
      equal(getPriority(), '${acquire().getPriority()}');
    """
  }

  @Test fun `os getPriority() should not fail even if stubbed`() = dual {
    assertNotNull(acquire().getPriority(), "should not get `null` from `os.getPriority()`")
  }.guest {
    // language=javascript
    """
      const { ok } = require("assert");
      const { getPriority } = require("os");
      ok(getPriority() !== null);
      ok(typeof getPriority() === 'number');
    """
  }

  @Test fun `os getPriority(pid) should not fail even if stubbed`() = dual {
    val pid = ProcessHandle.current().pid()
    assertNotNull(
      acquire().getPriority(Value.asValue(pid)),
      "should not get `null` from `os.getPriority(pid)`",
    )
  }.guest {
    // language=javascript
    """
      const { ok } = require("assert");
      const { getPriority } = require("os");
      ok(getPriority(${ProcessHandle.current().pid()}) !== null);
      ok(typeof getPriority(${ProcessHandle.current().pid()}) === 'number');
    """
  }

  @Test fun `os setPriority(0, 0) should return expected value for current process`() = conforms {
    assertNotNull(
      acquire().setPriority(Value.asValue(0), Value.asValue(0)),
      "should not get `null` from `os.setPriority()`",
    )
  }.guest {
    // language=javascript
    """
      const { setPriority } = require("os");
      setPriority(0, 0);
    """
  }

  @Test fun `os setPriority(0, 0) should not fail even if stubbed`() = dual {
    assertNotNull(
      acquire().setPriority(Value.asValue(0), Value.asValue(0)),
      "should not get `null` from `os.setPriority(0, 0)`",
    )
  }.guest {
    // language=javascript
    """
      const { setPriority } = require("os");
      setPriority(0, 0);
    """
  }

  @CsvSource(
    "mac os x, Darwin",
    "linux, Linux",
    "windows, Windows_NT",
  )
  @ParameterizedTest
  fun `os type() should resolve properly given each os name`(name: String, expected: String) {
    val osType = (acquire() as NodeOperatingSystem.BaseOS).typeForOsName(name)
    assertNotNull(osType, "should not get `null` from `os.type()` (os: '$name')")
    assertEquals(expected, osType, "should get expected value from `os.type()` (os: '$name')")
  }

  @CsvSource(
    "aix, aix",
    "linux, linux",
    "mac os x, darwin",
    "freebsd, freebsd",
    "openbsd, openbsd",
    "sunos, sunos",
    "windows, win32",
  )
  @ParameterizedTest
  fun `os platform() should resolve properly given each os name`(name: String, expected: String) {
    val osType = (acquire() as NodeOperatingSystem.BaseOS).mapJvmOsToNodeOs(name)
    assertNotNull(osType, "should not get `null` from `os.type()` (os: '$name')")
    assertEquals(expected, osType, "should get expected value from `os.type()` (os: '$name')")
  }

  @Test fun `os provides sane constants`() {
    assertNotNull(
      acquire().constants,
      "should not get `null` from `os.constants`",
    )
  }

  @Test fun `os posix constants provide expected objects`() {
    val constants = Posix.constants
    assertNotNull(
      constants,
      "should not get `null` from `Posix.constants`",
    )
    assertIs<PosixSystemConstants>(constants)
    assertNotNull(constants.dlopen, "should not get `null` from `Posix.constants.dlopen`")
    assertEquals(POSIX_RTLD_LAZY, constants.dlopen.RTLD_LAZY)
    assertEquals(POSIX_RTLD_NOW, constants.dlopen.RTLD_NOW)
    assertEquals(POSIX_RTLD_GLOBAL, constants.dlopen.RTLD_GLOBAL)
    assertEquals(POSIX_RTLD_LOCAL, constants.dlopen.RTLD_LOCAL)
    assertEquals(POSIX_RTLD_DEEPBIND, constants.dlopen.RTLD_DEEPBIND)
    assertNull(constants.dlopen.getMember("RTLD_SOME_OTHER_SYMBOL"))
    assertFalse(constants.dlopen.hasMember("RTLD_SOME_OTHER_SYMBOL"))
    assertThrows<UnsupportedOperationException> { constants.dlopen.removeMember("RTLD_LOCAL") }
    assertThrows<IllegalStateException> { constants.dlopen.putMember("RTLD_LOCAL", Value.asValue(1)) }
  }

  @CsvSource(
    SYMBOL_RTLD_LAZY,
    SYMBOL_RTLD_NOW,
    SYMBOL_RTLD_GLOBAL,
    SYMBOL_RTLD_LOCAL,
    SYMBOL_RTLD_DEEPBIND,
  )
  @ParameterizedTest
  fun `os posix constants - dlopen symbols`(symbol: String) {
    val constants = assertNotNull(
      Posix.constants,
      "should not get `null` from `Posix.constants`",
    )
    assertIs<PosixSystemConstants>(constants)
    assertNotNull(constants.dlopen, "should not get `null` from `Posix.constants.dlopen`")
    assertTrue(constants.dlopen.hasMember(symbol))
    assertNotNull(constants.dlopen.getMember(symbol))
    assertNotNull(constants.dlopen.getMember(symbol))
    assertIs<Int>(constants.dlopen.getMember(symbol))
  }

  @Test fun `os posix constants - priority`() {
    val constants = Posix.constants
    assertNotNull(constants, "should not get `null` from `Posix.constants`")
    assertIs<PosixSystemConstants>(constants)
    val prio = constants.priority
    assertNotNull(prio, "should not get `null` from `Posix.constants.priority`")
    assertEquals(PRIORITY_NORMAL, prio.PRIORITY_NORMAL)
    assertEquals(PRIORITY_LOW, prio.PRIORITY_LOW)
    assertEquals(PRIORITY_HIGH, prio.PRIORITY_HIGH)
    assertEquals(PRIORITY_HIGHEST, prio.PRIORITY_HIGHEST)
    assertEquals(PRIORITY_BELOW_NORMAL, prio.PRIORITY_BELOW_NORMAL)
    assertEquals(PRIORITY_ABOVE_NORMAL, prio.PRIORITY_ABOVE_NORMAL)
  }

  @CsvSource(
    "PRIORITY_LOW, $PRIORITY_LOW",
    "PRIORITY_BELOW_NORMAL, $PRIORITY_BELOW_NORMAL",
    "PRIORITY_NORMAL, $PRIORITY_NORMAL",
    "PRIORITY_ABOVE_NORMAL, $PRIORITY_ABOVE_NORMAL",
    "PRIORITY_HIGH, $PRIORITY_HIGH",
    "PRIORITY_HIGHEST, $PRIORITY_HIGHEST",
  )
  @ParameterizedTest
  fun `os posix constants - priority symbols`(symbol: String, expectedPriority: Int) {
    val constants = Posix.constants
    assertNotNull(constants, "should not get `null` from `Posix.constants`")
    assertIs<PosixSystemConstants>(constants)
    val prio = constants.priority
    assertNotNull(prio, "should not get `null` from `Posix.constants.priority`")
    assertTrue(prio.hasMember(symbol))
    assertNotNull(prio.getMember(symbol))
    assertNotNull(prio.getMember(symbol))
    assertIs<Int>(prio.getMember(symbol))
    assertEquals(expectedPriority, prio.getMember(symbol))
  }

  @CsvSource(
    "E2BIG, $SYMBOL_E2BIG, $E2BIG_CONST",
    "EACCES, $SYMBOL_EACCES, $EACCES_CONST",
    "EADDRINUSE, $SYMBOL_EADDRINUSE, $EADDRINUSE_CONST",
    "EADDRNOTAVAIL, $SYMBOL_EADDRNOTAVAIL, $EADDRNOTAVAIL_CONST",
    "EAFNOSUPPORT, $SYMBOL_EAFNOSUPPORT, $EAFNOSUPPORT_CONST",
    "EAGAIN, $SYMBOL_EAGAIN, $EAGAIN_CONST",
    "EALREADY, $SYMBOL_EALREADY, $EALREADY_CONST",
    "EBADF, $SYMBOL_EBADF, $EBADF_CONST",
    "EBADMSG, $SYMBOL_EBADMSG, $EBADMSG_CONST",
    "EBUSY, $SYMBOL_EBUSY, $EBUSY_CONST",
    "ECANCELED, $SYMBOL_ECANCELED, $ECANCELED_CONST",
    "ECHILD, $SYMBOL_ECHILD, $ECHILD_CONST",
    "ECONNABORTED, $SYMBOL_ECONNABORTED, $ECONNABORTED_CONST",
    "ECONNREFUSED, $SYMBOL_ECONNREFUSED, $ECONNREFUSED_CONST",
    "ECONNRESET, $SYMBOL_ECONNRESET, $ECONNRESET_CONST",
    "EDEADLK, $SYMBOL_EDEADLK, $EDEADLK_CONST",
    "EDESTADDRREQ, $SYMBOL_EDESTADDRREQ, $EDESTADDRREQ_CONST",
    "EDOM, $SYMBOL_EDOM, $EDOM_CONST",
    "EDQUOT, $SYMBOL_EDQUOT, $EDQUOT_CONST",
    "EEXIST, $SYMBOL_EEXIST, $EEXIST_CONST",
    "EFAULT, $SYMBOL_EFAULT, $EFAULT_CONST",
    "EFBIG, $SYMBOL_EFBIG, $EFBIG_CONST",
    "EHOSTUNREACH, $SYMBOL_EHOSTUNREACH, $EHOSTUNREACH_CONST",
    "EIDRM, $SYMBOL_EIDRM, $EIDRM_CONST",
    "EILSEQ, $SYMBOL_EILSEQ, $EILSEQ_CONST",
    "EINPROGRESS, $SYMBOL_EINPROGRESS, $EINPROGRESS_CONST",
    "EINTR, $SYMBOL_EINTR, $EINTR_CONST",
    "EINVAL, $SYMBOL_EINVAL, $EINVAL_CONST",
    "EIO, $SYMBOL_EIO, $EIO_CONST",
    "EISCONN, $SYMBOL_EISCONN, $EISCONN_CONST",
    "EISDIR, $SYMBOL_EISDIR, $EISDIR_CONST",
    "ELOOP, $SYMBOL_ELOOP, $ELOOP_CONST",
    "EMFILE, $SYMBOL_EMFILE, $EMFILE_CONST",
    "EMLINK, $SYMBOL_EMLINK, $EMLINK_CONST",
    "EMSGSIZE, $SYMBOL_EMSGSIZE, $EMSGSIZE_CONST",
    "EMULTIHOP, $SYMBOL_EMULTIHOP, $EMULTIHOP_CONST",
    "ENAMETOOLONG, $SYMBOL_ENAMETOOLONG, $ENAMETOOLONG_CONST",
    "ENETDOWN, $SYMBOL_ENETDOWN, $ENETDOWN_CONST",
    "ENETRESET, $SYMBOL_ENETRESET, $ENETRESET_CONST",
    "ENETUNREACH, $SYMBOL_ENETUNREACH, $ENETUNREACH_CONST",
    "ENFILE, $SYMBOL_ENFILE, $ENFILE_CONST",
    "ENOBUFS, $SYMBOL_ENOBUFS, $ENOBUFS_CONST",
    "ENODATA, $SYMBOL_ENODATA, $ENODATA_CONST",
    "ENODEV, $SYMBOL_ENODEV, $ENODEV_CONST",
    "ENOENT, $SYMBOL_ENOENT, $ENOENT_CONST",
    "ENOEXEC, $SYMBOL_ENOEXEC, $ENOEXEC_CONST",
    "ENOLCK, $SYMBOL_ENOLCK, $ENOLCK_CONST",
    "ENOLINK, $SYMBOL_ENOLINK, $ENOLINK_CONST",
    "ENOMEM, $SYMBOL_ENOMEM, $ENOMEM_CONST",
    "ENOMSG, $SYMBOL_ENOMSG, $ENOMSG_CONST",
    "ENOPROTOOPT, $SYMBOL_ENOPROTOOPT, $ENOPROTOOPT_CONST",
    "ENOSPC, $SYMBOL_ENOSPC, $ENOSPC_CONST",
    "ENOSR, $SYMBOL_ENOSR, $ENOSR_CONST",
    "ENOSTR, $SYMBOL_ENOSTR, $ENOSTR_CONST",
    "ENOSYS, $SYMBOL_ENOSYS, $ENOSYS_CONST",
    "ENOTCONN, $SYMBOL_ENOTCONN, $ENOTCONN_CONST",
    "ENOTDIR, $SYMBOL_ENOTDIR, $ENOTDIR_CONST",
    "ENOTEMPTY, $SYMBOL_ENOTEMPTY, $ENOTEMPTY_CONST",
    "ENOTSOCK, $SYMBOL_ENOTSOCK, $ENOTSOCK_CONST",
    "ENOTSUP, $SYMBOL_ENOTSUP, $ENOTSUP_CONST",
    "ENOTTY, $SYMBOL_ENOTTY, $ENOTTY_CONST",
    "ENXIO, $SYMBOL_ENXIO, $ENXIO_CONST",
    "EOPNOTSUPP, $SYMBOL_EOPNOTSUPP, $EOPNOTSUPP_CONST",
    "EOVERFLOW, $SYMBOL_EOVERFLOW, $EOVERFLOW_CONST",
    "EPERM, $SYMBOL_EPERM, $EPERM_CONST",
    "EPIPE, $SYMBOL_EPIPE, $EPIPE_CONST",
    "EPROTO, $SYMBOL_EPROTO, $EPROTO_CONST",
    "EPROTONOSUPPORT, $SYMBOL_EPROTONOSUPPORT, $EPROTONOSUPPORT_CONST",
    "EPROTOTYPE, $SYMBOL_EPROTOTYPE, $EPROTOTYPE_CONST",
    "ERANGE, $SYMBOL_ERANGE, $ERANGE_CONST",
    "EROFS, $SYMBOL_EROFS, $EROFS_CONST",
    "ESPIPE, $SYMBOL_ESPIPE, $ESPIPE_CONST",
    "ESRCH, $SYMBOL_ESRCH, $ESRCH_CONST",
    "ESTALE, $SYMBOL_ESTALE, $ESTALE_CONST",
    "ETIME, $SYMBOL_ETIME, $ETIME_CONST",
    "ETIMEDOUT, $SYMBOL_ETIMEDOUT, $ETIMEDOUT_CONST",
    "ETXTBSY, $SYMBOL_ETXTBSY, $ETXTBSY_CONST",
    "EWOULDBLOCK, $SYMBOL_EWOULDBLOCK, $EWOULDBLOCK_CONST",
    "EXDEV, $SYMBOL_EXDEV, $EXDEV_CONST",
  )
  @ParameterizedTest
  fun `os posix constants - errno symbols`(expectedSymbol: String, symbol: String, expectedValue: Int) {
    val constants = Posix.constants
    assertNotNull(constants, "should not get `null` from `Posix.constants`")
    assertEquals(expectedSymbol, symbol)
    assertIs<PosixSystemConstants>(constants)
    val errno = constants.errno
    assertNotNull(errno, "should not get `null` from `Posix.constants.errno`")
    assertTrue(errno.hasMember(symbol))
    assertNotNull(errno.getMember(symbol))
    val out = errno.getMember(symbol)
    assertNotNull(out)
    assertIs<Int>(out)
    assertEquals(expectedValue, errno.getMember(symbol))
  }
}

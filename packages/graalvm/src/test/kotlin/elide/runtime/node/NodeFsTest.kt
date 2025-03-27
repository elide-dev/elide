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
@file:Suppress("JSCheckFunctionSignatures", "JSUnresolvedReference", "JSDeprecatedSymbols", "LargeClass")

package elide.runtime.node

import org.graalvm.polyglot.Value
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.nio.charset.StandardCharsets
import java.nio.file.AccessMode.*
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE
import java.nio.file.attribute.PosixFilePermission.OWNER_WRITE
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Function
import java.util.stream.Stream
import jakarta.inject.Provider
import kotlin.streams.asStream
import kotlin.test.*
import elide.annotations.Inject
import elide.runtime.exec.GuestExecutorProvider
import elide.runtime.gvm.vfs.EmbeddedGuestVFS
import elide.runtime.intrinsics.js.err.AbstractJsException
import elide.runtime.intrinsics.js.err.Error
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.err.ValueError
import elide.runtime.intrinsics.js.node.WritableFilesystemAPI
import elide.runtime.intrinsics.js.node.fs.ReadFileOptions
import elide.runtime.intrinsics.js.node.fs.WriteFileOptions
import elide.runtime.node.fs.FilesystemConstants
import elide.runtime.node.fs.NodeFilesystemModule
import elide.runtime.node.fs.VfsInitializerListener
import elide.runtime.node.fs.resolveEncodingString
import elide.runtime.node.path.NodePathsModule
import elide.testing.annotations.TestCase
import elide.runtime.intrinsics.js.node.path.Path as NodePath

/** Tests for Elide's implementation of the Node `fs` built-in module. */
@TestCase internal class NodeFsTest : AbstractNodeFsTest<NodeFilesystemModule>() {
  @Inject private lateinit var path: NodePathsModule
  @Inject private lateinit var execProvider: GuestExecutorProvider

  private val filesystem: NodeFilesystemModule by lazy {
    provide()
  }

  override val moduleName: String get() = "fs"
  override fun provide(): NodeFilesystemModule = NodeFilesystemModule(
    path,
    Provider {
      VfsInitializerListener().also {
        it.onVfsCreated(EmbeddedGuestVFS.empty())
      }
    },
    execProvider,
  )

  // @TODO(sgammon): Not yet fully supported
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("constants")
    yield("FileHandle")
    yield("Dir")
    yield("Dirent")
    yield("ReadStream")
    yield("WriteStream")
    yield("Stats")
    yield("StatFs")
    yield("FSWatcher")
    yield("StatWatcher")
    yield("access")
    yield("accessSync")
    yield("appendFile")
    yield("appendFileSync")
    yield("chmod")
    yield("chmodSync")
    yield("chown")
    yield("chownSync")
    yield("close")
    yield("closeSync")
    yield("copyFile")
    yield("copyFileSync")
    yield("createReadStream")
    yield("createWriteStream")
    yield("exists")
    yield("existsSync")
    yield("fchmod")
    yield("fchmodSync")
    yield("fchown")
    yield("fchownSync")
    yield("fdatasync")
    yield("fdatasyncSync")
    yield("fstat")
    yield("fstatSync")
    yield("fsync")
    yield("fsyncSync")
    yield("ftruncate")
    yield("ftruncateSync")
    yield("futimes")
    yield("futimesSync")
    yield("glob")
    yield("globSync")
    yield("lchmod")
    yield("lchmodSync")
    yield("lchown")
    yield("lchownSync")
    yield("luntimes")
    yield("luntimesSync")
    yield("link")
    yield("linkSync")
    yield("lstat")
    yield("lstatSync")
    yield("mkdir")
    yield("mkdirSync")
    yield("mkdtemp")
    yield("mkdtempSync")
    yield("open")
    yield("openSync")
    yield("openAsBlob")
    yield("openAsBlobSync")
    yield("opendir")
    yield("opendirSync")
    yield("read")
    yield("readSync")
    yield("readdir")
    yield("readdirSync")
    yield("readFile")
    yield("readFileSync")
    yield("readlink")
    yield("readlinkSync")
    yield("realpath")
    yield("realpathSync")
    yield("rename")
    yield("renameSync")
    yield("rmdir")
    yield("rmdirSync")
    yield("rm")
    yield("rmSync")
    yield("stat")
    yield("statSync")
    yield("symlink")
    yield("symlinkSync")
    yield("truncate")
    yield("truncateSync")
    yield("unlink")
    yield("unlinkSync")
    yield("unwatchFile")
    yield("utimes")
    yield("utimesSync")
    yield("watch")
    yield("watchFile")
    yield("write")
    yield("writeSync")
    yield("writeFile")
    yield("writeFileSync")
    yield("writev")
    yield("writevSync")
  }

  @Test override fun testInjectable() {
    assertNotNull(filesystem, "should be able to inject host-side filesystem module")
  }

  @Test fun testUnknownEncoding() {
    assertThrows<ValueError> { resolveEncodingString("unknown") }
  }

  @TestFactory fun testEncodingStrings(): Stream<DynamicTest> = sequence {
    listOf(
      // ASCII
      "ASCII" to Charsets.US_ASCII,
      "ascii" to Charsets.US_ASCII,
      "AsCiI" to Charsets.US_ASCII,
      "us-ascii" to Charsets.US_ASCII,
      "US-ASCII" to Charsets.US_ASCII,
      "Us-AsCiI" to Charsets.US_ASCII,
      "usascii" to Charsets.US_ASCII,
      // UTF-8
      "utf-8" to Charsets.UTF_8,
      "UTF-8" to Charsets.UTF_8,
      "UtF-8" to Charsets.UTF_8,
      "utf8" to Charsets.UTF_8,
      "UTF8" to Charsets.UTF_8,
      "UtF8" to Charsets.UTF_8,
      // UTF-16
      "utf-16" to Charsets.UTF_16,
      "UTF-16" to Charsets.UTF_16,
      "UtF-16" to Charsets.UTF_16,
      "utf16" to Charsets.UTF_16,
      "UTF16" to Charsets.UTF_16,
      "UtF16" to Charsets.UTF_16,
      // UTF-32
      "utf-32" to Charsets.UTF_32,
      "UTF-32" to Charsets.UTF_32,
      "UtF-32" to Charsets.UTF_32,
      "utf32" to Charsets.UTF_32,
      "UTF32" to Charsets.UTF_32,
      "UtF32" to Charsets.UTF_32,
      // ISO-8859-1
      "latin1" to Charsets.ISO_8859_1,
      "LATIN1" to Charsets.ISO_8859_1,
      "LaTiN1" to Charsets.ISO_8859_1,
      "binary" to Charsets.ISO_8859_1,
      "Binary" to Charsets.ISO_8859_1,
      "BINARY" to Charsets.ISO_8859_1,
      "bInArY" to Charsets.ISO_8859_1,
    ).forEach { (label, expected) ->
      yield(
        dynamicTest("'$label'") {
          val encoding = assertNotNull(resolveEncodingString(label))
          assertEquals(expected, encoding, "label '$label' should yield encoding '$expected'")
        },
      )
    }
  }.asStream()

  @Test fun `exists() with text file`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      val missingPath = tmp.resolve("some-missing-file.txt").toAbsolutePath()
      Files.write(samplePath, "Hello, world!".toByteArray())
      assertTrue(Files.exists(samplePath), "should have written file")
      assertEquals("Hello, world!", Files.readString(samplePath))

      dual {
        fs.exists(NodePath.from(samplePath)) {
          assertTrue(it)
        }
        val existed = assertDoesNotThrow {
          val doesExist = AtomicReference<Boolean>(null)
          fs.exists(NodePath.from(missingPath)) {
            doesExist.set(it)
          }
          doesExist.get()
        }
        assertFalse(existed)
      }.guest {
        // language=javascript
        """
          const { exists } = require("node:fs");
          test(exists).isNotNull();
          let callbackDispatched = false;
          let doesExist = null;
          exists("$samplePath", (does) => {
            callbackDispatched = true;
            doesExist = does;
          });
          test(callbackDispatched).shouldBeTrue();
          test(doesExist).shouldBeTrue();
        """
      }
    }
  }

  @Test fun `existsSync() with text file`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      val missingPath = tmp.resolve("some-missing-file.txt").toAbsolutePath()
      Files.write(samplePath, "Hello, world!".toByteArray())
      assertTrue(Files.exists(samplePath), "should have written file")
      assertEquals("Hello, world!", Files.readString(samplePath))

      dual {
        assertTrue(
          assertDoesNotThrow { fs.existsSync(NodePath.from(samplePath)) },
        )
        assertFalse(
          assertDoesNotThrow { fs.existsSync(NodePath.from(missingPath)) },
        )
      }.guest {
        // language=javascript
        """
          const { existsSync } = require("node:fs");
          test(existsSync).isNotNull();
          test(existsSync("$samplePath")).isNotNull();
          test(existsSync("$samplePath")).shouldBeTrue();
          test(existsSync("$missingPath")).shouldBeFalse();
        """
      }
    }
  }

  @Test fun `exists() with directory`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-dir").toAbsolutePath()
      Files.createDirectory(samplePath)
      assertTrue(Files.exists(samplePath), "should have written directory")
      assertTrue(Files.isDirectory(samplePath))

      dual {
        fs.exists(NodePath.from(samplePath)) {
          assertTrue(it)
        }
      }.guest {
        // language=javascript
        """
          const { exists } = require("node:fs");
          test(exists).isNotNull();
          let callbackDispatched = false;
          let doesExist = null;

          exists("$samplePath", (does) => {
            callbackDispatched = true;
            doesExist = does;
          });
          test(callbackDispatched).shouldBeTrue();
          test(doesExist).shouldBeTrue();
        """
      }
    }
  }

  @Test fun `existsSync() with directory`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-dir").toAbsolutePath()
      Files.createDirectory(samplePath)
      assertTrue(Files.exists(samplePath), "should have written directory")
      assertTrue(Files.isDirectory(samplePath))

      dual {
        assertTrue(
          assertDoesNotThrow { fs.existsSync(NodePath.from(samplePath)) },
        )
      }.guest {
        // language=javascript
        """
          const { existsSync } = require("node:fs");
          test(existsSync).isNotNull();
          test(existsSync("$samplePath")).shouldBeTrue();
        """
      }
    }
  }

  @Test fun `access() with text file`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      Files.write(samplePath, "Hello, world!".toByteArray())
      assertTrue(Files.exists(samplePath), "should have written file")
      assertEquals("Hello, world!", Files.readString(samplePath))
      fs.access(NodePath.from(samplePath)) { err ->
        assertNull(err)
      }
      fs.access(
        Value.asValue(NodePath.from(samplePath).toString()),
        Value.asValue(FilesystemConstants.R_OK),
        Value.asValue(Function { err: AbstractJsException? -> assertNull(err) }),
      )

      assertThrows<TypeError> {
        fs.access(
          Value.asValue(NodePath.from(samplePath).toString()),
          Value.asValue(FilesystemConstants.R_OK),
          Value.asValue(true),
        )
      }

      executeGuest {
        // language=javascript
        """
          const { access } = require("node:fs");
          test(access).isNotNull();
          let callbackDispatched = false;
          let fileErr = null;
          access("$samplePath", (err) => {
            callbackDispatched = true;
            fileErr = err || null;
          });
          test(callbackDispatched).shouldBeTrue();
          test(fileErr).isNull();
        """
      }
      executeGuest {
        // language=javascript
        """
          const { access, constants } = require("node:fs");
          test(access).isNotNull();
          let callbackDispatched = false;
          let fileErr = null;
          access("$samplePath", constants.R_OK, (err) => {
            callbackDispatched = true;
            fileErr = err || null;
          });
          test(callbackDispatched).shouldBeTrue();
          test(fileErr).isNull();
        """
      }
    }
  }

  @Test fun `access() should fail with invalid callback type`() = withTemp { tmp ->
    filesystem.provideStd().let {
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      Files.write(samplePath, "Hello, world!".toByteArray())
      assertTrue(Files.exists(samplePath), "should have written file")
      assertEquals("Hello, world!", Files.readString(samplePath))
      executeGuest {
        // language=javascript
        """
          const { access } = require("node:fs");
          test(access).isNotNull();
          test(() => access("$samplePath", true)).fails();
        """
      }
      executeGuest {
        // language=javascript
        """
          const { access, constants } = require("node:fs");
          test(access).isNotNull();
          test(() => access("$samplePath", constants.R_OK, true)).fails();
        """
      }
    }
  }

  @Test fun `access() with text file and mode as READ`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      Files.write(samplePath, "Hello, world!".toByteArray())
      assertTrue(Files.exists(samplePath), "should have written file")
      assertEquals("Hello, world!", Files.readString(samplePath))

      dual {
        fs.access(NodePath.from(samplePath), mode = READ) { err ->
          assertNull(err)
        }
        fs.access(
          Value.asValue(samplePath.toString()),
          Value.asValue(FilesystemConstants.R_OK),
          Value.asValue({ err: Any? -> assertNull(err) }),
        )
      }.guest {
        // language=javascript
        """
          const { access, constants } = require("node:fs");
          test(access).isNotNull();
          let callbackDispatched = false;
          let fileErr = null;
          access("$samplePath", constants.R_OK, (err) => {
            fileErr = err || null;
          });
          test(callbackDispatched).shouldBeFalse();
          test(fileErr).isNull();
        """
      }
    }
  }

  @Test fun `access() with text file and mode as WRITE`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      Files.write(samplePath, "Hello, world!".toByteArray())
      assertTrue(Files.exists(samplePath), "should have written file")
      assertEquals("Hello, world!", Files.readString(samplePath))

      dual {
        fs.access(NodePath.from(samplePath), mode = WRITE) { err ->
          assertNull(err)
        }
      }.guest {
        // language=javascript
        """
          const { access, constants } = require("node:fs");
          test(access).isNotNull();
          let callbackDispatched = false;
          let fileErr = null;
          access("$samplePath", constants.W_OK, (err) => {
            fileErr = err || null;
          });
          test(callbackDispatched).shouldBeFalse();
          test(fileErr).isNull();
        """
      }
    }
  }

  @Test fun `access() with text file and mode as EXECUTE`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      Files.write(samplePath, "Hello, world!".toByteArray())
      assertTrue(Files.exists(samplePath), "should have written file")
      assertEquals("Hello, world!", Files.readString(samplePath))

      assertFalse(Files.isExecutable(samplePath), "sample path should not be executable at first")

      // should not be executable at first
      fs.access(NodePath.from(samplePath), mode = EXECUTE) { err ->
        assertNotNull(err)
      }

      // make it executable
      Files.getPosixFilePermissions(samplePath).let { perms ->
        Files.setPosixFilePermissions(samplePath, perms + OWNER_EXECUTE)
      }
      assertTrue(Files.isExecutable(samplePath), "path should now be executable")

      dual {
        // now it should be executable
        fs.access(NodePath.from(samplePath), mode = EXECUTE) { err ->
          assertNull(err)
        }
      }.guest {
        // language=javascript
        """
          const { access, constants } = require("node:fs");
          test(access).isNotNull();
          let callbackDispatched = false;
          let fileErr = null;
          access("$samplePath", constants.X_OK, (err) => {
            fileErr = err || null;
          });
          test(callbackDispatched).shouldBeFalse();
          test(fileErr).isNull();
        """
      }
    }
  }

  @Test fun `access() with text file and mode failure`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      Files.write(samplePath, "Hello, world!".toByteArray())
      assertTrue(Files.exists(samplePath), "should have written file")
      assertEquals("Hello, world!", Files.readString(samplePath))

      // remove read access from self
      val perms = Files.getPosixFilePermissions(samplePath)
      Files.setPosixFilePermissions(
        samplePath,
        setOf(
          OWNER_WRITE,
          OWNER_EXECUTE,
        ),
      )

      dual {
        fs.access(NodePath.from(samplePath), mode = READ) { err ->
          assertNotNull(err)
        }
      }.guest {
        // language=javascript
        """
          const { access, constants } = require("node:fs");
          test(access).isNotNull();
          let callbackDispatched = false;
          let fileErr = null;
          access("$samplePath", constants.R_OK, (err) => {
            fileErr = err || null;
          });
          test(callbackDispatched).shouldBeFalse();
          test(fileErr).isNotNull();
        """
      }

      // restore perms and delete
      Files.setPosixFilePermissions(samplePath, perms)
      Files.delete(samplePath)
    }
  }

  @Test fun `accessSync() with text file`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      Files.write(samplePath, "Hello, world!".toByteArray())
      assertTrue(Files.exists(samplePath), "should have written file")
      assertEquals("Hello, world!", Files.readString(samplePath))

      dual {
        assertDoesNotThrow { fs.accessSync(NodePath.from(samplePath)) }
        assertDoesNotThrow { fs.accessSync(Value.asValue(NodePath.from(samplePath).toString())) }
      }.guest {
        // language=javascript
        """
          const { accessSync } = require("node:fs");
          test(accessSync).isNotNull();
          test(() => accessSync("$samplePath")).doesNotFail();
        """
      }
    }
  }

  @Test fun `accessSync() with text file and mode as READ`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      Files.write(samplePath, "Hello, world!".toByteArray())
      assertTrue(Files.exists(samplePath), "should have written file")
      assertEquals("Hello, world!", Files.readString(samplePath))

      dual {
        assertDoesNotThrow { fs.accessSync(NodePath.from(samplePath), READ) }
        assertDoesNotThrow {
          fs.accessSync(
            Value.asValue(NodePath.from(samplePath).toString()),
            Value.asValue(FilesystemConstants.R_OK),
          )
        }
      }.guest {
        // language=javascript
        """
          const { accessSync, constants } = require("node:fs");
          test(accessSync).isNotNull();
          test(() => accessSync("$samplePath", constants.R_OK)).doesNotFail();
        """
      }
    }
  }

  @Test fun `accessSync() with text file and mode as WRITE`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      Files.write(samplePath, "Hello, world!".toByteArray())
      assertTrue(Files.exists(samplePath), "should have written file")
      assertEquals("Hello, world!", Files.readString(samplePath))

      dual {
        assertDoesNotThrow { fs.accessSync(NodePath.from(samplePath), WRITE) }
        assertDoesNotThrow {
          fs.accessSync(
            Value.asValue(NodePath.from(samplePath).toString()),
            Value.asValue(FilesystemConstants.W_OK),
          )
        }
      }.guest {
        // language=javascript
        """
          const { accessSync, constants } = require("node:fs");
          test(accessSync).isNotNull();
          test(() => accessSync("$samplePath", constants.W_OK)).doesNotFail();
        """
      }

      // remove read abilities by owner
      val perms = Files.getPosixFilePermissions(samplePath)
      Files.setPosixFilePermissions(
        samplePath,
        setOf(
          OWNER_WRITE,
          OWNER_EXECUTE,
        ),
      )

      assertFalse(Files.isReadable(samplePath), "should not be able to read file after read perms revoked")

      assertThrows<Error> {
        fs.accessSync(NodePath.from(samplePath), READ)
      }
      assertThrows<Error> {
        fs.accessSync(
          Value.asValue(NodePath.from(samplePath).toString()),
          Value.asValue(FilesystemConstants.R_OK),
        )
      }

      executeGuest {
        // language=javascript
        """
          const { accessSync, constants } = require("node:fs");
          test(accessSync).isNotNull();
          test(() => accessSync("$samplePath", constants.R_OK)).fails();
        """
      }

      // response perms
      Files.setPosixFilePermissions(samplePath, perms)
    }
  }

  @Test fun `readFile() with text file`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      Files.write(samplePath, "Hello, world!".toByteArray())
      assertTrue(Files.exists(samplePath), "should have written file")
      assertEquals("Hello, world!", Files.readString(samplePath))

      dual {
        fs.readFile(NodePath.from(samplePath), ReadFileOptions(encoding = "utf8")) { err, data ->
          assertNull(err)
          assertNotNull(data)
          assertIs<String>(data)
          assertEquals("Hello, world!", data)
        }

        fs.readFile(Value.asValue(samplePath.toString()), Value.asValue(null)) { err, data ->
          assertNull(err)
          assertNotNull(data)
          assertIs<String>(data)
          assertEquals("Hello, world!", data)
        }
      }.guest {
        // language=javascript
        """
          const { readFile } = require("node:fs");
          test(readFile).isNotNull();
          let callbackDispatched = false;
          let fileErr = null;
          let fileData = null;

          readFile("$samplePath", { encoding: 'utf-8' }, (err, data) => {
            callbackDispatched = true;
            fileErr = null;
            fileData = data;
          });
          test(callbackDispatched).shouldBeTrue();
          test(fileErr).isNull();
          test(typeof fileData === 'string').shouldBeTrue();
          test(fileData).isEqualTo("Hello, world!");
        """
      }
    }
  }

  @Test fun `readFile() with text file with default encoding`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      Files.write(samplePath, "Hello, world!".toByteArray())
      assertTrue(Files.exists(samplePath), "should have written file")
      assertEquals("Hello, world!", Files.readString(samplePath))

      dual {
        fs.readFile(NodePath.from(samplePath)) { err, data ->
          assertNull(err)
          assertNotNull(data)
          assertIs<String>(data)
          assertEquals("Hello, world!", data)
        }
      }.guest {
        // language=javascript
        """
          const { readFile } = require("node:fs");
          test(readFile).isNotNull();
          let callbackDispatched = false;
          let fileErr = null;
          let fileData = null;

          readFile("$samplePath", (err, data) => {
            callbackDispatched = true;
            fileErr = null;
            fileData = data;
          });
          test(callbackDispatched).shouldBeTrue("callback must be dispatched");
          test(fileErr).isNull();
          const fileDataType = typeof fileData;
          test(typeof fileData === 'string').shouldBeTrue(
            'returned file data should be of type `string` but was `' + fileDataType + '`'
          );
          test(fileData).isEqualTo("Hello, world!");
        """
      }
    }
  }

  @Test fun `readFileSync() with text file`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      Files.write(samplePath, "Hello, world!".toByteArray())
      assertTrue(Files.exists(samplePath), "should have written file")
      assertEquals("Hello, world!", Files.readString(samplePath))

      dual {
        assertEquals(
          "Hello, world!",
          assertDoesNotThrow { fs.readFileSync(NodePath.from(samplePath), ReadFileOptions(encoding = "utf8")) },
        )
      }.guest {
        // language=javascript
        """
          const { readFileSync } = require("node:fs");
          test(readFileSync).isNotNull();
          test(readFileSync("$samplePath", { encoding: 'utf-8' })).isNotNull();
          test(readFileSync("$samplePath", { encoding: 'utf-8' })).isEqualTo("Hello, world!");
        """
      }
    }
  }

  @Test fun `readFileSync() with text file and default encoding`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      Files.write(samplePath, "Hello, world!".toByteArray())
      assertTrue(Files.exists(samplePath), "should have written file")
      assertEquals("Hello, world!", Files.readString(samplePath))

      dual {
        assertEquals(
          "Hello, world!",
          assertDoesNotThrow { fs.readFileSync(NodePath.from(samplePath), ReadFileOptions(encoding = "utf8")) },
        )
      }.guest {
        // language=javascript
        """
          const { readFileSync } = require("node:fs");
          test(readFileSync).isNotNull();
          test(readFileSync("$samplePath")).isNotNull();
          test(readFileSync("$samplePath")).isEqualTo("Hello, world!");
        """
      }
    }
  }

  @Test fun `mkdir() with valid directory name`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-dir").toAbsolutePath()
      val samplePath2 = tmp.resolve("some-dir2").toAbsolutePath()
      assertFalse(Files.exists(samplePath), "directory should not exist before creation")
      assertFalse(Files.exists(samplePath2), "directory should not exist before creation")
      assertIs<WritableFilesystemAPI>(fs)

      fs.mkdir(NodePath.from(samplePath)) {
        assertNull(it)
        assertTrue(Files.exists(samplePath), "directory should exist after creation")
      }
      assertTrue(Files.exists(samplePath))
      assertTrue(Files.isDirectory(samplePath))

      executeGuest {
        // language=javascript
        """
          const { mkdir } = require("node:fs");
          test(mkdir).isNotNull();
          let created = false;
          let mkdirError = null;
          mkdir("$samplePath2", (err) => {
            created = !err;
            mkdirError = err ? err : mkdirError;
          });
          test(created).shouldBeTrue();
          test(mkdirError).isNull();
        """
      }.doesNotFail()

      assertTrue(Files.exists(samplePath2))
      assertTrue(Files.isDirectory(samplePath2))
    }
  }

  @Test fun `mkdirSync() with valid directory name`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-dir").toAbsolutePath()
      val samplePath2 = tmp.resolve("some-dir2").toAbsolutePath()
      assertFalse(Files.exists(samplePath), "directory should not exist before creation")
      assertFalse(Files.exists(samplePath2), "directory should not exist before creation")
      assertIs<WritableFilesystemAPI>(fs)

      assertEquals(
        samplePath.toString(),
        assertNotNull(assertDoesNotThrow { fs.mkdirSync(NodePath.from(samplePath)) }),
      )
      assertTrue(Files.exists(samplePath))
      assertTrue(Files.isDirectory(samplePath))

      executeGuest {
        // language=javascript
        """
          const { mkdirSync, existsSync } = require("node:fs");
          test(mkdirSync).isNotNull();
          mkdirSync("$samplePath2");
          test(existsSync("$samplePath2")).shouldBeTrue();
        """
      }.doesNotFail()

      assertTrue(Files.exists(samplePath2))
      assertTrue(Files.isDirectory(samplePath2))
    }
  }

  @Test fun `writeFile() with valid file and string as data`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      val samplePath2 = tmp.resolve("some-file2.txt").toAbsolutePath()
      assertFalse(Files.exists(samplePath), "file should not exist before creation")
      assertFalse(Files.exists(samplePath2), "file should not exist before creation")
      assertIs<WritableFilesystemAPI>(fs)

      fs.writeFile(NodePath.from(samplePath), "Hello, world!") { err ->
        assertNull(err)
      }
      assertTrue(Files.exists(samplePath), "file should exist after creation")
      assertEquals("Hello, world!", Files.readString(samplePath))

      executeGuest {
        // language=javascript
        """
          const { writeFile } = require("node:fs");
          test(writeFile).isNotNull();
          let writeErr = null;
          writeFile("$samplePath2", "Hello, world!", (err) => {
            writeErr = err || null;
          });
          test(writeErr).isNull();
        """
      }.doesNotFail()

      assertTrue(Files.exists(samplePath2))
      assertTrue(Files.isRegularFile(samplePath2))
      assertEquals("Hello, world!", Files.readString(samplePath2))
    }
  }

  @Test fun `writeFile() with valid file and host bytearray`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      assertFalse(Files.exists(samplePath), "file should not exist before creation")
      assertIs<WritableFilesystemAPI>(fs)

      fs.writeFile(NodePath.from(samplePath), "Hello, world!".toByteArray(StandardCharsets.UTF_8)) { err ->
        assertNull(err)
      }
      assertTrue(Files.exists(samplePath), "file should exist after creation")
      assertEquals("Hello, world!", Files.readString(samplePath))
    }
  }

  @Test fun `writeFile() with valid file and string as string`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      val samplePath2 = tmp.resolve("some-file2.txt").toAbsolutePath()
      assertFalse(Files.exists(samplePath), "file should not exist before creation")
      assertFalse(Files.exists(samplePath2), "file should not exist before creation")
      assertIs<WritableFilesystemAPI>(fs)

      fs.writeFile(NodePath.from(samplePath), "Hello, world!", WriteFileOptions(encoding = "utf-8")) { err ->
        assertNull(err)
      }
      assertTrue(Files.exists(samplePath), "file should exist after creation")
      assertEquals("Hello, world!", Files.readString(samplePath))

      executeGuest {
        // language=javascript
        """
          const { writeFile } = require("node:fs");
          test(writeFile).isNotNull();
          let writeErr = null;
          writeFile("$samplePath2", "Hello, world!", { encoding: 'utf-8' }, (err) => {
            writeErr = err || null;
          });
          test(writeErr).isNull();
        """
      }.doesNotFail()

      assertTrue(Files.exists(samplePath2))
      assertTrue(Files.isRegularFile(samplePath2))
      assertEquals("Hello, world!", Files.readString(samplePath2))
    }
  }

  @Test fun `writeFileSync() with valid file and string as data`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      val samplePath2 = tmp.resolve("some-file2.txt").toAbsolutePath()
      assertFalse(Files.exists(samplePath), "file should not exist before creation")
      assertFalse(Files.exists(samplePath2), "file should not exist before creation")
      assertIs<WritableFilesystemAPI>(fs)

      assertDoesNotThrow {
        fs.writeFileSync(NodePath.from(samplePath), "Hello, world!")
      }

      assertTrue(Files.exists(samplePath), "file should exist after creation")
      assertEquals("Hello, world!", Files.readString(samplePath))
      Files.delete(samplePath)

      assertDoesNotThrow {
        fs.writeFileSync(Value.asValue(NodePath.from(samplePath).toString()), Value.asValue("Hello, world!"))
      }

      assertTrue(Files.exists(samplePath), "file should exist after creation")
      assertEquals("Hello, world!", Files.readString(samplePath))
      Files.delete(samplePath)

      executeGuest {
        // language=javascript
        """
          const { writeFileSync } = require("node:fs");
          test(writeFileSync).isNotNull();
          writeFileSync("$samplePath2", "Hello, world!")
        """
      }.doesNotFail()

      assertTrue(Files.exists(samplePath2))
      assertTrue(Files.isRegularFile(samplePath2))
      assertEquals("Hello, world!", Files.readString(samplePath2))
    }
  }

  @Test fun `writeFileSync() with valid file and host bytearray`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      assertFalse(Files.exists(samplePath), "file should not exist before creation")
      assertIs<WritableFilesystemAPI>(fs)
      fs.writeFileSync(NodePath.from(samplePath), "Hello, world!".toByteArray(StandardCharsets.UTF_8))
      assertTrue(Files.exists(samplePath), "file should exist after creation")
      assertEquals("Hello, world!", Files.readString(samplePath))
    }
  }

  @Test fun `writeFileSync() with valid file and string as string`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      val samplePath2 = tmp.resolve("some-file2.txt").toAbsolutePath()
      assertFalse(Files.exists(samplePath), "file should not exist before creation")
      assertFalse(Files.exists(samplePath2), "file should not exist before creation")
      assertIs<WritableFilesystemAPI>(fs)

      fs.writeFileSync(NodePath.from(samplePath), "Hello, world!", WriteFileOptions(encoding = "utf-8"))
      assertTrue(Files.exists(samplePath), "file should exist after creation")
      assertEquals("Hello, world!", Files.readString(samplePath))

      executeGuest {
        // language=javascript
        """
          const { writeFileSync } = require("node:fs");
          test(writeFileSync).isNotNull();
          writeFileSync("$samplePath2", "Hello, world!", { encoding: 'utf-8' });
        """
      }.doesNotFail()

      assertTrue(Files.exists(samplePath2))
      assertTrue(Files.isRegularFile(samplePath2))
      assertEquals("Hello, world!", Files.readString(samplePath2))
    }
  }

  @Test fun `copyFile() with valid file`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      val srcPath = tmp.resolve("some-file.txt").toAbsolutePath()
      val destPath = tmp.resolve("some-file-2.txt").toAbsolutePath()
      Files.writeString(srcPath, "Hello, world!", StandardCharsets.UTF_8)

      assertTrue(Files.exists(srcPath), "src file should not exist before creation")
      assertFalse(Files.exists(destPath), "dest file should not exist before creation")
      assertIs<WritableFilesystemAPI>(fs)
      fs.copyFile(NodePath.from(srcPath), NodePath.from(destPath)) {
        assertNull(it, "copy file operation should not fail")
      }
      assertTrue(Files.exists(destPath), "file should exist after creation")
      assertEquals("Hello, world!", Files.readString(destPath))

      Files.delete(destPath)
      assertFalse(Files.exists(destPath), "file should not exist before guest test")

      executeGuest {
        // language=javascript
        """
          const { copyFile } = require("node:fs");
          test(copyFile).isNotNull();
          let fsErr = null;
          let didDispatch = false;
          copyFile("$srcPath", "$destPath", (err) => {
            fsErr = err || null;
            didDispatch = true;
          });
          test(fsErr).isNull();
          test(didDispatch).shouldBeTrue();
        """
      }.doesNotFail()

      assertTrue(Files.exists(destPath))
      assertTrue(Files.isRegularFile(destPath))
      assertEquals("Hello, world!", Files.readString(destPath))
    }
  }

  @Test fun `copyFileSync() with valid file`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      val srcPath = tmp.resolve("some-file.txt").toAbsolutePath()
      val destPath = tmp.resolve("some-file-2.txt").toAbsolutePath()
      Files.writeString(srcPath, "Hello, world!", StandardCharsets.UTF_8)

      assertTrue(Files.exists(srcPath), "src file should exist before creation")
      assertFalse(Files.exists(destPath), "dest file should not exist before creation")
      assertIs<WritableFilesystemAPI>(fs)
      fs.copyFileSync(NodePath.from(srcPath), NodePath.from(destPath))
      assertTrue(Files.exists(destPath), "file should exist after creation")
      assertEquals("Hello, world!", Files.readString(destPath))

      Files.delete(destPath)
      assertFalse(Files.exists(destPath), "file should not exist before guest test")

      fs.copyFileSync(
        Value.asValue(NodePath.from(srcPath).toString()),
        Value.asValue(NodePath.from(destPath).toString()),
      )

      assertTrue(Files.exists(destPath), "dest file should exist after another copy")
      assertEquals("Hello, world!", Files.readString(destPath))

      Files.delete(destPath)
      assertFalse(Files.exists(destPath), "file should not exist before guest test")

      executeGuest {
        // language=javascript
        """
          const { copyFileSync } = require("node:fs");
          test(copyFileSync).isNotNull();
          copyFileSync("$srcPath", "$destPath");
        """
      }.doesNotFail()

      assertTrue(Files.exists(destPath))
      assertTrue(Files.isRegularFile(destPath))
      assertEquals("Hello, world!", Files.readString(destPath))
    }
  }

  @Test fun `copyFile() with valid file and non-overwrite`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      val srcPath = tmp.resolve("some-file.txt").toAbsolutePath()
      val destPath = tmp.resolve("some-file-2.txt").toAbsolutePath()
      Files.writeString(srcPath, "Hello, world!", StandardCharsets.UTF_8)
      Files.writeString(destPath, "Should not overwrite", StandardCharsets.UTF_8)

      assertTrue(Files.exists(srcPath), "src file should exist before creation")
      assertTrue(Files.exists(destPath), "dest file should exist before creation")
      assertIs<WritableFilesystemAPI>(fs)
      fs.copyFile(NodePath.from(srcPath), NodePath.from(destPath), mode = FilesystemConstants.COPYFILE_EXCL) {
        assertNotNull(it, "copy operation should fail due to non-overwrite")
      }
      assertTrue(Files.exists(destPath), "file should exist after creation")
      assertEquals("Should not overwrite", Files.readString(destPath))

      executeGuest {
        // language=javascript
        """
          const { copyFile, constants } = require("node:fs");
          test(copyFile).isNotNull();
          let fsErr = null;
          let didDispatch = false;
          copyFile("$srcPath", "$destPath", constants.COPYFILE_EXCL, (err) => {
            fsErr = err || null;
            didDispatch = true;
          });
          test(fsErr).isNotNull();
          test(didDispatch).shouldBeTrue();
        """
      }.doesNotFail()

      assertTrue(Files.exists(destPath))
      assertTrue(Files.isRegularFile(destPath))
      assertEquals("Should not overwrite", Files.readString(destPath))
    }
  }

  @Test fun `copyFileSync() with valid file and non-overwrite`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      val srcPath = tmp.resolve("some-file.txt").toAbsolutePath()
      val destPath = tmp.resolve("some-file-2.txt").toAbsolutePath()
      Files.writeString(srcPath, "Hello, world!", StandardCharsets.UTF_8)
      Files.writeString(destPath, "Should not overwrite", StandardCharsets.UTF_8)

      assertTrue(Files.exists(srcPath), "src file should exist before creation")
      assertTrue(Files.exists(destPath), "dest file should exist before creation")
      assertIs<WritableFilesystemAPI>(fs)
      assertThrows<Throwable> {
        fs.copyFileSync(NodePath.from(srcPath), NodePath.from(destPath), mode = FilesystemConstants.COPYFILE_EXCL)
      }
      assertTrue(Files.exists(destPath), "file should exist after creation")
      assertEquals("Should not overwrite", Files.readString(destPath))

      executeGuest {
        // language=javascript
        """
          const { copyFileSync, constants } = require("node:fs");
          test(copyFileSync).isNotNull();
          test(() => { copyFileSync("$srcPath", "$destPath", constants.COPYFILE_EXCL) }).fails();
        """
      }.doesNotFail()

      assertTrue(Files.exists(destPath))
      assertTrue(Files.isRegularFile(destPath))
      assertEquals("Should not overwrite", Files.readString(destPath))
    }
  }

  @Test fun `copyFile() with missing file`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      val srcPath = tmp.resolve("some-file-missing.txt").toAbsolutePath()
      val destPath = tmp.resolve("some-file-2.txt").toAbsolutePath()

      assertFalse(Files.exists(srcPath), "src file should not exist before creation")
      assertFalse(Files.exists(destPath), "dest file should not exist before creation")
      assertIs<WritableFilesystemAPI>(fs)
      fs.copyFile(NodePath.from(srcPath), NodePath.from(destPath)) {
        assertNotNull(it, "copy file operation should fail")
      }
      assertFalse(Files.exists(destPath), "dest file should exist after error")

      executeGuest {
        // language=javascript
        """
          const { copyFile } = require("node:fs");
          test(copyFile).isNotNull();
          copyFile("$srcPath", "$destPath", (err) => {
            test(err).isNotNull()
          })
        """
      }.doesNotFail()
    }
  }

  @Test fun `copyFileSync() with missing file`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      val srcPath = tmp.resolve("some-file-missing.txt").toAbsolutePath()
      val destPath = tmp.resolve("some-file-2.txt").toAbsolutePath()

      assertFalse(Files.exists(srcPath), "src file should not exist before creation")
      assertFalse(Files.exists(destPath), "dest file should not exist before creation")
      assertIs<WritableFilesystemAPI>(fs)
      assertThrows<Throwable> {
        fs.copyFileSync(NodePath.from(srcPath), NodePath.from(destPath))
      }
      assertFalse(Files.exists(destPath), "dest file should exist after error")

      executeGuest {
        // language=javascript
        """
          const { copyFileSync } = require("node:fs");
          test(copyFileSync).isNotNull();
          test(() => { copyFileSync("$srcPath", "$destPath") }).fails();
        """
      }.doesNotFail()
    }
  }
}

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
package elide.runtime.gvm.internals.vfs

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.io.TempDir
import java.nio.channels.Channels
import java.nio.channels.SeekableByteChannel
import java.nio.channels.WritableByteChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.createFile
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.assertEquals

internal class HybridVfsTest {
  /** Temporary directory used for host-related test cases. */
  @TempDir lateinit var tempDirectory: Path

  /** Read all data from this channel as a UTF-8 string. */
  private fun SeekableByteChannel.readText(): String {
    return Channels.newReader(this, Charsets.UTF_8).readText()
  }

  /** Write a [text] message into the channel using the UTF-8 charset and immediately flush the stream. */
  private fun WritableByteChannel.writeText(text: String) {
    Channels.newWriter(this, Charsets.UTF_8).run {
      write(text)
      flush()
    }
  }

  /**
   * Create and configure a new [HybridVfs] for use in tests, using an embedded bundle from the test resources for
   * the in-memory layer.
   *
   * @see useVfs
   */
  private fun acquireVfs(): HybridVfs {
    val bundles = listOf(HybridVfsTest::class.java.getResource("/sample-vfs.tar")!!.toURI())
    return HybridVfs.acquire(writable = true, overlay = bundles)
  }

  /** Convenience method used to [acquire][acquireVfs] a [HybridVfs] instance and use it in a test. */
  private inline fun useVfs(block: (HybridVfs) -> Unit) = block(acquireVfs())

  @Test fun testReadFromEmbeddedBundles(): Unit = useVfs { vfs ->
    val path = vfs.parsePath("/hello.txt")

    val channel = assertDoesNotThrow("should allow reading known good bundled file") {
      vfs.newByteChannel(path, mutableSetOf(StandardOpenOption.READ))
    }

    assertEquals(
      expected = "hello",
      actual = channel.readText().trim(),
      message = "should read file contents from embedded bundle",
    )
  }

  @Test fun testReadFromHost() = useVfs { vfs ->
    val data = "host"
    val file = tempDirectory.resolve("hello.txt").createFile().apply { writeText(data) }

    val channel = assertDoesNotThrow("should allow reading from host") {
      vfs.newByteChannel(file, mutableSetOf(StandardOpenOption.READ))
    }

    assertEquals(
      expected = data,
      actual = channel.readText().trim(),
      message = "should read file contents written to host file",
    )
  }

  @Test fun testReadWithGenericPath() = useVfs { vfs ->
    // points to a file in the embedded bundle, but is constructed using the current
    // file system provider (not by calling vfs.parsePath)
    val inMemoryPath = Path.of("/hello.txt")
    assertDoesNotThrow("should accept generic path object when reading from memory") {
      vfs.newByteChannel(inMemoryPath, mutableSetOf(StandardOpenOption.READ))
    }

    // same as the previous case, but using a host path instead (ensure the file exists first)
    val hostPath = tempDirectory.resolve("hello.txt").createFile()
    assertDoesNotThrow("should accept generic path object when reading from host") {
      vfs.newByteChannel(hostPath, mutableSetOf(StandardOpenOption.READ))
    }
  }

  @Test fun testWrite() = useVfs { vfs ->
    val hostPath = tempDirectory.resolve("hello.txt")
    val data = "Hello"

    // force the creation of a new file, to avoid collisions with other tests
    val channel = assertDoesNotThrow("should open channel to file in host file system") {
      vfs.newByteChannel(hostPath, mutableSetOf(StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW))
    }

    assertDoesNotThrow("should allow writing to file in host file system") {
      channel.writeText(data)
    }

    assertEquals(
      expected = data,
      actual = hostPath.readText(),
      message = "file should contain the written data",
    )
  }
}

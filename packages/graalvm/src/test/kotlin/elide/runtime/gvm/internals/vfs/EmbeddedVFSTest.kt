package elide.runtime.gvm.internals.vfs

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.assertDoesNotThrow
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import elide.runtime.gvm.internals.vfs.EmbeddedGuestVFSImpl.Builder
import elide.runtime.gvm.internals.vfs.EmbeddedGuestVFSImpl.EmbeddedVFSFactory
import elide.runtime.gvm.internals.vfs.EmbeddedGuestVFSImpl.EmbeddedVFSFactory.buildFs
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

/** Tests for the embedded VFS implementation. */
@TestCase internal class EmbeddedVFSTest : AbstractVFSTest<EmbeddedGuestVFSImpl, Builder, EmbeddedVFSFactory>() {
  /** @return Embedded VFS instance factory. */
  override fun factory(): EmbeddedVFSFactory = EmbeddedVFSFactory

  /** @return New builder. */
  override fun newBuilder(): Builder = Builder.newBuilder()

  /** @return Indication that no host changes should be observed. */
  override fun shouldUseHost(): Boolean = false

  /** Test: Load a bundle from a regular (non-compressed) tarball. */
  @Test fun testBundleFromTarball() {
    // load sample tarball
    val sampleTarball = EmbeddedVFSTest::class.java.getResource("/sample-vfs.tar")
    assertNotNull(sampleTarball, "should be able to find sample tarball")

    // manually test loader fn
    val effective = EffectiveGuestVFSConfig.DEFAULTS
    val fsConfig = effective.buildFs()

    val result = EmbeddedGuestVFSImpl.loadBundleURIs(listOf(sampleTarball.toURI()), fsConfig)
    assertNotNull(result, "should not get `null` from `loadBundleFromURI` for known-good input")
    val (tree, databag) = result
    assertNotNull(tree, "should not get `null` from `loadBundleFromURI` for known-good input")
    assertNotNull(databag, "should not get `null` from `loadBundleFromURI` for known-good input")

    // test consistency of the tree
    assertTrue(tree.hasRoot(), "tree should always have root entry")
    assertTrue(tree.root.hasDirectory(), "root should always be a directory")
    val entries = tree.root.directory.childrenList
    assertEquals(1, entries.size, "root directory should have exactly 1 child")
    val testFile = entries.first().file
    assertEquals("hello.txt", testFile.name, "test file name should be preserved")
    assertEquals(6, testFile.size, "un-compressed size should be accurate")
    assertEquals(0, testFile.offset, "offset for first file should be 0")

    // build it into a VFS instance
    val vfs = newBuilder().setBundle(result).build()
    val path = vfs.getPath("hello.txt")
    assertNotNull(path, "should be able to parse path 'hello.txt'")
    assertNotNull(vfs, "should be able to create VFS from sample tarball")

    val exampleFileContents = vfs.readStream(path).bufferedReader(StandardCharsets.UTF_8).use {
      it.readText()
    }
    assertEquals("hello", exampleFileContents.trim(), "example file contents should decode correctly")
  }

  /** Test: Load a bundle from a regular (non-compressed) tarball which contains folders. */
  @Test fun testBundleFromTarballDirs() {
    // load sample tarball
    val sampleTarball = EmbeddedVFSTest::class.java.getResource("/sample-vfs-dirs.tar")
    assertNotNull(sampleTarball, "should be able to find sample tarball")

    // manually test loader fn
    val effective = EffectiveGuestVFSConfig.DEFAULTS
    val fsConfig = effective.buildFs()

    val result = EmbeddedGuestVFSImpl.loadBundleURIs(listOf(sampleTarball.toURI()), fsConfig)
    assertNotNull(result, "should not get `null` from `loadBundleFromURI` for known-good input")
    val (tree, databag) = result
    assertNotNull(tree, "should not get `null` from `loadBundleFromURI` for known-good input")
    assertNotNull(databag, "should not get `null` from `loadBundleFromURI` for known-good input")

    // test consistency of the tree
    assertTrue(tree.hasRoot(), "tree should always have root entry")
    assertTrue(tree.root.hasDirectory(), "root should always be a directory")
    val entries = tree.root.directory.childrenList
    assertEquals(3, entries.size, "root directory should have exactly 3 children")

    // test file should be present
    val testFile = entries.find { it.hasFile() && it.file.name == "hello.txt" }?.file
    assertNotNull(testFile, "should be able to locate test file ('hello.txt')")
    assertEquals("hello.txt", testFile.name, "test file name should be preserved")
    assertEquals(6, testFile.size, "un-compressed size should be accurate")
    assertEquals(0, testFile.offset, "offset for first file should be 0")

    // test folder 1 should be present
    val testFolder1 = entries.find { it.hasDirectory() && it.directory.name == "folder" }?.directory
    assertNotNull(testFolder1, "should be able to locate test dir 1 ('folder')")
    assertEquals("folder", testFolder1.name, "test folder name should be preserved")

    // test folder 2 should be present
    val testFolder2 = entries.find { it.hasDirectory() && it.directory.name == "another" }?.directory
    assertNotNull(testFolder2, "should be able to locate test dir 2 ('another')")
    assertEquals("another", testFolder2.name, "test folder name should be preserved")

    // test folder 3 should be present
    assertEquals(1, testFolder2.childrenCount, "should have 1 child under `another` dir")
    val testFolder3 = testFolder2.childrenList.find {
      it.hasDirectory() && it.directory.name == "nested"
    }?.directory
    assertNotNull(testFolder3, "should be able to locate test dir 3 ('another/nested')")
    assertEquals("nested", testFolder3.name, "test folder name should be preserved")

    // build it into a VFS instance
    val vfs = newBuilder().setBundle(result).build()
    val path = databag.getPath("hello.txt")
    assertNotNull(path, "should be able to parse path 'hello.txt'")
    assertNotNull(vfs, "should be able to create VFS from sample tarball")

    val exampleFileContents = vfs.readStream(path).bufferedReader(StandardCharsets.UTF_8).use {
      it.readText()
    }
    assertEquals("hello", exampleFileContents.trim(), "example file contents should decode correctly")

    // read the JSON example path
    val path2 = databag.getPath("folder", "something.json")
    assertNotNull(path2, "should be able to parse path 'folder/something.json'")
    val exampleFileContents2 = vfs.readStream(path2).bufferedReader(StandardCharsets.UTF_8).use {
      it.readText()
    }
    assertEquals(
      "{\"hi\": \"hello\"}",
      exampleFileContents2.trim(),
      "example file contents should decode correctly"
    )

    // read the 2nd txt example path
    val path3 = databag.getPath("another", "nested", "cool.txt")
    assertNotNull(path3, "should be able to parse path 'another/nested/cool.txt'")
    val exampleFileContents3 = vfs.readStream(path3).bufferedReader(StandardCharsets.UTF_8).use {
      it.readText()
    }
    assertEquals(
      "hello again",
      exampleFileContents3.trim(),
      "example file contents should decode correctly"
    )
  }

  /** Test: Load a bundle from a compressed tarball. */
  @Test fun testBundleFromTarballGzip() {
    // load sample tarball
    val sampleTarball = EmbeddedVFSTest::class.java.getResource("/sample-vfs.tar.gz")
    assertNotNull(sampleTarball, "should be able to find sample tarball")

    // manually test loader fn
    val effective = EffectiveGuestVFSConfig.DEFAULTS
    val fsConfig = effective.buildFs()

    val result = EmbeddedGuestVFSImpl.loadBundleURIs(listOf(sampleTarball.toURI()), fsConfig)
    assertNotNull(result, "should not get `null` from `loadBundleFromURI` for known-good input")
    val (tree, databag) = result
    assertNotNull(tree, "should not get `null` from `loadBundleFromURI` for known-good input")
    assertNotNull(databag, "should not get `null` from `loadBundleFromURI` for known-good input")

    // test consistency of the tree
    assertTrue(tree.hasRoot(), "tree should always have root entry")
    assertTrue(tree.root.hasDirectory(), "root should always be a directory")
    val entries = tree.root.directory.childrenList
    assertEquals(1, entries.size, "root directory should have exactly 1 child")
    val testFile = entries.first().file
    assertEquals("hello.txt", testFile.name, "test file name should be preserved")
    assertEquals(6, testFile.size, "un-compressed size should be accurate")
    assertEquals(0, testFile.offset, "offset for first file should be 0")

    // build it into a VFS instance
    val vfs = newBuilder().setBundle(result).build()
    assertNotNull(vfs, "should be able to create VFS from sample tarball")
    val path = databag.getPath("hello.txt")
    assertNotNull(path, "should be able to parse path 'hello.txt'")

    val exampleFileContents = vfs.readStream(path).bufferedReader(StandardCharsets.UTF_8).use {
      it.readText()
    }
    assertEquals("hello", exampleFileContents.trim(), "example file contents should decode correctly")
  }

  /** Test: Load a bundle from a compressed tarball which contains folders. */
  @Test fun testBundleFromTarballGzipDirs() {
    // load sample tarball
    val sampleTarball = EmbeddedVFSTest::class.java.getResource("/sample-vfs-dirs.tar.gz")
    assertNotNull(sampleTarball, "should be able to find sample compressed tarball")

    // manually test loader fn
    val effective = EffectiveGuestVFSConfig.DEFAULTS
    val fsConfig = effective.buildFs()

    val result = EmbeddedGuestVFSImpl.loadBundleURIs(listOf(sampleTarball.toURI()), fsConfig)
    assertNotNull(result, "should not get `null` from `loadBundleFromURI` for known-good input")
    val (tree, databag) = result
    assertNotNull(tree, "should not get `null` from `loadBundleFromURI` for known-good input")
    assertNotNull(databag, "should not get `null` from `loadBundleFromURI` for known-good input")

    // test consistency of the tree
    assertTrue(tree.hasRoot(), "tree should always have root entry")
    assertTrue(tree.root.hasDirectory(), "root should always be a directory")
    val entries = tree.root.directory.childrenList
    assertEquals(3, entries.size, "root directory should have exactly 3 children")

    // test file should be present
    val testFile = entries.find { it.hasFile() && it.file.name == "hello.txt" }?.file
    assertNotNull(testFile, "should be able to locate test file ('hello.txt')")
    assertEquals("hello.txt", testFile.name, "test file name should be preserved")
    assertEquals(6, testFile.size, "un-compressed size should be accurate")
    assertEquals(0, testFile.offset, "offset for first file should be 0")

    // test folder 1 should be present
    val testFolder1 = entries.find { it.hasDirectory() && it.directory.name == "folder" }?.directory
    assertNotNull(testFolder1, "should be able to locate test dir 1 ('folder')")
    assertEquals("folder", testFolder1.name, "test folder name should be preserved")

    // test folder 2 should be present
    val testFolder2 = entries.find { it.hasDirectory() && it.directory.name == "another" }?.directory
    assertNotNull(testFolder2, "should be able to locate test dir 2 ('another')")
    assertEquals("another", testFolder2.name, "test folder name should be preserved")

    // test folder 3 should be present
    assertEquals(1, testFolder2.childrenCount, "should have 1 child under `another` dir")
    val testFolder3 = testFolder2.childrenList.find {
      it.hasDirectory() && it.directory.name == "nested"
    }?.directory
    assertNotNull(testFolder3, "should be able to locate test dir 3 ('another/nested')")
    assertEquals("nested", testFolder3.name, "test folder name should be preserved")

    // build it into a VFS instance
    val vfs = newBuilder().setBundle(result).build()
    val path = databag.getPath("hello.txt")
    assertNotNull(path, "should be able to parse path 'hello.txt'")
    assertNotNull(vfs, "should be able to create VFS from sample tarball")

    val exampleFileContents = vfs.readStream(path).bufferedReader(StandardCharsets.UTF_8).use {
      it.readText()
    }
    assertEquals("hello", exampleFileContents.trim(), "example file contents should decode correctly")

    // read the JSON example path
    val path2 = databag.getPath("folder", "something.json")
    assertNotNull(path2, "should be able to parse path 'folder/something.json'")
    val exampleFileContents2 = vfs.readStream(path2).bufferedReader(StandardCharsets.UTF_8).use {
      it.readText()
    }
    assertEquals(
      "{\"hi\": \"hello\"}",
      exampleFileContents2.trim(),
      "example file contents should decode correctly"
    )

    // read the 2nd txt example path
    val path3 = databag.getPath("another", "nested", "cool.txt")
    assertNotNull(path3, "should be able to parse path 'another/nested/cool.txt'")
    val exampleFileContents3 = vfs.readStream(path3).bufferedReader(StandardCharsets.UTF_8).use {
      it.readText()
    }
    assertEquals(
      "hello again",
      exampleFileContents3.trim(),
      "example file contents should decode correctly"
    )
  }

  /** Test: write a file to an embedded VFS, and make sure it was written. */
  @Test fun testWriteFileToVFS() {
    // create writable embedded FS
    val target = factory().create(EffectiveGuestVFSConfig.DEFAULTS.copy(readOnly = false))

    // write to a file
    assertDoesNotThrow {
      target.writeStream(target.getPath("sample.txt")).use {
        it.write("hello".toByteArray(StandardCharsets.UTF_8))
      }
    }

    // read from the file
    val result = target.readStream(target.getPath("sample.txt")).bufferedReader(StandardCharsets.UTF_8).use {
      it.readText()
    }
    assertEquals("hello", result, "should be able to read back the file contents")
  }

  /** Test: Load a bundle expressed in Elide's internal format. */
  @Test @Disabled fun testBundleFromElideFormat() {

  }
}

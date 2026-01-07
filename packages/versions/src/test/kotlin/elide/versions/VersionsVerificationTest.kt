package elide.versions

import kotlinx.coroutines.test.runTest
import kotlin.io.path.deleteIfExists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import elide.annotations.Inject
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

/** @author Lauri Heino <datafox> */
@TestCase
class VersionsVerificationTest : AbstractVersionsTest() {
  @Inject private lateinit var manager: VersionManager

  val verificationFiles =
    listOf("verification/file1", "verification/file2", "verification/dir/file3", "verification/stampfile")

  @Test
  fun `test verifying stampfile`() = withTemp { path ->
    copyFiles(path, "verification", verificationFiles)
    runTest {
      assertEquals(emptyList(), manager.verifyInstall(path.toString()))
    }
  }

  @Test
  fun `test verifying stampfile with missing file`() = withTemp { path ->
    copyFiles(path, "verification", verificationFiles - "verification/file2")
    runTest {
      assertEquals(listOf("file2 does not exist"), manager.verifyInstall(path.toString()))
    }
  }

  @Test
  fun `test verifying stampfile with invalid file`() = withTemp { path ->
    copyFiles(path, "verification", verificationFiles)
    path.resolve("file2").writeText("modified")
    runTest {
      assertEquals(listOf("file2 is invalid"), manager.verifyInstall(path.toString()))
    }
  }

  @Test
  fun `test generating stampfile`() = withTemp { path ->
    copyFiles(path, "verification", verificationFiles)
    val stampfile = path.resolve("stampfile")
    val expected = stampfile.readText().trim()
    stampfile.deleteIfExists()
    runTest {
      assertEquals(expected, manager.generateStampFile(path.toString()))
    }
  }
}

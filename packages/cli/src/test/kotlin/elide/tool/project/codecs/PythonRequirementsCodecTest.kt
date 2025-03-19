package elide.tool.project.codecs

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.InputStream
import jakarta.inject.Inject
import kotlin.io.path.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import elide.tool.project.manifest.ElidePackageManifest
import elide.tool.project.manifest.PythonRequirementsManifest

@MicronautTest class PythonRequirementsCodecTest {
  @Inject lateinit var codec: PythonRequirementsManifestCodec

  @Test fun `should use standard default path`() {
    assertEquals(
      expected = Path("requirements.txt"),
      actual = codec.defaultPath(),
    )
  }

  @Test fun `should accept valid manifest paths`() {
    val validCases = mapOf(
      "requirements.txt" to "plain package path",
      "./my/project/requirements.txt" to "relative package path",
      "/home/me/projects/elide/requirements.txt" to "absolute package path",
    )

    validCases.forEach { value, reason ->
      assertTrue(codec.supported(Path(value)), "expected $reason '$value' to be supported")
    }
  }

  @Test fun `should reject invalid manifest paths`() {
    val invalidCases = mapOf(
      "requirements.py" to "wrong extension",
      "requirements.txtfile" to "wrong extension",
      "hello.txt" to "wrong name",
    )

    invalidCases.forEach { value, reason ->
      assertFalse(codec.supported(Path(value)), "expected $reason '$value' to be rejected")
    }
  }

  @Test fun `should read manifest file`() {
    val resource = sampleManifestResource()
    assertEquals(
      expected = SampleManifest,
      actual = codec.parse(resource),
    )
  }

  @Test fun `should write manifest file`() {
    val reference = sampleManifestResource().bufferedReader().use { it.readText() }
    val output = ByteArrayOutputStream()

    codec.write(SampleManifest, output)

    assertEquals(
      expected = reference.trim(),
      actual = output.toByteArray().decodeToString().trim(),
    )
  }

  @Test fun fromElidePackage() {
    assertEquals(
      expected = SampleManifest,
      actual = codec.fromElidePackage(SampleElideManifest),
    )
  }

  @Test fun toElidePackage() {
    assertEquals(
      expected = SampleElideManifest,
      actual = codec.toElidePackage(SampleManifest),
    )
  }

  companion object {
    fun sampleManifestResource(): InputStream {
      return PythonRequirementsCodecTest::class.java.getResourceAsStream("/manifests/requirements.txt")!!
    }

    val SampleManifest = PythonRequirementsManifest(
      listOf(
        "pytest",
        "pytest-cov",
        "beautifulsoup4",
        "docopt == 0.6.1",
        """requests [security] >= 2.8.1, == 2.8.* ; python_version < "2.7"""",
        "urllib3 @ https://github.com/urllib3/urllib3/archive/refs/tags/1.26.8.zip",
        "-r other-requirements.txt",
        "-c constraints.txt",
        "./downloads/numpy-1.9.2-cp34-none-win32.whl",
        "http://wxpython.org/Phoenix/snapshot-builds/wxPython_Phoenix-3.0.3.dev1820+49a8884-cp34-none-win_amd64.whl",
      ),
    )

    val SampleElideManifest = ElidePackageManifest(
      dependencies = ElidePackageManifest.DependencyResolution(
        pip = ElidePackageManifest.PipDependencies(
          packages = listOf(
            "pytest",
            "pytest-cov",
            "beautifulsoup4",
            "docopt == 0.6.1",
            """requests [security] >= 2.8.1, == 2.8.* ; python_version < "2.7"""",
            "urllib3 @ https://github.com/urllib3/urllib3/archive/refs/tags/1.26.8.zip",
            "-r other-requirements.txt",
            "-c constraints.txt",
            "./downloads/numpy-1.9.2-cp34-none-win32.whl",
            "http://wxpython.org/Phoenix/snapshot-builds/wxPython_Phoenix-3.0.3.dev1820+49a8884-cp34-none-win_amd64.whl",
          ).map { ElidePackageManifest.PipPackage(it) },
        ),
      ),
    )
  }
}

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
import elide.tool.project.manifest.NodePackageManifest

@MicronautTest class NodeManifestCodecTest {
  @Inject lateinit var codec: NodeManifestCodec

  @Test fun `should use standard default path`() {
    assertEquals(
      expected = Path("package.json"),
      actual = codec.defaultPath(),
    )
  }

  @Test fun `should accept valid manifest paths`() {
    val validCases = mapOf(
      "package.json" to "plain package path",
      "./my/project/package.json" to "relative package path",
      "/home/me/projects/elide/package.json" to "absolute package path",
    )

    validCases.forEach { value, reason ->
      assertTrue(codec.supported(Path(value)), "expected $reason '$value' to be supported")
    }
  }

  @Test fun `should reject invalid manifest paths`() {
    val invalidCases = mapOf(
      "package.js" to "wrong extension",
      "package.jsonfile" to "wrong extension",
      "hello.json" to "wrong name",
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
      return NodeManifestCodecTest::class.java.getResourceAsStream("/manifests/package.json")!!
    }

    val SampleManifest = NodePackageManifest(
      name = "elide-js-sample",
      version = "1.0.0",
      description = "A sample Elide app",
      main = "index.js",
      scripts = mapOf("start" to "elide index.js"),
      dependencies = mapOf("foo" to "^1.2.3"),
      devDependencies = mapOf("bar" to "^0.1.1"),
    )

    val SampleElideManifest = ElidePackageManifest(
      name = "elide-js-sample",
      version = "1.0.0",
      description = "A sample Elide app",
      entrypoint = "index.js",
      scripts = mapOf("start" to "elide index.js"),
      dependencies = ElidePackageManifest.DependencyResolution(
        npm = ElidePackageManifest.NpmDependencies(
          packages = listOf(
            ElidePackageManifest.NpmPackage("foo", "^1.2.3"),
          ),
          devPackages = listOf(
            ElidePackageManifest.NpmPackage("bar", "^0.1.1"),
          ),
        ),
      ),
    )
  }
}

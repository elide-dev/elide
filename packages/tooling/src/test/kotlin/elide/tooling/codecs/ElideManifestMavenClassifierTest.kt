package elide.tooling.codecs

import elide.tooling.project.codecs.ElidePackageManifestCodec
import elide.tooling.project.manifest.ElidePackageManifest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class ElideManifestMavenClassifierTest {
  private fun parse(text: String): ElidePackageManifest {
    val codec = ElidePackageManifestCodec()
    val bytes = text.toByteArray(StandardCharsets.UTF_8)
    return codec.parse(ByteArrayInputStream(bytes))
  }

  @Test fun `should parse maven package with classifier in Pkl`() {
    val pkl = """
      amends "elide:project.pkl"

      name = "classifier-test"

      dependencies {
        maven {
          packages = new {
            new {
              group = "com.example"
              name = "lib"
              version = "1.2.3"
              classifier = "native"
            }
          }
        }
      }
    """.trimIndent()

    val manifest = parse(pkl)
    val dep = manifest.dependencies.maven.packages.first()
    assertEquals("com.example", dep.group)
    assertEquals("lib", dep.name)
    assertEquals("1.2.3", dep.version)
    assertEquals("native", dep.classifier)
    // tiny regression: ensure coordinate is synthesized when omitted
    assertEquals("com.example:lib:native:1.2.3", dep.coordinate)
  }
}


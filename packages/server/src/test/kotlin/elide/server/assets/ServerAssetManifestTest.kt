package elide.server.assets

import com.google.common.truth.extensions.proto.ProtoTruth.assertThat
import elide.server.TestUtil
import org.junit.jupiter.api.assertDoesNotThrow
import tools.elide.assets.AssetBundle
import tools.elide.assets.ManifestFormat
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import kotlin.test.*

/** Tests for [ServerAssetManifest] and [ServerAssetManifestProvider]. */
class ServerAssetManifestTest {
  @Test fun testParseSampleManifestBinary() {
    val data = TestUtil.loadBinary("/manifests/app.assets.pb")
    val baos = ByteArrayInputStream(data)
    val provider = ServerAssetManifestProvider()
    val manifest = assertDoesNotThrow {
      provider.deserializeLoadManifest(
        ManifestFormat.BINARY to baos,
      )
    }
    assertNotNull(
      manifest,
      "loaded asset manifest should not be `null`"
    )
    assertTrue(
      manifest.isInitialized,
      "manifest proto should be initialized after loading"
    )
    assertTrue(
      manifest.hasSettings(),
      "manifest should have settings data present"
    )
    assertTrue(
      manifest.hasGenerated(),
      "manifest should have a generated-at timestamp"
    )
  }

  @Test fun testParseStabilityBinary() {
    val data = TestUtil.loadBinary("/manifests/app.assets.pb")
    val provider = ServerAssetManifestProvider()
    val manifest = assertDoesNotThrow {
      provider.deserializeLoadManifest(
        ManifestFormat.BINARY to ByteArrayInputStream(data),
      )
    }
    val reparsed = assertDoesNotThrow {
      provider.deserializeLoadManifest(
        ManifestFormat.BINARY to ByteArrayInputStream(data),
      )
    }
    assertThat(manifest).isEqualTo(
      reparsed
    )
  }

  @Test fun testParseStabilityJson() {
    val data = TestUtil.loadBinary("/manifests/app.assets.pb.json")
    val provider = ServerAssetManifestProvider()
    val manifest = assertDoesNotThrow {
      provider.deserializeLoadManifest(
        ManifestFormat.JSON to ByteArrayInputStream(data),
      )
    }
    val reparsed = assertDoesNotThrow {
      provider.deserializeLoadManifest(
        ManifestFormat.JSON to ByteArrayInputStream(data),
      )
    }
    assertThat(manifest).isEqualTo(
      reparsed
    )
  }

  @Test fun testManifestEqualsAcrossFormats() {
    val dataBinary = TestUtil.loadBinary("/manifests/app.assets.pb")
    val dataJson = TestUtil.loadBinary("/manifests/app.assets.pb.json")
    val provider = ServerAssetManifestProvider()
    val manifest1 = assertDoesNotThrow {
      provider.deserializeLoadManifest(
        ManifestFormat.BINARY to ByteArrayInputStream(dataBinary),
      )
    }
    val manifest2 = assertDoesNotThrow {
      provider.deserializeLoadManifest(
        ManifestFormat.JSON to ByteArrayInputStream(dataJson),
      )
    }
    assertThat(manifest1)
      .ignoringFields(AssetBundle.getDescriptor().findFieldByName("styles").number)
      .ignoringFields(AssetBundle.getDescriptor().findFieldByName("scripts").number)
      .ignoringFields(AssetBundle.getDescriptor().findFieldByName("generated").number)
      .isEqualTo(manifest2)
  }

  @Test fun testParseSampleManifestJson() {
    val data = TestUtil.loadBinary("/manifests/app.assets.pb.json")
    val baos = ByteArrayInputStream(data)
    val provider = ServerAssetManifestProvider()
    val manifest = assertDoesNotThrow {
      provider.deserializeLoadManifest(
        ManifestFormat.JSON to baos,
      )
    }
    assertNotNull(
      manifest,
      "loaded asset manifest should not be `null`"
    )
    assertTrue(
      manifest.isInitialized,
      "manifest proto should be initialized after loading"
    )
    assertTrue(
      manifest.hasSettings(),
      "manifest should have settings data present"
    )
    assertTrue(
      manifest.hasGenerated(),
      "manifest should have a generated-at timestamp"
    )
  }

  @Test fun testParseInvalidFormat() {
    assertNull(
      assertDoesNotThrow {
        ServerAssetManifestProvider().deserializeLoadManifest(
          ManifestFormat.TEXT to ByteArrayInputStream(ByteArray(0)),
        )
      }
    )
  }

  @Test fun testParseInvalidData() {
    val data = "i am not a valid manifest".toByteArray(StandardCharsets.UTF_8)
    val baos = ByteArrayInputStream(data)
    val provider = ServerAssetManifestProvider()
    assertNull(
      assertDoesNotThrow {
        provider.deserializeLoadManifest(
          ManifestFormat.BINARY to baos,
        )
      }
    )
    assertNull(
      assertDoesNotThrow {
        provider.deserializeLoadManifest(
          ManifestFormat.JSON to baos,
        )
      }
    )
  }

  @Test fun testFindManifestFound() {
    val path = "/manifests/app.assets.pb"
    val provider = ServerAssetManifestProvider()
    val manifest = assertDoesNotThrow {
      provider.findManifest(
        listOf(
          ManifestFormat.BINARY to "/manifest/some-bad-path.pb",
          ManifestFormat.JSON to "/manifest/some-bad-path.pb.json",
          ManifestFormat.BINARY to path,
          ManifestFormat.TEXT to "/manifest/some-bad-path.pb.txt"
        )
      )
    }
    assertNotNull(
      manifest,
      "should be able to find present manifest"
    )
  }

  @Test fun testFindManifestNotFound() {
    val path = "/manifests/some.non.existent.manifest.file.pb"
    val provider = ServerAssetManifestProvider()
    val manifest = assertDoesNotThrow {
      provider.findManifest(
        listOf(
          ManifestFormat.BINARY to "/manifest/some-bad-path.pb",
          ManifestFormat.JSON to "/manifest/some-bad-path.pb.json",
          ManifestFormat.BINARY to path,
          ManifestFormat.TEXT to "/manifest/some-bad-path.pb.txt"
        )
      )
    }
    assertNull(
      manifest,
      "should not be able to find missing manifest"
    )
  }

  @Test fun testFindManifestDefault() {
    assertDoesNotThrow {
      ServerAssetManifestProvider().findManifest()
    }
  }

  @Test fun testFindLoadManifestFound() {
    val path = "/manifests/app.assets.pb"
    val provider = ServerAssetManifestProvider()
    val manifest = assertDoesNotThrow {
      provider.findLoadManifest(
        listOf(ManifestFormat.BINARY to path),
      )
    }
    assertNotNull(
      manifest,
      "should be able to find present manifest"
    )
    val manifest2 = assertDoesNotThrow {
      provider.findLoadManifest(
        listOf(ManifestFormat.BINARY to path),
      )
    }
    assertThat(manifest).isEqualTo(
      manifest2
    )
  }

  @Test fun testFindLoadManifestNotFound() {
    val path = "/manifests/i.do.not.exist.assets.pb"
    val provider = ServerAssetManifestProvider()
    val manifest = assertDoesNotThrow {
      provider.findLoadManifest(
        listOf(ManifestFormat.BINARY to path),
      )
    }
    assertNull(
      manifest,
      "not finding a manifest should result in `null`"
    )
  }

  @Test fun testFindLoadManifestDefault() {
    assertDoesNotThrow {
      ServerAssetManifestProvider().findLoadManifest()
    }
  }
}

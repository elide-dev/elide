package elide.server.assets

import com.google.common.annotations.VisibleForTesting
import com.google.protobuf.util.JsonFormat
import io.micronaut.context.annotation.Context
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tools.elide.assets.AssetBundle
import tools.elide.assets.ManifestFormat
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.*

/** Loads an asset manifest from the application JAR or native image. */
@Context internal class ServerAssetManifestProvider : AssetManifestLoader {
  private val logging: Logger = LoggerFactory.getLogger(ServerAssetManifestProvider::class.java)

  @VisibleForTesting
  @Suppress("TooGenericExceptionCaught")
  internal fun deserializeLoadManifest(subject: Pair<ManifestFormat, InputStream>): AssetBundle? {
    val (format, stream) = subject
    logging.debug(
      "Decoding manifest from detected format '${format.name}'"
    )
    val result = try {
      when (format) {
        ManifestFormat.BINARY -> stream.buffered().use {
          AssetBundle.parseFrom(it)
        }

        ManifestFormat.JSON -> stream.bufferedReader(StandardCharsets.UTF_8).use { buf ->
          val builder = AssetBundle.newBuilder()
          JsonFormat.parser().ignoringUnknownFields().merge(
            buf,
            builder,
          )
          builder.build()
        }

        else -> {
          logging.warn(
            "Cannot de-serialize asset manifest with format: '${format.name}'. Asset loading disabled."
          )
          null
        }
      }
    } catch (thr: Throwable) {
      logging.error("Failed to load asset manifest", thr)
      null
    }
    return if (result == null) {
      null
    } else {
      val algo = result.settings.digestSettings.algorithm
      val encoded = Base64.getEncoder().withoutPadding()
        .encodeToString(result.digest.toByteArray())
      logging.debug(
        "Resolved asset manifest with fingerprint ${algo.name}($encoded)"
      )
      result
    }
  }

  /** @inheritDoc */
  override fun findLoadManifest(candidates: List<Pair<ManifestFormat, String>>): AssetBundle? {
    val found = findManifest(candidates)
    logging.debug(
      if (found != null) {
        "Located asset manifest: loading"
      } else {
        "No asset manifest located. Asset loading will be disabled."
      }
    )
    return if (found == null) {
      // we couldn't locate a manifest.
      null
    } else deserializeLoadManifest(
      found
    )
  }

  /** @inheritDoc */
  override fun findManifest(candidates: List<Pair<ManifestFormat, String>>): Pair<ManifestFormat, InputStream>? {
    // find the first manifest that exists
    return candidates.firstNotNullOfOrNull {
      val (format, path) = it
      logging.trace(
        "Checking for manifest at resource location '$path'"
      )
      val result = ServerAssetManager::class.java.getResourceAsStream(path)
      logging.trace(
        if (result != null) {
          "Found manifest at resource location '$path'"
        } else {
          "No manifest found at resource location '$path'"
        }
      )
      if (result == null) {
        null
      } else {
        format to result
      }
    }
  }
}

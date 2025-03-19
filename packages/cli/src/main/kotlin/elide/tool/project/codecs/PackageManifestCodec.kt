package elide.tool.project.codecs

import elide.tool.project.manifest.ElidePackageManifest
import elide.tool.project.manifest.PackageManifest
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path

interface PackageManifestCodec<T : PackageManifest> {
  fun defaultPath(): Path

  fun supported(path: Path): Boolean

  fun parse(source: InputStream): T

  fun write(manifest: T, output: OutputStream)

  fun fromElidePackage(source: ElidePackageManifest): T

  fun toElidePackage(source: T): ElidePackageManifest
}

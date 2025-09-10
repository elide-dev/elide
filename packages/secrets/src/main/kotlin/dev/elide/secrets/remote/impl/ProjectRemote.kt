package dev.elide.secrets.remote.impl

import dev.elide.secrets.Utils
import dev.elide.secrets.Utils.exists
import dev.elide.secrets.Utils.read
import dev.elide.secrets.Utils.write
import dev.elide.secrets.Values
import dev.elide.secrets.remote.Remote
import kotlinx.io.bytestring.ByteString
import kotlinx.io.files.Path

/** @author Lauri Heino <datafox> */
internal class ProjectRemote(private val path: Path) : Remote {
  override val writeAccess: Boolean = true

  override suspend fun getMetadata(): ByteString? = getFile(Values.METADATA_FILE)

  override suspend fun getProfile(profile: String): ByteString? = getFile(Utils.profileName(profile))

  override suspend fun getAccess(access: String): ByteString? = getFile(Utils.accessName(access))

  override suspend fun getSuperAccess(): ByteString? = getFile(Values.SUPER_ACCESS_FILE)

  override suspend fun update(
    metadata: ByteString,
    profiles: Map<String, ByteString>
  ) {
    writeFile(Values.METADATA_FILE, metadata)
    profiles.forEach { writeFile(it.key, it.value) }
  }

  override suspend fun superUpdate(
    metadata: ByteString,
    profiles: Map<String, ByteString>,
    superAccess: ByteString,
    access: Map<String, ByteString>
  ) {
    writeFile(Values.METADATA_FILE, metadata)
    profiles.forEach { writeFile(Utils.profileName(it.key), it.value) }
    writeFile(Values.SUPER_ACCESS_FILE, superAccess)
    access.forEach { writeFile(Utils.accessName(it.key), it.value) }
  }

  private fun getFile(path: String): ByteString? =
    Path(this.path, path).run { if (exists()) read() else null }

  private fun writeFile(path: String, data: ByteString) {
    data.write(Path(this.path, path))
  }
}

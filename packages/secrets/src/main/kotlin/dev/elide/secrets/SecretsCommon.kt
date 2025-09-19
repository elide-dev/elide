package dev.elide.secrets

import java.nio.file.Path
import kotlinx.io.bytestring.ByteString
import kotlin.reflect.KClass
import elide.tooling.project.manifest.ElidePackageManifest

/**
 * Common access to secrets.
 *
 * @author Lauri Heino <datafox>
 */
public sealed interface SecretsCommon {
  public val initialized: Boolean

  public suspend fun init(path: Path, manifest: ElidePackageManifest?)

  public fun listProfiles(): Set<String>

  public fun loadProfile(profile: String)

  public fun getProfile(): String?

  public fun unloadProfile()

  public fun getEnv(): Map<String, String>

  public fun getSecret(name: String): Any?

  public fun getStringSecret(name: String): String?

  public fun getBinarySecret(name: String): ByteString?

  public fun listSecrets(): Map<String, KClass<*>>
}

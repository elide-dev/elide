package dev.elide.secrets

import kotlinx.io.bytestring.ByteString
import kotlinx.io.files.Path
import kotlin.reflect.KClass

/**
 * Non-interactive read-only access to secrets.
 *
 * @author Lauri Heino <datafox>
 */
public interface Secrets {
  public suspend fun init(path: Path)

  public fun listProfiles(): Set<String>

  public fun loadProfile(profile: String)

  public fun unloadProfile()

  public fun getEnv(): Map<String, String>

  public fun getStringSecret(name: String): String?

  public fun getBinarySecret(name: String): ByteString?

  public fun listSecrets(): Map<String, KClass<*>>
}

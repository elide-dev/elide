package dev.elide.secrets

import kotlinx.io.bytestring.ByteString

/**
 * Interactive read/write access to secrets.
 *
 * @author Lauri Heino <datafox>
 */
public interface SecretManagement : Secrets {
  public fun createProfile(profile: String)

  public fun removeProfile(profile: String)

  public fun setStringSecret(name: String, value: String, envVar: String? = null)

  public fun setBinarySecret(name: String, value: ByteString)

  public fun removeSecret(name: String)

  public fun writeChanges()

  public suspend fun pullFromRemote()

  public suspend fun pushToRemote()

  public suspend fun manageRemote(): RemoteManagement

  public fun setBinarySecret(name: String, value: ByteArray): Unit = setBinarySecret(name, ByteString(value))

}

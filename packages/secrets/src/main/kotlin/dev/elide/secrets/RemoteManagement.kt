package dev.elide.secrets


/**
 * Superuser access to remote secrets.
 *
 * @author Lauri Heino <datafox>
 */
public interface RemoteManagement {
  public suspend fun init()

  public fun listAccesses(): Set<String>

  public fun createAccess(name: String)

  public fun removeAccess(name: String)

  public fun selectAccess(name: String)

  public fun addProfile(profile: String)

  public fun removeProfile(profile: String)

  public fun listProfiles(): Set<String>

  public fun deselectAccess()

  public suspend fun push()
}

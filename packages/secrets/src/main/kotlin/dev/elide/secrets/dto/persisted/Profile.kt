package dev.elide.secrets.dto.persisted

/** @author Lauri Heino <datafox> */
internal interface Profile : Named {
  val secrets: Map<String, Secret<*>>

  fun getEnv(): Map<String, String> {
    return secrets.values.filterIsInstance<StringSecret>().filter { it.env != null }.associate { it.env!! to it.value }
  }

  companion object {
    inline operator fun <reified T> Profile.get(name: String): T? =
      if (T::class == Secret::class) secrets[name] as? T else secrets[name]?.value as? T
  }
}

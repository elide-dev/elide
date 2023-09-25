package elide.tool.extensions

import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.IntrinsicsResolver
import elide.runtime.plugins.AbstractLanguageConfig
import elide.runtime.gvm.GuestLanguage as LegacyGuestLanguage

/**
 * Install globally resolved intrinsics into this language plugin. The [intrinsics] resolver is used to load all
 * intrinsics for the given [language] (note that this language is provided by the 'graalvm' module).
 */
internal fun AbstractLanguageConfig.installIntrinsics(
  intrinsics: IntrinsicsResolver,
  language: LegacyGuestLanguage,
  version: Pair<String, String>,
) = bindings {
  // create an intermediate container to obtain the bindings
  val container = MutableIntrinsicBindings.Factory.create()
  intrinsics.resolve(language).forEach { it.install(container) }

  // transfer the bindings to the language plugin configuration
  container.entries.forEach { (key, value) -> put(key.symbol, value) }
  installCliBaseline(version)
}

/**
 * Install baseline properties provided by the runtime when operating as a CLI.
 */
internal fun AbstractLanguageConfig.installCliBaseline(
  version: Pair<String, String>,
) = bindings {
  // add runtime version
  put(version.first, version.second)
}

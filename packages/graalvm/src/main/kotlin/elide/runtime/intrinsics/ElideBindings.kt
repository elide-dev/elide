package elide.runtime.intrinsics

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.GuestLanguage
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotValue
import elide.runtime.gvm.internals.intrinsics.js.JsProxy

/** Helper functions for defining and installing language bindings. */
@DelicateElideApi public object ElideBindings {
  /** Delimiter for structured intrinsic paths, see [install] for more information. */
  private const val PATH_DELIMITER = "."

  /**
   * Installs a [value] as a binding for the target [language] in a given [context].
   *
   * The specified [path] may contain one or more segments separated by `.`, which will cause intermediate proxy
   * objects to be created, allowing the creation of more structured intrinsics.
   *
   * @param path The path specifying the binding point for the value.
   * @param value The value being used as binding.
   * @param context The context to which the binding will be installed.
   * @param language The target language to which the binding will be made available.
   */
  public fun install(path: String, value: Any, context: PolyglotContext, language: GuestLanguage) {
    // starting with the root bindings object (and until the final value which acts as key), resolve or create
    // objects according to the path specification until the final key segment is reached
    val container = path.split(PATH_DELIMITER).dropLast(1).fold(context.bindings(language)) { container, key ->
      if (container.hasMember(key)) {
        // found a nested container, use it
        container.getMember(key)
      } else {
        // no nested object found, create one
        val nested = PolyglotValue.asValue(JsProxy.build { /* empty */ })
        container.putMember(key, nested)

        nested
      }
    }

    // now we have the final container to which we need to add the value
    container.putMember(path.substringAfterLast(PATH_DELIMITER), value)
  }
}
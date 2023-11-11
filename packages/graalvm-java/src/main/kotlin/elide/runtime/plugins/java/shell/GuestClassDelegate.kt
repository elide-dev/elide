package elide.runtime.plugins.java.shell

import kotlin.properties.ReadOnlyProperty
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotValue
import elide.runtime.plugins.jvm.Jvm

@DelicateElideApi internal fun PolyglotContext.guestClass(
  name: String
): ReadOnlyProperty<Any, PolyglotValue> {
  var cached: PolyglotValue? = null
  return ReadOnlyProperty { _, _ ->
    // early return if already resolved
    cached?.let { return@ReadOnlyProperty it }

    // resolve the guest class from the bindings and validate the returned value
    val guestValue = bindings(Jvm).getMember(name)?.takeUnless { it.isNull } ?: error(
      "Failed to resolve guest class <$name>",
    )

    // cache the resolved value
    guestValue.also { cached = it }
  }
}

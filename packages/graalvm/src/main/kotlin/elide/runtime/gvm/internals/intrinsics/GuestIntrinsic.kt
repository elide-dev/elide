package elide.runtime.gvm.internals.intrinsics

import elide.runtime.gvm.GuestLanguage
import java.util.TreeMap
import java.util.TreeSet
import java.util.function.BiFunction
import java.util.function.Function

/**
 * # Guest Intrinsic
 *
 * Applied to all intrinsic classes which are implemented for a guest language, in addition to various annotations which
 * designate the use context of a given implementation.
 */
internal interface GuestIntrinsic {
  /**
   * TBD.
   */
  interface IntrinsicBindings : Map<String, Any>

  /**
   * TBD.
   */
  interface MutableIntrinsicBindings : IntrinsicBindings, MutableMap<String, Any> {
    /** factory for creating empty bindings. */
    object Factory {
      /** @return Mutable intrinsic bindings backed by a map. */
      @JvmStatic fun create(): MutableIntrinsicBindings = wrap(TreeMap<String, Any>())

      /** @return Mutable intrinsic bindings backed by a map. */
      @JvmStatic fun wrap(target: MutableMap<String, Any>): MutableIntrinsicBindings {
        val bindingSet = TreeSet<String>()
        return object: MutableIntrinsicBindings, MutableMap<String, Any> by target {
          // Check uniqueness of an intrinsic binding name.
          private fun checkName(key: String) {
            check(key !in bindingSet) {
              "Intrinsic binding '$key' is already bound."
            }
          }

          // Throw a consistent error for removals, which are not allowed.
          private fun notAllowed(): Nothing = error(
            "Operation not allowed on intrinsic binding proxy."
          )

          /** Removing intrinsics is not allowed; this method always throws. */
          override fun remove(key: String): Any = notAllowed()

          /** Clearing intrinsics is not allowed; this method always throws. */
          override fun clear() = notAllowed()

          override fun put(key: String, value: Any): Any? {
            checkName(key)
            return target.put(key ,value)
          }

          override fun putAll(from: Map<out String, Any>) {
            from.keys.forEach {
              checkName(it)
              target[it] = from[it]!!
            }
          }

          /** @inheritDoc */
          override fun compute(key: String, remappingFunction: BiFunction<in String, in Any?, out Any?>): Any
            = notAllowed()

          /** @inheritDoc */
          override fun computeIfAbsent(key: String, mappingFunction: Function<in String, out Any>): Any
            = notAllowed()

          /** @inheritDoc */
          override fun computeIfPresent(key: String, remappingFunction: BiFunction<in String, in Any, out Any?>): Any
            = notAllowed()
        }
      }
    }
  }

  /**
   * Indicate the language which this intrinsic is intended to be used with.
   *
   * @return Guest language bound to this intrinsic.
   */
  fun language(): GuestLanguage

  /**
   * Indicate whether this intrinsic is intended to be used with a given guest [language].
   *
   * @param language Language to check.
   * @return `true` if this intrinsic is intended to be used with the given language, `false` otherwise.
   */
  fun supports(language: GuestLanguage): Boolean {
    return language().symbol == language.symbol
  }

  /**
   * Install this intrinsic into the provided context [bindings] for a fresh context; this will only be called once per
   * spawned context.
   *
   * The default (abstract) implementation of this method should scan for intrinsics on the current object and mount
   * each at their specified global, as applicable.
   *
   * @param bindings Language bindings target where the intrinsic should be installed.
   */
  fun install(bindings: MutableIntrinsicBindings)

  /**
   * TBD.
   */
  fun displayName(): String
}

package elide.runtime.gvm.internals.intrinsics

import elide.runtime.gvm.GuestLanguage
import elide.runtime.gvm.internals.intrinsics.js.JsSymbol
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyObject
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
  interface IntrinsicBindings : Map<JsSymbol, Any>

  /**
   * TBD.
   */
  interface MutableIntrinsicBindings : IntrinsicBindings, MutableMap<JsSymbol, Any>, ProxyObject {
    /** factory for creating empty bindings. */
    object Factory {
      /** @return Mutable intrinsic bindings backed by a map. */
      @JvmStatic fun create(): MutableIntrinsicBindings = wrap(TreeMap<JsSymbol, Any>())

      /** @return Mutable intrinsic bindings backed by a map. */
      @JvmStatic fun wrap(target: MutableMap<JsSymbol, Any>): MutableIntrinsicBindings {
        val bindingSet = TreeSet<String>()
        return object: MutableIntrinsicBindings, MutableMap<JsSymbol, Any> by target {
          // Check uniqueness of an intrinsic binding name.
          private fun checkName(key: JsSymbol) {
            check(key.symbol !in bindingSet) {
              "Intrinsic binding '$key' is already bound."
            }
          }

          // Throw a consistent error for removals, which are not allowed.
          private fun notAllowed(): Nothing = error(
            "Operation not allowed on intrinsic binding proxy."
          )

          /** Removing intrinsics is not allowed; this method always throws. */
          override fun remove(key: JsSymbol): Any = notAllowed()

          /** Clearing intrinsics is not allowed; this method always throws. */
          override fun clear() = notAllowed()

          /** @inheritDoc */
          override fun put(key: JsSymbol, value: Any): Any? {
            checkName(key)
            bindingSet.add(key.symbol)
            return target.put(key ,value)
          }

          /** @inheritDoc */
          override fun putAll(from: Map<out JsSymbol, Any>) {
            from.keys.forEach {
              checkName(it)
              bindingSet.add(it.symbol)
              target[it] = from[it]!!
            }
          }

          /** @inheritDoc */
          override fun compute(key: JsSymbol, remappingFunction: BiFunction<in JsSymbol, in Any?, out Any?>)
            = notAllowed()

          /** @inheritDoc */
          override fun computeIfAbsent(key: JsSymbol, mappingFunction: Function<in JsSymbol, out Any>)
            = notAllowed()

          /** @inheritDoc */
          override fun computeIfPresent(key: JsSymbol, remappingFunction: BiFunction<in JsSymbol, in Any, out Any?>)
            = notAllowed()

          /** @inheritDoc */
          override fun getMember(key: String): Any = target[JsSymbol(key)] ?:
            throw IllegalArgumentException("Intrinsic '$key' could not be resolved: not bound.")

          /** @inheritDoc */
          override fun getMemberKeys(): Any = bindingSet.toTypedArray()

          /** @inheritDoc */
          override fun hasMember(key: String): Boolean = bindingSet.contains(key)

          /** @inheritDoc */
          override fun putMember(key: String, value: Value?) = error(
            "Cannot assign to `Intrinsics` members at runtime"
          )
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

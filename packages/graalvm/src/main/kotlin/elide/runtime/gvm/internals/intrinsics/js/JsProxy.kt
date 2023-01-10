package elide.runtime.gvm.internals.intrinsics.js

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyObject

/** Base implementations for JS proxy objects. */
public object JsProxy {
  /** @return Wrapped [delegate]. */
  public fun wrap(delegate: Any): PropertyProxy = PropertyProxy(delegate)

  /** Property wrapping proxy for a given delegate. */
  public class PropertyProxy internal constructor (delegate: Any? = null) : ProxyObject {
    // Host-delegate object.
    private val hostDelegate: Value = Context.getCurrent().asValue(delegate)

    // Keys for object properties.
    private val keys: MutableSet<String> = LinkedHashSet()

    // Collected getters.
    private val getters: MutableMap<String, Value> = LinkedHashMap()

    // Collected setters.
    private val setters: MutableMap<String, Value> = LinkedHashMap()

    // Collected methods.
    private val methods: MutableMap<String, Value> = LinkedHashMap()

    init {
      for (key in hostDelegate.memberKeys) {
        val v = hostDelegate.getMember(key)
        if (!v.canExecute()) {
          // filter regular fields for now to avoid disambiguation
          continue
        }
        var property: String?
        if (key.startsWith("get")) {
          property = firstLetterLowerCase(key.substring(3, key.length))
          getters[property] = v
          keys.add(property)
        } else if (key.startsWith("is")) {
          property = firstLetterLowerCase(key.substring(2, key.length))
          getters[property] = v
          keys.add(property)
        } else if (key.startsWith("set")) {
          property = firstLetterLowerCase(key.substring(3, key.length))
          setters[property] = v
          keys.add(property)
        } else {
          methods[key] = v
          keys.add(key)
        }
      }
    }

    internal companion object {
      // Convert a getter/setter name to a property.
      @JvmStatic private fun firstLetterLowerCase(name: String): String {
        return name.ifEmpty { name[0].lowercaseChar().toString() + name.substring(1, name.length) }
      }
    }

    /** @inheritDoc */
    override fun getMember(key: String): Any {
      val getter = getters[key]
      return if (getter != null) {
        getter.execute()
      } else {
        val method = methods[key]
        if (method != null) {
          return method
        }
        throw UnsupportedOperationException()
      }
    }

    /** @inheritDoc */
    override fun getMemberKeys(): Any = keys

    /** @inheritDoc */
    override fun hasMember(key: String): Boolean = keys.contains(key)

    /** @inheritDoc */
    override fun putMember(key: String, value: Value) {
      val setter = setters[key]
      if (setter != null) {
        setter.execute(value)
      } else {
        throw java.lang.UnsupportedOperationException()
      }
    }

    /** @inheritDoc */
    override fun hashCode(): Int = hostDelegate.hashCode()

    /** @inheritDoc */
    override fun equals(other: Any?): Boolean {
      return if (other is PropertyProxy) {
        hostDelegate == other.hostDelegate
      } else false
    }

    /** @inheritDoc */
    override fun toString(): String = hostDelegate.toString()
  }
}

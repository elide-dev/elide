package elide.runtime.gvm.internals.intrinsics.js

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject

/** Base implementations for JS proxy objects. */
public object JsProxy {
  /** @return Wrapped [delegate]. */
  public fun wrap(delegate: Any): PropertyProxy = PropertyProxy(delegate)

  /** Returns a new mutable [ProxyObject] containing the properties added by the [builder]. */
  public fun build(builder: MutableObjectProxy.Builder.() -> Unit): ProxyObject {
    return MutableObjectProxy.Builder().apply(builder).build()
  }

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


  /** A mutable [ProxyObject] that can be used to mimic JS plain objects created in the JVM. */
  @JvmInline public value class MutableObjectProxy internal constructor(
    private val members: MutableMap<String, Any?> = mutableMapOf(),
  ) : ProxyObject {
    /** DSL scope for [building][JsProxy.build] [MutableObjectProxy] instances. */
    @JvmInline public value class Builder internal constructor(
      private val members: MutableMap<String, Any?> = mutableMapOf()
    ) {
      /**
       * Set the [value] associated with a given [key] in the proxy. The [value] will be converted to a polyglot value
       * using [Value.asValue].
       */
      public fun put(key: String, value: Any?) {
        members[key] = value
      }

      /**
       * Associates a new object obtained using the [builder] function to the specified [key].
       */
      public fun putObject(key: String, builder: Builder.() -> Unit) {
        put(key, build(builder))
      }

      /**
       * Associates a new empty object to the specified [key].
       */
      public fun putObject(key: String) {
        put(key, MutableObjectProxy())
      }

      /**
       * Associates a [ProxyExecutable] [function] with the specified [key]. The [function] receives a [Value] array,
       * containing the arguments passed by the caller.
       */
      public fun putFunction(key: String, function: ProxyExecutable) {
        put(key, function)
      }

      /** Returns the fully constructed proxy. */
      public fun build(): MutableObjectProxy = MutableObjectProxy(members)
    }

    override fun getMember(key: String): Any = members[key] ?: error("Member not found: $key")
    override fun getMemberKeys(): Any = members.keys
    override fun hasMember(key: String): Boolean = members.containsKey(key)
    override fun putMember(key: String, value: Value?): Unit = members.set(key, value)
  }
}

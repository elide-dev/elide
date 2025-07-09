/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */
package elide.runtime.node.util

import com.github.ajalt.mordant.rendering.TextStyle
import org.graalvm.polyglot.Value
import java.util.SortedMap
import java.util.SortedSet
import elide.runtime.intrinsics.js.node.UtilAPI
import elide.runtime.intrinsics.js.node.util.InspectOptionsAPI

// Utility methods for implementing `util.inspect`.
internal object ObjectInspector {
  // Rendering context for inspection.
  private interface InspectRenderer : Appendable, CharSequence {
    fun utils(): UtilAPI
    fun options(): InspectOptionsAPI
    fun styles(): InspectStyling = options().styles
  }

  // Render with a given text style.
  private fun InspectRenderer.styled(style: InspectStyling.() -> TextStyle?, text: String) {
    val styleState = style(styles())
    if (styleState != null && options().colors) {
      append(styleState(text))
    } else {
      append(text)
    }
  }

  // Render a suite of items with a known count, potentially truncating them.
  private fun InspectRenderer.renderItems(
    count: Int,
    items: Iterable<*>,
    options: InspectOptionsAPI,
    contextOf: Value?,
  ) {
    for ((i, value) in items.withIndex()) {
      if ((i + 1) >= options.maxArrayLength) {
        append("...")
        break
      }
      renderValue(value, options, contextOf)
      if (i < count - 1) {
        append(", ")
      }
    }
  }

  // Render a host set value for inspection.
  private fun InspectRenderer.renderSet(obj: Set<*>, options: InspectOptionsAPI, contextOf: Value?) {
    val count = obj.size
    val isSorted = obj is SortedSet<*>
    val label = if (isSorted) "SortedSet" else "Set"
    append(label)
    append('(')
    append(count.toString())
    append(") { ")
    renderItems(count, obj, options, contextOf)
    append(" }")
  }

  // Render a host list value for inspection.
  private fun InspectRenderer.renderList(obj: Collection<*>, options: InspectOptionsAPI, contextOf: Value?) {
    val count = obj.size
    append("[ ")
    renderItems(count, obj, options, contextOf)
    append(" ]")
  }

  // Render a host map value for inspection.
  private fun InspectRenderer.renderMap(obj: Map<String, *>, options: InspectOptionsAPI, contextOf: Value?) {
    val isSorted = obj is SortedMap<*, *>
    val label = if (isSorted) "SortedMap" else "Map"
    append(label)
    append('(')
    append(obj.size.toString())
    append(") { ")
    var first = true
    for ((key, value) in obj) {
      if (!first) append(", ")
      first = false
      renderValue(key, options, contextOf)
      append(" => ")
      renderValue(value, options, contextOf)
    }
    append(" }")
  }

  // Render a host object value for inspection.
  @Suppress("UNCHECKED_CAST")
  private fun InspectRenderer.inspectHostObject(obj: Value, options: InspectOptionsAPI, contextOf: Value?) {
    val hostObj = obj.asHostObject<Any>()
    when (hostObj) {
      is List<*> -> renderList(hostObj, options, contextOf)
      is Set<*> -> renderSet(hostObj, options, contextOf)
      is Map<*, *> -> renderMap(hostObj as Map<String, *>, options, contextOf)
      else -> append(hostObj.toString())
    }
  }

  // Render a complex value for inspection.
  private fun InspectRenderer.inspectStructured(obj: Value, options: InspectOptionsAPI, contextOf: Value?) {
    when {
      utils().isArray(obj) || obj.hasArrayElements() -> renderArrayLike(obj, options)
      NodeTypechecks.isMap(obj) -> renderHashLike("Map", obj, options)
      obj.hasHashEntries() -> renderHashLike("MapLike", obj, options)
      obj.hasMembers() -> renderObject(obj, options)

      else -> buildString {
        append("Cannot inspect value `$obj`")
        if (contextOf != null) {
          append(" in context of `$contextOf`")
        }
        append(": no inspection available yet")
      }.let {
        TODO(it)
      }
    }
  }

  // Render a `Map`-like value for inspection.
  private fun InspectRenderer.renderHashLike(label: String, obj: Value, options: InspectOptionsAPI) {
    append("$label(${obj.hashSize})")
    append(" { ")
    var i = 0
    val iter = obj.hashKeysIterator
    var hasNext = iter.hasIteratorNextElement()
    while (hasNext) {
      if ((i + 1) >= options.maxArrayLength) {
        append("...")
        break
      }
      i += 1

      val key = iter.iteratorNextElement
      renderValue(key, options, contextOf = obj)
      append(" => ")
      val value = obj.getHashValue(key)
      renderValue(value, options, contextOf = obj)
      hasNext = iter.hasIteratorNextElement()
      if (hasNext) {
        append(", ")
      }
    }
    append(" }")
  }

  // Render a `Set` or array-like value for inspection.
  private fun InspectRenderer.renderArrayLike(obj: Value, options: InspectOptionsAPI) {
    val count = obj.arraySize
    val isSet = NodeTypechecks.isSet(obj)
    if (isSet) {
      append("Set($count) { ")
    } else {
      append("[ ")
    }
    for (i in 0 until count) {
      if ((i + 1) >= options.maxArrayLength) {
        append("...")
        break
      }
      val value = obj.getArrayElement(i)
      renderValue(value, options, contextOf = obj)
      if (i < count - 1) {
        append(", ")
      }
    }
    if (isSet) {
      append(" }")
    } else {
      append(" ]")
    }
  }

  // Render a object-like value for inspection.
  private fun InspectRenderer.renderObject(obj: Value, options: InspectOptionsAPI, label: String? = null) {
    TODO("support for inspecting raw objects")
  }

  // Render a single (potentially complex) value for inspection.
  private fun InspectRenderer.renderValue(obj: Any?, options: InspectOptionsAPI, contextOf: Value? = null) {
    when (obj) {
      null -> styled({ nullValue }, "null")

      is Value -> when {
        obj.isString -> styled({ stringValue }, "'${obj.asString()}'")
        obj.isBoolean -> styled({ primitiveValue }, obj.asBoolean().toString())
        obj.isNumber -> styled({ primitiveValue }, obj.toString())
        obj.isDate || obj.isInstant || obj.isDuration -> styled({ complexStringValue }, obj.toString())
        obj.isNull -> styled({ nullValue }, "null")
        obj.metaObject.toString() == "undefined" -> styled({ nullValue }, "undefined")
        obj.isHostObject -> inspectHostObject(obj, options, contextOf)
        else -> inspectStructured(obj, options, contextOf)
      }

      else -> renderValue(Value.asValue(obj), options, contextOf)
    }
  }

  internal fun UtilAPI.renderInspected(obj: Value, options: InspectOptionsAPI): String = buildString {
    object: InspectRenderer, Appendable by this, CharSequence by this {
      override fun utils(): UtilAPI = this@renderInspected
      override fun options(): InspectOptionsAPI = options
    }.apply {
      renderValue(obj, options)
    }
  }
}

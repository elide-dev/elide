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
package elide.runtime.node.stream

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import elide.annotations.Factory
import elide.annotations.Singleton
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.node.StreamPromisesAPI
import elide.runtime.lang.javascript.NodeModuleName
import elide.runtime.lang.javascript.asJsSymbolString
import elide.runtime.intrinsics.js.CompletableJsPromise
import elide.runtime.intrinsics.js.JsPromise as JsPromiseFactory

// Internal symbol where the Node built-in module is installed.
private val STREAM_PROMISES_MODULE_SYMBOL = "node_${NodeModuleName.STREAM_PROMISES.asJsSymbolString()}"

// Constants for the stream promises module.
private const val PIPELINE_FN = "pipeline"
private const val FINISHED_FN = "finished"

// All module props.
private val ALL_PROMISES_PROPS = arrayOf(
  PIPELINE_FN,
  FINISHED_FN,
)

// Installs the Node stream promises module into the intrinsic bindings.
@Intrinsic
@Factory internal class NodeStreamPromisesModule : AbstractNodeBuiltinModule() {
  @Singleton internal fun provide(): StreamPromisesAPI = NodeStreamPromises.obtain()

  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[STREAM_PROMISES_MODULE_SYMBOL.asJsSymbol()] = provide()
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.STREAM_PROMISES)) { provide() }
  }
}

/**
 * # Node API: `stream/promises`
 */
internal class NodeStreamPromises : ReadOnlyProxyObject, StreamPromisesAPI {
  //
  private fun valueBooleanOrNull(obj: Value, name: String): Boolean? {
    return try {
      if (obj.hasMembers() && obj.hasMember(name)) {
        val v = obj.getMember(name)
        if (v.isBoolean) v.asBoolean() else null
      } else null
    } catch (_: Throwable) { null }
  }

  private fun valueOrNull(obj: Value, name: String): Value? {
    return try {
      if (obj.hasMembers() && obj.hasMember(name)) obj.getMember(name) else null
    } catch (_: Throwable) { null }
  }

  private fun finished(stream: Value): CompletableJsPromise<Unit> {
    val promise: CompletableJsPromise<Unit> = JsPromiseFactory()

    // If already errored, reject immediately.
    valueOrNull(stream, "errored")?.let { err ->
      if (!err.isNull) {
        promise.reject(err)
        return promise
      }
    }

    // If already ended/finished, resolve immediately.
    val readableEnded = valueBooleanOrNull(stream, "readableEnded") == true
    val writableFinished = valueBooleanOrNull(stream, "writableFinished") == true
    if (readableEnded || writableFinished) {
      promise.resolve(Unit)
      return promise
    }

    // Attach listeners.
    val listeners = mutableListOf<Pair<String, Value>>()
    fun addOnce(event: String, listener: ProxyExecutable) {
      val v = Value.asValue(listener)
      listeners += event to v
      stream.invokeMember("once", event, v)
    }
    fun cleanup() {
      if (stream.canInvokeMember("off")) {
        listeners.forEach { (event, l) ->
          try {
            stream.invokeMember("off", event, l)
          } catch (_: Throwable) { /* ignore */ }
        }
      }
      listeners.clear()
    }

    addOnce(StreamEventName.END, ProxyExecutable {
      cleanup()
      promise.resolve(Unit)
    })
    addOnce(StreamEventName.FINISH, ProxyExecutable {
      cleanup()
      promise.resolve(Unit)
    })
    addOnce(StreamEventName.ERROR, ProxyExecutable { args ->
      cleanup()
      promise.reject(args.getOrNull(0))
    })

    return promise
  }

  private fun pipeline(vararg streams: Value): CompletableJsPromise<Unit> {
    val promise: CompletableJsPromise<Unit> = JsPromiseFactory()

    if (streams.isEmpty()) {
      // Resolve immediately for empty pipeline.
      promise.resolve(Unit)
      return promise
    }

    // Connect streams via .pipe()
    try {
      for (i in 0 until streams.size - 1) {
        val src = streams[i]
        val dest = streams[i + 1]
        if (src.canInvokeMember("pipe")) {
          src.invokeMember("pipe", dest)
        }
      }
    } catch (t: Throwable) {
      promise.reject(t)
      return promise
    }

    // Cleanup helpers
    val listenerMap = HashMap<Value, MutableList<Pair<String, Value>>>(streams.size)
    fun addOnce(target: Value, event: String, listener: ProxyExecutable) {
      val v = Value.asValue(listener)
      listenerMap.getOrPut(target) { mutableListOf() }.add(event to v)
      target.invokeMember("once", event, v)
    }
    fun cleanupAll() {
      streams.forEach { s ->
        if (s.canInvokeMember("off")) {
          listenerMap[s]?.forEach { (event, l) ->
            try {
              s.invokeMember("off", event, l)
            } catch (_: Throwable) { /* ignore */ }
          }
        }
      }
      listenerMap.clear()
    }

    // Reject on first error from any stream.
    streams.forEach { s ->
      if (s.canInvokeMember("once")) {
        addOnce(s, StreamEventName.ERROR, ProxyExecutable { args ->
          cleanupAll()
          promise.reject(args.getOrNull(0))
        })
      }
    }

    // Resolve when last stream finishes/ends.
    val last = streams.last()
    if (last.canInvokeMember("once")) {
      addOnce(last, StreamEventName.END, ProxyExecutable {
        cleanupAll()
        promise.resolve(Unit)
      })
      addOnce(last, StreamEventName.FINISH, ProxyExecutable {
        cleanupAll()
        promise.resolve(Unit)
      })
    }

    // Handle already-completed/errored cases.
    valueOrNull(last, "errored")?.let { err ->
      if (!err.isNull) {
        promise.reject(err)
        return promise
      }
    }
    val lastEnded = valueBooleanOrNull(last, "readableEnded") == true ||
      valueBooleanOrNull(last, "writableFinished") == true
    if (lastEnded) {
      promise.resolve(Unit)
      return promise
    }

    return promise
  }

  override fun getMemberKeys(): Array<String> = ALL_PROMISES_PROPS

  override fun getMember(key: String?): Any? = when (key) {
    PIPELINE_FN -> ProxyExecutable { args ->
      @Suppress("SpreadOperator")
      pipeline(*args)
    }
    FINISHED_FN -> ProxyExecutable { args ->
      val stream = args.getOrNull(0)
        ?: return@ProxyExecutable elide.runtime.intrinsics.js.JsPromise.rejected<Unit>(
          IllegalArgumentException("stream is required"),
        )
      finished(stream)
    }
    else -> null
  }

  internal companion object {
    private val SINGLETON = NodeStreamPromises()
    fun obtain(): NodeStreamPromises = SINGLETON
  }
}

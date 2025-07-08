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

import com.oracle.truffle.js.lang.JavaScriptLanguage
import com.oracle.truffle.js.runtime.JSErrorType
import com.oracle.truffle.js.runtime.builtins.JSError
import com.oracle.truffle.js.runtime.builtins.JSErrorObject
import com.oracle.truffle.js.runtime.objects.JSDynamicObject
import com.oracle.truffle.js.runtime.objects.Null
import com.oracle.truffle.js.runtime.objects.Undefined
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import java.util.function.Consumer
import elide.annotations.Inject
import elide.runtime.exec.GuestExecutorProvider
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.internals.intrinsics.js.codec.TextDecoder
import elide.runtime.gvm.internals.intrinsics.js.codec.TextEncoder
import elide.runtime.gvm.js.JsError
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.AbortController
import elide.runtime.intrinsics.js.AbortSignal
import elide.runtime.intrinsics.js.JsPromise.Companion.promise
import elide.runtime.intrinsics.js.node.UtilAPI
import elide.runtime.intrinsics.js.node.util.DebugLogger
import elide.runtime.lang.javascript.NodeModuleName
import elide.runtime.lang.javascript.SyntheticJSModule
import elide.vm.annotations.Polyglot
import elide.runtime.gvm.internals.intrinsics.js.abort.AbortController.Factory as AbortControllerFactory

private const val F_CALLBACKIFY = "callbackify"
private const val F_PROMISIFY = "promisify"
private const val F_DEBUGLOG = "debuglog"
private const val F_DEBUGLOG_ALT = "debug"
private const val F_DEPRECATE = "deprecate"
private const val F_TRANSFERABLE_ABORT_CONTROLLER = "transferableAbortController"
private const val F_TRANSFERABLE_ABORT_SIGNAL = "transferableAbortSignal"
private const val P_CLS_TEXTENCODER = "TextEncoder"
private const val P_CLS_TEXTDECODER = "TextDecoder"

private val moduleMembers = arrayOf(
  F_CALLBACKIFY,
  F_PROMISIFY,
  P_CLS_TEXTENCODER,
  P_CLS_TEXTDECODER,
  F_DEBUGLOG,
  F_DEBUGLOG_ALT,
  F_DEPRECATE,
  F_TRANSFERABLE_ABORT_CONTROLLER,
  F_TRANSFERABLE_ABORT_SIGNAL,
)

// Installs the Node `util` module into the intrinsic bindings.
@Intrinsic internal class NodeUtilModule @Inject constructor (
  private val exec: GuestExecutorProvider
) : SyntheticJSModule<UtilAPI>, AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeUtil.create(exec) }
  override fun provide(): NodeUtil = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.UTIL)) { singleton }
  }
}

/**
 * # Node API: `util`
 *
 * Implements the Node util module.
 */
internal class NodeUtil private constructor (private val exec: GuestExecutorProvider) : ReadOnlyProxyObject, UtilAPI {
  companion object {
    @JvmStatic fun create(exec: GuestExecutorProvider): NodeUtil = NodeUtil(exec)
  }

  override fun getMemberKeys(): Array<String> = moduleMembers

  override fun getMember(key: String?): Any? = when (key) {
    F_CALLBACKIFY -> ProxyExecutable { callbackify(it.firstOrNull()) }
    F_PROMISIFY -> ProxyExecutable { promisify(it.firstOrNull()) }
    P_CLS_TEXTENCODER -> TextEncoder.Factory
    P_CLS_TEXTDECODER -> TextDecoder.Factory
    F_DEBUGLOG, F_DEBUGLOG_ALT -> ProxyExecutable { debuglog(it.firstOrNull()) }
    F_DEPRECATE -> ProxyExecutable { args ->
      when (args.size) {
        0 -> throw JsError.typeError("`deprecate` requires at least one argument (a function)")
        1 -> deprecate(args.first())
        else -> deprecate(args.first(), args[1], args.getOrNull(2))
      }
    }
    F_TRANSFERABLE_ABORT_CONTROLLER -> ProxyExecutable { transferableAbortController() }
    F_TRANSFERABLE_ABORT_SIGNAL -> ProxyExecutable {
      runCatching { it.firstOrNull()?.asProxyObject<AbortSignal>() }
        .getOrNull()?.let { transferableAbortSignal(it) }
        ?: throw JsError.typeError("`transferableAbortSignal` requires an AbortSignal instance")
    }
    else -> null
  }

  private fun fireCallbackForValue(cbk: Value, value: Any?) {
    cbk.executeVoid(Null.instance, value)
  }

  private fun fireCallbackForError(cbk: Value, throwable: Value?) {
    requireNotNull(throwable) { "Received `null` error for callback" }
    when {
      throwable.isNull -> cbk.executeVoid(Null.instance, Undefined.instance)
      JSError.isJSError(throwable) -> cbk.executeVoid(throwable, Undefined.instance)
      else -> {
        when (val jsObject = runCatching {
          throwable.`as`<JSDynamicObject>(JSDynamicObject::class.java)
        }.getOrNull()?.takeIf {
          JSError.isJSError(it)
        }) {
          null -> cbk.executeVoid(
            JSError.create(
              JSErrorType.Error,
              JavaScriptLanguage.getCurrentJSRealm(),
              throwable.asString(),
            ),
            Undefined.instance,
          )

          // already a js error object
          else -> cbk.executeVoid(jsObject, Undefined.instance)
        }
      }
    }
  }

  @Suppress("SpreadOperator")
  @Polyglot override fun callbackify(asyncFn: Value?): ProxyExecutable {
    if (asyncFn == null || asyncFn.isNull || !asyncFn.canExecute()) {
      throw JsError.typeError("`callbackify` requires an async function")
    }
    return ProxyExecutable { args ->
      val cbk = args.last()
      if (cbk == null || cbk.isNull || !cbk.canExecute()) {
        throw JsError.typeError("`callbackify`-wrapped fn requires a callback as the last argument")
      }
      val argsWithoutCallback = args.dropLast(1).toTypedArray()
      val fut = if (argsWithoutCallback.isEmpty()) {
        asyncFn.execute()
      } else {
        asyncFn.execute(*argsWithoutCallback)
      }
      fut.invokeMember("then", object: Consumer<Value?> {
        override fun accept(t: Value?) {
          fireCallbackForValue(cbk, t ?: Undefined.instance)
        }
      }, object: Consumer<Value?> {
        override fun accept(t: Value?) {
          fireCallbackForError(cbk, t)
        }
      })
    }
  }

  @Suppress("SpreadOperator")
  @Polyglot override fun promisify(fn: Value?): ProxyExecutable {
    if (fn == null || fn.isNull || !fn.canExecute()) {
      throw JsError.typeError("`promisify` requires a function")
    }
    return ProxyExecutable { args ->
      exec.executor().promise<Value> {
        val cbk = ProxyExecutable { inner ->
          val first = inner.firstOrNull()
          val second = inner.getOrNull(1)
          val firstAsJsError = runCatching { first?.`as`(JSErrorObject::class.java) }.getOrNull().takeIf {
            JSError.isJSError(it)
          }
          when {
            firstAsJsError != null -> reject(firstAsJsError)
            second != null -> resolve(second)
            else -> error("No value provided for promisified result")  // unreachable
          }
        }
        val finalized = args.plus(Value.asValue(cbk))
        fn.executeVoid(*finalized)
      }
    }
  }

  override fun debuglog(name: String): DebugLogger = DebugLogger.named(name)

  override fun deprecate(fn: Value, message: String?, code: String?): ProxyExecutable = DeprecatedCallable.create(
    fn,
    message,
    code,
  )

  @Polyglot override fun transferableAbortController(): AbortController = AbortControllerFactory.newInstance().apply {
    markTransferable()
  }

  @Polyglot override fun transferableAbortSignal(signal: AbortSignal): AbortSignal = signal.apply {
    markTransferable()
  }
}

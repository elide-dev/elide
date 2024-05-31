/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

package elide.runtime.gvm.internals.intrinsics.js.base64

import io.micronaut.core.annotation.ReflectiveAccess
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import elide.core.encoding.base64.DefaultBase64
import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.internals.intrinsics.js.JsError
import elide.runtime.gvm.internals.intrinsics.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.intrinsics.js.JavaScriptBase64
import elide.vm.annotations.Polyglot

// Properties and methods made available to guest code.
private val BASE64_METHODS_AND_PROPS = arrayOf(
  "encode",
  "decode"
)

/** Implements [JavaScriptBase64] via the default Base64 implementation. */
@ReflectiveAccess
@Intrinsic(global = Base64Intrinsic.GLOBAL_BASE64)
internal class Base64Intrinsic : JavaScriptBase64, ProxyObject, AbstractJsIntrinsic() {
  internal companion object {
    /** Injected name of the Base64 global. */
    const val GLOBAL_BASE64 = "Base64"

    /** Injected name of the `btoa` intrinsic. */
    private const val GLOBAL_BTOA = "btoa"

    /** Injected name of the `atob` intrinsic. */
    private const val GLOBAL_ATOB = "atob"

    /** Base64 symbol. */
    private val BASE64_SYMBOL = GLOBAL_BASE64.asJsSymbol()
  }

  /** @inheritDoc */
  @ReflectiveAccess
  @Polyglot override fun encode(input: String, websafe: Boolean): String =
    if (websafe) DefaultBase64.encodeWebSafe(input) else DefaultBase64.encodeToString(input)

  /** @inheritDoc */
  @ReflectiveAccess
  @Polyglot @Intrinsic(global = GLOBAL_BTOA) override fun encode(input: String): String =
    encode(input, false)

  /** @inheritDoc */
  @ReflectiveAccess
  @Polyglot @Intrinsic(global = GLOBAL_ATOB) override fun decode(input: String): String =
    DefaultBase64.decodeToString(input)

  /** @inheritDoc */
  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    // mount `Base64`
    bindings[BASE64_SYMBOL] = this

    // mount `atob`
    bindings[GLOBAL_ATOB.asJsSymbol()] = ProxyExecutable {
      return@ProxyExecutable decode(it.firstOrNull()?.asString() ?: error("Cannot decode $it as string"))
    }
    // mount `btoa`
    bindings[GLOBAL_BTOA.asJsSymbol()] = ProxyExecutable {
      return@ProxyExecutable encode(it.firstOrNull()?.asString() ?: error("Cannot decode $it as string"))
    }
  }

  override fun getMemberKeys(): Array<String> = BASE64_METHODS_AND_PROPS
  override fun hasMember(key: String?): Boolean = key != null && key in BASE64_METHODS_AND_PROPS
  override fun putMember(key: String?, value: Value?) {
    // no-op
  }

  override fun removeMember(key: String?): Boolean = false

  override fun getMember(key: String?): Any? = when (key) {
    "encode" -> ProxyExecutable {
      encode(
        it.firstOrNull()?.asString() ?: throw JsError.typeError("Cannot encode $it as string"),
        it.getOrNull(1)?.asBoolean() ?: false
      )
    }
    "decode" -> ProxyExecutable {
      decode(it.firstOrNull()?.asString() ?: throw JsError.typeError("Cannot decode $it as string"))
    }

    else -> null
  }
}

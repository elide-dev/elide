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
package elide.runtime.secrets

import com.oracle.truffle.api.CompilerDirectives
import elide.secrets.Secrets
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.js.JsError
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.intrinsics.secrets.SecretsAPI
import jakarta.inject.Provider
import kotlinx.io.bytestring.ByteString
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import elide.runtime.gvm.GuestLanguage
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.plugins.python.secrets.SecretsPythonAPI

private const val SECRETS_MODULE = "secrets"
private const val SECRETS_INTRINSIC = "SecretsIntrinsic"
private const val SECRETS_GET = "get"

private val moduleMembers = arrayOf(SECRETS_GET)

internal sealed class SecretsModule(secretAccess: Provider<Secrets>) : GuestIntrinsic {
  protected val secrets by lazy { GuestSecrets.create(secretAccess.get()) }

  override fun symbolicName() = SECRETS_MODULE

  @Deprecated("Use symbolicName instead")
  override fun displayName() = SECRETS_MODULE
}

@Intrinsic
internal class SecretsJsIntrinsic(secretAccess: Provider<Secrets>) : SecretsModule(secretAccess) {
  override fun language(): GuestLanguage = GuestLanguage.JAVASCRIPT

  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) = Unit

  init {
    ModuleRegistry.deferred(ModuleInfo.of(SECRETS_MODULE)) { secrets }
  }
}

@Intrinsic
internal class SecretsPythonIntrinsic(secretAccess: Provider<Secrets>) : ReadOnlyProxyObject, SecretsModule(secretAccess), SecretsPythonAPI {
  override fun language(): GuestLanguage = GuestLanguage.PYTHON

  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    bindings[SECRETS_INTRINSIC.asJsSymbol()] = secrets
  }

  override fun getMemberKeys(): Array<String> = secrets.memberKeys

  override fun getMember(key: String?): Any? = secrets.getMember(key)
}

internal class GuestSecrets(private val secretAccess: Secrets) : ReadOnlyProxyObject, SecretsAPI {
  override fun get(name: Value): Value {
    if (!secretAccess.initialized || secretAccess.getProfile() == null)
      throw JsError.of("Secrets were not initialized properly.")
    val secret = secretAccess.getSecret(name.asString())
    return Context.getCurrent().asValue(if (secret is ByteString) secret.toByteArray() else secret)
  }

  override fun getMemberKeys(): Array<String> = moduleMembers

  override fun getMember(key: String?): Any? =
    when (key) {
      SECRETS_GET ->
        ProxyExecutable { args ->
          if (args.size != 1) throw JsError.typeError("Invalid number of arguments to `secrets.get`.")
          get(args[0])
        }
      else -> null
    }

  internal companion object {
    private var instance: GuestSecrets? = null

    fun create(secretAccess: Secrets) = instance ?: GuestSecrets(secretAccess).apply { instance = this }
  }
}

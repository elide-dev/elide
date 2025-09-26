/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node.constants

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.ConstantsAPI
import elide.runtime.lang.javascript.NodeModuleName

private const val P_OS = "os"
private const val P_FS = "fs"

private val ALL_MEMBERS = arrayOf(
  P_OS,
  P_FS,
)

@Intrinsic internal class NodeConstantsModule : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeConstants.create() }
  internal fun provide(): ConstantsAPI = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.CONSTANTS)) { singleton }
  }
}

/** Minimal `constants` module facade. */
internal class NodeConstants private constructor() : ReadOnlyProxyObject, ConstantsAPI {
  companion object { @JvmStatic fun create(): NodeConstants = NodeConstants() }

  override fun getMemberKeys(): Array<String> = ALL_MEMBERS

  override fun putMember(key: String?, value: Value?): Unit = error("Cannot modify `constants`")

  override fun getMember(key: String?): Any? = when (key) {
    P_OS -> elide.runtime.node.os.PosixConstants
    P_FS -> elide.runtime.node.fs.FilesystemConstants
    else -> null
  }
}


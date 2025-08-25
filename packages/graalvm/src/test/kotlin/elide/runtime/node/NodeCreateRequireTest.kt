/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node

import elide.runtime.node.module.NodeModulesModule
import elide.testing.annotations.TestCase
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertNotNull

/** Targeted tests for Node `module` minimal API (createRequire). */
@TestCase
internal class NodeCreateRequireTest : NodeModuleConformanceTest<NodeModulesModule>() {
  override val moduleName: String get() = "module"
  override fun provide(): NodeModulesModule = NodeModulesModule()
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("builtinModules"); yield("isBuiltin"); yield("createRequire")
  }

  @Test fun `module - createRequire can load builtins`() {
    val mod = require("node:module")
    val createRequire = mod.getMember("createRequire")
    assertNotNull(createRequire)
    // Use a simple string for import.meta.url stand-in; our createRequire ignores it for builtins.
    val req = createRequire.execute("file:///tmp/test.mjs")
    val urlMod = req.execute("node:url")
    assertNotNull(urlMod)
    assertContains(urlMod.memberKeys, "URL")
  }
}


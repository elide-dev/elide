/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node

import elide.runtime.node.url.NodeURLModule
import elide.testing.annotations.TestCase
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertNotNull

/** Targeted tests for Node `url` helpers implemented by NodeURL. */
@TestCase
internal class NodeUrlHelpersTest : NodeModuleConformanceTest<NodeURLModule>() {
  override val moduleName: String get() = "url"
  override fun provide(): NodeURLModule = NodeURLModule()
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("URL")
    yield("URLSearchParams")
    yield("domainToASCII")
    yield("domainToUnicode")
    yield("fileURLToPath")
    yield("pathToFileURL")
    yield("urlToHttpOptions")
  }

  @Test fun `url - has helper members`() {
    val mod = require("node:url")
    val keys = mod.memberKeys
    listOf(
      "URL","URLSearchParams","domainToASCII","domainToUnicode","fileURLToPath","pathToFileURL","urlToHttpOptions"
    ).forEach { k -> assertContains(keys, k) }
  }

  @Test fun `url - domainToASCII and domainToUnicode basic`() {
    val mod = require("node:url")
    val toAscii = mod.getMember("domainToASCII")
    val toUnicode = mod.getMember("domainToUnicode")
    assertNotNull(toAscii); assertNotNull(toUnicode)
    val ascii = toAscii.execute("mañana.com").asString()
    val unicode = toUnicode.execute(ascii).asString()
    // Round-trip should recover original unicode domain
    kotlin.test.assertTrue(unicode.contains("mañana"))
  }

  @Test fun `url - fileURLToPath and pathToFileURL basic`() {
    val mod = require("node:url")
    val toPath = mod.getMember("fileURLToPath")
    val toUrl = mod.getMember("pathToFileURL")
    assertNotNull(toPath); assertNotNull(toUrl)
    val fsUrl = "file:///C:/Windows" // ok for Windows path style; acceptable in tests
    val path = toPath.execute(fsUrl).asString()
    val backUrl = toUrl.execute(path)
    assertNotNull(backUrl)
  }

  @Test fun `url - urlToHttpOptions basic`() {
    val mod = require("node:url")
    val toOpts = mod.getMember("urlToHttpOptions")
    assertNotNull(toOpts)
    val opts = toOpts.execute("http://example.com:8080/hello?x=1")
    assertContains(opts.memberKeys, "protocol")
    assertContains(opts.memberKeys, "hostname")
    assertContains(opts.memberKeys, "port")
    assertContains(opts.memberKeys, "path")
  }
}


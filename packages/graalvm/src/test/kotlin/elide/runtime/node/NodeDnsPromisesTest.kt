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
package elide.runtime.node

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import elide.annotations.Inject
import elide.runtime.node.dns.NodeDNSPromisesModule
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `dns` built-in module. */
@TestCase internal class NodeDnsPromisesTest : NodeModuleConformanceTest<NodeDNSPromisesModule>() {
  override val moduleName: String get() = "dns/promises"
  override fun provide(): NodeDNSPromisesModule = NodeDNSPromisesModule()
  @Inject lateinit var dns: NodeDNSPromisesModule

  // @TODO(sgammon): Not yet fully supported
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("Resolver")
    yield("getServers")
    yield("lookupService")
    yield("resolve")
    yield("resolve4")
    yield("resolve6")
    yield("resolveAny")
    yield("resolveCname")
    yield("resolveCaa")
    yield("resolveMx")
    yield("resolveNaptr")
    yield("resolveNs")
    yield("resolvePtr")
    yield("resolveSoa")
    yield("resolveSrv")
    yield("resolveTxt")
    yield("reverse")
    yield("setDefaultResultOrder")
    yield("getDefaultResultOrder")
  }

  @Test override fun testInjectable() {
    assertNotNull(dns)
  }

  @Test fun `dns promises - resolve returns an array`() {
    val code = """
      import dns from "dns/promises";
      const res = await dns.resolve("localhost");
      export const isArray = Array.isArray(res);
    """.trimIndent()
    val out = assertNotNull(executeESM(true) { code }.await())
    assertTrue(out.hasMembers())
    assertTrue(out.getMember("isArray").asBoolean())
  }

  @Test fun `dns promises - resolve4 yields ipv4 strings (or empty)`() {
    val code = """
      import dns from "dns/promises";
      const res = await dns.resolve4("localhost");
      export const ok = Array.isArray(res) && res.every(x => typeof x === 'string' && x.includes('.'));
    """.trimIndent()
    val out = assertNotNull(executeESM(true) { code }.await())
    assertTrue(out.hasMembers())
    assertTrue(out.getMember("ok").asBoolean())
  }

  @Test fun `dns promises - resolve6 yields ipv6 strings (or empty)`() {
    val code = """
      import dns from "dns/promises";
      const res = await dns.resolve6("localhost");
      export const ok = Array.isArray(res) && res.every(x => typeof x === 'string' && x.includes(':'));
    """.trimIndent()
    val out = assertNotNull(executeESM(true) { code }.await())
    assertTrue(out.hasMembers())
    assertTrue(out.getMember("ok").asBoolean())
  }

  @Test fun `dns promises - reverse returns an array`() {
    val code = """
      import dns from "dns/promises";
      const res = await dns.reverse("127.0.0.1");
      export const isArray = Array.isArray(res);
    """.trimIndent()
    val out = assertNotNull(executeESM(true) { code }.await())
    assertTrue(out.hasMembers())
    assertTrue(out.getMember("isArray").asBoolean())
  }

  @Test fun `dns promises - result order set and get`() {
    val code = """
      import dns from "dns/promises";
      dns.setDefaultResultOrder("ipv4first");
      export const order = dns.getDefaultResultOrder();
    """.trimIndent()
    val out = assertNotNull(executeESM(true) { code }.await())
    assertTrue(out.hasMembers())
    assertEquals("ipv4first", out.getMember("order").asString())
  }

  @Test fun `dns promises - unsupported rrtypes reject with ENOTSUP`() {
    val code = """
      import dns from "dns/promises";
      async function capture(fn) {
        try { await fn(); return 'ok'; } catch (e) { return e; }
      }
      const res = await Promise.all([
        capture(() => dns.resolveAny("example.com")),
        capture(() => dns.resolveCname("example.com")),
        capture(() => dns.resolveCaa("example.com")),
        capture(() => dns.resolveMx("example.com")),
        capture(() => dns.resolveNaptr("example.com")),
        capture(() => dns.resolveNs("example.com")),
        capture(() => dns.resolvePtr("example.com")),
        capture(() => dns.resolveSoa("example.com")),
        capture(() => dns.resolveSrv("example.com")),
        capture(() => dns.resolveTxt("example.com")),
      ]);
      export const ok = res.every(x => x === 'ENOTSUP');
    """.trimIndent()
    val out = assertNotNull(executeESM(true) { code }.await())
    assertTrue(out.hasMembers())
    assertTrue(out.getMember("ok").asBoolean())
  }
}

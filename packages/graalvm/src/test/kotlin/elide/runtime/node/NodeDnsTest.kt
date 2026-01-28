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
import elide.annotations.Inject
import elide.runtime.exec.GuestExecution
import elide.runtime.exec.GuestExecutorProvider
import elide.runtime.node.dns.NodeDNSModule
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `dns` built-in module. */
@TestCase internal class NodeDnsTest : NodeModuleConformanceTest<NodeDNSModule>() {
  override val moduleName: String get() = "dns"
  override fun provide(): NodeDNSModule = NodeDNSModule(GuestExecutorProvider { GuestExecution.direct() })
  @Inject lateinit var dns: NodeDNSModule

  // @TODO(sgammon): Not yet fully supported
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("Resolver")
    yield("getServers")
    yield("lookup")
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

  @Test fun `test getDefaultResultOrder returns valid order`() {
    executeGuest {
      // language=JavaScript
      """
        const dns = require('node:dns');
        const order = dns.getDefaultResultOrder();
        test(order === 'verbatim' || order === 'ipv4first' || order === 'ipv6first').shouldBeTrue();
      """
    }.doesNotFail()
  }

  @Test fun `test setDefaultResultOrder changes result order`() {
    executeGuest {
      // language=JavaScript
      """
        const dns = require('node:dns');
        dns.setDefaultResultOrder('ipv4first');
        test(dns.getDefaultResultOrder()).isEqualTo('ipv4first');
        dns.setDefaultResultOrder('verbatim');
        test(dns.getDefaultResultOrder()).isEqualTo('verbatim');
      """
    }.doesNotFail()
  }

  @Test fun `test getServers returns an array`() {
    executeGuest {
      // language=JavaScript
      """
        const dns = require('node:dns');
        const servers = dns.getServers();
        test(Array.isArray(servers)).shouldBeTrue();
      """
    }.doesNotFail()
  }

  @Test fun `test resolve4 resolves google dns with callback`() {
    executeGuest {
      // language=JavaScript
      """
        const dns = require('node:dns');
        let resolved = false;
        let addresses = null;
        dns.resolve4('dns.google', (err, addrs) => {
          if (!err && addrs && addrs.length > 0) {
            resolved = true;
            addresses = addrs;
          }
        });
        // Give time for async resolution
        test(resolved).shouldBeTrue();
        test(addresses.length > 0).shouldBeTrue();
        // dns.google has well-known IPs: 8.8.8.8 and 8.8.4.4
        const hasExpectedIP = addresses.some(ip => ip === '8.8.8.8' || ip === '8.8.4.4');
        test(hasExpectedIP).shouldBeTrue();
      """
    }.doesNotFail()
  }

  @Test fun `test lookup resolves hostname`() {
    executeGuest {
      // language=JavaScript
      """
        const dns = require('node:dns');
        let resolved = false;
        let result = null;
        dns.lookup('dns.google', (err, address, family) => {
          if (!err && address) {
            resolved = true;
            result = { address, family };
          }
        });
        test(resolved).shouldBeTrue();
        test(result.address).isNotNull();
        test(result.family === 4 || result.family === 6).shouldBeTrue();
      """
    }.doesNotFail()
  }

  @Test fun `test resolve6 resolves google dns`() {
    executeGuest {
      // language=JavaScript
      """
        const dns = require('node:dns');
        let resolved = false;
        let addresses = null;
        dns.resolve6('dns.google', (err, addrs) => {
          if (!err && addrs && addrs.length > 0) {
            resolved = true;
            addresses = addrs;
          }
        });
        test(resolved).shouldBeTrue();
        test(addresses.length > 0).shouldBeTrue();
        // IPv6 addresses contain colons
        test(addresses[0].includes(':')).shouldBeTrue();
      """
    }.doesNotFail()
  }

  @Test fun `test Resolver class can be instantiated`() {
    executeGuest {
      // language=JavaScript
      """
        const dns = require('node:dns');
        const resolver = new dns.Resolver();
        test(resolver).isNotNull();
        test(typeof resolver.resolve4).isEqualTo('function');
        test(typeof resolver.resolve6).isEqualTo('function');
        test(typeof resolver.getServers).isEqualTo('function');
      """
    }.doesNotFail()
  }

  @Test fun `test dns promises module is available`() {
    executeGuest {
      // language=JavaScript
      """
        const dns = require('node:dns');
        test(dns.promises).isNotNull();
        test(typeof dns.promises.resolve4).isEqualTo('function');
        test(typeof dns.promises.resolve6).isEqualTo('function');
      """
    }.doesNotFail()
  }
}

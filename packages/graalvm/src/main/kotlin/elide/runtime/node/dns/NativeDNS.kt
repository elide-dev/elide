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
package elide.runtime.node.dns

import org.graalvm.nativeimage.ImageInfo

// Library name for DNS natives.
private const val LIB_DNS = "dns"

/**
 * Native DNS operations implemented in Rust using hickory-dns.
 *
 * This object provides JNI bindings to the native DNS resolver implementation.
 */
internal object NativeDNS {
  @Volatile private var libLoaded = false

  /**
   * Initialize the native DNS library.
   */
  @JvmStatic fun initialize() {
    if (!libLoaded && (!ImageInfo.inImageCode() || System.getProperty("elide.nativeTest") == "true")) {
      System.loadLibrary(LIB_DNS)
      libLoaded = true
    }
  }

  /**
   * Check if the native library is available.
   */
  fun isAvailable(): Boolean = libLoaded

  /**
   * Resolve IPv4 (A) records for a hostname.
   *
   * @param hostname The hostname to resolve.
   * @return Array of IPv4 addresses as strings.
   */
  @JvmStatic
  @JvmName("resolve4")
  external fun resolve4(hostname: String): Array<String>

  /**
   * Resolve IPv6 (AAAA) records for a hostname.
   *
   * @param hostname The hostname to resolve.
   * @return Array of IPv6 addresses as strings.
   */
  @JvmStatic
  @JvmName("resolve6")
  external fun resolve6(hostname: String): Array<String>

  /**
   * Resolve both IPv4 and IPv6 addresses for a hostname.
   *
   * @param hostname The hostname to resolve.
   * @return Array of IP addresses as strings.
   */
  @JvmStatic
  @JvmName("resolveAny")
  external fun resolveAny(hostname: String): Array<String>

  /**
   * Resolve MX records for a hostname.
   *
   * @param hostname The hostname to resolve.
   * @return Array of MX records formatted as "priority:exchange".
   */
  @JvmStatic
  @JvmName("resolveMx")
  external fun resolveMx(hostname: String): Array<String>

  /**
   * Resolve TXT records for a hostname.
   *
   * @param hostname The hostname to resolve.
   * @return Array of TXT record strings.
   */
  @JvmStatic
  @JvmName("resolveTxt")
  external fun resolveTxt(hostname: String): Array<String>

  /**
   * Resolve SRV records for a hostname.
   *
   * @param hostname The hostname to resolve.
   * @return Array of SRV records formatted as "priority:weight:port:target".
   */
  @JvmStatic
  @JvmName("resolveSrv")
  external fun resolveSrv(hostname: String): Array<String>

  /**
   * Resolve NS records for a hostname.
   *
   * @param hostname The hostname to resolve.
   * @return Array of nameserver hostnames.
   */
  @JvmStatic
  @JvmName("resolveNs")
  external fun resolveNs(hostname: String): Array<String>

  /**
   * Resolve CNAME records for a hostname.
   *
   * @param hostname The hostname to resolve.
   * @return Array of canonical names.
   */
  @JvmStatic
  @JvmName("resolveCname")
  external fun resolveCname(hostname: String): Array<String>

  /**
   * Resolve CAA records for a hostname.
   *
   * @param hostname The hostname to resolve.
   * @return Array of CAA records formatted as "tag:value".
   */
  @JvmStatic
  @JvmName("resolveCaa")
  external fun resolveCaa(hostname: String): Array<String>

  /**
   * Resolve PTR records for a hostname.
   *
   * @param hostname The hostname to resolve.
   * @return Array of PTR record values.
   */
  @JvmStatic
  @JvmName("resolvePtr")
  external fun resolvePtr(hostname: String): Array<String>

  /**
   * Resolve NAPTR records for a hostname.
   *
   * @param hostname The hostname to resolve.
   * @return Array of NAPTR records formatted as "order:preference:flags:services:regexp:replacement".
   */
  @JvmStatic
  @JvmName("resolveNaptr")
  external fun resolveNaptr(hostname: String): Array<String>

  /**
   * Resolve TLSA records (certificate associations) for a hostname.
   *
   * @param hostname The hostname to resolve.
   * @return Array of TLSA records formatted as "certUsage:selector:matchingType:dataHex".
   */
  @JvmStatic
  @JvmName("resolveTlsa")
  external fun resolveTlsa(hostname: String): Array<String>

  /**
   * Resolve SOA record for a hostname.
   *
   * @param hostname The hostname to resolve.
   * @return SOA record formatted as "mname:rname:serial:refresh:retry:expire:minimum", or empty string if not found.
   */
  @JvmStatic
  @JvmName("resolveSoa")
  external fun resolveSoa(hostname: String): String

  /**
   * Perform reverse DNS lookup.
   *
   * @param ip The IP address to reverse lookup.
   * @return Array of hostnames associated with the IP.
   */
  @JvmStatic
  @JvmName("reverse")
  external fun reverse(ip: String): Array<String>

  /**
   * Get the currently configured DNS servers.
   *
   * @return Array of DNS server addresses.
   */
  @JvmStatic
  @JvmName("getServers")
  external fun getServers(): Array<String>

  /**
   * Set the DNS servers to use.
   *
   * @param servers Array of DNS server addresses.
   */
  @JvmStatic
  @JvmName("setServers")
  external fun setServers(servers: Array<String>)

  /**
   * Get the default result order.
   *
   * @return Either "ipv4first" or "verbatim".
   */
  @JvmStatic
  @JvmName("getDefaultResultOrder")
  external fun getDefaultResultOrder(): String

  /**
   * Set the default result order.
   *
   * @param order Either "ipv4first" or "verbatim".
   */
  @JvmStatic
  @JvmName("setDefaultResultOrder")
  external fun setDefaultResultOrder(order: String)

  /**
   * Lookup service name for an address and port.
   *
   * @param address The IP address.
   * @param port The port number.
   * @return Service info formatted as "hostname:service", or empty string if not found.
   */
  @JvmStatic
  @JvmName("lookupService")
  external fun lookupService(address: String, port: Int): String

  /**
   * Perform hostname lookup using getaddrinfo (respects /etc/hosts, NSS, etc).
   *
   * @param hostname The hostname to lookup.
   * @param family 0 for any, 4 for IPv4, 6 for IPv6.
   * @param all If true, return all addresses; if false, return only the first.
   * @return Array with "OK" prefix followed by "address:family" entries, or error.
   */
  @JvmStatic
  @JvmName("lookup")
  external fun lookup(hostname: String, family: Int, all: Boolean): Array<String>
}

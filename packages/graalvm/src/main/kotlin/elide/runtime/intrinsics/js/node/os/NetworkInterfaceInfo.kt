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
package elide.runtime.intrinsics.js.node.os

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyObject
import elide.runtime.gvm.internals.node.os.NetworkInterfaceInfoData
import elide.vm.annotations.Polyglot

/**
 * # Operating System: Network Interface Info
 *
 * Represents information about a network interface; typically returned by `os.networkInterfaces()`. When OS info is
 * host-restricted, stubbed values are returned.
 */
public interface NetworkInterfaceInfo : ProxyObject {
  /**
   * The assigned IPv4 or IPv6 address.
   */
  @get:Polyglot public val address: String

  /**
   * The IPv4 or IPv6 network mask.
   */
  @get:Polyglot public val netmask: String

  /**
   * Either `IPv4` or `IPv6`.
   */
  @get:Polyglot public val family: String

  /**
   * The MAC address of the network interface
   */
  @get:Polyglot public val mac: String

  /**
   * `true` if the network interface is a loopback or similar interface that is not remotely accessible; otherwise
   * `false`.
   */
  @get:Polyglot public val internal: Boolean

  /**
   * The assigned IPv4 or IPv6 address with the routing prefix in CIDR notation. If the `netmask` is invalid, this
   * property is set to `null`.
   */
  @get:Polyglot public val cidr: String

  /**
   * The numeric IPv6 scope ID (only specified when the `family` is `IPv6`).
   */
  @get:Polyglot public val scopeid: Int?

  override fun getMemberKeys(): Array<String> = arrayOf(
    "address",
    "netmask",
    "family",
    "mac",
    "internal",
    "cidr",
    "scopeid",
  )

  override fun getMember(key: String): Any? = when (key) {
    "address" -> address
    "netmask" -> netmask
    "family" -> family
    "mac" -> mac
    "internal" -> internal
    "cidr" -> cidr
    "scopeid" -> scopeid
    else -> throw IllegalArgumentException("Unknown key: $key")
  }

  override fun hasMember(key: String?): Boolean = key in memberKeys

  override fun putMember(key: String?, value: Value?) {
    throw UnsupportedOperationException("NetworkInterfaceInfo is read-only")
  }

  override fun removeMember(key: String?): Boolean {
    throw UnsupportedOperationException("NetworkInterfaceInfo is read-only")
  }

  /** Factory to create new [NetworkInterfaceInfo] records. */
  public companion object {
    /**
     * Creates a new instance of [NetworkInterfaceInfo].
     *
     * @param address The assigned IPv4 or IPv6 address.
     * @param netmask The IPv4 or IPv6 network mask.
     * @param family Either `IPv4` or `IPv6`.
     * @param mac The MAC address of the network interface.
     * @param internal `true` if the network interface is a loopback or similar interface that is not remotely
     *   accessible; otherwise `false`.
     * @param cidr The assigned IPv4 or IPv6 address with the routing prefix in CIDR notation. If the `netmask` is
     *   invalid, this property is set to `null`.
     * @param scopeid The numeric IPv6 scope ID (only specified when the `family` is `IPv6`).
     * @return A new instance of [NetworkInterfaceInfo].
     */
    @JvmStatic public fun of(
      address: String,
      netmask: String,
      family: String,
      mac: String,
      internal: Boolean,
      cidr: String,
      scopeid: Int? = null,
    ): NetworkInterfaceInfo = NetworkInterfaceInfoData(
      address = address,
      netmask = netmask,
      family = family,
      mac = mac,
      internal = internal,
      cidr = cidr,
      scopeid = scopeid,
    )
  }
}

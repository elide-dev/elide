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
package elide.runtime.gvm.internals.node

import elide.runtime.gvm.internals.node.asserts.NodeAssert
import elide.runtime.gvm.internals.node.asserts.NodeAssertStrict
import elide.runtime.gvm.internals.node.buffer.NodeBufferModuleFacade
import elide.runtime.gvm.internals.node.childProcess.NodeChildProcess
import elide.runtime.gvm.internals.node.cluster.NodeCluster
import elide.runtime.gvm.internals.node.console.NodeConsole
import elide.runtime.gvm.internals.node.crypto.NodeCrypto
import elide.runtime.gvm.internals.node.dgram.NodeDatagram
import elide.runtime.gvm.internals.node.diagnostics.NodeDiagnosticsChannel
import elide.runtime.gvm.internals.node.dns.NodeDNS
import elide.runtime.gvm.internals.node.dns.NodeDNSPromises
import elide.runtime.gvm.internals.node.domain.NodeDomain
import elide.runtime.gvm.internals.node.events.NodeEventsModuleFacade
import elide.runtime.gvm.internals.node.fs.NodeFilesystem
import elide.runtime.gvm.internals.node.http.NodeHttp
import elide.runtime.gvm.internals.node.http2.NodeHttp2
import elide.runtime.gvm.internals.node.https.NodeHttps
import elide.runtime.gvm.internals.node.inspector.NodeInspector
import elide.runtime.gvm.internals.node.module.NodeModules
import elide.runtime.gvm.internals.node.net.NodeNetwork
import elide.runtime.gvm.internals.node.os.NodeOperatingSystem
import elide.runtime.gvm.internals.node.path.NodePaths
import elide.runtime.gvm.internals.node.perfHooks.NodePerformanceHooks
import elide.runtime.gvm.internals.node.process.NodeProcess
import elide.runtime.gvm.internals.node.querystring.NodeQuerystring
import elide.runtime.gvm.internals.node.readline.NodeReadline
import elide.runtime.gvm.internals.node.readline.NodeReadlinePromises
import elide.runtime.gvm.internals.node.stream.NodeStream
import elide.runtime.gvm.internals.node.stream.NodeStreamConsumers
import elide.runtime.gvm.internals.node.stream.NodeStreamPromises
import elide.runtime.gvm.internals.node.stream.NodeWebStreams
import elide.runtime.gvm.internals.node.stringDecoder.NodeStringDecoder
import elide.runtime.gvm.internals.node.test.NodeTest
import elide.runtime.gvm.internals.node.worker.NodeWorker
import elide.runtime.gvm.internals.node.zlib.NodeZlib
import elide.runtime.intrinsics.js.node.*

/**
 * # Node Standard Library
 *
 * Offers lazily-initialized implementations of Node API modules. Properties are named for their equivalent Node module
 * name; each is initialized on first access.
 *
 * Refer to module logic implementations for more information.
 */
public object NodeStdlib {
  /**
   * ## `assert`
   *
   * Provides access to a compliant implementation of the Node Assert API, at the built-in module name `assert`.
   */
  public val assert: AssertAPI by lazy { NodeAssert.obtain() }

  /**
   * ## `assert/strict`
   *
   * Provides access to a compliant implementation of the Node Assert API, at the built-in module name `assert`.
   */
  public val assertStrict: AssertStrictAPI by lazy { NodeAssertStrict.obtain() }

  /**
   * ## `buffer`
   *
   * Provides access to a compliant implementation of the Node Buffer API, at the built-in module name `buffer`.
   */
  public val buffer: BufferAPI by lazy { NodeBufferModuleFacade() }

  /**
   * ## `child_process`
   *
   * Provides access to a compliant implementation of the Node Child Process API, at the built-in module name
   * `child_process`.
   */
  public val childProcess: ChildProcessAPI by lazy { NodeChildProcess.obtain() }

  /**
   * ## `cluster`
   *
   * Provides access to a compliant implementation of the Node Cluster API, at the built-in module name `cluster`.
   */
  public val cluster: ClusterAPI by lazy { NodeCluster.obtain() }

  /**
   * ## `console`
   *
   * Provides access to a compliant implementation of the Node Console API, at the built-in module name `console`.
   */
  public val console: ConsoleAPI by lazy { NodeConsole.obtain() }

  /**
   * ## `crypto`
   *
   * Provides access to a compliant implementation of the Node Crypto API, at the built-in module name `crypto`.
   */
  public val crypto: CryptoAPI by lazy { NodeCrypto.obtain() }

  /**
   * ## `datagram`
   *
   * Provides access to a compliant implementation of the Node Datagram API, at the built-in module name `dgram`.
   */
  public val dgram: DatagramAPI by lazy { NodeDatagram.obtain() }

  /**
   * ## `diagnostics_channel`
   *
   * Provides access to a compliant implementation of the Node Diagnostics Channel API, at the built-in module name
   * `diagnostics_channel`.
   */
  public val diagnosticsChannel: DiagnosticsChannelAPI by lazy {
    NodeDiagnosticsChannel.obtain()
  }

  /**
   * ## `dns`
   *
   * Provides access to a compliant implementation of the Node DNS API, at the built-in module name `dns`.
   */
  public val dns: DNSAPI by lazy { NodeDNS.obtain() }

  /**
   * ## `dns/promises`
   *
   * Provides access to a compliant implementation of the Node DNS API, using promises, at the built-in module name
   * `dns/promises`.
   */
  public val dnsPromises: DNSPromisesAPI by lazy { NodeDNSPromises.obtain() }

  /**
   * ## `domain`
   *
   * Provides access to a compliant implementation of the Node Domain API, at the built-in module name `domain`.
   */
  public val domain: DomainAPI by lazy { NodeDomain.obtain() }

  /**
   * ## `events`
   *
   * Provides access to a compliant implementation of the Node Events API, at the built-in module name `events`.
   */
  public val events: EventsAPI by lazy { NodeEventsModuleFacade.obtain() }

  /**
   * ## `http`
   *
   * Provides access to a compliant implementation of the Node HTTP API, at the built-in module name `http`.
   */
  public val http: HTTPAPI by lazy { NodeHttp.obtain() }

  /**
   * ## `http2`
   *
   * Provides access to a compliant implementation of the Node HTTP/2 API, at the built-in module name `http2`.
   */
  public val http2: HTTP2API by lazy { NodeHttp2.obtain() }

  /**
   * ## `https`
   *
   * Provides access to a compliant implementation of the Node HTTPS API, at the built-in module name `https`.
   */
  public val https: HTTPSAPI by lazy { NodeHttps.obtain() }

  /**
   * ## `inspector`
   *
   * Provides access to a compliant implementation of the Node Inspector API, at the built-in module name `inspector`.
   */
  public val inspector: InspectorAPI by lazy { NodeInspector.obtain() }

  /**
   * ## `module`
   *
   * Provides access to a compliant implementation of the Node Module API, at the built-in module name `module`.
   */
  public val module: ModuleAPI by lazy { NodeModules.obtain() }

  /**
   * ## `net`
   *
   * Provides access to a compliant implementation of the Node Networking API, at the built-in module name `net`.
   */
  public val net: NetAPI by lazy { NodeNetwork.obtain() }

  /**
   * ## `os`
   *
   * Provides access to a compliant implementation of the Node Operating System API, at the built-in module name `os`.
   */
  public val os: OperatingSystemAPI by lazy { NodeOperatingSystem.obtain() }

  /**
   * ## `path`
   *
   * Provides access to a compliant implementation of the Node Path API, at the built-in module name `path`.
   */
  public val path: PathAPI by lazy { NodePaths.create() }

  /**
   * ## `perf_hooks`
   *
   * Provides access to a compliant implementation of the Node Performance Hooks API, at the built-in module name
   * `perf_hooks`.
   */
  public val perfHooks: PerformanceHooksAPI by lazy { NodePerformanceHooks.obtain() }

  /**
   * ## `process`
   *
   * Provides access to a compliant implementation of the Node Process API, at the built-in module name `process`.
   */
  public val process: ProcessAPI by lazy { NodeProcess.obtain() }

  /**
   * ## `querystring`
   *
   * Provides access to a compliant implementation of the Node Querystring API, at the built-in module name
   * `querystring`.
   */
  public val querystring: QuerystringAPI by lazy { NodeQuerystring.obtain() }

  /**
   * ## `readline`
   *
   * Provides access to a compliant implementation of the Node Readline API, at the built-in module name `readline`.
   */
  public val readline: ReadlineAPI by lazy { NodeReadline.obtain() }

  /**
   * ## `readline/promises`
   *
   * Provides access to a compliant implementation of the Node Readline Promises API, at the built-in module name
   * `readline/promises`.
   */
  public val readlinePromises: ReadlinePromisesAPI by lazy { NodeReadlinePromises.obtain() }

  /**
   * ## `stream`
   *
   * Provides access to a compliant implementation of the Node Streams API, at the built-in module name `stream`.
   */
  public val stream: StreamAPI by lazy { NodeStream.obtain() }

  /**
   * ## `stream/consumers`
   *
   * Provides access to a compliant implementation of the Node Stream Consumers API, at the built-in module name
   * `stream/consumers`.
   */
  public val streamConsumers: StreamConsumersAPI by lazy { NodeStreamConsumers.obtain() }

  /**
   * ## `stream/promises`
   *
   * Provides access to a compliant implementation of the Node Stream Promises API, at the built-in module name
   * `stream/promises`.
   */
  public val streamPromises: StreamPromisesAPI by lazy { NodeStreamPromises.obtain() }

  /**
   * ## `stream/web`
   *
   * Provides access to a compliant implementation of the Web Streams API, at the built-in module name `stream/web`.
   */
  public val webstream: WebStreamsAPI by lazy { NodeWebStreams.obtain() }

  /**
   * ## `string_decoder`
   *
   * Provides access to a compliant implementation of the Node String Decoder API, at the built-in module name
   * `string_decoder`.
   */
  public val stringDecoder: StringDecoderAPI by lazy { NodeStringDecoder.obtain() }

  /**
   * ## `test`
   *
   * Provides access to a compliant implementation of the Node Test API, at the built-in module name `test`.
   */
  public val test: TestAPI by lazy { NodeTest.obtain() }

  /**
   * ## `worker`
   *
   * Provides access to a compliant implementation of the Node Worker API, at the built-in module name `worker`.
   */
  public val worker: WorkerAPI by lazy { NodeWorker.obtain() }

  /**
   * ## `zlib`
   *
   * Provides access to a compliant implementation of the Node Zlib API, at the built-in module name `zlib`.
   */
  public val zlib: ZlibAPI by lazy { NodeZlib.obtain() }
}

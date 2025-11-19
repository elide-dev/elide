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
package elide.runtime.feature.js.node

import org.graalvm.nativeimage.hosted.Feature.BeforeAnalysisAccess
import org.graalvm.nativeimage.hosted.RuntimeReflection
import kotlin.reflect.KClass
import elide.annotations.engine.VMFeature
import elide.runtime.feature.FrameworkFeature
import elide.runtime.gvm.internals.intrinsics.js.url.URLIntrinsic
import elide.runtime.gvm.internals.intrinsics.js.url.URLSearchParamsIntrinsic
import elide.runtime.intrinsics.js.JavaScriptConsole
import elide.runtime.node.asserts.NodeAssert
import elide.runtime.node.buffer.NodeBufferModuleFacade
import elide.runtime.node.childProcess.NodeChildProcess
import elide.runtime.node.cluster.NodeCluster
import elide.runtime.node.console.NodeConsole
import elide.runtime.node.crypto.NodeCrypto
import elide.runtime.node.dgram.NodeDatagram
import elide.runtime.node.diagnostics.NodeDiagnosticsChannel
import elide.runtime.node.dns.NodeDNS
import elide.runtime.node.dns.NodeDNSPromises
import elide.runtime.node.domain.NodeDomain
import elide.runtime.node.events.NodeEventsModuleFacade
import elide.runtime.node.fs.FilesystemBase
import elide.runtime.node.fs.NodeFilesystemProxy
import elide.runtime.node.http2.NodeHttp2
import elide.runtime.node.https.NodeHttps
import elide.runtime.node.inspector.NodeInspector
import elide.runtime.node.module.NodeModules
import elide.runtime.node.net.NodeNetwork
import elide.runtime.node.path.NodePaths
import elide.runtime.node.perfHooks.NodePerformanceHooks
import elide.runtime.node.process.NodeProcess
import elide.runtime.node.querystring.NodeQuerystring
import elide.runtime.node.readline.NodeReadline
import elide.runtime.node.stream.NodeStream
import elide.runtime.node.stream.NodeStreamConsumers
import elide.runtime.node.stream.NodeStreamPromises
import elide.runtime.node.stream.NodeWebStreams
import elide.runtime.node.stringDecoder.NodeStringDecoder
import elide.runtime.node.test.NodeTest
import elide.runtime.node.url.NodeURL
import elide.runtime.node.worker.NodeWorker
import elide.runtime.node.zlib.NodeZlib
import elide.runtime.intrinsics.js.URL
import elide.runtime.intrinsics.js.URLSearchParams
import elide.runtime.intrinsics.js.node.*
import elide.runtime.intrinsics.js.node.events.Event
import elide.runtime.intrinsics.js.node.events.EventEmitter
import elide.runtime.intrinsics.js.node.events.EventTarget
import elide.runtime.intrinsics.js.node.process.ProcessStandardInputStream
import elide.runtime.intrinsics.js.node.process.ProcessStandardOutputStream
import elide.runtime.intrinsics.js.node.process.ProcessStandardStream
import elide.runtime.intrinsics.js.node.stream.Duplex
import elide.runtime.intrinsics.js.node.stream.StatefulStream
import elide.runtime.intrinsics.js.node.stream.Writable
import elide.runtime.node.http.NodeHttp

// Whether to register modules for reflective access.
private const val REGISTER_ALL_MODULES_FOR_REFLECTION = true

/** GraalVM feature which enables reflective access to built-in Node modules. */
@VMFeature internal class NodeJsFeature : FrameworkFeature {
  override fun getDescription(): String = "Enables support for Node.js built-in modules"

  private inline fun <reified T: Any> BeforeAnalysisAccess.cls(kclass: KClass<T>) {
    if (REGISTER_ALL_MODULES_FOR_REFLECTION) {
      RuntimeReflection.register(kclass.java)
      RuntimeReflection.registerAllClasses(kclass.java)
      RuntimeReflection.registerAllMethods(kclass.java)
      RuntimeReflection.registerAllFields(kclass.java)
      if (kclass.java.isRecord) {
        RuntimeReflection.registerAllRecordComponents(kclass.java)
      }
    }
  }

  private inline fun BeforeAnalysisAccess.registrations(op: BeforeAnalysisAccess.() -> Unit) {
    op()
  }

  override fun beforeAnalysis(access: BeforeAnalysisAccess) = access.registrations {
    // `assert` + `assert/strict`
    cls(AssertAPI::class)
    cls(AssertStrictAPI::class)
    cls(NodeAssert::class)

    // `buffer`
    cls(BufferAPI::class)
    cls(NodeBufferModuleFacade::class)

    // `child_process`
    cls(ChildProcessAPI::class)
    cls(NodeChildProcess::class)

    // `cluster`
    cls(ClusterAPI::class)
    cls(NodeCluster::class)

    // `console`
    cls(ConsoleAPI::class)
    cls(JavaScriptConsole::class)
    cls(NodeConsole::class)

    // `crypto`
    cls(CryptoAPI::class)
    cls(NodeCrypto::class)

    // `dgram`
    cls(DatagramAPI::class)
    cls(NodeDatagram::class)

    // `diagnostics_channel`
    cls(DiagnosticsChannelAPI::class)
    cls(NodeDiagnosticsChannel::class)

    // `dns` + `dns/promises`
    cls(DNSAPI::class)
    cls(DNSPromisesAPI::class)
    cls(NodeDNS::class)
    cls(NodeDNSPromises::class)

    // `domain`
    cls(DomainAPI::class)
    cls(NodeDomain::class)

    // `events`
    cls(Event::class)
    cls(EventTarget::class)
    cls(EventEmitter::class)
    cls(EventsAPI::class)
    cls(NodeEventsModuleFacade::class)

    // `fs` + `fs/promises`
    cls(FilesystemAPI::class)
    cls(FilesystemPromiseAPI::class)
    cls(FilesystemBase::class)
    cls(NodeFilesystemProxy::class)

    // `http`, `http2`, `https`
    cls(HttpAPI::class)
    cls(Http2API::class)
    cls(HttpsAPI::class)
    cls(NodeHttp::class)
    cls(NodeHttp2::class)
    cls(NodeHttps::class)

    // `inspector` + `inspector/promises`
    cls(InspectorAPI::class)
    cls(InspectorPromisesAPI::class)
    cls(NodeInspector::class)
    cls(elide.runtime.node.inspector.NodeInspectorPromises::class)

    // `module`
    cls(ModuleAPI::class)
    cls(NodeModules::class)

    // `net`
    cls(NetAPI::class)
    cls(NodeNetwork::class)

    // `os`
    cls(OperatingSystemAPI::class)
    cls(elide.runtime.node.os.NodeOperatingSystem.BaseOS::class)
    cls(elide.runtime.node.os.NodeOperatingSystem.Posix::class)
    cls(elide.runtime.node.os.NodeOperatingSystem.Win32::class)

    // `path`
    cls(PathAPI::class)
    cls(NodePaths.BasePaths::class)
    cls(NodePaths.PosixPaths::class)
    cls(NodePaths.WindowsPaths::class)

    // `perf_hooks`
    cls(PerformanceHooksAPI::class)
    cls(NodePerformanceHooks::class)

    // `process`
    cls(ProcessAPI::class)
    cls(NodeProcess.NodeProcessModuleImpl::class)
    cls(ProcessStandardStream::class)
    cls(ProcessStandardInputStream::class)
    cls(ProcessStandardOutputStream::class)

    // `querystring`
    cls(QuerystringAPI::class)
    cls(NodeQuerystring::class)

    // `readline` + `readline/promises`
    cls(ReadlineAPI::class)
    cls(ReadlinePromisesAPI::class)
    cls(NodeReadline::class)
    cls(elide.runtime.node.readline.NodeReadlinePromises::class)

    // `stream` + `stream/consumers` + `stream/promises` + `stream/web`
    cls(StreamAPI::class)
    cls(NodeStream::class)
    cls(StreamConsumersAPI::class)
    cls(NodeStreamConsumers::class)
    cls(StreamPromisesAPI::class)
    cls(NodeStreamPromises::class)
    cls(WebStreamsAPI::class)
    cls(NodeWebStreams::class)
    cls(Readable::class)
    cls(Writable::class)
    cls(Duplex::class)
    cls(StatefulStream::class)

    // `string_decoder`
    cls(StringDecoderAPI::class)
    cls(NodeStringDecoder::class)

    // `test`
    cls(TestAPI::class)
    cls(NodeTest::class)

    // `url`
    cls(URLAPI::class)
    cls(NodeURL::class)
    cls(URL::class)
    cls(URLIntrinsic::class)
    cls(URLSearchParams::class)
    cls(URLSearchParamsIntrinsic.URLSearchParams::class)
    cls(URLSearchParamsIntrinsic.MutableURLSearchParams::class)

    // `worker`
    cls(WorkerAPI::class)
    cls(NodeWorker::class)

    // `zlib`
    cls(ZlibAPI::class)
    cls(NodeZlib::class)
  }
}

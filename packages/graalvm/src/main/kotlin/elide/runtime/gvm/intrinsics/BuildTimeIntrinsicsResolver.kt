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
@file:OptIn(DelicateElideApi::class)

package elide.runtime.gvm.intrinsics

import com.oracle.truffle.api.CompilerDirectives
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Replaces
import java.util.*
import jakarta.inject.Provider
import jakarta.inject.Singleton
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.EntrypointRegistry
import elide.runtime.core.RuntimeLatch
import elide.runtime.core.SharedContextFactory
import elide.runtime.exec.GuestExecutor
import elide.runtime.exec.GuestExecutorProvider
import elide.runtime.gvm.GuestLanguage
import elide.runtime.gvm.internals.intrinsics.ElideIntrinsic
import elide.runtime.gvm.internals.intrinsics.js.abort.AbortControllerIntrinsic
import elide.runtime.gvm.internals.intrinsics.js.abort.AbortSignalIntrinsic
import elide.runtime.gvm.internals.intrinsics.js.base64.Base64Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.codec.JsEncodingIntrinsics
import elide.runtime.gvm.internals.intrinsics.js.console.ConsoleIntrinsic
import elide.runtime.gvm.internals.intrinsics.js.crypto.WebCryptoIntrinsic
import elide.runtime.gvm.internals.intrinsics.js.fetch.FetchIntrinsic
import elide.runtime.gvm.internals.intrinsics.js.url.URLIntrinsic
import elide.runtime.gvm.internals.intrinsics.js.url.URLSearchParamsIntrinsic
import elide.runtime.gvm.internals.intrinsics.js.webstreams.ReadableStreamIntrinsic
import elide.runtime.gvm.internals.intrinsics.js.webstreams.TransformStreamIntrinsic
import elide.runtime.gvm.internals.intrinsics.js.webstreams.WritableStreamIntrinsic
import elide.runtime.gvm.internals.js.JsTimerExecutorProviderImpl
import elide.runtime.gvm.internals.js.JsTimersIntrinsic
import elide.runtime.gvm.internals.sqlite.ElideSqliteModule
import elide.runtime.gvm.internals.testing.ElideTestingModule
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.intrinsics.IntrinsicsResolver
import elide.runtime.intrinsics.ai.ElideLLMModule
import elide.runtime.intrinsics.js.err.ValueErrorIntrinsic
import elide.runtime.intrinsics.server.http.v2.flask.FlaskHttpIntrinsic
import elide.runtime.intrinsics.testing.TestingRegistrar
import elide.runtime.javascript.*
import elide.runtime.node.asserts.NodeAssertModule
import elide.runtime.node.asserts.NodeAssertStrictModule
import elide.runtime.node.buffer.NodeBufferModule
import elide.runtime.node.childProcess.NodeChildProcessModule
import elide.runtime.node.cluster.NodeClusterModule
import elide.runtime.node.console.NodeConsoleModule
import elide.runtime.node.diagnostics.NodeDiagnosticsChannelModule
import elide.runtime.node.dns.NodeDNSModule
import elide.runtime.node.dns.NodeDNSPromisesModule
import elide.runtime.node.domain.NodeDomainModule
import elide.runtime.node.events.NodeEventsModule
import elide.runtime.node.fs.NodeFilesystemModule
import elide.runtime.node.fs.VfsInitializerListener
import elide.runtime.node.http.NodeHttpModule
import elide.runtime.node.http2.NodeHttp2Module
import elide.runtime.node.https.NodeHttpsModule
import elide.runtime.node.inspector.NodeInspectorModule
import elide.runtime.node.module.NodeModulesModule
import elide.runtime.node.net.NodeNetworkModule
import elide.runtime.node.os.NodeOperatingSystemModule
import elide.runtime.node.path.NodePathsModule
import elide.runtime.node.process.NodeProcessModule
import elide.runtime.node.querystring.NodeQuerystringModule
import elide.runtime.node.readline.NodeReadlineModule
import elide.runtime.node.readline.NodeReadlinePromisesModule
import elide.runtime.node.stream.NodeStreamConsumersModule
import elide.runtime.node.stream.NodeStreamModule
import elide.runtime.node.stream.NodeStreamPromisesModule
import elide.runtime.node.stringDecoder.NodeStringDecoderModule
import elide.runtime.node.url.NodeURLModule
import elide.runtime.node.util.NodeUtilModule
import elide.runtime.node.worker.NodeWorkerModule
import elide.runtime.node.zlib.NodeZlibModule
import elide.runtime.plugins.env.EnvConfig

/** Implements build-time intrinsic prep. */
@Replaces(BuiltinIntrinsicsResolver::class)
@Context @Singleton public class BuildTimeIntrinsicsResolver(
  guestExec: GuestExecutor,
  envConfigProvider: Optional<EnvConfig?>,
  vfsInitializerListener: VfsInitializerListener,
  testRegistrar: TestingRegistrar,
  entrypoint: EntrypointRegistry,
  contextFactory: SharedContextFactory,
  latch: RuntimeLatch,
) : IntrinsicsResolver {
  init {
    exec = guestExec
    envConfigSupplier = Provider { envConfigProvider.orElse(null) }
    listener = vfsInitializerListener
    registrar = testRegistrar
    entrypointProvider = entrypoint
    contextProvider = contextFactory
    runtimeLatch = latch
  }

  public companion object {
    @CompilerDirectives.CompilationFinal @Volatile private lateinit var exec: GuestExecutor
    @CompilerDirectives.CompilationFinal @Volatile private lateinit var envConfigSupplier: Provider<EnvConfig?>
    @CompilerDirectives.CompilationFinal @Volatile private lateinit var listener: VfsInitializerListener
    @CompilerDirectives.CompilationFinal @Volatile private lateinit var registrar: TestingRegistrar
    @CompilerDirectives.CompilationFinal @Volatile private lateinit var entrypointProvider: EntrypointRegistry
    @CompilerDirectives.CompilationFinal @Volatile private lateinit var contextProvider: SharedContextFactory
    @CompilerDirectives.CompilationFinal @Volatile private lateinit var runtimeLatch: RuntimeLatch
    @JvmStatic private val execProvider = GuestExecutorProvider { exec }
    @JvmStatic private val timerExecutor = JsTimerExecutorProviderImpl()
    @JvmStatic private val vfsListenerProvider = Provider { listener }
    @JvmStatic private val registrarProvider = Provider { registrar }
    @JvmStatic private val assert = NodeAssertModule()
    @JvmStatic private val assertStrict = NodeAssertStrictModule()
    @JvmStatic private val diag = NodeDiagnosticsChannelModule()
    @JvmStatic private val cluster = NodeClusterModule()
    @JvmStatic private val events = NodeEventsModule()
    @JvmStatic private val fetch = FetchIntrinsic()
    @JvmStatic private val console = ConsoleIntrinsic()
    @JvmStatic private val consoleModule = NodeConsoleModule()
    @JvmStatic private val url = URLIntrinsic()
    @JvmStatic private val urlModule = NodeURLModule()
    @JvmStatic private val urlSearchParams = URLSearchParamsIntrinsic()
    @JvmStatic private val nodeStreams = NodeStreamModule()
    @JvmStatic private val streamsPromises = NodeStreamPromisesModule()
    @JvmStatic private val streamsConsumers = NodeStreamConsumersModule()
    @JvmStatic private val timers = JsTimersIntrinsic(timerExecutor)
    @JvmStatic private val buffer = NodeBufferModule()
    @JvmStatic private val querystring = NodeQuerystringModule()
    @JvmStatic private val http = NodeHttpModule()
    @JvmStatic private val https = NodeHttpsModule()
    @JvmStatic private val http2 = NodeHttp2Module()
    @JvmStatic private val encoding = JsEncodingIntrinsics()
    @JvmStatic private val domain = NodeDomainModule()
    @JvmStatic private val dns = NodeDNSModule()
    @JvmStatic private val dnsPromises = NodeDNSPromisesModule()
    @JvmStatic private val modules = NodeModulesModule()
    @JvmStatic private val paths = NodePathsModule()
    @JvmStatic private val fs = NodeFilesystemModule(paths, vfsListenerProvider, execProvider)
    @JvmStatic private val base64 = Base64Intrinsic()
    @JvmStatic private val abort = AbortControllerIntrinsic()
    @JvmStatic private val os = NodeOperatingSystemModule()
    @JvmStatic private val readline = NodeReadlineModule()
    @JvmStatic private val readlinePromises = NodeReadlinePromisesModule()
    @JvmStatic private val net = NodeNetworkModule()
    @JvmStatic private val abortSignal = AbortSignalIntrinsic(execProvider)
    @JvmStatic private val worker = NodeWorkerModule()
    @JvmStatic private val navigator = NavigatorBuiltin()
    @JvmStatic private val util = NodeUtilModule(execProvider)
    @JvmStatic private val zlib = NodeZlibModule()
    @JvmStatic private val webCrypto = WebCryptoIntrinsic()
    @JvmStatic private val inspector = NodeInspectorModule()
    @JvmStatic private val childProcess = NodeChildProcessModule(fs, execProvider)
    @JvmStatic private val queueMicrotaskCallable = QueueMicrotaskCallable(execProvider)
    @JvmStatic private val messageChannel = MessageChannelBuiltin()
    @JvmStatic private val stringDecoder = NodeStringDecoderModule()
    @JvmStatic private val process = NodeProcessModule(Provider { envConfigSupplier.get() })
    @JvmStatic private val structuredClone = StructuredCloneBuiltin()
    @JvmStatic private val valueError = ValueErrorIntrinsic()
    @JvmStatic private val elideTesting = ElideTestingModule(registrarProvider)
    @JvmStatic private val elideSqlite = ElideSqliteModule()
    @JvmStatic private val elideLlm = ElideLLMModule(execProvider)
    @JvmStatic private val elideBuiltin = ElideIntrinsic()
    @JvmStatic private val readableStream = ReadableStreamIntrinsic()
    @JvmStatic private val writableStream = WritableStreamIntrinsic()
    @JvmStatic private val transformStream = TransformStreamIntrinsic()
    @JvmStatic private val browserStubs = BrowserStubs()

    @JvmStatic private val flask = FlaskHttpIntrinsic(
      runtimeLatchProvider = { runtimeLatch },
      entrypointProvider = { entrypointProvider },
      contextProvider = { contextProvider },
    )

    // All built-ins and intrinsics.
    @JvmStatic private val all = arrayOf(
      assert,
      assertStrict,
      abort,
      abortSignal,
      diag,
      base64,
      cluster,
      console,
      consoleModule,
      nodeStreams,
      streamsPromises,
      streamsConsumers,
      readline,
      readlinePromises,
      events,
      fetch,
      net,
      timers,
      os,
      buffer,
      querystring,
      http,
      https,
      http2,
      encoding,
      domain,
      dns,
      dnsPromises,
      modules,
      navigator,
      paths,
      fs,
      url,
      urlSearchParams,
      urlModule,
      worker,
      zlib,
      webCrypto,
      inspector,
      readableStream,
      writableStream,
      transformStream,
      childProcess,
      queueMicrotaskCallable,
      messageChannel,
      stringDecoder,
      process,
      valueError,
      structuredClone,
      elideBuiltin,
      elideSqlite,
      elideTesting,
      elideLlm,
      util,
      browserStubs,
      flask,
    )
  }

  override fun generate(language: GuestLanguage, internals: Boolean): Sequence<GuestIntrinsic> {
    return all.asSequence().filter {
      it.supports(language)
    }
  }
}

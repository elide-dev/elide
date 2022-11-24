package elide.tool.ssg

import elide.runtime.Logger
import elide.runtime.Logging
import io.micronaut.context.BeanContext
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.client.DefaultHttpClientConfiguration
import io.micronaut.http.client.HttpClient
import io.micronaut.http.ssl.ClientSslConfiguration
import io.micronaut.runtime.server.EmbeddedServer
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.elide.meta.*
import java.nio.ByteBuffer
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Provides baseline logic for tests which invoke the SSG compiler. */
@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("unused", "MemberVisibilityCanBePrivate", "SameParameterValue")
abstract class AbstractSSGCompilerTest {
  companion object {
    const val helloWorldManifest = "classpath:helloworld.manifest.pb"
    const val emptyManifest = "classpath:empty.manifest.pb"
  }

  // Test logger.
  private val logging: Logger = Logging.named("ssg-test")

  // Embedded Micronaut server.
  @Inject protected lateinit var server: EmbeddedServer

  // SSG request factory implementation.
  @Inject protected lateinit var requestFactory: RequestFactory

  // Bean context.
  @Inject protected lateinit var beanContext: BeanContext

  /** @return HTTP client targeted at the current [server]. */
  protected fun client(): HttpClient {
    val cfg = DefaultHttpClientConfiguration()
    val ssl = (cfg.sslConfiguration as ClientSslConfiguration)
    ssl.isInsecureTrustAllCertificates = true
    return HttpClient.create(server.url, cfg)
  }

  // Compiler under test.
  private val compiler: AtomicReference<SiteCompiler> = AtomicReference(null)

  // Whether the current compiler is ready or not.
  private val initialized: AtomicBoolean = AtomicBoolean(false)

  // Build root provided by Gradle.
  private val buildRoot: AtomicReference<String> = AtomicReference(null)

  // Random filename or folder name for testing.
  private fun randomFilename(): String {
    return "test-ssg-${System.currentTimeMillis()}"
  }

  // Generate an output configuration for the provided output `type` and `name`.
  private fun outputTarget(
    type: SiteCompilerParams.OutputMode,
    name: String,
    format: SiteCompilerParams.OutputFormat? = null,
    compressed: Boolean = true,
  ): SiteCompilerParams.Output = when (type) {
    SiteCompilerParams.OutputMode.DIRECTORY -> SiteCompilerParams.Output.Directory(
      // create at provided `name` under a fresh temporary folder
      Files.createTempDirectory("elide-ssg-test")
        .toFile()
        .toPath()
        .resolveSibling(name)
        .toAbsolutePath()
        .toString()
    )

    SiteCompilerParams.OutputMode.FILE -> SiteCompilerParams.Output.File(
      // create at provided `name` under a fresh temporary folder
      Files.createTempDirectory("elide-ssg-test")
        .toFile()
        .toPath()
        .resolveSibling(name)
        .toAbsolutePath()
        .toString(),
      format ?: SiteCompilerParams.OutputFormat.ZIP,
      compressed,
    )
  }.let {
    logging.info("Generated output target: $it")
    it
  }

  // Generate an output directory path to use for testing.
  protected fun outputDirectory(name: String? = null): SiteCompilerParams.Output.Directory = outputTarget(
    SiteCompilerParams.OutputMode.DIRECTORY,
    name = name ?: randomFilename(),
  ) as SiteCompilerParams.Output.Directory

  // Generate an output archive path to use for testing.
  protected fun outputArchive(
    format: SiteCompilerParams.OutputFormat = SiteCompilerParams.OutputFormat.ZIP,
    name: String = randomFilename(),
  ): SiteCompilerParams.Output.File = outputTarget(
    SiteCompilerParams.OutputMode.FILE,
    "$name." + when (format) {
      SiteCompilerParams.OutputFormat.ZIP -> "zip"
      SiteCompilerParams.OutputFormat.TAR -> "tar"
    },
  ) as SiteCompilerParams.Output.File

  // Return the embedded server under test.
  protected fun embeddedApp(): EmbeddedServer = server

  // Run the provided `op` with the compiler under test, injecting the provided `config`.
  protected fun <R: Any> withCompiler(config: SiteCompilerParams?, op: suspend (SiteCompiler) -> R): R = runBlocking {
    require(initialized.get()) { "Compiler not initialized" }
    val compiler = compiler.get()
    assertNotNull(compiler, "Compiler not initialized")
    op(compiler.configure(config))
  }

  // Run the provided `op` with the compiler under test.
  protected fun <R: Any> withCompiler(op: suspend (SiteCompiler) -> R): R = withCompiler(
    null,
    op,
  )

  // Invoke the compiler over the CLI interface with provided arguments, then return exit code.
  protected fun cli(vararg args: String): Int {
    return SiteCompiler.exec(args.toList().toTypedArray())
  }

  // Make a fake endpoint payload based on the provided inputs.
  protected fun endpoint(
    type: EndpointType = EndpointType.PAGE,
    impl: String = "some.page.impl.Index",
    member: String = "someMember",
    tag: String? = null,
    name: String? = null,
    pageName: String? = null,
    base: String = "/",
    tail: String = "",
    consumes: List<String> = listOf(MediaType.TEXT_HTML),
    produces: List<String> = listOf(MediaType.TEXT_HTML),
    methods: List<EndpointMethods> = listOf(EndpointMethods.GET),
    precompilable: Boolean = true,
    stateful: Boolean = false,
    options: EndpointOptions? = null,
  ): Endpoint = endpoint {
    this.base = base
    this.tail = tail
    this.impl = impl
    this.member = member
    this.type = type
    this.consumes.addAll(consumes)
    this.produces.addAll(produces)
    this.method.addAll(methods)
    if (!pageName.isNullOrBlank()) this.handler = pageName
    if (!name.isNullOrBlank()) this.name = "${pageName ?: impl.split(".").last()}:$name"
    if (!tag.isNullOrBlank()) this.tag = tag
    this.options = options ?: endpointOptions {
      this.precompilable = precompilable
      this.stateful = stateful
    }
  }

  // Make a fake static fragment payload based on the provided inputs.
  protected fun staticFragment(
    request: HttpRequest<*>,
    response: HttpResponse<*>,
    endpoint: Endpoint,
    content: ByteBuffer,
    discovered: List<StaticFragmentSpec> = emptyList(),
  ): StaticFragment = StaticFragment(
    request,
    endpoint,
    response,
    content,
    discovered,
  )

  // Make a fake static fragment payload based on the provided inputs.
  protected fun staticFragment(
    endpoint: Endpoint,
    content: ByteArray,
    discovered: List<StaticFragmentSpec> = emptyList(),
    mimeType: String = MediaType.TEXT_HTML,
  ): StaticFragment = StaticFragment(
    requestFactory.create(endpoint, null),
    endpoint,
    HttpResponse.ok(content).contentLength(
      content.size.toLong()
    ).contentType(
      mimeType
    ),
    ByteBuffer.wrap(content),
    discovered,
  )

  // Make a fake static fragment payload based on the provided inputs.
  protected fun staticFragment(
    endpoint: Endpoint,
    content: String,
    discovered: List<StaticFragmentSpec> = emptyList(),
    mimeType: String = MediaType.TEXT_HTML,
  ): StaticFragment = staticFragment(
    endpoint,
    content.toByteArray(),
    discovered,
    mimeType,
  )

  // Assert based on the exit code of a CLI-based compiler call.
  protected fun assertExit(
    expected: Int,
    actual: Int,
    message: String? = "Expected exit code $expected but got $actual",
  ) {
    assertEquals(
      expected,
      actual,
      message,
    )
  }

  // Assert a successful CLI call.
  protected fun assertSuccess(code: Int) {
    assertExit(0, code)
  }

  // Assert a successful CLI call.
  protected fun assertSuccess(op: () -> Int): Int {
    val exit = op.invoke()
    assertExit(0, exit)
    return exit
  }

  @BeforeEach fun setup() {
    buildRoot.set(
      System.getProperty("tests.buildDir") ?:
      Files.createTempDirectory("elide-ssg-test")
        .toFile()
        .absolutePath
    )
    compiler.set(
      beanContext.getBean(SiteCompiler::class.java),
    )
    initialized.compareAndSet(false, true)
  }

  @AfterEach fun cleanup() {
    compiler.set(null)
    initialized.compareAndSet(true, false)
  }

  @Test fun testInjectable() {
    assertNotNull(server, "embedded server should be injected")
  }

  @Test fun testSampleRequest() = runTest {
    // make sure a generic request works against the server
    val req = HttpRequest.GET<Any>("/")
    val response = client().exchange(req).awaitFirst()
    assertNotNull(response, "should not get `null` response from client")
    assertEquals(200, response.status.code, "response status should be HTTP 200 for warmup")
  }
}

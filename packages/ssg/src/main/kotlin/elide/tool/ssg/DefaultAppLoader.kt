package elide.tool.ssg

import elide.runtime.Logger
import elide.runtime.Logging
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.client.DefaultHttpClientConfiguration
import io.micronaut.http.client.HttpClient
import io.micronaut.http.ssl.ClientSslConfiguration
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.reactive.awaitFirst
import tools.elide.meta.AppManifest
import tools.elide.meta.Endpoint
import java.io.Closeable
import java.io.File
import java.net.JarURLConnection
import java.net.URL
import java.net.URLClassLoader
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.jar.Attributes

/** Default [AppLoader] implementation, which works based on an isolated class-loader. */
@Singleton public class DefaultAppLoader @Inject internal constructor (
  private val contentReader: StaticContentReader,
) : AppLoader {
  /** Defines a local interface for an app loader implementation. */
  private sealed interface AppLoaderImpl : Closeable, AutoCloseable {
    /** Perform any preparation or validation steps which can be executed ahead of time. */
    fun prepare(info: LoadedAppInfo)

    /** Connect to the target service, or JAR file. */
    suspend fun connect()

    /** Load a specific class, if supported by the implementation; or, pass `null` to load the main class. */
    suspend fun classForName(qualifiedName: String?): Class<*>? {
      return null  // not supported unless mode is JAR
    }

    /** Execute the provided [request] against the backing application, to produce a response or error. */
    suspend fun execute(request: HttpRequest<*>): HttpResponse<ByteArray>
  }

  /** Defines a local implementation of a JAR app loader. */
  private inner class JARAppLoader(
    private val target: URL,
    private val path: String,
    ): AppLoaderImpl, URLClassLoader(arrayOf(target)) {
    // Whether we currently have an open JAR file.
    private val loaded: AtomicBoolean = AtomicBoolean(false)

    // JAR connection.
    private val connection: AtomicReference<JarURLConnection> = AtomicReference(null)

    // JAR file.
    private val file: AtomicReference<File> = AtomicReference(null)

    override fun prepare(info: LoadedAppInfo) {
      require(!loaded.get()) {
        "Cannot prepare an inner loader twice"
      }
      val subject = File(path)
      if (subject.exists()) {
        file.set(subject)
      } else throw SSGCompilerError.IOError(
        "Failed to locate JAR file at path '${subject.absolutePath}'"
      )
    }

    override suspend fun connect() {
      connection.set(withContext(Dispatchers.IO) {
        target.openConnection()
      } as JarURLConnection)

      loaded.set(true)
    }

    override fun close() {
      if (loaded.get()) {
        connection.set(null)
        loaded.set(false)
      }
    }

    override suspend fun classForName(qualifiedName: String?): Class<*>? {
      return if (qualifiedName != null) {
        loadClass(qualifiedName, true)
      } else {
        val main = connection.get().mainAttributes[Attributes.Name.MAIN_CLASS] as? String
        if (!main.isNullOrBlank()) {
          loadClass(main, true)
        } else {
          null
        }
      }
    }

    override suspend fun execute(request: HttpRequest<*>): HttpResponse<ByteArray> {
      TODO("JAR-based request execution is not implemented yet.")
    }
  }

  /** Defines a local implementation of an HTTP app loader. */
  private inner class HTTPAppLoader(private val client: HttpClient): AppLoaderImpl {
    // Whether we are currently connected to the server.
    private val connected: AtomicBoolean = AtomicBoolean(false)

    override fun prepare(info: LoadedAppInfo) {
      require(!connected.get()) {
        "Cannot prepare an inner loader twice"
      }
    }

    override suspend fun connect() {
      client.start()
      connected.set(true)
    }

    override fun close() {
      client.stop()
      connected.set(false)
    }

    override suspend fun execute(request: HttpRequest<*>): HttpResponse<ByteArray> {
      logging.debug("Beginning HTTP request '${request.method.name} ${request.uri}'")
      val response = try {
        client.exchange(request, ByteArray::class.java).awaitFirst()
      } catch (err: Throwable) {
        logging.debug("Failed to execute HTTP request '${request.method.name} ${request.uri}'", err)
        throw err
      }
      logging.debug("HTTP response finished with code: '${response.code()}'")
      return response
    }
  }

  // Private logger.
  private val logging: Logger = Logging.of(DefaultAppLoader::class)

  // Whether the app loader is currently active/holding open resources.
  private val active: AtomicBoolean = AtomicBoolean(false)

  // HTTP client to use when executing requests against the application.
  private lateinit var httpClient: HttpClient

  // Interpreted configuration.
  private lateinit var config: LoadedAppInfo

  // Request factory provided by the compiler.
  private lateinit var requestFactory: RequestFactory

  // Inner loader implementation.
  private val inner: AtomicReference<AppLoaderImpl> = AtomicReference(null)

  // Indicate whether any endpoints within the provided `app` manifest are eligible for SSG compilation.
  private fun appEligibleForSSG(app: AppManifest): Boolean {
    return app.app.endpointsMap.values.any { it.options.precompilable }
  }

  // Load a JAR-based class loader, or the HTTP app target, as applicable.
  private suspend fun loadAppIfNeeded(info: LoadedAppInfo): LoadedAppInfo = coroutineScope {
    val target = inner.get()
    config = info
    target.prepare(info)
    target.connect()
    info
  }

  // Generate a static fragment spec for the provided inputs.
  private suspend fun fragmentSpecFromEndpoint(endpoint: Endpoint): StaticFragmentSpec {
    return StaticFragmentSpec.fromEndpoint(
      endpoint = endpoint,
      request = requestFactory.create(
        endpoint,
        inner.get().classForName(endpoint.impl),
      ),
    )
  }

  // Generate a static fragment spec from a detected URL in a response.
  private fun fragmentSpecFromUrl(
    @Suppress("UNUSED_PARAMETER") parent: StaticFragmentSpec,
    request: HttpRequest<*>,
    artifact: DetectedArtifact,
  ): StaticFragmentSpec = StaticFragmentSpec.fromDetectedArtifact(
    request,
    artifact,
  )

  // Warn about a request which failed to execute.
  private fun warnFailedRequest(request: HttpRequest<*>, endpoint: Endpoint?) {
    logging.warn(
      "Failed to execute app request '${request.method.name} ${request.uri}'" +
      if (endpoint != null) {
        " (endpoint: '${endpoint.impl}.${endpoint.member}')"
      } else {
        " (endpoint: dynamic)"
      }
    )
  }

  /**
   * Close resources held by the default [AppLoader] after compilation has finished.
   */
  override fun close() {
    if (active.get()) {
      inner.get().close()
      active.set(false)
    }
  }

  /** @inheritDoc */
  override suspend fun prepAsync(
    params: SiteCompilerParams,
    app: AppManifest,
  ): Deferred<LoadedAppInfo> = coroutineScope {
    // parse the `target` value
    val url = URL(if (params.target.contains("://")) {
      params.target
    } else {
      // if the target has no protocol, assume `file://`
      "jar:file://${params.target}!/"
    })
    if (params.options.httpMode) {
      logging.debug("Detected HTTP mode is active; validating target as HTTP URL.")
      require(url.protocol == "http" || url.protocol == "https") {
        "HTTP mode requires a HTTP or HTTPS URL, but got '${url.protocol}' (value: \"${params.target}\")"
      }
      val cfg = DefaultHttpClientConfiguration()
      if (params.options.ignoreCertErrors) {
        (cfg.sslConfiguration as ClientSslConfiguration).isInsecureTrustAllCertificates = true
      }
      httpClient = HttpClient.create(url, cfg)
      inner.set(HTTPAppLoader(httpClient))
    } else {
      logging.debug("Detected HTTP mode is inactive; validating target as file URL.")
      require(url.protocol == "jar") {
        "File mode requires a file path, but got \"${params.target}\""
      }
      inner.set(JARAppLoader(url, params.target))
    }
    require(!active.get()) {
      "Cannot prep the app loader while it is already active."
    }
    active.compareAndSet(false, true)

    async {
      // prep interpreted app info, then pre-warm/connect/load app
      loadAppIfNeeded(LoadedAppInfo(
        target = url,
        manifest = app,
        params = params,
        eligible = appEligibleForSSG(app),
      ))
    }
  }

  /** @inheritDoc */
  override suspend fun generateRequests(factory: RequestFactory): Pair<Int, Sequence<StaticFragmentSpec>> {
    requestFactory = factory
    val count = config.manifest.app.endpointsCount
    val specs = ArrayList<StaticFragmentSpec>(count)
    for ((_, endpoint) in config.manifest.app.endpointsMap) {
      if (!endpoint.options.precompilable) {
        continue  // skip ineligible handlers
      }
      specs.add(fragmentSpecFromEndpoint(endpoint))
    }
    return count to specs.asSequence()
  }

  /** @inheritDoc */
  override suspend fun executeRequest(spec: StaticFragmentSpec): StaticFragment? {
    // execute the request against the app, logging a warning if it fails; at this stage, if the request fails due to an
    // application-level error, it's a non-halting event because it may be coming from user code.
    val response = try {
      inner.get().execute(spec.request)
    } catch (err: Throwable) {
      warnFailedRequest(spec.request, spec.endpoint)
      return null
    }

    // with a response in hand, we consume and process the body. if the response indicates HTML content, we try to parse
    // it to determine any additional URLs we need to fetch as followup tasks.
    val (shouldScan, post) = contentReader.consume(response)
    val followup = if (shouldScan && config.params.options.crawl) {
      contentReader.parse(spec.request, response, post)
    } else {
      emptyList()
    }
    val url = spec.request().uri
    val synthesized = if (spec.request().path == "/") {
      // special case: if we are fetching the home/root path, synthesize a request for the `/favicon.ico`, which may or
      // may not be present in the page. we should also add a mapping for `/robots.txt` and `/humans.txt`, which are not
      // typically shown in the page but may be present anyway.
      listOf(
        StaticFragmentSpec.SynthesizedSpec.fromRequest(
          base = spec.request(),
          request = requestFactory.create(url.toURL(), "/favicon.ico"),
          expectedType = StaticContentReader.ArtifactType.IMAGE,
        ),
        StaticFragmentSpec.SynthesizedSpec.fromRequest(
          base = spec.request(),
          request = requestFactory.create(url.toURL(), "/robots.txt"),
          expectedType = StaticContentReader.ArtifactType.TEXT,
        ),
        StaticFragmentSpec.SynthesizedSpec.fromRequest(
          base = spec.request(),
          request = requestFactory.create(url.toURL(), "/humans.txt"),
          expectedType = StaticContentReader.ArtifactType.TEXT,
        ),
      )
    } else {
      emptyList()
    }

    // followup tasks then need to be spawned into specs.
    val addlSpecs = followup.map {
      fragmentSpecFromUrl(
        spec,
        requestFactory.create(spec, it),
        it,
      )
    }.plus(synthesized)

    // make sure to factory a specific fragment implementation based on the spec implementation.
    return when (spec) {
      is StaticFragmentSpec.EndpointSpec -> StaticFragment.fromEndpoint(
        spec,
        response,
        post,
        addlSpecs,
      )

      is StaticFragmentSpec.SynthesizedSpec -> StaticFragment.fromDetectedArtifact(
        spec.artifact,
        response,
        post,
        addlSpecs,
      )
    }
  }
}

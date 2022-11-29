package elide.tool.ssg

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import jakarta.inject.Inject
import tools.elide.meta.*
import java.nio.ByteBuffer

/** Baseline utilities that don't boot the SSG compiler. */
abstract class AbstractSSGTest {
  companion object {
    const val helloWorldManifest = "classpath:helloworld.manifest.pb"
    const val emptyManifest = "classpath:empty.manifest.pb"
  }

  // SSG request factory implementation.
  @Inject protected lateinit var requestFactory: RequestFactory

  // Default manifest reader.
  @Inject protected lateinit var manifestReader: ManifestReader

  /** @return Manifest data from path at [target]. */
  protected suspend fun manifest(
    target: String = helloWorldManifest,
  ): AppManifest {
    return manifestReader.readManifest(target)
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
  ): StaticFragment = StaticFragment.EndpointFragment(
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
  ): StaticFragment = StaticFragment.EndpointFragment(
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
}

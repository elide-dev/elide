package elide.tools.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.protobuf.Timestamp
import elide.runtime.Runtime
import elide.tools.processor.util.annotationArgument
import elide.tools.processor.util.annotationArgumentWithDefault
import elide.util.Hex
import kotlinx.datetime.Clock
import tools.elide.meta.*
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicReference

/**
 * # Route Processor
 *
 * This KSP processor reads [elide.server.annotations.Page] annotations, and for each compatible handler, generates a
 * route configuration which is embedded into an [AppManifest] payload. The manifest is embedded with the application,
 * and is consumed by tools such as the SSG site compiler.
 *
 * ## Page annotation metadata
 *
 * For each `Page` target, annotations are scanned to determine sub-routes (only `GET` is supported for SSG, notably,
 * because static sites cannot accept data). After fanning out these routes (called "tail" routes), each page is
 * annotated within the manifest with:
 *
 * - The page's URL base and tail, as applicable
 * - The page's consumed and produced types
 * - Any other known inputs which are needed for build-time identification of pages
 *
 * @param codeGenerator Code generator for the processor.
 * @param logger Logger output tool for this processor.
 */
public class RouteProcessor(
  private val codeGenerator: CodeGenerator,
  private val logger: KSPLogger
) : SymbolProcessor {
  private companion object {
    private const val tagDigestAlgorithm = "MD5"
    private const val mediaTypeHtml = "text/html"
  }

  // Generated app manifest.
  private val manifest: AtomicReference<AppManifest?> = AtomicReference(null)

  /**
   * Route Processor: Factory
   *
   * Builds an instance of [RouteProcessor].
   */
  internal class RouteProcessorFactory : SymbolProcessorProvider {
    /** @inheritDoc */
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
      return RouteProcessor(environment.codeGenerator, environment.logger)
    }
  }

  /**
   * Generate a URL tag for the provided route information; a "tag" simply identifies a route uniquely.
   *
   * Note that tags are only generated within the context of a given page.
   *
   * @param name Name of the URL route, if assigned.
   * @param page Class declaration for the handler for this route.
   * @param url URL base for this route.
   * @return Generated/calculated tag value.
   */
  private fun tagForRoute(
    name: String?,
    page: KSClassDeclaration,
    url: String,
  ): String {
    val preimage = StringBuilder().apply {
      if (name != null) {
        append(name)
      } else {
        append((page.qualifiedName ?: page.simpleName).asString())
        append(url)
      }
    }.toString().toByteArray(StandardCharsets.UTF_8)

    val md = MessageDigest.getInstance(tagDigestAlgorithm)
    return Hex.encodeToString(md.digest(preimage)).uppercase()
  }

  /**
   * Determine the [EndpointType] to use for a given endpoint; the [produces] and [route] values are used to infer a
   * type based on file extensions and MIME types.
   *
   * @param route Route which is assigned to this endpoint.
   * @param produces MIME types produced by this endpoint.
   * @return Detected/inferred endpoint type.
   */
  private fun determineEndpointType(route: String, produces: Array<String>): EndpointType = when {
    // if there is nothing declared for `produces` or `consumes`, or `text/html` is in `produces`, the endpoint is then
    // considered to be of type `PAGE`.
    produces.isEmpty() || produces.find { it.contains(mediaTypeHtml) } != null -> EndpointType.PAGE

    // if the endpoint produces JSON, it is considered to be of type `API`.
    produces.find { it.contains("application/json") } != null -> EndpointType.API

    // if the endpoint produces CSS, JavaScript, a font file, or an image file, then it is considered to be of type
    // `ASSET`.
    produces.find {
      route.endsWith(".css") || it.contains("text/css") ||
      route.endsWith(".js") || it.contains("application/javascript") ||
      it.contains("font/") ||
      it.contains("image/")
    } != null -> EndpointType.ASSET

    // otherwise, the endpoint type is not specified.
    else -> EndpointType.ENDPOINT_TYPE_UNSPECIFIED
  }

  /**
   * Generate a route configuration for the provided [page] handler and [anno] pair; the provided annotation must be
   * well-formed, or it is skipped for processing.
   *
   * @param page Page handler class declaration.
   * @param anno [elide.server.annotations.Page] annotation fetched from the declared class.
   * @return Pair of the generated endpoint tag and [Endpoint] configuration for this page, or `null` if none could be
   *   generated, in which case this route is skipped.
   */
  private fun routeConfigForPage(page: KSClassDeclaration, anno: KSAnnotation): Pair<String, Endpoint>? {
    logger.info("Processing page '${page.qualifiedName?.asString()}'")

    // resolve route parameter, which we need to build the URL map
    val route: String? = annotationArgument("route", anno)
    if (route.isNullOrBlank()) {
      val targetName = page.qualifiedName ?: page.simpleName
      logger.warn("Failed to resolve route for annotated page class '${targetName.asString()}'")
      return null
    }

    // resolve "name" if available
    val name: String? = annotationArgument("name", anno)

    // resolve produces/consumes arrays
    val produceTypes = annotationArgument("produces", anno, arrayOf(mediaTypeHtml)) ?: emptyArray()
    val consumeTypes = annotationArgument("consumes", anno, arrayOf(mediaTypeHtml)) ?: emptyArray()
    val endpointType = determineEndpointType(route, produceTypes)

    // resolve pre-compilation setting and endpoint tag
    val precompiled = annotationArgumentWithDefault<Boolean>("precompile", anno)
    val endpointTag = tagForRoute(name, page, route)

    // generate route tag
    return endpointTag to endpoint {
      base = route
      type = endpointType
      method.add(EndpointMethods.GET)
      produces.addAll(produceTypes.toList())
      consumes.addAll(consumeTypes.toList())
      if (!name.isNullOrBlank()) this.name = name

      options = endpointOptions {
        precompilable = precompiled
      }
    }
  }

  /**
   * Generate build info to include in the application manifest; notably, this includes a timestamp describing when the
   * app manifest was last generated.
   *
   * @return Build info.
   */
  private fun buildInfo(): BuildInfo {
    val now = Clock.System.now()
    return buildInfo {
      stamp = Timestamp.newBuilder()
        .setSeconds(now.epochSeconds)
        .setNanos(now.nanosecondsOfSecond)
        .build()
    }
  }

  /**
   * Generate a full app manifest based on the provided input [routes] and [buildInfo].
   *
   * @param routes Endpoint routes configured for this application.
   * @param buildInfo Static build info to include in the manifest.
   * @return Constructed manifest record.
   */
  private fun appManifest(routes: Map<String, Endpoint>, buildInfo: BuildInfo): AppManifest {
    return appManifest {
      build = buildInfo
      app = appInfo {
        endpoints.putAll(routes)
      }
    }
  }

  /** @inheritDoc */
  override fun process(resolver: Resolver): List<KSAnnotated> {
    // scan for all `Page`-annotated classes. for each matching class, generate a pair of `String`, `Endpoint`.
    val pageAnnoName = object: KSName {
      override fun asString(): String = "elide.server.annotations.Page"
      override fun getQualifier(): String = "elide.server.annotations"
      override fun getShortName(): String = "Page"
    }
    val pageAnno = resolver.getClassDeclarationByName(
      pageAnnoName
    )
    if (pageAnno == null) {
      logger.warn("Page annotation is not present in classpath; no pages will be generated.")
      return emptyList()  // page annotation is not on the classpath
    }

    // locate annotated pages
    val fragments = resolver.getSymbolsWithAnnotation(pageAnnoName.asString()).mapNotNull {
      val cls = it as? KSClassDeclaration
      if (cls != null) {
        val anno = cls.annotations.find { subjectAnno ->
          subjectAnno.shortName.getShortName() == pageAnnoName.getShortName() &&
          subjectAnno.annotationType.resolve().declaration.qualifiedName?.asString() == pageAnnoName.asString()
        }
        if (anno != null) {
          // we found an annotation, hand it over to the route info generator.
          return@mapNotNull routeConfigForPage(cls, anno)
        }
      }
      null
    }

    // use discovered routes and `buildInfo` to build an `AppManifest`.
    manifest.set(appManifest(
      fragments.toMap(),
      buildInfo(),
    ))
    return emptyList()
  }

  /** @inheritDoc */
  override fun finish(): Unit = when (val manifest = manifest.get()) {
    null -> logger.info("No app manifest found.")
    else -> {
      // generate a file and write it to the output directory.
      val targetManifest = codeGenerator.createNewFile(
        Dependencies(false),
        Runtime.generatedPackage,
        "app.manifest",
        "proto"
      )

      // serialize the `AppManifest` into a binary proto-payload.
      targetManifest.write(manifest.toByteArray())
    }
  }
}

/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.tools.processor

import com.google.auto.service.AutoService
import com.google.devtools.ksp.hasAnnotation
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.protobuf.Timestamp
import tools.elide.meta.*
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicReference
import kotlinx.datetime.Clock
import elide.runtime.Runtime
import elide.tools.processor.util.annotationArgument
import elide.tools.processor.util.annotationArgumentWithDefault
import elide.util.Hex

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
 * @param annotation Fully-qualified path to the `Page` annotation to scan for.
 */
public class RouteProcessor(
  private val codeGenerator: CodeGenerator,
  private val logger: KSPLogger,
  private val annotation: String,
) : SymbolProcessor {
  private companion object {
    private const val tagDigestAlgorithm = "MD5"
    private const val mediaTypeHtml = "text/html"
    private const val defaultPageAnnotation = "elide.server.annotations.Page"
  }

  // Page annotation to scan for (handler-level).
  private val pageAnnoName = object : KSName {
    override fun asString(): String = "${getQualifier()}.${getShortName()}"
    override fun getQualifier(): String = defaultPageAnnotation
      .split(".").dropLast(1).joinToString(".")
    override fun getShortName(): String = defaultPageAnnotation
      .split(".").last()
  }

  // Micronaut `GET` annotation.
  private val micronautGet = object : KSName {
    override fun asString(): String = "${getQualifier()}.${getShortName()}"
    override fun getQualifier(): String = "io.micronaut.http.annotation"
    override fun getShortName(): String = "Get"
  }

  // Annotations which enable eligibility for a page controller method.
  private val eligibleEntrypointAnnotations = setOf(
    micronautGet,
  )

  // Generated app manifest.
  private val manifest: AtomicReference<AppManifest?> = AtomicReference(null)

  /**
   * Route Processor: Factory
   *
   * Builds an instance of [RouteProcessor].
   */
  @AutoService(SymbolProcessorProvider::class)
  public class RouteProcessorFactory : SymbolProcessorProvider {
    /** @inheritDoc */
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
      return RouteProcessor(
        environment.codeGenerator,
        environment.logger,
        environment.options["annotation"] ?: defaultPageAnnotation,
      )
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
    entry: KSFunctionDeclaration,
    url: String,
  ): String {
    val preimage = StringBuilder().apply {
      if (name != null) {
        append(name)
      } else {
        append((page.qualifiedName ?: page.simpleName).asString())
        append(url)
      }
      append(entry.simpleName.asString())
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
    // if the endpoint produces JSON, it is considered to be of type `API`.
    produces.find {
      it.contains("application/json") ||
      it.contains("proto")
    } != null -> EndpointType.API

    // if the endpoint produces CSS, JavaScript, a font file, or an image file, then it is considered to be of type
    // `ASSET`.
    (
      route.endsWith(".css") ||
      route.endsWith(".js")
    ) || produces.find {
      it.contains("text/css") ||
      it.contains("application/javascript") ||
      it.contains("font/") ||
      it.contains("image/")
    } != null -> EndpointType.ASSET

    // if there is nothing declared for `produces` or `consumes`, or `text/html` is in `produces`, the endpoint is then
    // considered to be of type `PAGE`.
    produces.isEmpty() || produces.find { it.contains(mediaTypeHtml) } != null -> EndpointType.PAGE

    // otherwise, the endpoint type is not specified.
    else -> EndpointType.ENDPOINT_TYPE_UNSPECIFIED
  }

  /**
   * Generate a route configuration for the provided [page] handler and [anno] pair; the provided annotation must be
   * well-formed, or it is skipped for processing.
   *
   * @param page Page handler class declaration.
   * @param entry Entrypoint method for this page call.
   * @param anno [elide.server.annotations.Page] annotation fetched from the declared class.
   * @param scoped Function-level annotation fetched from the inner method.
   * @param check Method to check an endpoint for validity and uniqueness.
   * @param httpMethod HTTP methods assigned to this function point.
   * @return Pair of the generated endpoint tag and [Endpoint] configuration for this page, or `null` if none could be
   *   generated, in which case this route is skipped.
   */
  private fun routeConfigForPage(
    page: KSClassDeclaration,
    entry: KSFunctionDeclaration,
    anno: KSAnnotation,
    scoped: KSAnnotation,
    vararg httpMethod: EndpointMethods = arrayOf(EndpointMethods.GET),
    check: (Endpoint) -> Endpoint,
  ): Pair<String, Endpoint>? {
    logger.info("Processing entry '${page.qualifiedName?.asString()}.${entry.simpleName.asString()}'")

    // resolve route base parameter, which we need to build the URL map
    val route: String? = annotationArgument("route", anno)
    if (route.isNullOrBlank()) {
      val targetName = page.qualifiedName ?: page.simpleName
      logger.warn("Failed to resolve route for annotated page class '${targetName.asString()}'")
      return null
    }

    // resolve route tail parameter, which specializes this specific route
    val tailUrl: String = annotationArgument("value", scoped)
      ?: annotationArgumentWithDefault("uri", scoped)

    // resolve "name" if available
    val className = page.qualifiedName ?: error(
      "Failed to resolve qualified name for page class ${page.simpleName.asString()}",
    )
    logger.info("Assigning implementation class name '${className.asString()}'")
    val name: String? = annotationArgument("name", anno)

    // resolve produces/consumes arrays
    val produceTypes = annotationArgument("produces", anno) ?: emptyArray<String>()
    val consumeTypes = annotationArgument("consumes", anno) ?: emptyArray<String>()
    val endpointType = determineEndpointType(tailUrl, produceTypes)

    // resolve pre-compilation setting and endpoint tag
    val precompiled = annotationArgumentWithDefault<Boolean>("precompile", anno)
    val endpointTag = tagForRoute(name, page, entry, route)

    // if the endpoint is marked as an asset, and it does not declare any explicit `produces` MIME type, we should try
    // to infer the MIME type from the file extension. similarly, if the endpoint is marked as a page, and it does not
    // specify a MIME type, we should set it to `text/html`.
    val finalProduceTypes = if (produceTypes.isEmpty() && endpointType == EndpointType.ASSET) {
      if (tailUrl.endsWith(".css")) {
        arrayOf("text/css")
      } else if (tailUrl.endsWith(".js")) {
        arrayOf("application/javascript")
      } else {
        emptyArray()
      }
    } else if (produceTypes.isEmpty() && endpointType == EndpointType.PAGE) {
      arrayOf("text/html")
    } else {
      produceTypes
    }

    // generate route tag
    return endpointTag to check.invoke(
      endpoint {
      base = route
      type = endpointType
      tail = tailUrl
      impl = className.asString()
      member = entry.simpleName.getShortName()
      httpMethod.forEach { method.add(it) }
      produces.addAll(finalProduceTypes.toList())
      consumes.addAll(consumeTypes.toList())
      if (!name.isNullOrBlank()) {
        this.handler = name
        this.name = "$name:${entry.simpleName.asString()}"
      }
      options = endpointOptions {
        precompilable = precompiled
      }
    },
    )
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

  /**
   * Determine the HTTP method indicated by an eligible page entrypoint method annotation.
   *
   * @param anno Annotation to determine an HTTP method from.
   * @return Endpoint method calculated from the provided [anno].
   * @throws IllegalStateException if the provided annotation is not eligible for processing.
   */
  private fun determineHttpMethod(anno: KSAnnotation): EndpointMethods = when (anno.shortName.asString()) {
    micronautGet.getShortName() -> EndpointMethods.GET
    else -> error("Failed to determine endpoint method for target '${anno.shortName.asString()}'")
  }

  /**
   * Scan the provided [page] class declaration for methods which are eligible for AOT processing; eligibility is
   * determined by the criteria listed below. For each matching method, dispatch [routeConfigForPage] to generate route
   * info, and then return the full set of matching routes.
   *
   * @param page Page class declaration to scan for eligible entry methods.
   * @param anno [elide.server.annotations.Page] annotation fetched from the declared class.
   * @return List of result pairs scoped to this [page], where each pair is an endpoint tag and configured [Endpoint].
   */
  private fun scanHandlerForMethods(page: KSClassDeclaration, anno: KSAnnotation): List<Pair<String, Endpoint>> {
    // grab all methods on the class. for each method, determine if it should be eligible as a page entrypoint; the
    // following criteria must be satisfied for this to be true:
    //
    // - the method must be public
    // - the method must be annotated with an HTTP method
    // - the HTTP method must be GET
    //
    // multiple sets of eligible annotations must be supported to facilitate injection across server implementations.
    // these are defined on this class and scanned herein.
    logger.info("Processing page '${page.qualifiedName?.asString()}'")
    val eligible: List<Pair<KSFunctionDeclaration, List<KSAnnotation>>> = page.getAllFunctions().filter {
      // must be public
      it.isPublic() &&

      // must have at least one eligible annotation
      eligibleEntrypointAnnotations.any { anno -> it.hasAnnotation(anno.asString()) }
    }.map { fn ->
      fn to fn.annotations.filter { sub ->
        eligibleEntrypointAnnotations.any { subject ->
          subject.getShortName() == sub.shortName.getShortName()
        }
      }.toList()
    }.toList()

    val checker: (Endpoint) -> Endpoint = { endpoint ->
      endpoint
    }

    // log about it and send each for route config
    logger.info("Found ${eligible.size} eligible entrypoint(s) on page '${page.qualifiedName?.asString()}'")
    return eligible.mapNotNull { entry ->
      routeConfigForPage(
        page = page,
        entry = entry.first,
        anno = anno,
        scoped = entry.second.first(),
        httpMethod = entry.second.map { determineHttpMethod(it) }.toTypedArray(),
        check = checker,
      )
    }
  }

  /** @inheritDoc */
  override fun process(resolver: Resolver): List<KSAnnotated> {
    // scan for all `Page`-annotated classes. for each matching class, generate a pair of `String`, `Endpoint`.
    val pageAnno = resolver.getClassDeclarationByName(
      pageAnnoName,
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
          // this class is eligible for processing. scan for methods.
          return@mapNotNull scanHandlerForMethods(cls, anno)
        }
      }
      null
    }.flatten()

    // use discovered routes and `buildInfo` to build an `AppManifest`.
    manifest.set(
      appManifest(
      fragments.toMap(),
      buildInfo(),
    ),
    )
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
        "pb",
      )

      // serialize the `AppManifest` into a binary proto-payload.
      targetManifest.write(manifest.toByteArray())
    }
  }
}

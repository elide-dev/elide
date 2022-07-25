package elide.server.cfg

import elide.annotations.Logic
import elide.server.annotations.Eager
import io.micronaut.context.ApplicationContextBuilder
import io.micronaut.context.ApplicationContextConfigurer
import io.micronaut.context.annotation.ContextConfigurer
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.PropertySource
import io.micronaut.context.env.PropertySourcePropertyResolver
import java.util.SortedMap
import java.util.SortedSet

/**
 * Configures Micronaut on behalf of an Elide application with default configuration state.
 *
 * The developer may override these properties via their own configuration sources, which can be placed on the classpath
 * as resources, loaded via Kubernetes configuration, or loaded via custom implementations of [PropertySource] /
 * [PropertySourcePropertyResolver].
 *
 * On behalf of the developer, the following application components are configured:
 * - **Netty**: Workers, native transports, threading, allocation, event loops
 * - **SSL**: Self-signed certs for development, modern suite of TLS ciphers and protocols
 * - **HTTP**: HTTP/2, compression thresholds and modes, HTTP->HTTPS redirect
 * - **Access Log**: Shows the HTTP access log via `stdout` during development
 * - **DI Container**: Eager initialization of [Logic] objects
 *
 * No action is needed to enable the above components; the DI container loads and applies this configuration
 * automatically at application startup.
 */
@Requires(notEnv = ["test"])
@ContextConfigurer public class ServerConfigurator : ApplicationContextConfigurer {
  public companion object {
    // Properties which cause errors.
    public val bannedConfig: SortedSet<String> = sortedSetOf(
      "micronaut.server.netty.http2.push-enabled",
    )

    // JVM configurations applied at server startup.
    public val systemProps: SortedMap<String, String> = sortedMapOf(
      "jdk.tls.client.protocols" to "TLSv1.2,TLSv1.1,TLSv1",
      "jdk.jar.disabledAlgorithms" to sortedSetOf(
        "anon",
        "NULL",
        "MD2",
        "EC keySize < 224",
        "DH keySize < 1024",
        "RSA keySize < 2048",
        "SSLv3",
        "RC4",
        "RC4_40",
        "DES40_CBC",
        "MD5withRSA",
        "DES",
        "3DES_EDE_CBC",
        "SHA1 jdkCA & usage TLSServer",
      ).joinToString(", "),
    )

    // Cipher suites to support, in order of preference.
    public val cipherSuites: List<String> = listOf(
      "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
      "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
      "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
      "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
      "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
      "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
      "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
      "TLS_DHE_DSS_WITH_AES_256_GCM_SHA384",
      "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
      "TLS_DHE_DSS_WITH_AES_128_GCM_SHA256",
      "TLS_DHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
      "TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384",
      "TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384",
      "TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256",
      "TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256",
      "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
      "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
      "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
      "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
      "TLS_RSA_WITH_AES_256_GCM_SHA384",
      "TLS_RSA_WITH_AES_128_GCM_SHA256",
    )

    // URL paths which should be excluded by default from the access log.
    public val defaultAccessLogExclusions: SortedSet<String> = sortedSetOf(
      "/health",
    )

    // Base properties applied to all runs (unless disabled).
    public val baseMap: SortedMap<String, Any> = sortedMapOf(
      "jackson.module-scan" to false,
      "jackson.bean-introspection-module" to true,
      "micronaut.application.default-charset" to "utf-8",
      "micronaut.server.ssl.ciphers" to cipherSuites.joinToString(","),
      "micronaut.server.ssl.protocols" to arrayOf("TLSv1.2"),
      "micronaut.server.ssl.protocol" to "TLS",
      "micronaut.server.default-charset" to "utf-8",
      "micronaut.server.max-request-size" to "10MB",
      "micronaut.server.server-header" to System.getProperty("elide.server.name", "elide/v1"),
      "micronaut.server.thread-selection" to "AUTO",
      "micronaut.server.locale-resolution.session-attribute" to "locale",
      "micronaut.server.locale-resolution.header" to true,
      "micronaut.validator.enabled" to true,
      "micronaut.jcache.enabled" to true,
      "micronaut.views.csp.enabled" to true,
      "micronaut.views.csp.generate-nonce" to true,
      "micronaut.views.soy.enabled" to false,
      "micronaut.views.soy.renaming-enabled" to false,
      "netty.default.allocator.max-order" to 3,
      "elide.assets.enabled" to true,
      "elide.assets.prefix" to "/_/assets",
      "elide.assets.etags" to true,
      "elide.assets.prefer-weak-etags" to true,
    )

    // Properties applied only outside of tests.
    public val nonTestMap: SortedMap<String, Any> = sortedMapOf(
      "micronaut.server.netty.chunked-supported" to true,
      "micronaut.server.netty.compression-threshold" to 400,
      "micronaut.server.netty.compression-level" to 4,
      "micronaut.server.netty.validate-headers" to true,
      "micronaut.server.netty.use-native-transport" to true,
      "micronaut.server.netty.access-logger.enabled" to true,
      "micronaut.server.netty.access-logger.log-format" to "common",
      "micronaut.server.netty.access-logger.logger-name" to "http:access",
      "micronaut.server.netty.access-logger.exclusions" to defaultAccessLogExclusions.toList(),
    )

    // Experimental properties.
    public val labsMap: SortedMap<String, Any> = sortedMapOf(
      "micronaut.server.netty.parent.prefer-native-transport" to true,
      "micronaut.server.netty.worker.prefer-native-transport" to true,
    )

    // Properties applied only in dev mode.
    public val devMap: SortedMap<String, Any> = sortedMapOf(
      "micronaut.server.ssl.enabled" to true,
      "micronaut.server.http-to-https-redirect" to true,
      "micronaut.server.http-version" to 2.0,
      "micronaut.server.dual-protocol" to true,
      "micronaut.server.http-to-https-redirect" to false,
      "micronaut.server.ssl.build-self-signed" to (
        System.getProperty("elide.ssl.build-self-signed", "true").toBoolean()),
    )
  }

  /** @inheritDoc */
  override fun configure(builder: ApplicationContextBuilder) {
    systemProps.forEach { entry ->
      System.setProperty(entry.key, entry.value)
    }
    // basics
    builder.eagerInitSingletons(true)
      .banner(false)
      .deduceEnvironment(true)
      .environmentPropertySource(true)
      .eagerInitConfiguration(true)
      .eagerInitAnnotated(Eager::class.java)
      .eagerInitAnnotated(Logic::class.java)
      .bootstrapEnvironment(true)

    // inject configuration unless disabled
    if (System.getProperty("elide.config.noInject") != "true") {
      builder.propertySources(
        PropertySource.of(baseMap.plus(nonTestMap).plus(
          if (System.getProperty("elide.dev") == "true") {
            devMap
          } else {
            emptyMap()
          }
        ).filter {
          !bannedConfig.contains(it.key)
        }),
      )
    }
  }
}

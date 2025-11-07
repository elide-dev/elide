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
package elide.runtime.http.server

import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import io.netty.pkitesting.CertificateBuilder
import io.netty.pkitesting.X509Bundle
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.file.Path
import java.time.Instant
import elide.runtime.http.server.netty.HttpApplicationStack

/**
 * An application capable of handling incoming calls. An implementation-specific [CallContext] is attached to each
 * call to aid with state management.
 */
public interface HttpApplication<C : CallContext> : HttpCallHandler<C> {
  /**
   * Prepare a new [CallContext] that will be attached to the call created for this [request]. The returned
   * instance will be available through [HttpCall.context].
   *
   * Exceptions thrown from this method are treated as a failure to handle the call and will cause a
   * `500-Internal Server Error` response to be sent to the client.
   */
  public fun newContext(
    request: HttpRequest,
    response: HttpResponse,
    requestBody: ReadableContentStream,
    responseBody: WritableContentStream,
  ): C

  /**
   * Called by the server engine when an exception is thrown by the application handler, or when an internal error
   * occurs. Implementations should not throw from this method, any exceptions thrown will be caught and logged.
   *
   * This callback is intended to help when debugging server issues, and to allow high-level applications to observe
   * failures in the server pipeline.
   */
  public fun onError(error: Throwable?): Unit = Unit

  /**
   * Called by the server engine when the stack this application is part of has started. This provides access to
   * resolved socket addresses and other service information that is only available after binding.
   */
  public fun onStart(stack: HttpApplicationStack): Unit = Unit
}

/**
 * Configuration options for HTTP server applications. Both the cleartext and TLS servers will support `HTTP/1.0`,
 * `HTTP/1.1`, and `HTTP/2` out of the box, with automatic protocol negotiation and upgrades.
 *
 * A single application may bind multiple servers, depending on the requested feature set: if [https] and [http3]
 * are configured, they will each use a separate server socket (with their respective configured addresses).
 */
public data class HttpApplicationOptions(
  /** Configuration options for the default cleartext HTTP server. */
  val http: CleartextOptions? = CleartextOptions(),
  /** Configuration options for the HTTPS server. */
  val https: HttpsOptions? = null,
  /** Configuration options for the experimental HTTP/3 server. */
  val http3: Http3Options? = null,
  /** Default value of the `Server` header. added to responses if not set by the application. */
  val serverName: String = DEFAULT_SERVER_NAME,
) {
  public companion object {
    public const val DEFAULT_SERVER_NAME: String = "elide"
  }
}

/** Configuration options for a cleartext HTTP server, intended for development and testing purposes. */
public data class CleartextOptions(
  /** Address used to bind the server, can be a Unix domain socket on supported platforms. */
  val address: SocketAddress = InetSocketAddress(DEFAULT_HTTP_HOST, DEFAULT_HTTP_PORT),
) {
  public companion object {
    public const val DEFAULT_HTTP_HOST: String = "localhost"
    public const val DEFAULT_HTTP_PORT: Int = 8080
  }
}

/** Configuration options for a secure HTTPS server, suitable for most uses. */
public data class HttpsOptions(
  /** The certificate provider used for the server. */
  val certificate: CertificateSource,
  /** Address used to bind the server, can be a Unix domain socket on supported platforms. */
  val address: SocketAddress = InetSocketAddress(DEFAULT_HTTPS_HOST, DEFAULT_HTTPS_PORT),
) {
  public companion object {
    public const val DEFAULT_HTTPS_HOST: String = "localhost"
    public const val DEFAULT_HTTPS_PORT: Int = 8443
  }
}

/**
 * Configuration options for an HTTP/3-over-Quic server. Support for HTTP/3 is experimental and may be removed in the
 * future, it is not recommended for production as it may use insecure component implementations.
 */
public data class Http3Options(
  /** The certificate provider used for the server. */
  val certificate: CertificateSource,
  /** Address used to bind the server, can be a Unix domain socket on supported platforms. */
  val address: SocketAddress = InetSocketAddress(DEFAULT_HTTP3_HOST, DEFAULT_HTTP3_PORT),
  /** Whether other services should advertise HTTP/3 support using an `alt-svc` header. */
  val advertise: Boolean = true,
) {
  public companion object {
    public const val DEFAULT_HTTP3_HOST: String = "localhost"
    public const val DEFAULT_HTTP3_PORT: Int = 8443
  }
}

/**
 * Describes a source for SSL certificates used to secure an HTTP server.
 *
 * [File] sources should be preferred since they can provide real certificates used in production; during development
 * or testing, in-memory [SelfSigned] certificates can be used instead for convenience.
 */
public sealed interface CertificateSource {
  /**
   * Describes a source for **insecure**, in-memory, self-signed certificates, intended for development and testing
   * purposes. The certificate can be configured with a custom [subject] and validity window.
   */
  public data class SelfSigned(
    /** Subject of the certificate, also added as an SAN DNS entry. */
    val subject: String = DEFAULT_SUBJECT,
    /** Sets the instant the certificate's validity period starts. Defaults to a minute before [Instant.now]. */
    val notBefore: Instant = Instant.now().minusSeconds(DEFAULT_START_OFFSET_SECONDS),
    /** Sets the instant the certificate's validity period ends. Defaults to a year after [Instant.now]. */
    val notAfter: Instant = Instant.now().plusSeconds(DEFAULT_END_OFFSET_SECONDS),
  ) : CertificateSource {
    public companion object {
      public const val DEFAULT_SUBJECT: String = "localhost"
      public const val DEFAULT_START_OFFSET_SECONDS: Long = 3_600 // a minute ago
      public const val DEFAULT_END_OFFSET_SECONDS: Long = 365 * 24 * 3600 // a year from now
    }
  }

  /**
   * Describes a set of files providing an [X.509 certificate chain][certFile] and a [PKCS#8 private key][keyFile] in
   * PEM format. A [password][keyPassphrase] may be specified if the [key file][keyFile] is password-protected.
   *
   * File sources should always be used over [SelfSigned] certificates for uses other than development or testing.
   */
  public data class File(
    /** Path to a file containing an X.509 certificate chain in PEM format. */
    val certFile: Path,
    /** Path to a file containing a PKCS#8 private key in PEM format. */
    val keyFile: Path,
    /** Password required by the [keyFile], may be `null` if the key is not password-protected. */
    val keyPassphrase: String? = null,
  ) : CertificateSource
}

/** Build a self-signed [X509Bundle] using the configuration from this source, for development or test servers. */
public fun CertificateSource.SelfSigned.buildSelfSignedBundle(): X509Bundle {
  return CertificateBuilder()
    .subject("CN=$subject")
    .addSanDnsName(subject)
    .notBefore(notBefore)
    .notAfter(notAfter)
    .ecp256()
    .setIsCertificateAuthority(true)
    .buildSelfSigned()
}

package elide.runtime.gvm.internals.intrinsics.js.url

import elide.vm.annotations.Polyglot
import elide.runtime.gvm.internals.intrinsics.GuestIntrinsic
import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.internals.intrinsics.js.JsError.jsErrors
import elide.runtime.gvm.internals.intrinsics.js.JsError.typeError
import elide.runtime.gvm.internals.intrinsics.js.JsError.valueError
import elide.runtime.gvm.internals.intrinsics.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.intrinsics.js.Blob
import elide.runtime.intrinsics.js.File
import elide.runtime.intrinsics.js.URL
import elide.runtime.intrinsics.js.URLSearchParams
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.err.ValueError
import org.graalvm.polyglot.proxy.ProxyInstantiable
import java.io.Serializable
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.*
import org.graalvm.polyglot.Value as GuestValue
import java.net.URI as NativeURL

/** Implements an intrinsic for the `URL` global defined by the WhatWG URL Specification. */
@Intrinsic internal class URLIntrinsic : AbstractJsIntrinsic() {
  internal companion object {
    /** Global where the `URL` constructor is mounted. */
    private const val GLOBAL_URL = "URL"

    // `URL` class symbol.
    private val URL_SYMBOL = GLOBAL_URL.asJsSymbol()

    // Resolve a known protocol for the provided URI, or `null`.
    @JvmStatic private fun knownProtocol(target: NativeURL): KnownProtocol? = when (val scheme = target.scheme) {
      // special case: protocol-relative URLs
      null -> KnownProtocol.RELATIVE
      else -> KnownProtocol.values().firstOrNull { it.scheme == scheme }
    }

    // Resolve a known protocol for the provided URI, or `null`.
    @JvmStatic private fun knownProtocol(port: Int): KnownProtocol? = KnownProtocol.values().find {
      it.port != -1 && it.port == port
    }

    // Wrap the provided [initialValue] (if any) and deferred [processor] with a cached parse-able value.
    @JvmStatic private fun <T: Serializable> cachedParse(initialValue: T? = null, processor: () -> T): CachedURLValue<T>
      = CachedURLValue(initialValue, processor)

    // Check whether a port is in the valid range of all ports.
    @JvmStatic private fun validPort(port: Int) = port in 1..65535
  }

  /** Abstract internal base for URL types. */
  internal interface BaseURLType : elide.runtime.intrinsics.js.MutableURL {
    /** @return Native URL managed by this URL object. Not available from guest code. */
    fun wrappedURL(): NativeURL

    /** @return Parsed URL managed by this URL object. Not available from guest code. */
    fun parsedURL(): ParsedURL

    /** @return Lock a URL as immutable. */
    fun lock(): URL

    /** @return Indication of whether this URL is mutable. */
    fun isMutable(): Boolean
  }

  /**
   * Enumerates protocols recognized by the URL intrinsic, paired with standard ports, schemes, and host support.
   *
   * @param scheme URI scheme expected for this protocol.
   * @param port Standard port for this protocol, or `-1` if ports are not relevant. Defaults to `-1`.
   * @param hasHost Set to `true` if hosts are relevant to this protocol. Defaults to `true` if `port` is not `-1`.
   */
  internal enum class KnownProtocol (
    val scheme: String = "",
    val port: Int = -1,
    val hasHost: Boolean = port != -1,
  ) {
    /** Special case: protocol-relative URLs. */
    RELATIVE,

    /** Protocol: HTTP. */
    HTTP(scheme = "http", port = 80),

    /** Protocol: HTTPS (HTTP+TLS). */
    HTTPS(scheme = "https", port = 443),

    /** Protocol: FTP. */
    FTP(scheme = "ftp", port = 21),

    /** Protocol: Files. */
    FILE(scheme = "file"),

    /** Protocol: Blobs. */
    BLOB(scheme = "blob"),
  }

  /** Wrapper class which enables lazy processing of parsed URL values. */
  internal class CachedURLValue<T: Serializable> (
    initialValue: T?,
    private val processor: (() -> T)? = null,
  ) {
    // Value held by this container.
    private val value: AtomicReference<T> = AtomicReference(initialValue)

    // Whether the container has been filled yet.
    private val initialized: AtomicBoolean = AtomicBoolean(initialValue != null)

    /** @return Processed value `T`, or lazily processed value `T`. */
    fun resolve(): T {
      if (!initialized.getAndSet(true)) {
        value.set((processor!!).invoke())
      }
      return value.get()
    }

    companion object {
      /** @return Wrapped [value] [T], which we happen to have on-hand (skipping the lazy call). */
      @JvmStatic fun <T: Serializable> of(value: T): CachedURLValue<T> = CachedURLValue(value)
    }
  }

  /**
   * Internal representation of a parsed URL; the structure is expressed as a data class to facilitate easy immutable
   * copy operations.
   *
   * Properties which are needed for internal operations (and, as such, are preloaded upon URL construction) are held in
   * this data class, and the copy semantics linked to those are enforced.
   *
   * @param uri Parsed URI representation of this URL.
   * @param absolute Cached access to an absolute string version of this URL.
   * @param knownProtocol Known protocol detected for this URL, if any.
   * @param protocol Parsed spec-compliant protocol value for this URL. Always present.
   * @param port Port calculated for this URI. If no port is applicable, or the port is standard, this is `-1`.
   * @param host Host calculated for this URL. If no host is applicable, this is an empty string.
   * @param hostname Hostname calculated for this URL. If no host is applicable, this is an empty string.
   * @param pathname Path-name (spec-compliant) calculated for this URL. This field is never empty (`/` is default).
   * @param search Search string (query parameters) for this URL. If not applicable, this is an empty string.
   * @param hash Fragment, or "hash", portion of the URL. If no hash is applicable, this is an empty string.
   * @param username Username value parsed from the URL. If none is present, then this is an empty string.
   * @param password Password value parsed from the URL. If none is present, or no [username] is present, then this
   *   value is an empty string.
   */
  internal data class ParsedURL(
    val uri: NativeURL,
    val absolute: String,
    val knownProtocol: KnownProtocol? = knownProtocol(uri),
    val protocol: String = computeProtocol(uri, knownProtocol),
    val port: Int? = computePort(uri),
    val host: String = computeHost(uri, port, knownProtocol),
    val hostname: CachedURLValue<String> = computeHostname(uri, knownProtocol),
    val pathname: CachedURLValue<String> = computePathname(uri, knownProtocol),
    val search: CachedURLValue<String> = computeSearch(uri),
    val searchParams: CachedURLValue<URLSearchParams> = computeSearchParams(uri, knownProtocol),
    val hash: CachedURLValue<String> = computeHash(uri),
    val username: CachedURLValue<String> = computeUsername(uri),
    val password: CachedURLValue<String> = computePassword(uri),
    private val hashCode: Int = computeHashCode(
      uri,
      knownProtocol,
      protocol,
      port,
      host,
    )
  ) {
    companion object {
      // Check input before parsing a URL.
      private fun checkParseInput(input: String): String = input.apply {
        require(!contains("％００") && !contains("%ef%bc%85%ef%bc%90%ef%bc%90")) {
          "Cannot create URL with null escape sequence"
        }
      }

      // Sanitize a URL string, removing any invalid characters, before parsing.
      private fun sanitize(url: String): String = checkParseInput(url).replace(
        Regex("[\t\n\r]"),
        ""
      )

      // Parse the provided URL, translating any errors into the expected error types.
      private fun parseUrl(url: String): NativeURL = jsErrors {
        NativeURL.create(sanitize(url))
      }

      // Find the host name for a protocol-relative URL.
      @JvmStatic private fun hostForProtocolRelative(uri: NativeURL) = uri.toString()
        .drop(2)  // trim `//` prefix
        .substringBefore("/")

      // Use an already-parsed native URI type, pre-initializing anything else we need.
      @JvmStatic fun fromURL(url: NativeURL): ParsedURL = ParsedURL(
        uri = url,
        absolute = url.toString(),
      )

      // Parse a URL and wrap it in a `ParsedURL` object, pre-initializing anything else we need.
      @JvmStatic fun fromString(string: String): ParsedURL = parseUrl(string).let { parsed ->
        ParsedURL(uri = parsed, absolute = parsed.toString())
      }

      // Calculate a spec-compliant value for the `protocol` property.
      @JvmStatic private fun computeProtocol(uri: NativeURL, proto: KnownProtocol?): String {
        if (proto == KnownProtocol.RELATIVE)
          return ""
        return "${proto?.scheme ?: uri.scheme}:"
      }

      // Calculate a spec-compliant value for the `port` property.
      @JvmStatic private fun computePort(uri: NativeURL): Int? {
        val uriPort = uri.port
        return (if (uriPort == -1) {
          // ports are not applicable to this type of URL
          null
        } else uriPort)
      }

      // Calculate a spec-compliant value for the `host` property.
      @JvmStatic private fun computeHost(uri: NativeURL, port: Int?, proto: KnownProtocol?): String = when {
        // protocol-relative URLs need special consideration, `URI` mis-parses them
        proto == KnownProtocol.RELATIVE -> hostForProtocolRelative(uri)

        // if the protocol is known and expected to have a host, make sure we return something reasonable no matter what
        proto == null || proto.hasHost -> {
          if (port != null) {
            // there is a port declared, and the host is not using it, so we should include it.
            "${uri.host}:$port"
          } else {
            // we require a host, but we can't safely figure out the port, so return it directly.
            uri.host ?: ""
          }
        }

        // otherwise, the host value is not demanded by the protocol, and we can't safely find one, so we return the
        // empty string by spec (this is the case for non-network URLs, such as file paths and blobs).
        else -> ""
      }

      // Calculate a spec-compliant value for the `hostname` property.
      @JvmStatic private fun computeHostname(uri: NativeURL, proto: KnownProtocol?) = cachedParse {
        when {
          // special case: protocol-relative URIs.
          proto == KnownProtocol.RELATIVE -> hostForProtocolRelative(uri)

          // the URI uses a recognized protocol and does not expect a hostname. return the empty string. this covers
          // cases like file paths and blobs.
          proto?.hasHost == false -> ""

          // otherwise, we should return the host directly.
          else -> uri.host
        }
      }

      // Calculate a spec-compliant value for the `pathname` property.
      @JvmStatic private fun computePathname(uri: NativeURL, proto: KnownProtocol?) = cachedParse {
        if (proto?.hasHost != false) {
          val path = uri.path
          if (path == null || path.isEmpty() || path.isBlank()) {
            "/"
          } else {
            path
          }
        } else {
          // for protocols like `file:` and `blob:`, we need to drop the protocol manually.
          var portion = uri.toString()
          var scanZero = false

          // remove protocol/scheme and separator, so that we are left with `<hostname>/<path>...`
          portion = if (portion.startsWith("//")) {
            // special case: drop prefix for protocol-relative URLs
            portion.drop(2)
          } else {
            val trimmed = portion.substringAfter("://")
            if (proto == KnownProtocol.FILE && !trimmed.startsWith(".") && !trimmed.startsWith("/")) {
              // correct for file paths
              "/$trimmed"
            } else {
              scanZero = true  // `blob:` paths do not start with a `/`, but we need that portion as the pathname
              trimmed
            }
          }

          // find the beginning position of the path, and the beginning position of the fragment/query, as applicable,
          // so that we may strip them out.
          val positionPath = if (scanZero) 0 else portion.indexOf('/')
          val positionHash = portion.indexOf('#')
          val positionQuery = if (positionHash != -1) -1 else portion.indexOf('?')

          // drop protocol
          if (positionPath != -1) {
            when {
              // if the URL has a hash portion, by standard, it is after the query portion, so trimming that should trim
              // the query portion, if any, as well.
              positionHash != -1 -> portion.substring(positionPath, positionHash)

              // if the URL has a query portion, but no hash portion, we should make sure to slice that off. the path
              // name property does not contain the query string.
              positionQuery != -1 -> portion.substring(positionPath, positionQuery)

              // if the path begins at `0`, as it will at this point for
              positionPath == 0 -> portion

              // otherwise, the URL has neither a query nor a fragment, so we can simply slice off any prefix characters
              // which are not part of the path.
              else -> portion.substring(positionPath)
            }
          } else {
            "/"
          }
        }
      }

      // Calculate a spec-compliant value for the `search` property.
      @JvmStatic private fun computeSearch(uri: NativeURL) = cachedParse {
        val query = uri.query
        if (query.isNullOrBlank()) {
          ""
        } else {
          "?$query"
        }
      }

      // Calculate a spec-compliant value for the `searchParams` property.
      @Suppress("UNUSED_PARAMETER")
      @JvmStatic private fun computeSearchParams(uri: NativeURL, proto: KnownProtocol?) = cachedParse<URLSearchParams> {
        TODO("not yet implemented")
      }

      // Calculate a spec-compliant value for the `host` property.
      @JvmStatic private fun computeHash(uri: NativeURL) = cachedParse {
        val hash = uri.fragment
        if (hash.isNullOrBlank()) {
          ""
        } else {
          "#$hash"
        }
      }

      // Calculate a spec-compliant value for the `username` property.
      @JvmStatic private fun computeUsername(uri: NativeURL) = cachedParse {
        val userinfo = uri.userInfo
        if (userinfo.isNullOrBlank()) {
          ""
        } else {
          userinfo.split(":").first()
        }
      }

      // Calculate a spec-compliant value for the `password` property.
      @JvmStatic private fun computePassword(uri: NativeURL) = cachedParse {
        val username = computeUsername(uri).resolve()
        if (username.isBlank()) {
          ""
        } else {
          val userinfo = uri.userInfo
          if (userinfo.contains(":")) {
            userinfo.split(":").last()
          } else {
            ""
          }
        }
      }

      // Normalize a URI path value by ensuring it is never null or empty (`/` is the default).
      private fun normalizedPath(path: String?): String = if (path.isNullOrBlank()) "/" else path

      // Normalize a URI fragment value by ensuring it is never null, but only empty or beginning with a `#`. It should
      // never just be the `#` character (this is considered an empty fragment, which should be an empty string).
      private fun normalizedFragment(fragment: String?): String = when {
        fragment.isNullOrBlank() || fragment == "#" -> ""
        fragment.startsWith("#") -> fragment
        else -> "#$fragment"
      }

      // Normalize a URI query value by ensuring it is never null, but only empty or beginning with a `?`. It should
      // never just be the `?` character (this is considered an empty query, which should be an empty string).
      private fun normalizedQuery(query: String?): String = when {
        query.isNullOrBlank() || query == "?" -> ""
        query.startsWith("?") -> query
        else -> "?$query"
      }

      // Normalize a URI user-info value by ensuring it is never null, but only empty, or a fully-specified username and
      // password pair, separated by a single `:` character. It should never just be the `:` character (this is
      // considered an empty username, which yields an empty password, rendering the entire value an empty string). It
      // should also never be a password value without a username, as this is illegal by spec.
      private fun normalizedUsername(userinfo: String?): String = when {
        userinfo.isNullOrBlank() || userinfo == ":" || userinfo.startsWith(":") -> ""
        !userinfo.contains(":") -> ""
        else -> userinfo.substringBefore(":")
      }

      // Normalize a URI user-info password value by ensuring it is never null, but only empty, or a fully-specified
      // username and password pair, separated by a single `:` character. It should never just be the `:` character
      // (this is  considered an empty username, which yields an empty password, rendering the entire value an empty
      // string). It should also never be a password value without a username, as this is illegal by spec.
      private fun normalizedPassword(userinfo: String?): String = when {
        userinfo.isNullOrBlank() || userinfo == ":" || userinfo.startsWith(":") -> ""
        !userinfo.contains(":") -> ""
        else -> userinfo.substringAfter(":")
      }

      // Pre-calculate a URL comparison and storage hashcode.
      @JvmStatic private fun computeHashCode(
        uri: NativeURL,
        knownProtocol: KnownProtocol?,
        protocol: String,
        port: Int?,
        host: String,
      ): Int {
        val path = uri.path
        val frag = uri.fragment
        val query = uri.query
        val userinfo = uri.userInfo
        var result = knownProtocol.hashCode()
        result = 31 * result + protocol.hashCode()
        result = 31 * result + host.hashCode()
        result = 31 * result + (port ?: -1)
        result = 31 * result + normalizedPath(path).hashCode()
        result = 31 * result + normalizedFragment(frag).hashCode()
        result = 31 * result + normalizedQuery(query).hashCode()
        result = 31 * result + normalizedUsername(userinfo).hashCode()
        result = 31 * result + normalizedPassword(userinfo).hashCode()
        return result
      }
    }

    /** @inheritDoc */
    override fun equals(other: Any?): Boolean {
      if (other?.javaClass != this.javaClass)
        return false
      return when {
        this === other -> true  // if they are the exact same object, they are equal
        hashCode() != other.hashCode() -> false  // comparison hashcode differs
        else -> true  // they are functionally the same
      }
    }

    /** @inheritDoc */
    override fun hashCode(): Int = hashCode

    /** @inheritDoc */
    fun absoluteString(): String = absolute

    // Splice a new protocol into the current URL, and return it re-wrapped as a parsed URL.
    private fun copySpliceProtocol(protocol: String): ParsedURL = when {
      protocol.endsWith(":") -> protocol.dropLast(1)
      protocol.endsWith("://") -> protocol.dropLast(3)
      else -> protocol
    }.let { cleanedProtocol ->
      "$cleanedProtocol:".let { composedProtocol ->
        if (cleanedProtocol.isNotEmpty()) {
          if (composedProtocol == this.protocol) {
            this  // special case: the protocol change is a no-op, because they are already equal.
          } else {
            require(cleanedProtocol.isNotBlank()) {
              "Protocol cannot be blank"
            }
            val spliced = if (absolute.startsWith("//")) {
              // special case: protocol-relative URLs, transitioning to protocol-absolute URLs
              "${cleanedProtocol}:$absolute"
            } else {
              "${cleanedProtocol}://${absolute.substringAfter("://")}"
            }
            val reparsed = parseUrl(spliced)
            val knownProto = knownProtocol(reparsed)
            val splicedPort = computePort(reparsed)

            ParsedURL(
              uri = reparsed,
              absolute = spliced,
              port = splicedPort,
              knownProtocol = knownProto,
              host = computeHost(reparsed, splicedPort, knownProto),
              protocol = composedProtocol,  // can reuse
              hostname = this.hostname,  // no change
              pathname = this.pathname,  // no change
              search = this.search,  // no change
              searchParams = this.searchParams,  // no change
              hash = this.hash,  // no change
              username = this.username,  // no change
              password = this.password,  // no change
            )
          }
        } else {
          if (absolute.startsWith("//")) {
            this  // already protocol relative: nothing to do
          } else {
            // protocol relative
            fromString("//${absolute.substringAfter("://")}")
          }
        }
      }
    }

    // Splice a new host into the current URL, and return it re-wrapped as a parsed URL.
    private fun copySplicePort(port: Int): ParsedURL = if (validPort(port)) {
      if (port == this.port) {
        this  // special case: port matches, change is a no-op
      } else {
        val knownProto = knownProtocol(port)
        val reassembled = NativeURL(
          uri.scheme,
          uri.userInfo,
          uri.host,
          port,
          uri.path,
          uri.query,
          uri.fragment,
        )

        // recompute the rest of the parsed URL
        val splicedPort = computePort(reassembled)
        ParsedURL(
          uri = reassembled,
          absolute = reassembled.toString(),
          port = splicedPort,
          knownProtocol = knownProto,
          host = computeHost(reassembled, splicedPort, knownProto),
          protocol = this.protocol,  // no change
          hostname = this.hostname,  // no change
          pathname = this.pathname,  // no change
          search = this.search,  // no change
          searchParams = this.searchParams,  // no change
          hash = this.hash,  // no change
          username = this.username,  // no change
          password = this.password,  // no change
        )
      }
    } else throw valueError(
      // port number was not in allowed range
      "Invalid port number: $port (not between 1 and 65535)"
    )

    // Splice a new host into the current URL, and return it re-wrapped as a parsed URL.
    private fun copySpliceHost(host: String): ParsedURL = when {
      host == this.host -> this  // special case: host matches, change is a no-op
      host.isEmpty() -> throw valueError("Host cannot be empty")
      host.isBlank() -> throw valueError("Host cannot be blank")
      else -> {
        val colonCount = host.count { it == ':' }
        val reassembled = if (colonCount > 0) {
          if (colonCount > 1)
            throw valueError("Cannot parse port: Too many colons in: '$host'")

          // potentially includes a port
          if (host.endsWith(":"))
            throw valueError("Host port cannot be empty if specified: '$host'")
          val hostPort = host.substringAfterLast(":").toIntOrNull() ?:
            throw valueError("Failed to parse port value: '$host'")
          if (!validPort(hostPort))
            throw valueError("Invalid port number: $hostPort (not between 1 and 65535)")
          val hostName = host.substringBefore(":")

          // assemble with port
          NativeURL(
            uri.scheme,
            uri.userInfo,
            hostName,
            hostPort,
            uri.path,
            uri.query,
            uri.fragment,
          )

        } else {
          // no port: just splice in the host as a name
          NativeURL(
            uri.scheme,
            uri.userInfo,
            host,
            uri.port,
            uri.path,
            uri.query,
            uri.fragment,
          )
        }

        // recompute the rest of the parsed URL
        val knownProto = knownProtocol(reassembled)
        val splicedPort = computePort(reassembled)
        ParsedURL(
          uri = reassembled,
          absolute = reassembled.toString(),
          port = splicedPort,
          host = computeHost(reassembled, splicedPort, knownProto),
          knownProtocol = knownProto,
          protocol = this.protocol,  // no change
          pathname = this.pathname,  // no change
          search = this.search,  // no change
          searchParams = this.searchParams,  // no change
          hash = this.hash,  // no change
          username = this.username,  // no change
          password = this.password,  // no change
          // `hostname` omitted
        )
      }
    }

    // Splice a new hostname (no port) into the current URL, and return it re-wrapped as a parsed URL.
    private fun copySpliceHostname(hostname: String): ParsedURL = when {
      hostname == this.hostname.resolve() -> this  // special case: hostname matches, change is a no-op
      hostname.isEmpty() -> throw valueError("Hostname cannot be empty")
      hostname.isBlank() -> throw valueError("Hostname cannot be blank")
      hostname.contains("://") -> throw valueError("Hostname cannot contain a scheme: use `href` instead")
      hostname.contains(":") -> throw valueError("Hostname cannot include port: use `host` instead")

      else -> try {
        NativeURL(
          uri.scheme,
          uri.userInfo,
          hostname,
          uri.port,
          uri.path,
          uri.query,
          uri.fragment,
        )
      } catch (e: java.net.URISyntaxException) {
        throw valueError("Invalid hostname: '$hostname'")
      }.let { reassembled ->
        // recompute the rest of the parsed URL
        ParsedURL(
          uri = reassembled,
          absolute = reassembled.toString(),
          hostname = CachedURLValue.of(hostname),  // can re-use value
          knownProtocol = this.knownProtocol,  // no change
          protocol = this.protocol,  // no change
          port = this.port,  // no change
          pathname = this.pathname,  // no change
          search = this.search,  // no change
          searchParams = this.searchParams,  // no change
          hash = this.hash,  // no change
          username = this.username,  // no change
          password = this.password,  // no change
          // `host` omitted
        )
      }
    }

    // Splice a new path-name into the current URL, and return it re-wrapped as a parsed URL.
    private fun copySplicePathname(pathname: String): ParsedURL = when (pathname) {
      // special case: pathname matches, change is a no-op
      this.pathname.resolve() -> this

      else -> try {
        // if the pathname is specified, but non-blank and non-empty, that's an error: it must start with a slash. this
        // case happens to cover completely blank URLs, which is why we skip that check here.
        if (pathname.isNotEmpty() && !pathname.startsWith("/"))
          throw java.net.URISyntaxException(pathname, "Path-name should start with '/' (got: '$pathname')")

        NativeURL(
          uri.scheme,
          uri.userInfo,
          uri.host,
          uri.port,
          pathname,
          uri.query,
          uri.fragment,
        )
      } catch (e: java.net.URISyntaxException) {
        throw valueError("Invalid path-name: '$pathname'")
      }.let { reassembled ->
        // recompute the rest of the parsed URL
        ParsedURL(
          uri = reassembled,
          absolute = reassembled.toString(),
          pathname = CachedURLValue.of(pathname),  // can re-use value
          knownProtocol = this.knownProtocol,
          host = this.host,
          hostname = this.hostname,
          port = this.port,
          protocol = this.protocol,  // no change
          search = this.search,  // no change
          searchParams = this.searchParams,  // no change
          hash = this.hash,  // no change
          username = this.username,  // no change
          password = this.password,  // no change
        )
      }
    }

    // Splice a new query value into the current URL, and return it re-wrapped as a parsed URL.
    private fun copySpliceSearch(query: String): ParsedURL = if (query.isNotEmpty() && query.startsWith("?")) {
      val dropped = query.drop(1)
      dropped
    } else {
      query
    }.let { cleanedQuery ->
      "?$cleanedQuery".let { composedQuery ->
        if (composedQuery == this.search.resolve()) {
          this  // special case: pathname matches, change is a no-op
        } else {
          try {
            if (cleanedQuery.isNotEmpty() && cleanedQuery.isBlank())
              throw java.net.URISyntaxException(query, "Cannot set blank query-string value")
            if (cleanedQuery.contains("?"))
              throw java.net.URISyntaxException(query, "Cannot contain invalid character '?'")
            if (cleanedQuery.contains("\n"))
              throw java.net.URISyntaxException(query, "Cannot contain invalid character (newline)")
            val transformedPath = if (cleanedQuery.isNotBlank() && uri.path.isNullOrBlank()) {
              // special case: if there is no path, make sure we put a `/` slash in there to be pedantic
              "/"
            } else if (cleanedQuery == "" && uri.path == "/" && uri.fragment.isNullOrEmpty()) {
              // special case: if there is a path, but the query is blank, remove the path for a clean URL, since the
              // root slash is implied with no other path characters.
              ""
            } else uri.path

            NativeURL(
              uri.scheme,
              uri.userInfo,
              uri.host,
              uri.port,
              transformedPath,
              cleanedQuery.ifEmpty { null },
              uri.fragment,
            )
          } catch (e: java.net.URISyntaxException) {
            throw valueError("Invalid query string: '$query'")
          }.let { reassembled ->
            ParsedURL(
              uri = reassembled,
              absolute = reassembled.toString(),
              search = CachedURLValue.of(composedQuery),  // can re-use value
              knownProtocol = this.knownProtocol,  // no change
              port = this.port,  // no change
              protocol = this.protocol,  // no change
              host = this.host,  // no change
              hostname = this.hostname,  // no change
              pathname = this.pathname,  // no change
              hash = this.hash,  // no change
              username = this.username,  // no change
              password = this.password,  // no change
              // `searchParams` omitted to trigger re-calculation
            )
          }
        }
      }
    }

    // Splice a new fragment value into the current URL, and return it re-wrapped as a parsed URL.
    private fun copySpliceHash(fragment: String): ParsedURL = when {
      // drop any fragment prefix
      fragment.startsWith("#") -> fragment.drop(1)

      // otherwise, pass it along unmodified
      else -> fragment
    }.let { cleanedFragment ->
      "#$cleanedFragment".let { composedFragment ->
        if (composedFragment == this.hash.resolve()) {
          this  // special case: update is a no-op
        } else try {
          if (fragment.contains("?"))
            throw java.net.URISyntaxException(fragment, "Cannot contain invalid characters")
          val transformedPath = if (cleanedFragment.isNotBlank() && uri.path.isNullOrBlank()) {
            // special case: if there is no path, make sure we put a `/` slash in there to be pedantic
            "/"
          } else if (cleanedFragment.isBlank() && uri.path == "/" && uri.query.isNullOrBlank()) {
            // special case: if there is no path, and the fragment is blank, remove the path for a clean URL, since the
            // root slash is implied with no other path characters.
            ""
          } else uri.path

          NativeURL(
            uri.scheme,
            uri.userInfo,
            uri.host,
            uri.port,
            transformedPath,
            uri.query,
            cleanedFragment.ifBlank { null },
          )
        } catch (e: java.net.URISyntaxException) {
          throw valueError("Invalid fragment: '$fragment'")
        }.let { reassembled ->
          ParsedURL(
            uri = reassembled,
            absolute = reassembled.toString(),
            hash = CachedURLValue.of(if (cleanedFragment.isBlank()) "" else composedFragment),
            knownProtocol = this.knownProtocol,  // no change
            port = this.port,  // no change
            protocol = this.protocol,  // no change
            host = this.host,  // no change
            search = this.search,  // no change
            searchParams = this.searchParams,  // no change
            hostname = this.hostname,  // no change
            pathname = this.pathname,  // no change
            username = this.username,  // no change
            password = this.password,  // no change
          )
        }
      }
    }

    // Splice a new username value into the current URL, and return it re-wrapped as a parsed URL.
    private fun copySpliceUsername(username: String): ParsedURL = when {
      username == this.username.resolve() -> this  // special case: update is a no-op
      username.isNotEmpty() && username.isBlank() ->
        throw valueError("Cannot set `URL.username` to blank value")
      else -> try {
        val desiredUser = if (username.isBlank()) {
          // if the username is blank, we're clearing the value; if the username is being cleared, the password needs to
          // be cleared, too.
          null
        } else if (username.contains(":") || username.contains("\n")) {
          throw java.net.URISyntaxException(username, "Cannot contain invalid characters")
        } else username

        NativeURL(
          uri.scheme,
          desiredUser,
          uri.host,
          uri.port,
          uri.path,
          uri.query,
          uri.fragment,
        )
      } catch (syntaxErr: java.net.URISyntaxException) {
        throw valueError("Invalid username: '$username'")
      }.let { reassembled ->
        ParsedURL(
          uri = reassembled,
          absolute = reassembled.toString(),
          username = CachedURLValue.of(username),
          hash = this.hash,
          knownProtocol = this.knownProtocol,  // no change
          port = this.port,  // no change
          protocol = this.protocol,  // no change
          host = this.host,  // no change
          search = this.search,  // no change
          searchParams = this.searchParams,  // no change
          hostname = this.hostname,  // no change
          pathname = this.pathname,  // no change

          // corner case: clear the password forcibly if the username is cleared
          password = if (username.isBlank()) {
            CachedURLValue.of("")
          } else {
            this.password
          }
        )
      }
    }

    // Splice a new username and password value into the current URL, and return it re-wrapped as a parsed URL.
    private fun copySplicePassword(username: String, password: String): ParsedURL = when (password) {
      this.password.resolve() -> this  // special case: update is a no-op
      else -> try {
        if (password.contains("\n"))
          throw java.net.URISyntaxException(password, "Cannot contain invalid characters")
        val desiredUserInfo = if (password.isBlank()) {
          username
        } else {
          "$username:$password"
        }

        NativeURL(
          uri.scheme,
          desiredUserInfo,
          uri.host,
          uri.port,
          uri.path,
          uri.query,
          uri.fragment,
        )
      } catch (syntaxErr: java.net.URISyntaxException) {
        throw valueError("Invalid password: '$username'")
      }.let { reassembled ->
        ParsedURL(
          uri = reassembled,
          absolute = reassembled.toString(),
          username = CachedURLValue.of(username),
          password = CachedURLValue.of(password),
          hash = this.hash,
          knownProtocol = this.knownProtocol,  // no change
          port = this.port,  // no change
          protocol = this.protocol,  // no change
          host = this.host,  // no change
          search = this.search,  // no change
          searchParams = this.searchParams,  // no change
          hostname = this.hostname,  // no change
          pathname = this.pathname,  // no change
        )
      }
    }

    /**
     * Internal function to conduct a splice-and-copy operation, where the provided parameters are validated, then
     * spliced into a copy of the current [ParsedURL], with relevant recalculated values being re-constructed, copied,
     * or re-computed, as applicable.
     *
     * This method is the only place where mutability of a [ParsedURL] is allowed. While [copy] is technically available
     * within the `internal` scope, it is not used, aside from native internal copying of URL objects.
     *
     * @param protocol Protocol to replace in the current URL, if present. Defaults to `null`, in which case the current
     *   [protocol] value is used without modification.
     * @param port Port to replace in the current URL, if present. Defaults to `null`, in which case the current [port]
     *   value is used without modification.
     * @param host Host to replace in the current URL, if present. Defaults to `null`, in which case the current [host]
     *   value is used without modification.
     * @param pathname Path to replace in the current URL, if present. Defaults to `null`, in which case the current
     *   [pathname] value is used without modification.
     * @param search Query to replace in the current URL, if present. Defaults to `null`, in which case the current
     *   [search] value is used without modification.
     * @param hash Fragment to replace in the current URL, if present. Defaults to `null`, in which case the current
     *   [hash] value is used without modification.
     * @param username Username to replace in the current URL, if present. Defaults to `null`, in which case the current
     *   [username] value is used without modification.
     * @param password Password to replace in the current URL, if present. Defaults to `null`, in which case the current
     *   [password] value is used without modification.
     * @return Copy of the current [ParsedURL], but with the provided spliced-in parameters.
     * @throws ValueError if any of the provided values do not constitute a valid URL component, for their respective
     *   variable assignment.
     */
    fun copySplice(
      protocol: String? = null,
      port: Int? = null,
      host: String? = null,
      hostname: String? = null,
      pathname: String? = null,
      search: String? = null,
      hash: String? = null,
      username: String? = null,
      password: String? = null,
    ) : ParsedURL = jsErrors {
      when {
        // mutable field update: `protocol`
        protocol != null -> copySpliceProtocol(protocol)

        // mutable field update: `port`
        port != null -> copySplicePort(port)

        // mutable field update: `host`
        host != null -> copySpliceHost(host)

        // mutable field update: `hostname`
        hostname != null -> copySpliceHostname(hostname)

        // mutable field update: `pathname`
        pathname != null -> copySplicePathname(pathname)

        // mutable field update: `search`
        search != null -> copySpliceSearch(search)

        // mutable field update: `hash`
        hash != null -> copySpliceHash(hash)

        // mutable field update: `password`
        password != null -> copySplicePassword(username = this.username.resolve(), password = password)

        // mutable field update: `username`
        username != null -> copySpliceUsername(username)

        else -> error("Invalid state: Could not determine changes for URL object. Please report this bug.")
      }
    }
  }

  /** URL value class implementation. */
  internal class URLValue private constructor (private val target: AtomicReference<ParsedURL>) :
    Comparable<URLValue>,
    BaseURLType {
    /** `URL` value factory. */
    internal companion object Factory : URL.URLConstructors, URL.URLStaticMethods {
      // -- Factories: Java -- //

      /** @return Wrapped intrinsic URL from a regular Java URL. */
      @JvmStatic fun fromURL(url: NativeURL): URLValue = URLValue(AtomicReference(ParsedURL.fromURL(url)))

      /** @return Wrapped intrinsic URL from a regular Java URL. */
      @JvmStatic fun fromURL(url: URLValue): URLValue = URLValue(url)

      /** @return Wrapped intrinsic URL from a regular Java URL. */
      @JvmStatic fun fromString(url: String): URLValue = URLValue(AtomicReference(ParsedURL.fromString(url)))

      // -- Factories: JS -- //

      /** @inheritDoc */
      @JvmStatic @Polyglot override fun create(url: URL): URL =
        fromURL(url as URLValue)

      /** @inheritDoc */
      @JvmStatic @Polyglot override fun create(url: String): URL =
        fromString(url)

      /** @inheritDoc */
      @JvmStatic @Polyglot override fun create(url: String, base: String): URL =
        fromString(url)  // @TODO(sgammon): relative URLs

      /** @inheritDoc */
      @JvmStatic @Polyglot override fun createObjectURL(file: File): URL =
        error("Not implemented: createObjectURL")

      /** @inheritDoc */
      @JvmStatic @Polyglot override fun createObjectURL(blob: Blob): URL =
        error("Not implemented: createObjectURL")

      /** @inheritDoc */
      @JvmStatic @Polyglot override fun revokeObjectURL(url: URL) =
        error("Not implemented: createObjectURL")

      // -- Internals -- //

      // Shortcut to parse a string as a URL (used internally only).
      @JvmStatic private fun parseString(url: String): AtomicReference<ParsedURL> =
        if (url.isNotEmpty() && url.isNotBlank()) {
          if (!url.startsWith("//") && url.startsWith("/"))
            throw valueError("Invalid URL: Relative URLs are not supported")
          AtomicReference(ParsedURL.fromString(url))
        } else throw valueError(
          "Cannot construct URL from empty string value"
        )

      // Relative entrypoint for string parsing (with base URL).
      @Suppress("UNUSED_PARAMETER")
      @JvmStatic private fun parseString(url: GuestValue, base: Any?): AtomicReference<ParsedURL> =
        parseString(url.asString())  // @TODO(sgammon): base URL support during parsing

      // Shortcut for parsing a constructor guest value.
      @JvmStatic private fun constructFromGuestValue(
        target: GuestValue,
        base: Any? = null,
      ): AtomicReference<ParsedURL> = when {
        // if we are given a guest value string, handle it as a regular URL string
        target.isString -> parseString(target, base)

        // if we are given another URL class, let's clone it
        target.isHostObject && target.`as`(URLValue::class.java) != null ->
          target.`as`(URLValue::class.java).target

        // if we are given anything else, it is considered an error
        else -> throw typeError("Invalid URL: $target")
      }
    }

    /**
     * Constructor: universal. Accepts a [String], another [URLValue] intrinsic, or a guest value which evaluates to any
     * of these things; from Java, [java.net.URI] and [java.net.URL] may also be passed.
     *
     * @param target Absolute URL string, or a [URLValue], or a [GuestValue] of either of those things. If a
     *   [java.net.URL] or [java.net.URI] is passed from the host, it will be converted and wrapped.
     * @throws ValueError if the provided [target] is not a valid URL.
     * @throws TypeError if the provided [target] is not a valid type from which a URL can be constructed.
     */
    @Polyglot constructor (target: Any?) : this(when (target) {
      null -> throw typeError("Cannot construct URL from: `null`")
      is GuestValue -> constructFromGuestValue(target)
      is String -> parseString(target)
      is NativeURL -> AtomicReference(ParsedURL.fromURL(target))
      is java.net.URL -> AtomicReference(ParsedURL.fromURL(target.toURI()))
      is URLValue -> AtomicReference(target.target.get())
      else -> throw typeError("Cannot construct URL from: $target")
    })

    /**
     * Constructor: relative-capable. Accepts a [String], another [URLValue] intrinsic, or a guest value which evaluates
     * to any of these things; from Java, [java.net.URI] and [java.net.URL] may also be passed; if the [target] is
     * relative, a [base] URL must also be provided, which can be a [String] or [URLValue].
     *
     * @param target Absolute URL string, or a [URLValue], or a [GuestValue] of either of those things. If a
     *   [java.net.URL] or [java.net.URI] is passed from the host, it will be converted and wrapped.
     * @param base Base URL from which to resolve [target] if it is relative; a guest [String] may be provided, or
     *   another [URLValue], or a [java.net.URL] or [java.net.URI].
     * @throws ValueError if the provided [target] is not a valid URL.
     * @throws TypeError if the provided [target] is not a valid type from which a URL can be constructed.
     */
    @Polyglot constructor (target: Any?, base: Any?) : this(when (target) {
      null -> throw typeError("Cannot construct URL from: `null`")
      is GuestValue -> constructFromGuestValue(target, base)
      is String -> parseString(target)
      is NativeURL -> AtomicReference(ParsedURL.fromURL(target))
      is java.net.URL -> AtomicReference(ParsedURL.fromURL(target.toURI()))
      is URLValue -> AtomicReference(target.target.get())
      else -> throw typeError("Cannot construct URL from: $target")
    })

    // Run the provided `op` to mutate the current URL, which returns a new URL value; after the transformation is done,
    // replace the current atomic URL reference with the updated reference.
    private fun mutateURL(op: ParsedURL.() -> ParsedURL) {
      check(isMutable()) {
        "Locked URL object is immutable"
      }
      val subject = target.get()
      val changed = op.invoke(subject)
      if (subject !== changed) {
        // if we got a copy back, it was changed, and we should swap it for the new parsed URL object.
        target.set(changed)
      }
    }

    // Whether this URL is locked (and, therefore, immutable).
    private val locked: AtomicBoolean = AtomicBoolean(false)

    /** @inheritDoc */
    override fun wrappedURL(): URI = parsedURL().uri

    /** @inheritDoc */
    override fun parsedURL(): ParsedURL = target.get()

    /** @inheritDoc */
    override fun lock(): URL {
      check(isMutable()) {
        "Locked URL object is immutable"
      }
      locked.set(true)
      return this
    }

    /** @inheritDoc */
    override fun isMutable(): Boolean = !locked.get()

    /** @inheritDoc */
    @Polyglot override fun compareTo(other: URLValue): Int = target.get().absoluteString().compareTo(other.toString())

    /** @inheritDoc */
    @Polyglot override fun equals(other: Any?): Boolean {
      if (other?.javaClass != this.javaClass)
        return false
      return when (other) {
        is URLValue -> target.get().equals(other.target.get())
        else -> false
      }
    }

    /** @inheritDoc */
    @Polyglot override fun hashCode(): Int {
      return target.get().hashCode()
    }

    /** @inheritDoc */
    @Polyglot override fun toString(): String = target.get().absoluteString()

    /** @inheritDoc */
    @get:Polyglot @set:Polyglot override var hash: String
      get() = target.get().hash.resolve()
      set(value) = mutateURL { copySplice(hash = value) }

    /** @inheritDoc */
    @get:Polyglot @set:Polyglot override var host: String
      get() = target.get().host
      set(value) = mutateURL { copySplice(host = value) }

    /** @inheritDoc */
    @get:Polyglot @set:Polyglot override var hostname: String
      get() = target.get().hostname.resolve()
      set(value) = mutateURL { copySplice(hostname = value) }

    /** @inheritDoc */
    @get:Polyglot @set:Polyglot override var href: String
      get() = target.get().absoluteString()
      set(value) = mutateURL { ParsedURL.fromString(value) }

    /** @inheritDoc */
    @get:Polyglot @set:Polyglot override var password: String
      get() = target.get().password.resolve()
      set(value) = mutateURL {
        val user = this.username.resolve()
        if (user.isBlank()) {
          this  // silently drop update: by spec, the `username` must be set before the password.
        } else {
          copySplice(password = value)
        }
      }

    /** @inheritDoc */
    @get:Polyglot @set:Polyglot override var pathname: String
      get() = target.get().pathname.resolve()
      set(value) = mutateURL { copySplice(pathname = value) }

    /** @inheritDoc */
    @get:Polyglot @set:Polyglot override var port: Int?
      get() = target.get().port
      set(value) = mutateURL { copySplice(port = value) }

    /** @inheritDoc */
    @get:Polyglot @set:Polyglot override var protocol: String
      get() = target.get().protocol
      set(value) = mutateURL { copySplice(protocol = value) }

    /** @inheritDoc */
    @get:Polyglot @set:Polyglot override var search: String
      get() = target.get().search.resolve()
      set(value) = mutateURL { copySplice(search = value) }

    /** @inheritDoc */
    @get:Polyglot @set:Polyglot override var username: String
      get() = target.get().username.resolve()
      set(value) = mutateURL { copySplice(username = value) }

    /** @inheritDoc */
    @get:Polyglot override val origin: String get() = throw typeError(
      "Property `URL.origin` is not supported in server-side environments"
    )

    /** @inheritDoc */
    @get:Polyglot override val searchParams: URLSearchParams get() = target.get().searchParams.resolve()

    /** @inheritDoc */
    @Polyglot override fun toJSON(): String = toString()
  }

  /** @inheritDoc */
  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    // mount `URL`
    bindings[URL_SYMBOL] = ProxyInstantiable { arguments ->
      when (arguments.size) {
        1 -> URLValue(arguments[0])
        2 -> URLValue(arguments[0], arguments[1])
        else -> throw valueError("Invalid number of arguments: ${arguments.size}")
      }
    }
  }
}

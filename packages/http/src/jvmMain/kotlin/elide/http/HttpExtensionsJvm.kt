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

package elide.http

import java.util.function.BiPredicate
import elide.http.ProtocolVersion.HTTP_1_0
import elide.http.ProtocolVersion.HTTP_1_1
import elide.http.ProtocolVersion.HTTP_2
import java.net.http.HttpClient.Version as JdkHttpVersion
import java.net.http.HttpRequest as JdkRequest
import java.net.http.HttpHeaders as JdkHeaders
import java.net.http.HttpResponse as JdkResponse
import io.micronaut.http.HttpVersion as MicronautHttpVersion
import io.micronaut.http.HttpHeaders as MicronautHeaders
import io.micronaut.http.HttpRequest as MicronautRequest
import io.netty.handler.codec.http.HttpHeaders as NettyHeaders
import io.netty.handler.codec.http.HttpMessage as NettyHttpMessage

// Default protocol versions.
internal val defaultJdkProtocol = JdkHttpVersion.HTTP_1_1
internal val defaultMicronautProtocol = MicronautHttpVersion.HTTP_1_1

// Useful shared constants.
internal val alwaysTruePredicate: BiPredicate<String, String> = BiPredicate { _, _ -> true }

// Return the protocol version for a JDK request.
internal fun JdkHttpVersion.toProtocolVersion(): ProtocolVersion = when (this) {
  JdkHttpVersion.HTTP_1_1 -> ProtocolVersion.HTTP_1_1
  JdkHttpVersion.HTTP_2 -> ProtocolVersion.HTTP_2
}

// Return the protocol version for a JDK request.
internal fun JdkRequest.toProtocolVersion(): ProtocolVersion =
  (version().orElse(defaultJdkProtocol) ?: defaultJdkProtocol).toProtocolVersion()

// Return the protocol version for a JDK response.
internal fun JdkResponse<*>.toProtocolVersion(): ProtocolVersion =
  (version() ?: defaultJdkProtocol).toProtocolVersion()

// Return the protocol version for a Micronaut request.
internal fun MicronautHttpVersion.toProtocolVersion(): ProtocolVersion = when (this) {
  MicronautHttpVersion.HTTP_1_0 -> ProtocolVersion.HTTP_1_0
  MicronautHttpVersion.HTTP_1_1 -> ProtocolVersion.HTTP_1_1
  MicronautHttpVersion.HTTP_2_0 -> ProtocolVersion.HTTP_2
}

// Return the protocol version for a Micronaut request.
internal fun MicronautRequest<*>.toProtocolVersion(): ProtocolVersion =
  (httpVersion ?: defaultMicronautProtocol).toProtocolVersion()

// Pull a `HeaderValue` from a map.
internal fun Map<String, List<String>>.asHeaderValue(name: String): HeaderValue? = get(name)?.let {
  when (it.size) {
    0 -> null
    1 -> HeaderValue.single(it[0])
    else -> HeaderValue.multi(it)
  }
}

// Pull a `HeaderValue` from a JDK `HttpHeaders` instance.
internal fun JdkHeaders.asHeaderValue(name: String): HeaderValue? = allValues(name).let {
  when (it.size) {
    0 -> null
    1 -> HeaderValue.single(it[0])
    else -> HeaderValue.multi(it)
  }
}

// Pull a `HeaderValue` from a Micronaut `HttpHeaders` instance.
internal fun MicronautHeaders.asHeaderValue(name: String): HeaderValue? = when (contains(name)) {
  false -> null
  true -> getAll(name).let {
    when (it.size) {
      0 -> null
      1 -> HeaderValue.single(it[0])
      else -> HeaderValue.multi(it)
    }
  }
}

// Pull a `HeaderValue` from a Netty `HttpHeaders` instance.
internal fun NettyHeaders.asHeaderValue(name: String): HeaderValue? = when (contains(name)) {
  false -> null
  true -> getAll(name).let {
    when (it.size) {
      0 -> null
      1 -> HeaderValue.single(it[0])
      else -> HeaderValue.multi(it)
    }
  }
}

// Resolve a `ProtocolVersion` from a Netty `HttpMessage`.
internal fun NettyHttpMessage.toProtocolVersion(): ProtocolVersion = protocolVersion().let { version ->
  when {
    version.majorVersion() == 2 -> ProtocolVersion.HTTP_2
    version.majorVersion() == 1 -> when {
      version.minorVersion() == 0 -> ProtocolVersion.HTTP_1_0
      version.minorVersion() == 1 -> ProtocolVersion.HTTP_1_1
      else -> error("Unrecognized HTTP minor version: ${version.minorVersion()}")
    }
    else -> error("Unrecognized HTTP major version: ${version.majorVersion()}")
  }
}

/**
 * All built-in protocol version entries.
 */
public val ProtocolVersion.Companion.entries: Sequence<ProtocolVersion> get() = sequenceOf(HTTP_1_0, HTTP_1_1, HTTP_2)

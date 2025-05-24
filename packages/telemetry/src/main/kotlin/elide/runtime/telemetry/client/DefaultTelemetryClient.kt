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

package elide.runtime.telemetry.client

import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.jdk.DefaultJdkHttpClient
import org.slf4j.event.Level
import java.net.URI
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.serialization.json.Json
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.runtime.Logging
import elide.runtime.telemetry.Event
import elide.runtime.telemetry.TelemetryConfig

@Singleton
internal class DefaultTelemetryClient @Inject constructor (private val http: DefaultJdkHttpClient) : TelemetryClient {
  private companion object {
    private const val ELIDE_VERSION_HEADER: String = "x-elide-version"
    private const val ELIDE_PLATFORM_HEADER: String = "x-elide-platform"

    private val logging by lazy {
      Logging.of(TelemetryClient::class.java)
    }

    private val json: Json by lazy {
      Json {
        encodeDefaults = true
        prettyPrint = false
      }
    }

    private val endpoint: URI by lazy {
      URI.create(TelemetryConfig.ENDPOINT)
    }
  }

  private fun resolveOs(): String = System.getProperty("os.name").lowercase()
  private fun resolveArch(): String = when (val tag = System.getProperty("os.arch").lowercase()) {
    "x86_64", "amd64" -> "amd64"
    "aarch64", "arm64" -> "arm64"
    else -> tag
  }

  private fun handleResponse(event: Event, response: HttpResponse<String>): EventDelivery {
    if (logging.isEnabledForLevel(Level.DEBUG)) {
      logging.debug {
        "Telemetry event '${event::class.simpleName}' sent to telemetry endpoint: status=${response.status}"
      }
    }
    return if (response.status == HttpStatus.ACCEPTED) {
      EventDelivery.EventDelivered
    } else {
      EventDelivery.EventFailure
    }
  }

  @OptIn(DelicateCoroutinesApi::class)
  override suspend fun <E : Event> deliver(event: E): Deferred<EventDelivery> {
    val encoded = json.encodeToString<Event>(event)
    val req = HttpRequest.POST<String>(endpoint, encoded)
    req.headers.set(ELIDE_VERSION_HEADER, elide.runtime.telemetry.gen.BuildConfig.ELIDE_VERSION)
    req.headers.set(ELIDE_PLATFORM_HEADER, "${resolveOs()}-${resolveArch()}")
    req.headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
    try {
      val response = http.exchange(req, String::class.java).awaitLast()
      handleResponse(event, response)
      @Suppress("TooGenericExceptionCaught")
      return GlobalScope.async {
        EventDelivery.EventDelivered
      }
    } catch (e: Exception) {
      logging.debug("Error delivering telemetry event", e)
      throw e
    }

  }
}

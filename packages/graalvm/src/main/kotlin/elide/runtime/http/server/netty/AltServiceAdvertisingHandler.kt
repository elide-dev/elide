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
package elide.runtime.http.server.netty

import elide.runtime.http.server.netty.AltServiceAdvertisingHandler.Companion.forH1
import elide.runtime.http.server.netty.AltServiceAdvertisingHandler.Companion.forH2
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpResponse

/**
 * Preliminary configuration for alternative service advertising. The full configuration is resolved once the stack
 * is bound, since the actual hostnames and ports cannot be resolved until that point.
 */
internal data class AltServicesConfig(
  val h2c: AltServiceSource? = null,
  val h2: AltServiceSource? = null,
  val h3: AltServiceSource? = null,
)

/**
 * Resolved alt services configuration, created once the application stack is bound. Note that alt services present in
 * the source [AltServicesConfig] may not be present in the resolved records, for example, if the alt service failed
 * to start.
 */
internal data class AltServices(
  val h2c: AltService? = null,
  val h2: AltService? = null,
  val h3: AltService? = null,
)

/** Describes the data used to compose an Alt-Svc header. */
internal data class AltService(
  val protocol: String,
  val authority: String,
)

/** Describes a configuration for Alt-Svc advertising of a single service. */
internal data class AltServiceSource(
  /** Protocol being advertised. */
  val protocol: String,
  /** Label for the service doing the advertising. */
  val sponsorService: String,
  /** Label for the alt service being advertised. */
  val altService: String,
  /**
   * Original, unresolved hostname for the alt service, used in the authority field if the resolved hostname differs
   * from that of the sponsor.
   */
  val altServiceHost: String,
)

/**
 * A simple outbound handler that advertises the configured [services] by adding `Alt-Svc` headers to outgoing
 * requests.
 *
 * All [services] are advertised unconditionally, use [forH1] and [forH2] to obtain an adequate list of alternative
 * services for a given protocol.
 */
@Sharable internal class AltServiceAdvertisingHandler(
  private val services: List<AltService>,
) : ChannelDuplexHandler() {
  override fun write(ctx: ChannelHandlerContext?, msg: Any, promise: ChannelPromise?) {
    if (msg is HttpResponse) services.forEach { service ->
      msg.headers().add(HttpHeaderNames.ALT_SVC, buildHeaderValue(service))
    }

    super.write(ctx, msg, promise)
  }

  internal companion object {
    /** Build the value of an Alt-Svc header from the given [service] info. */
    @JvmStatic internal fun buildHeaderValue(service: AltService): String {
      return "${service.protocol}=${service.authority}"
    }

    /**
     * Returns an [AltServiceAdvertisingHandler] that advertises HTTP/3 (h3) and HTTP/2 over TLS (h2), in that order, if
     * they are available. For security reasons, cleartext HTTP/2 (h2c) is only advertised when safer protocols are
     * unavailable.
     */
    @JvmStatic internal fun forH1(services: AltServices, cleartext: Boolean = false): AltServiceAdvertisingHandler {
      val services = buildList {
        services.h3?.let(::add)
        services.h2?.let(::add)
        // only advertise h2c if safer protocols are unavailable
        if (cleartext && services.h2 == null && services.h3 == null) services.h2c?.let(::add)
      }

      return AltServiceAdvertisingHandler(services)
    }

    /**
     * Returns an [AltServiceAdvertisingHandler] that advertises HTTP/3 (h3) if it is available; if the handler is
     * created for a [cleartext] HTTP/2 service, it will also advertise HTTP/2 over TLS (h2) when possible.
     */
    @JvmStatic internal fun forH2(services: AltServices, cleartext: Boolean = false): AltServiceAdvertisingHandler {
      val services = buildList {
        services.h3?.let(::add)
        // h2c can still upgrade to h2
        if (cleartext) services.h2?.let(::add)
      }

      return AltServiceAdvertisingHandler(services)
    }
  }
}

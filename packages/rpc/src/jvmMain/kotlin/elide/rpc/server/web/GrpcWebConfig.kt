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

package elide.rpc.server.web

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.core.util.Toggleable
import java.time.Duration

/**
 * Describes active configuration for Elide's RPC layer as related to integration with gRPC Web.
 */
@ConfigurationProperties("elide.grpc.web")
public interface GrpcWebConfig : Toggleable {
  override fun isEnabled(): Boolean = DEFAULT_ENABLED

  /**
   * @return RPC endpoint prefix.
   */
  public val endpoint: String get() = defaultEndpoint

  /**
   * @return Timeout duration for RPC calls.
   */
  public val timeout: Duration get() = Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS)

  public companion object {
    /** Whether to enable the gRPC-Web integration by default. */
    public const val DEFAULT_ENABLED: Boolean = false

    /** Whether to enable the gRPC-Web integration by default. */
    public const val DEFAULT_TIMEOUT_SECONDS: Long = 30L

    /** Default gRPC-Web RPC traffic endpoint prefix. */
    public const val defaultEndpoint: String = "/_/rpc"

    /** Default gRPC-Web settings. */
    public val DEFAULTS: GrpcWebConfig = object : GrpcWebConfig {}
  }
}

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
package elide.secrets.impl

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import elide.annotations.Component
import elide.annotations.Factory
import elide.secrets.SecretValues

/**
 * Factory for external dependencies.
 *
 * @author Lauri Heino <datafox>
 */
@Factory
internal class DependencyFactory {
  @Component val json: Json = Json.Default
  @Component @OptIn(ExperimentalSerializationApi::class) val cbor: BinaryFormat = Cbor.Default
  @Component
  val httpClient: HttpClient =
    HttpClient(CIO) {
      install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
      install(HttpTimeout) {
        requestTimeoutMillis = SecretValues.CLIENT_TIMEOUT
        connectTimeoutMillis = SecretValues.CLIENT_TIMEOUT
      }
    }
}

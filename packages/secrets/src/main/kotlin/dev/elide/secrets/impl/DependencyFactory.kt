package dev.elide.secrets.impl

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
        requestTimeoutMillis = 10000
        connectTimeoutMillis = 10000
      }
    }
}

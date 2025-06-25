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
@file:OptIn(ExperimentalSerializationApi::class)

package elide.tooling.coverage

import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream

/**
 * # Coverage JSON Report
 *
 * Describes the structure of coverage reports emitted by Elide in JSON format. These reports are used to generate
 * second-order reports in other formats, so they need to be parsable after being written.
 *
 * @property entries Coverage info stanzas, each of which describes coverage info for a given file.
 */
@Serializable public data class CoverageJsonReport(public val entries: List<CoverageInfoStanza>) {
  @Serializable public data class CoverageSourceSection(
    public val characters: String,
    @SerialName("start_column") public val startColumn: Int,
    @SerialName("start_line") public val startLine: Int,
    @SerialName("end_column") public val endColumn: Int,
    @SerialName("end_line") public val endLine: Int,
    @SerialName("char_length") public val charLength: Int,
    @SerialName("char_index") public val charIndex: Int,
    @SerialName("char_end_index") public val charEndIndex: Int,
  )

  @Serializable public data class CoverageSection(
    public val covered: Boolean,
    public val count: Int? = null,
    @SerialName("source_section") public val sourceSection: CoverageSourceSection,
  )

  @Serializable public data class CoverageRoot(
    public val name: String,
    public val covered: Boolean,
    public val count: Int? = null,
    @SerialName("source_section") public val sourceSection: CoverageSourceSection,
    public val sections: List<CoverageSection>,
  )

  @Serializable public data class CoverageInfoStanza(
    public val path: String,
    public val name: String,
    public val roots: List<CoverageRoot>,
  )

  /** Factories for parsing coverage reports from JSON. */
  public companion object {
    /** @return Parsed coverage report from JSON string. */
    @JvmStatic public fun parse(from: String): CoverageJsonReport {
      return CoverageJsonReport(Json.decodeFromString<List<CoverageInfoStanza>>(from))
    }

    /** @return Parsed coverage report from JSON stream. */
    @JvmStatic public fun parse(from: InputStream): CoverageJsonReport = from.use {
      CoverageJsonReport(Json.decodeFromStream<List<CoverageInfoStanza>>(from))
    }
  }

  public fun toJson(): String = Json.encodeToString(
    ListSerializer(CoverageInfoStanza.serializer()),
    entries,
  )

  public fun toJson(out: OutputStream): Unit = Json.encodeToStream(
    ListSerializer(CoverageInfoStanza.serializer()),
    entries,
    out,
  )
}

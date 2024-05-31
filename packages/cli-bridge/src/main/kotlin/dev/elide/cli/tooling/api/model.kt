/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
package dev.elide.cli.tooling.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable @JvmRecord public data class CodeLocation (
  public val file: String,
  public val line: UInt,
  public val column: UInt
)

@Serializable public enum class Severity(public val string: String) {
  @SerialName("Info")
  Info("Info"),
  @SerialName("Warning")
  Warning("Warning"),
  @SerialName("Error")
  Error("Error"),
}

@Serializable @JvmRecord public data class DiagnosticNote (
  public val id: String,
  public val tool: String,
  public val code: String,
  public val message: String,
  public val location: CodeLocation,
  public val severity: Severity
)

@Serializable public enum class ToolType(public val string: String) {
  @SerialName("Linter")
  Linter("Linter"),
  @SerialName("Compiler")
  Compiler("Compiler"),
}

@Serializable @JvmRecord public data class ToolInfo (
  public val name: String,
  public val version: String,
  public val language: String,
  public val experimental: Boolean,
  public val kind: ToolType
)

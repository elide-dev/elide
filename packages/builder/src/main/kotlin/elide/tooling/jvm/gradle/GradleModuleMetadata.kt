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

package elide.tooling.jvm.gradle

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull

/**
 * Root structure of a Gradle Module Metadata file.
 *
 * @property formatVersion The version of the module metadata format (e.g., "1.1")
 * @property component The component identification information
 * @property createdBy Information about what created this metadata
 * @property variants List of variants available for this module
 */
@Serializable
@JvmRecord
public data class GradleModuleMetadata(
  val formatVersion: String,
  val component: Component,
  val createdBy: CreatedBy? = null,
  val variants: List<Variant> = emptyList()
) {
  /**
   * Component identification.
   *
   * @property group The group ID (e.g., "com.example")
   * @property module The module/artifact ID (e.g., "my-library")
   * @property version The version string (e.g., "1.0.0")
   * @property url Optional URL for the component
   */
  @Serializable
  @JvmRecord
  public data class Component(
    val group: String,
    val module: String,
    val version: String,
    val url: String? = null,
    val attributes: Map<String, AttributeValue>? = null,
  )

  /**
   * Information about the tool that created this metadata.
   *
   * @property gradle Gradle version information
   */
  @Serializable
  @JvmRecord
  public data class CreatedBy(
    val gradle: GradleInfo? = null
  ) {
    @Serializable
    @JvmRecord
    public data class GradleInfo(
      val version: String,
      val buildId: String? = null
    )
  }
}

/**
 * Represents a variant of the module with specific attributes and dependencies.
 *
 * @property name The name of the variant (e.g., "apiElements", "runtimeElements")
 * @property attributes Key-value pairs describing variant characteristics
 * @property available Optional availability information
 * @property dependencies List of dependencies for this variant
 * @property dependencyConstraints List of dependency constraints
 * @property files List of files/artifacts for this variant
 * @property capabilities List of capabilities provided by this variant
 */
@Serializable
@JvmRecord
public data class Variant(
  val name: String,
  val attributes: Map<String, AttributeValue> = emptyMap(),
  val available: AvailableAt? = null,
  val dependencies: List<Dependency> = emptyList(),
  val dependencyConstraints: List<DependencyConstraint> = emptyList(),
  val files: List<FileInfo> = emptyList(),
  val capabilities: List<Capability> = emptyList()
) {
  /**
   * Indicates where this variant is available if not in the same module.
   */
  @Serializable
  @JvmRecord
  public data class AvailableAt(
    val url: String,
    val group: String,
    val module: String,
    val version: String
  )
}

/**
 * Represents a dependency with rich version constraints.
 *
 * @property group The group ID of the dependency
 * @property module The module/artifact ID
 * @property version Version constraint (can be a simple string or rich constraint)
 * @property excludes List of exclusions
 * @property reason Optional reason for this dependency
 * @property attributes Attributes for variant-aware resolution
 * @property requestedCapabilities Requested capabilities from this dependency
 * @property endorseStrictVersions Whether to endorse strict versions transitively
 */
@Serializable
@JvmRecord
public data class Dependency(
  val group: String,
  val module: String,
  val version: VersionConstraint? = null,
  val excludes: List<Exclusion> = emptyList(),
  val reason: String? = null,
  val attributes: Map<String, AttributeValue> = emptyMap(),
  val requestedCapabilities: List<Capability> = emptyList(),
  val endorseStrictVersions: Boolean? = null
)

/**
 * Represents a dependency constraint without introducing a dependency.
 */
@Serializable
@JvmRecord
public data class DependencyConstraint(
  val group: String,
  val module: String,
  val version: VersionConstraint? = null,
  val reason: String? = null,
  val attributes: Map<String, AttributeValue> = emptyMap()
)

/**
 * Rich version constraints supporting Gradle's sophisticated version model.
 */
@Serializable(with = VersionConstraintSerializer::class)
public sealed interface VersionConstraint {
  /**
   * Simple string version constraint (e.g., "1.0", "1.+", "[1.0,2.0)")
   */
  @Serializable
  @JvmInline
  public value class Simple(public val version: String) : VersionConstraint

  /**
   * Rich version constraint with multiple clauses.
   *
   * @property requires Minimum required version
   * @property prefers Preferred version when multiple are available
   * @property strictly Strict version requirement (overrides transitive)
   * @property rejects List of rejected versions or ranges
   */
  @Serializable
  @JvmRecord
  public data class Rich(
    val requires: String? = null,
    val prefers: String? = null,
    val strictly: String? = null,
    val rejects: List<String> = emptyList()
  ) : VersionConstraint
}

/**
 * Exclusion rule for transitive dependencies.
 *
 * @property group Group to exclude (can be "*" for all)
 * @property module Module to exclude (can be "*" for all)
 */
@Serializable
@JvmRecord
public data class Exclusion(
  val group: String,
  val module: String = "*"
)

/**
 * File/artifact information.
 *
 * @property name File name
 * @property url URL to download the file
 * @property size File size in bytes
 * @property sha512 SHA-512 checksum
 * @property sha256 SHA-256 checksum
 * @property sha1 SHA-1 checksum
 * @property md5 MD5 checksum (legacy)
 */
@Serializable
@JvmRecord
public data class FileInfo(
  val name: String,
  val url: String,
  val size: Long? = null,
  val sha512: String? = null,
  val sha256: String? = null,
  val sha1: String? = null,
  val md5: String? = null
)

/**
 * Capability provided or required by a component.
 *
 * @property group Capability group
 * @property name Capability name
 * @property version Capability version
 */
@Serializable
@JvmRecord
public data class Capability(
  val group: String,
  val name: String,
  val version: String? = null
)

public object GradleAttributeSerializer : KSerializer<AttributeValue> {
  override val descriptor: SerialDescriptor = buildClassSerialDescriptor("AttributeValue")

  override fun serialize(encoder: Encoder, value: AttributeValue) {
    when (value) {
      is AttributeValue.StringValue -> encoder.encodeString(value.value)
      is AttributeValue.NumberValue -> encoder.encodeInt(value.value)
      is AttributeValue.BooleanValue -> encoder.encodeBoolean(value.value)
    }
  }

  override fun deserialize(decoder: Decoder): AttributeValue {
    decoder as JsonDecoder
    val element = decoder.decodeJsonElement()
    element as JsonPrimitive
    return when {
      element.isString -> AttributeValue.StringValue(element.content)
      element.intOrNull != null -> AttributeValue.NumberValue(element.int)
      element.booleanOrNull != null -> AttributeValue.BooleanValue(element.boolean)
      else -> error(
        "Failed to deserialize Gradle attribute value: '$element'",
      )
    }
  }
}

public object VersionConstraintSerializer : KSerializer<VersionConstraint> {
  override val descriptor: SerialDescriptor = buildClassSerialDescriptor("VersionConstraint")

  private fun stringProperty(value: JsonElement?): String? {
    return when (value) {
      null -> null
      is JsonPrimitive -> when {
        value.isString -> value.content
        else -> null
      }
      else -> null
    }
  }

  private fun stringListProperty(value: JsonElement?): List<String> {
    return when (value) {
      null -> null
      is JsonArray -> buildList {
        value.forEach { inner ->
          stringProperty(inner)?.let { add(it) }
        }
      }
      else -> null
    } ?: emptyList()
  }

  override fun serialize(encoder: Encoder, value: VersionConstraint) {
    when (value) {
      is VersionConstraint.Simple -> encoder.encodeString(value.version)
      is VersionConstraint.Rich -> encoder.encodeSerializableValue(VersionConstraint.Rich.serializer(), value)
    }
  }

  override fun deserialize(decoder: Decoder): VersionConstraint {
    return when (val element = (decoder as JsonDecoder).decodeJsonElement()) {
      is JsonObject -> VersionConstraint.Rich(
        requires = stringProperty(element["requires"]),
        prefers = stringProperty(element["prefers"]),
        strictly = stringProperty(element["strictly"]),
        rejects = stringListProperty(element["rejects"]),
      )

      is JsonPrimitive -> VersionConstraint.Simple(element.content)
      else -> error("Invalid JSON leaf type for version constraint: '$element'")
    }
  }
}

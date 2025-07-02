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

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Attribute values can be strings, booleans, or numbers.
 */
@Serializable(with = GradleAttributeSerializer::class)
public sealed interface AttributeValue {
  @Serializable
  @SerialName("string")
  @JvmRecord
  public data class StringValue(val value: String) : AttributeValue

  @Serializable
  @SerialName("boolean")
  @JvmRecord
  public data class BooleanValue(val value: Boolean) : AttributeValue

  @Serializable
  @SerialName("number")
  @JvmRecord
  public data class NumberValue(val value: Int) : AttributeValue
}


// Serialization helpers for common attribute names
public object StandardAttributes {
  public const val USAGE: String = "org.gradle.usage"
  public const val CATEGORY: String = "org.gradle.category"
  public const val LIBRARYELEMENTS: String = "org.gradle.libraryelements"
  public const val BUNDLING: String = "org.gradle.bundling"
  public const val STATUS: String = "org.gradle.status"
  public const val DOCS_TYPE: String = "org.gradle.docstype"

  // JVM specific
  public const val JVM_VERSION: String = "org.gradle.jvm.version"
  public const val JVM_ENVIRONMENT: String = "org.gradle.jvm.environment"

  // Kotlin specific
  public const val KOTLIN_PLATFORM_TYPE: String = "org.jetbrains.kotlin.platform.type"
  public const val KOTLIN_NATIVE_TARGET: String = "org.jetbrains.kotlin.native.target"
}

// Common attribute values
public object AttributeValues {
  // Usage values
  public const val JAVA_API: String = "java-api"
  public const val JAVA_RUNTIME: String = "java-runtime"
  public const val JAVA_RUNTIME_JARS: String = "java-runtime-jars"

  // Category values
  public const val LIBRARY: String = "library"
  public const val PLATFORM: String = "platform"
  public const val DOCUMENTATION: String = "documentation"

  // Library elements values
  public const val JAR: String = "jar"
  public const val CLASSES: String = "classes"
  public const val RESOURCES: String = "resources"
  public const val SOURCES: String = "sources"
  public const val JAVADOC: String = "javadoc"

  // Status values
  public const val RELEASE: String = "release"
  public const val MILESTONE: String = "milestone"
  public const val INTEGRATION: String = "integration"

  // Bundling values
  public const val EXTERNAL: String = "external"
  public const val EMBEDDED: String = "embedded"
  public const val SHADOWED: String = "shadowed"
}

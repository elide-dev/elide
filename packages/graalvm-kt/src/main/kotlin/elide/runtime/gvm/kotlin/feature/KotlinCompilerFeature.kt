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
@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE", "deprecation")

package elide.runtime.gvm.kotlin.feature

import com.oracle.svm.core.jdk.FileSystemProviderSupport
import jdk.internal.jrtfs.JrtFileSystemProvider
import org.graalvm.nativeimage.ImageSingletons
import org.graalvm.nativeimage.hosted.Feature
import org.graalvm.nativeimage.impl.RuntimeClassInitializationSupport
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import elide.runtime.feature.FrameworkFeature

/**
 * ## Kotlin Compiler Feature
 *
 * Configures the Native Image build to be aware of Kotlin language resources that must be installed alongside Elide at
 * runtime to support compilation of Kotlin source.
 */
@Suppress("unused") internal class KotlinCompilerFeature : FrameworkFeature {
  override fun getDescription(): String = "Configures the Kotlin compiler"

  @Suppress("DEPRECATION")
  @OptIn(ExperimentalCompilerApi::class)
  override fun afterRegistration(access: Feature.AfterRegistrationAccess) {
    // we need access to the JRT file system provider, but we do not want to include the build-time JDK image.
    FileSystemProviderSupport.register(JrtFileSystemProvider())

    // Initialize problematic packages at runtime to avoid blocklist violations when LoadNativeNode reaches
    // JShell diagnostics code that uses String.format(), which cascades through collections and I/O operations.
    // This prevents 168 blocklist violations that surface when TruffleRuby is added to the classpath.
    val support = ImageSingletons.lookup(RuntimeClassInitializationSupport::class.java)

    // JShell and Java compiler (all violations trace through these)
    support.initializeAtRunTime("jdk.jshell", "JShell REPL diagnostics")
    support.initializeAtRunTime("com.sun.tools.javac", "Java compiler internals")

    // Kotlin compiler repackaged dependencies
    support.initializeAtRunTime("org.jetbrains.kotlin.com", "Kotlin repackaged Guava/IntelliJ classes")
    support.initializeAtRunTime("org.jetbrains.kotlin.it.unimi", "Kotlin repackaged FastUtil")
    support.initializeAtRunTime("org.jetbrains.kotlin.protobuf", "Kotlin repackaged Protocol Buffers")

    // Compression libraries with JNI bindings
    support.initializeAtRunTime("com.aayushatharva.brotli4j", "Brotli compression JNI")
    support.initializeAtRunTime("org.apache.commons.compress", "Apache Commons compression")

    // Micronaut concurrent collections
    support.initializeAtRunTime("io.micronaut.core.util.clhm", "Micronaut ConcurrentLinkedHashMap")
  }
}

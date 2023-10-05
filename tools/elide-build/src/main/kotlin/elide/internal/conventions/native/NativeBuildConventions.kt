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

package elide.internal.conventions.native

import io.micronaut.gradle.docker.MicronautDockerfile
import io.micronaut.gradle.docker.NativeImageDockerfile
import org.graalvm.buildtools.gradle.dsl.GraalVMExtension
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.bundling.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JvmVendorSpec
import elide.internal.conventions.Constants.Versions
import elide.internal.conventions.native.NativeTarget.*
import org.graalvm.buildtools.gradle.dsl.GraalVMReachabilityMetadataRepositoryExtension as GraalVMMetadataExtension

public enum class NativeTarget {
  APP,
  LIB,
  SAMPLE,
}

/** Create a Maven publication for projects compiling shared native libaries. */
internal fun Project.publishNativeLibrary() {
  extensions.getByType(PublishingExtension::class.java).apply {
    publications.create("maven", MavenPublication::class.java) {
      from(components.getByName("kotlin"))
    }
  }
}

/**
 * Configure GraalVM Native build for a given [target]. Shared libaries, apps, and native samples all declare
 * different configurations.
 */
internal fun Project.configureNativeBuild(
  target: NativeTarget,
  enableAgent: Boolean,
  customLauncher: Boolean,
  toolchains: JavaToolchainService,
) {
  // TODO(@darvld): replace these with convention properties when the API is ready
  val quickBuildEnabled = properties["elide.release"] != "true" || properties["elide.buildMode"] == "dev"

  // package reachability metadata into a JAR 
  if (target == LIB) tasks.named("jar", Jar::class.java).configure {
    from("collectReachabilityMetadata")
  }


  // configure GraalVM extension
  extensions.getByType(GraalVMExtension::class.java).apply {
    // metadata repository settings
    (this as ExtensionAware).extensions.getByType(GraalVMMetadataExtension::class.java).apply {
      enabled.set(true)
      version.set(Versions.GRAALVM_METADATA)
    }


    // shared libraries and apps have test support enabled (not applied to native samples)
    testSupport.set(target == LIB || target == APP)

    // shared agent configuration
    agent.apply {
      enabled.set(enableAgent)
      defaultMode.set("standard")
      builtinCallerFilter.set(true)
      builtinHeuristicFilter.set(true)
      enableExperimentalPredefinedClasses.set(false)
      enableExperimentalUnsafeAllocationTracing.set(false)
      trackReflectionMetadata.set(true)

      modes.apply {
        standard { }
      }

      metadataCopy.apply {
        inputTaskNames.add("test")
        outputDirectories.add("src/main/resources/META-INF/native-image")
        mergeWithExisting.set(true)
      }
    }

    // setup native binaries (target-specific)
    binaries.apply {
      // all targets include a "main" binary
      named("main") {
        quickBuild.set(quickBuildEnabled)

        // generate a shared native lib if requested
        sharedLibrary.set(target == LIB)

        // base args (not applicable for samples)
        if (target == APP || target == LIB) buildArgs(
          "--language:js",
          "--language:regex",
          "-Dpolyglot.image-build-time.PreinitializeContexts=js",
        )

        // app-specific settings
        if (target == APP) {
          buildArgs("--enable-all-security-services")
          if (customLauncher) javaLauncher.set(getLauncherForNativeApp(toolchains))
        }
      }

      // all targets include a "test" binary
      named("test") {
        quickBuild.set(quickBuildEnabled)

        // base args (not applicable for samples)
        if (target == APP || target == LIB) buildArgs(
          "--language:js",
          "--language:regex",
          "-Dpolyglot.image-build-time.PreinitializeContexts=js",
        )

        // app-specific settings
        if (target == APP) {
          buildArgs("--enable-all-security-services")
          if (customLauncher) javaLauncher.set(getLauncherForNativeApp(toolchains))
        }
      }

      // apps include an "optimized" binary
      if (target == APP) named("optimized") {
        quickBuild.set(quickBuildEnabled)
        buildArgs(
          "--language:js",
          "--language:regex",
          "-O2",
          "--enable-all-security-services",
          "-Dpolyglot.image-build-time.PreinitializeContexts=js",
        )
      }
    }
  }

  // native samples have their dockerfiles configured automatically
  if (target == SAMPLE) {
    configureNativeDocker()
    configureSampleRunTask()
  }
}

private fun Project.getLauncherForNativeApp(toolchains: JavaToolchainService): Provider<JavaLauncher> {
  return toolchains.launcherFor {
    // TODO(@darvld): replace with convention properties
    languageVersion.set(JavaLanguageVersion.of(findProperty("versions.java.language") as String))

    // TODO(@darvld): replace with convention properties
    if (hasProperty("elide.graalvm.variant")) {
      val variant = property("elide.graalvm.variant") as String

      if (variant != "COMMUNITY") vendor.set(
        JvmVendorSpec.matching(
          when (variant.trim()) {
            "ENTERPRISE" -> "Oracle"
            else -> "GraalVM Community"
          },
        ),
      )
    }
  }
}

private fun Project.configureNativeDocker() {
  tasks.named("dockerfile", MicronautDockerfile::class.java).configure {
    baseImage("${project.properties["elide.publish.repo.docker.tools"]}/base:latest")
  }

  tasks.named("optimizedDockerfile", MicronautDockerfile::class.java).configure {
    baseImage("${project.properties["elide.publish.repo.docker.tools"]}/runtime/jvm17")
  }

  tasks.named("dockerfileNative", NativeImageDockerfile::class.java).configure {
    graalImage.set("${project.properties["elide.publish.repo.docker.tools"]}/builder:latest")

    baseImage("${project.properties["elide.publish.repo.docker.tools"]}/runtime/native:latest")
    args("-H:+StaticExecutableWithDynamicLibC")
  }

  tasks.named("optimizedDockerfileNative", NativeImageDockerfile::class.java).configure {
    graalImage.set("${project.properties["elide.publish.repo.docker.tools"]}/builder:latest")

    baseImage("${project.properties["elide.publish.repo.docker.tools"]}/runtime/native:latest")
    args("-H:+StaticExecutableWithDynamicLibC")
  }
}

private fun Project.configureSampleRunTask() {
  tasks.named("run", JavaExec::class.java).configure {
    jvmArgs(listOf("-Delide.dev=true"))

    val argsList = ArrayList<String>()
    if (project.hasProperty("elide.vm.inspect") && project.properties["elide.vm.inspect"] == "true") {
      argsList.add("--elide.vm.inspect=true")
    } else {
      argsList.add("--elide.vm.inspect=false")
    }

    args(*argsList.toTypedArray())
  }
}

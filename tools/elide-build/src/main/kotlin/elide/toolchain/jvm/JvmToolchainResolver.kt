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
@file:Suppress("UnstableApiUsage")

package elide.toolchain.jvm

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.jvm.toolchain.JavaToolchainDownload
import org.gradle.jvm.toolchain.JavaToolchainRequest
import org.gradle.jvm.toolchain.JavaToolchainResolver
import org.gradle.jvm.toolchain.JavaToolchainResolverRegistry
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.platform.Architecture
import org.gradle.platform.OperatingSystem
import java.net.URI
import java.util.Optional
import javax.inject.Inject

// Whether to enable the custom toolchain resolver.
private const val USE_CUSTOM_TOOLCHAINS = false

// Latest Oracle GVM version ("GraalVM Enterprise").
private val latestOracleGvmVersion = "24.0.0-ea.02" to JavaVersion.VERSION_24

// Latest GraalVM Community Edition version.
private val latestGraalVmCeVersion = "24.2.0-dev-20241213_1237" to JavaVersion.VERSION_24

// Release track for a given JDK toolchain.
@Suppress("unused") private enum class ReleaseTrack {
  RELEASE,
  EARLY_ACCESS,
}

// Type/kind of GVM JDK release.
private enum class ReleaseType {
  COMMUNITY,
  ENTERPRISE,
}

private object GraalVMPlatform {
  const val LINUX_AMD64 = "linux-x64"
  const val LINUX_ARM64 = "linux-aarch64"
  const val DARWIN_AMD64 = "darwin-x64"
  const val DARWIN_ARM64 = "darwin-aarch64"
  const val WINDOWS_AMD64 = "windows-amd64"

  @JvmStatic fun fromRequest(arch: Architecture, os: OperatingSystem): String {
    val first = when (os) {
      OperatingSystem.LINUX -> "linux"
      OperatingSystem.MAC_OS -> "darwin"
      OperatingSystem.WINDOWS -> "windows"
      else -> error("Unsupported OS for GVM: ${os.name}")
    }
    val second = when (arch) {
      Architecture.X86_64 -> "x64"
      Architecture.AARCH64 -> "aarch64"
      else -> error("Unsupported architecture for GVM: ${arch.name}")
    }
    return "$first-$second"
  }

  @Suppress("CyclomaticComplexMethod")
  @JvmStatic fun current(): String = System.getProperty("os.arch").let { arch ->
    return when (System.getProperty("os.name")?.lowercase()?.trim() ?: "unknown") {
      "mac os x" -> when (arch) {
        "x86_64" -> DARWIN_AMD64
        "aarch64" -> DARWIN_ARM64
        else -> null
      }

      "win32", "windows", "win" -> when (arch) {
        "x86_64" -> WINDOWS_AMD64
        else -> null
      }

      "linux" -> when (arch) {
        "x86_64" -> LINUX_AMD64
        "aarch64" -> LINUX_ARM64
        else -> null
      }

      else -> null
    } ?: error(
      "Failed to resolve current arch/os (os: ${System.getProperty("os.name")}, arch: ${System.getProperty("os.arch")})"
    )
  }
}

// Suite of download links for a given JDK.
private sealed interface DownloadSuite {
  val linuxAmd64: String
  val linuxArm64: String
  val darwinAmd64: String
  val darwinArm64: String
  val windowsAmd64: String

  fun linkFor(platform: String): String = when (platform) {
    GraalVMPlatform.LINUX_AMD64 -> linuxAmd64
    GraalVMPlatform.LINUX_ARM64 -> linuxArm64
    GraalVMPlatform.DARWIN_AMD64 -> darwinAmd64
    GraalVMPlatform.DARWIN_ARM64 -> darwinArm64
    GraalVMPlatform.WINDOWS_AMD64 -> windowsAmd64
    else -> error("Unrecognized platform for GVM toolchain: $platform")
  }
}

// GraalVM runtime information, in addition to download links.
private sealed interface GraalVMToolchainSuite : DownloadSuite {
  val javaVersion: JavaVersion
  val graalvmVersion: String
  val releaseTrack: ReleaseTrack
  val releaseType: ReleaseType
}

// Suite of download links used for each type.
@JvmRecord private data class GraalVMDownloadSuite(
  override val linuxAmd64: String,
  override val linuxArm64: String,
  override val darwinAmd64: String,
  override val darwinArm64: String,
  override val windowsAmd64: String,
) : DownloadSuite

// GraalVM EE suite type.
private data class OracleGraalVMSuite(
  override val javaVersion: JavaVersion,
  override val graalvmVersion: String,
  override val releaseTrack: ReleaseTrack,
  private val downloads: GraalVMDownloadSuite,
) : DownloadSuite by downloads, GraalVMToolchainSuite {
  override val releaseType: ReleaseType get() = ReleaseType.ENTERPRISE
}

// GraalVM CE suite type.
private class GraalVMCommunitySuite(
  override val javaVersion: JavaVersion,
  override val graalvmVersion: String,
  override val releaseTrack: ReleaseTrack,
  private val downloads: GraalVMDownloadSuite,
) : DownloadSuite by downloads, GraalVMToolchainSuite {
  override val releaseType: ReleaseType get() = ReleaseType.COMMUNITY
}

private fun interface DownloadLink {
  fun generate(gvmVersion: String, platform: String): String
}

private val oracleDownloadUrl = DownloadLink { gvm, platform ->
  "https://github.com/graalvm/oracle-graalvm-ea-builds/releases/download/jdk-$gvm/graalvm-jdk-${gvm}_${platform}_bin.tar.gz"
}

private val gvmCeDownloadUrl = DownloadLink { gvm, platform ->
  val platformCe = platform.replace("x64", "amd64")
  "https://github.com/graalvm/graalvm-ce-dev-builds/releases/download/$gvm/graalvm-community-java24-${platformCe}-dev.tar.gz"
}

private fun linkFor(type: ReleaseType, gvm: String, platform: String): String {
  return when (type) {
    ReleaseType.COMMUNITY -> gvmCeDownloadUrl.generate(gvm, platform)
    ReleaseType.ENTERPRISE -> oracleDownloadUrl.generate(gvm, platform)
  }
}

private fun Pair<String, JavaVersion>.toOracleSuite(track: ReleaseTrack): OracleGraalVMSuite {
  return OracleGraalVMSuite(second, first, track, GraalVMDownloadSuite(
    linuxAmd64 = linkFor(ReleaseType.ENTERPRISE, first, GraalVMPlatform.LINUX_AMD64),
    linuxArm64 = linkFor(ReleaseType.ENTERPRISE, first, GraalVMPlatform.LINUX_ARM64),
    darwinAmd64 = linkFor(ReleaseType.ENTERPRISE, first, GraalVMPlatform.DARWIN_AMD64),
    darwinArm64 = linkFor(ReleaseType.ENTERPRISE, first, GraalVMPlatform.DARWIN_ARM64),
    windowsAmd64 = linkFor(ReleaseType.ENTERPRISE, first, GraalVMPlatform.WINDOWS_AMD64),
  ))
}

private fun Pair<String, JavaVersion>.toCommunitySuite(track: ReleaseTrack) : GraalVMCommunitySuite {
  return GraalVMCommunitySuite(second, first, track, GraalVMDownloadSuite(
    linuxAmd64 = linkFor(ReleaseType.COMMUNITY, first, GraalVMPlatform.LINUX_AMD64),
    linuxArm64 = linkFor(ReleaseType.COMMUNITY, first, GraalVMPlatform.LINUX_ARM64),
    darwinAmd64 = linkFor(ReleaseType.COMMUNITY, first, GraalVMPlatform.DARWIN_AMD64),
    darwinArm64 = linkFor(ReleaseType.COMMUNITY, first, GraalVMPlatform.DARWIN_ARM64),
    windowsAmd64 = linkFor(ReleaseType.COMMUNITY, first, GraalVMPlatform.WINDOWS_AMD64),
  ))
}

public abstract class JvmToolchainResolver : JavaToolchainResolver {
  private companion object {
    // Latest Oracle GraalVM toolchain.
    private val ORACLE_GVM_LATEST = latestOracleGvmVersion.toOracleSuite(ReleaseTrack.EARLY_ACCESS)

    // Latest GraalVM Community Edition toolchain.
    private val GVM_CE_LATEST = latestGraalVmCeVersion.toCommunitySuite(ReleaseTrack.EARLY_ACCESS)

    // All enabled toolchain suites.
    private val enabledSuites = listOf(
      ORACLE_GVM_LATEST,
      GVM_CE_LATEST,
    )

    @JvmStatic fun resolveToolchain(useEnterprise: Boolean, request: JavaToolchainRequest): GraalVMToolchainSuite {
      val jvmVersion = request.javaToolchainSpec.languageVersion.orNull
      val expectedType = if (useEnterprise) ReleaseType.ENTERPRISE else ReleaseType.COMMUNITY

      return enabledSuites.asSequence().filter {
        it.releaseType == expectedType
      }.filter {
        it.javaVersion.majorVersion.toInt() >= (jvmVersion?.asInt() ?: -1)
      }.firstOrNull() ?: error(
        "Failed to locate custom toolchain for parameters"
      )
    }
  }

  override fun resolve(request: JavaToolchainRequest): Optional<JavaToolchainDownload> {
    if (!USE_CUSTOM_TOOLCHAINS) return Optional.empty()
    return resolveToolchain(
      request.javaToolchainSpec.vendor.get() != JvmVendorSpec.GRAAL_VM,
      request,
    ).let {
      Optional.of(JavaToolchainDownload.fromUri(URI.create(it.linkFor(GraalVMPlatform.fromRequest(
        request.buildPlatform.architecture,
        request.buildPlatform.operatingSystem,
      )))))
    }
  }
}

public abstract class JvmToolchainResolverPlugin : Plugin<Settings> {
  @Inject protected abstract fun getToolchainResolverRegistry(): JavaToolchainResolverRegistry

  override fun apply(target: Settings) {
    target.plugins.apply("jvm-toolchain-management")
    getToolchainResolverRegistry().register(JvmToolchainResolver::class.java)
  }
}

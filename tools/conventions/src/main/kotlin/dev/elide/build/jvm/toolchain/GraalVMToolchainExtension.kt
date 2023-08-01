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

package dev.elide.build.jvm.toolchain

import org.gradle.api.initialization.Settings
import org.gradle.api.provider.Property
import java.net.URI
import java.net.URL

/** Settings-time extension for GraalVM toolchain settings. */
@Suppress("MemberVisibilityCanBePrivate", "RedundantVisibilityModifier")
abstract class GraalVMToolchainExtension {
  /** Describes a URL format component. */
  private interface FormatComponent {
    /** Retrieve the symbolic value for this object. */
    val symbol: String
  }

  /** Enumerates editions of GraalVM. */
  enum class GraalVMEdition constructor (override val symbol: String) : FormatComponent {
    /** GraalVM Community Edition. */
    COMMUNITY("ce"),

    /** GraalVM Enterprise Edition. */
    ENTERPRISE("ee"),
  }

  /** Supported OS options for GraalVM. */
  enum class HostOS constructor (override val symbol: String) : FormatComponent {
    /** Linux. */
    LINUX("linux"),

    /** macOS. */
    MACOS("darwin"),

    /** Windows. */
    WINDOWS("windows");

    internal companion object {
      /** @return Resolved current host OS. */
      @JvmStatic fun resolve(): HostOS = System.getProperty("os.name").lowercase().let {
        when {
          it.contains("mac") -> MACOS
          it.contains("win") -> WINDOWS
          it.contains("nux") -> LINUX
          else -> throw IllegalArgumentException("Unsupported OS: $it")
        }
      }
    }
  }

  /** Restricted JVM version type. */
  @JvmInline value class JVMVersion private constructor (private val value: Int) : FormatComponent {
    override val symbol: String get() = "java$value"

    @Suppress("unused") companion object {
      /** JVM version 8. */
      val JAVA8 = JVMVersion(8)

      /** JVM version 11. */
      val JAVA11 = JVMVersion(11)

      /** JVM version 17. */
      val JAVA17 = JVMVersion(17)

      /** JVM version 19. */
      val JAVA19 = JVMVersion(19)
    }
  }

  /** Restricted CPU architecture version type. */
  @JvmInline value class CPUArchitecture private constructor (private val value: String) : FormatComponent {
    override val symbol: String get() = value

    companion object {
      /** x86_64. */
      val X86_64 = CPUArchitecture("amd64")

      /** AMD64. */
      val AMD64 = CPUArchitecture("aarch64")

      /** @return Resolved current CPU architecture. */
      @JvmStatic fun resolve(): CPUArchitecture = System.getProperty("os.arch").lowercase().let {
        when {
          it.contains("amd64") -> X86_64
          it.contains("aarch64") -> AMD64
          else -> throw IllegalArgumentException("Unsupported CPU architecture: $it")
        }
      }
    }
  }

  /**
   * Specifies desired GraalVM toolchain.
   *
   * @param version Version of GraalVM.
   * @param edition Edition of GraalVM.
   * @param javaVersion JVM version of the engine to download.
   * @param host Host OS information.
   * @param arch CPU architecture of the desired VM.
   */
  data class GraalVMSpec internal constructor (
    val version: String,
    val edition: GraalVMEdition,
    val javaVersion: JVMVersion,
    val host: HostOS,
    val arch: CPUArchitecture,
  ) {
    internal companion object {
      /**
       * Create a new GraalVM toolchain specification.
       *
       * @param version Version of GraalVM.
       * @param edition Edition of GraalVM.
       * @param javaVersion JVM version of the engine to download.
       * @param arch CPU architecture of the desired VM.
       */
      @JvmStatic fun create(
        version: String,
        edition: GraalVMEdition,
        javaVersion: JVMVersion,
        arch: CPUArchitecture? = null,
      ): GraalVMSpec = GraalVMSpec(
        version,
        edition,
        javaVersion,
        HostOS.resolve(),
        arch ?: CPUArchitecture.resolve(),
      )
    }
  }

  /** Configuration properties supported by both editions. */
  sealed interface ConfigurableGraalVMToolchain {
    /** Version of GraalVM to use. */
    var version: String

    /** JVM version to use. */
    var javaVersion: JVMVersion
  }

  /** Configurable properties for GraalVM CE. */
  interface GraalVMCommunityEditionToolchain : ConfigurableGraalVMToolchain {
    /** Explicit GitHub download token to use. */
    var githubToken: String?
  }

  /** Configurable properties for GraalVM EE. */
  interface GraalVMEnterpriseEditionToolchain : ConfigurableGraalVMToolchain {
    /** Oracle Enterprise download token to use. */
    var enterpriseToken: String?
  }

  companion object {
    /** Latest known version of GraalVM. */
    const val latestVersion = "22.3.0"

    /** Latest Java version supported by GraalVM. */
    val latestJavaVersion = JVMVersion.JAVA19

    /** URL base for GraalVM Community Edition downloads. */
    const val communityBase = "https://github.com/graalvm/graalvm-ce-builds/releases/download/"

    /** Components of a GVM Community Edition download URL. */
    val communityUrl: List<URLFormatCallable> = listOf(
      { "vm-$version/" },
      { "graalvm-${edition.symbol}-" },
      { "${javaVersion.symbol}-" },
      { "${host.symbol}-" },
      { "${arch.symbol}-" },
      { version },
      { "tar.gz" },
    )

    // Fill default values.
    private fun fillDefaults(ext: GraalVMToolchainExtension): GraalVMToolchainExtension {
      return ext.apply {
        edition.set(GraalVMEdition.COMMUNITY)
        version.set(latestVersion)
        cpuArch.set(CPUArchitecture.resolve())
        javaVersion.set(JVMVersion.JAVA19)
      }
    }

    /** @return GraalVM toolchain management extension. */
    fun Settings.graalvmToolchain(): GraalVMToolchainExtension {
      return fillDefaults(
        extensions.create("elide-graalvm-toolchain", GraalVMToolchainExtension::class.java)
      )
    }
  }

  /** @return Built [GraalVMSpec] for the current settings. */
  internal fun buildSpec(): GraalVMSpec = GraalVMSpec.create(
    version.get(),
    edition.get(),
    javaVersion.get(),
  )

  /** @return Rendered download URL for the specified GraalVM toolchain. */
  internal fun renderDownloadUrl(): URL {
    val spec = buildSpec()
    val base = if (spec.edition == GraalVMEdition.COMMUNITY) communityBase else TODO()
    val components = if (spec.edition == GraalVMEdition.COMMUNITY) communityUrl else TODO()
    return URI.create(
      base +
      components.joinToString(separator = "") { it(spec) }
    ).toURL()
  }

  /** GraalVM edition to use. */
  public abstract val edition: Property<GraalVMEdition>

  /** GraalVM version to use. */
  public abstract val version: Property<String>

  /** GraalVM JVM version to use. */
  public abstract val javaVersion: Property<JVMVersion>

  /** GraalVM CPU arch to use. If `null`, the host arch is used. */
  public abstract val cpuArch: Property<CPUArchitecture?>

  /** GitHub token to use for CE downloads. */
  public abstract val githubToken: Property<String?>

  /** Oracle token to use for EE downloads. */
  public abstract val enterpriseToken: Property<String?>

  /** Configure GraalVM Community edition. */
  public fun communityEdition(
    githubToken: String? = null,
    version: String = latestVersion,
    javaVersion: JVMVersion = latestJavaVersion,
    cpuArch: CPUArchitecture? = null,
  ) {
    this.edition.set(GraalVMEdition.COMMUNITY)
    this.version.set(version)
    this.javaVersion.set(javaVersion)
    if (cpuArch != null) this.cpuArch.set(cpuArch)
    if (githubToken != null) this.githubToken.set(githubToken)
  }

  /** Configure GraalVM Enterprise edition. */
  public fun enterpriseEdition(
    enterpriseToken: String? = null,
    version: String = latestVersion,
    javaVersion: JVMVersion = latestJavaVersion,
    cpuArch: CPUArchitecture? = null,
  ) {
    this.edition.set(GraalVMEdition.COMMUNITY)
    this.version.set(version)
    this.javaVersion.set(javaVersion)
    if (cpuArch != null) this.cpuArch.set(cpuArch)
    if (enterpriseToken != null) this.enterpriseToken.set(enterpriseToken)
  }

  /** Configure GraalVM community edition with a closure. */
  public fun communityEdition(closure: GraalVMCommunityEditionToolchain.() -> Unit) {
    val outer = this
    edition.set(
      GraalVMEdition.COMMUNITY
    )
    val proxy = object : GraalVMCommunityEditionToolchain {
      override var version: String
        get() = outer.version.get()
        set(value) { outer.version.set(value) }

      override var javaVersion: JVMVersion
        get() = outer.javaVersion.get()
        set(value) { outer.javaVersion.set(value) }

      override var githubToken: String?
        get() = outer.githubToken.get()
        set(value) { outer.githubToken.set(value) }
    }
    closure.invoke(proxy)
  }

  /** Configure GraalVM enterprise edition with a closure. */
  public fun enterpriseEdition(closure: GraalVMEnterpriseEditionToolchain.() -> Unit) {
    val outer = this
    edition.set(
      GraalVMEdition.ENTERPRISE
    )
    val proxy = object : GraalVMEnterpriseEditionToolchain {
      override var version: String
        get() = outer.version.get()
        set(value) { outer.version.set(value) }

      override var javaVersion: JVMVersion
        get() = outer.javaVersion.get()
        set(value) { outer.javaVersion.set(value) }

      override var enterpriseToken: String?
        get() = outer.enterpriseToken.get()
        set(value) { outer.enterpriseToken.set(value) }
    }
    closure.invoke(proxy)
  }
}

/** Type alias for a single URL format callable. */
private typealias URLFormatCallable = GraalVMToolchainExtension.GraalVMSpec.() -> String

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

package elide.toolchain.host

import org.gradle.api.Project
import org.jetbrains.kotlin.konan.target.HostManager
import java.util.LinkedList
import java.util.function.Predicate

private const val ELIDE_TARGET = "elide.target"
private const val ELIDE_OS = "elide.targetOs"
private const val ELIDE_ARCH = "elide.targetArch"
private const val ELIDE_LIBC = "elide.targetLibc"

private const val ARM64 = "arm64"
private const val AMD64 = "amd64"
private const val X86_64 = "x86_64"
private const val AARCH64 = "aarch64"
private const val ARMV7 = "armv7"
private const val GNU = "gnu"
private const val MUSL = "musl"
private const val BIONIC = "bionic"
private const val MSVC = "msvc"
private const val APPLE = "apple"
private const val DARWIN = "darwin"
private const val LINUX = "linux"
private const val WINDOWS = "windows"
private const val ANDROID = "android"
private const val ANDROIDEABI = "androideabi"
private const val UNKNOWN = "unknown"
private const val PC = "pc"
private const val LINUX_AMD64_MUSL = "$LINUX-$AMD64-$MUSL"
private const val LINUX_AMD64_GNU = "$LINUX-$AMD64"
private const val LINUX_AARCH64_MUSL = "$LINUX-$AARCH64-$MUSL"
private const val LINUX_AARCH64_GNU = "$LINUX-$AARCH64"
private const val DARWIN_AARCH64 = "$DARWIN-$AARCH64"
private const val DARWIN_AMD64 = "$DARWIN-$AMD64"
private const val WINDOWS_AMD64 = "$WINDOWS-$AMD64"
private const val ANDROID_X86_64 = "$X86_64-$LINUX-$ANDROID"
private const val ANDROID_ARMV7 = "$ARMV7-$LINUX-$ANDROIDEABI"

private fun Project.getProp(name: String): String? = (properties[name] as? String)?.ifBlank { null }

/**
 * Supported operating systems.
 */
public enum class OperatingSystem {
  Mac,
  Linux,
  Windows;

  public companion object {
    @JvmStatic public fun from(token: String): OperatingSystem = when (token.lowercase().trim()) {
      DARWIN -> Mac
      LINUX -> Linux
      WINDOWS -> Windows
      else -> error("Unsupported OS: $token")
    }
  }
}

/**
 * Supported architectures.
 */
public enum class Architecture {
  Amd64,
  Arm64;

  public companion object {
    @JvmStatic public fun from(token: String): Architecture = when (token.lowercase().trim()) {
      AMD64, X86_64 -> Amd64
      ARM64, AARCH64 -> Arm64
      else -> error("Unsupported arch: $token")
    }
  }
}

/**
 * Supported libc implementations.
 */
public enum class Libc {
  Gnu,
  Musl,
  Bionic,
  Msvc;

  public companion object {
    @JvmStatic public fun from(token: String): Libc = when (token.lowercase().trim()) {
      MUSL -> Musl
      BIONIC -> Bionic
      MSVC -> Msvc
      else -> Gnu
    }
  }
}

public interface CriteriaBuilder {
  public fun withOs(vararg allowed: OperatingSystem): CriteriaBuilder
  public fun matchingOs(vararg allowed: OperatingSystem): TargetCriteria.OperatingSystemCriteria
  public fun withArch(vararg allowed: Architecture): CriteriaBuilder
  public fun matchingArch(vararg allowed: Architecture): TargetCriteria.ArchitectureCriteria
  public fun withLibc(vararg allowed: Libc): CriteriaBuilder
  public fun matchingLibc(vararg allowed: Libc): TargetCriteria.LibcCriteria
  public fun build(): TargetPredicate
}

public fun interface TargetPredicate : Predicate<TargetInfo> {
  /**
   * Combine criteria with another to create a compound criteria that matches both.
   *
   * @param other The other criteria to combine with.
   * @return A new criteria that matches both the original and the other.
   */
  public operator fun plus(other: TargetPredicate): TargetPredicate = TargetPredicate {
    test(it) && other.test(it)
  }
}

/**
 * Describes one or more criteria for matching a target.
 *
 * Criteria can match based on any arbitrary condition; pre-made conditions are available for operating systems, libc
 * implementations, and architectures.
 */
public sealed interface TargetCriteria<X, V>: TargetPredicate where V: TargetPredicate {
  /**
   * Test whether a target component matches.
   *
   * Component-based matching is type-safe; type [X] is typically an enumerated constant of an OS, architecture, or some
   * other target characteristic.
   *
   * Target info is provided for context.
   *
   * @param target The target information.
   * @param component The component to test.
   * @return Whether the component matches the criteria.
   */
  public fun matches(target: TargetInfo?, component: X): Boolean

  /** Generic extensible criteria that acts as a normal predicate. */
  public interface GenericCriteria<X>: TargetCriteria<X, GenericCriteria<X>>, Predicate<TargetInfo>

  /** Criteria matching against the operating system. */
  public fun interface OperatingSystemCriteria:
    TargetCriteria<OperatingSystem, OperatingSystemCriteria>,
    Predicate<TargetInfo> {
    override fun test(t: TargetInfo): Boolean = matches(t, OperatingSystem.from(t.os))
  }

  /** Criteria matching against the architecture. */
  public fun interface ArchitectureCriteria:
    TargetCriteria<Architecture, ArchitectureCriteria>,
    Predicate<TargetInfo> {
    override fun test(t: TargetInfo): Boolean = matches(t, Architecture.from(t.arch))
  }

  /** Criteria matching against the libc implementation. */
  public fun interface LibcCriteria:
    TargetCriteria<Libc, LibcCriteria>,
    Predicate<TargetInfo> {
    override fun test(t: TargetInfo): Boolean = matches(t, Libc.from(t.libc))
  }

  public companion object {
    public fun matchingOs(vararg allowed: OperatingSystem): OperatingSystemCriteria = OperatingSystemCriteria { _, os ->
      allowed.contains(os)
    }

    public fun matchingArch(vararg allowed: Architecture): ArchitectureCriteria = ArchitectureCriteria { _, arch ->
      allowed.contains(arch)
    }

    public fun matchingLibc(vararg allowed: Libc): LibcCriteria = LibcCriteria { _, component ->
      allowed.contains(component)
    }

    public fun allOf(vararg criteria: TargetPredicate): TargetPredicate = TargetPredicate {
      criteria.all { item -> item.test(it) }
    }

    public fun anyOf(vararg criteria: TargetPredicate): TargetPredicate = TargetPredicate {
      criteria.any { item -> item.test(it) }
    }

    /**
     * Match the provided [target] against all provided [criteria].
     *
     * @param target The target to match.
     * @param criteria The criteria builder.
     */
    public fun allOf(target: TargetInfo, vararg criteria: TargetPredicate): Boolean {
      return allOf(*criteria).test(target)
    }

    /**
     * Match the provided [target] against any provided [criteria].
     *
     * @param target The target to match.
     * @param criteria The criteria builder.
     */
    public fun anyOf(target: TargetInfo, vararg criteria: TargetPredicate): Boolean {
      return anyOf(*criteria).test(target)
    }

    /**
     * Match the provided [target] against the criteria assembled by [builder].
     *
     * @param target The target to match.
     * @param builder The criteria builder.
     */
    public fun matches(target: TargetInfo, builder: CriteriaBuilder.() -> Unit): Boolean {
      val criteriaSuite = LinkedList<Predicate<TargetInfo>>()

      val ctx = object: CriteriaBuilder {
        override fun withOs(vararg allowed: OperatingSystem): CriteriaBuilder = apply {
          criteriaSuite.add(matchingOs(*allowed))
        }

        override fun withLibc(vararg allowed: Libc): CriteriaBuilder = apply {
          criteriaSuite.add(matchingLibc(*allowed))
        }

        override fun withArch(vararg allowed: Architecture): CriteriaBuilder = apply {
          criteriaSuite.add(matchingArch(*allowed))
        }

        override fun matchingArch(vararg allowed: Architecture): ArchitectureCriteria =
          TargetCriteria.matchingArch(*allowed)

        override fun matchingLibc(vararg allowed: Libc): LibcCriteria =
          TargetCriteria.matchingLibc(*allowed)

        override fun matchingOs(vararg allowed: OperatingSystem): OperatingSystemCriteria =
          TargetCriteria.matchingOs(*allowed)

        override fun build(): TargetPredicate = TargetPredicate {
          criteriaSuite.all { item -> item.test(it) }
        }
      }
      builder.invoke(ctx)
      return ctx.build().test(target)
    }

    /**
     * Match the provided [target] against the criteria assembled by [builder].
     *
     * @param target The target to match.
     * @param builder The criteria builder.
     */
    public fun <R> withTarget(target: TargetInfo, builder: CriteriaBuilder.() -> Unit, thenDo: () -> R): R {
      return if (matches(target, builder)) {
        thenDo()
      } else {
        error("Target does not match criteria.")
      }
    }
  }
}

/**
 * Defines static criteria which can be used to match against [TargetInfo].
 */
public data object Criteria {
  public val Mac: TargetCriteria.OperatingSystemCriteria = TargetCriteria.matchingOs(OperatingSystem.Mac)
  public val Linux: TargetCriteria.OperatingSystemCriteria = TargetCriteria.matchingOs(OperatingSystem.Linux)
  public val Windows: TargetCriteria.OperatingSystemCriteria = TargetCriteria.matchingOs(OperatingSystem.Windows)

  public val Amd64: TargetCriteria.ArchitectureCriteria = TargetCriteria.matchingArch(Architecture.Amd64)
  public val Arm64: TargetCriteria.ArchitectureCriteria = TargetCriteria.matchingArch(Architecture.Arm64)

  public val Gnu: TargetCriteria.LibcCriteria = TargetCriteria.matchingLibc(Libc.Gnu)
  public val Musl: TargetCriteria.LibcCriteria = TargetCriteria.matchingLibc(Libc.Musl)
  public val Msvc: TargetCriteria.LibcCriteria = TargetCriteria.matchingLibc(Libc.Msvc)

  public val LinuxAmd64: TargetPredicate = Linux + Amd64
  public val LinuxArm64: TargetPredicate = Linux + Arm64
  public val LinuxGnu: TargetPredicate = Linux + Gnu
  public val LinuxMusl: TargetPredicate = Linux + Musl
  public val LinuxAmd64Gnu: TargetPredicate = Linux + Gnu + Amd64
  public val LinuxAmd64Musl: TargetPredicate = Linux + Musl + Amd64
  public val LinuxArm64Gnu: TargetPredicate = Linux + Gnu + Arm64
  public val LinuxArm64Musl: TargetPredicate = Linux + Musl + Arm64
  public val MacAmd64: TargetPredicate = Mac + Amd64
  public val MacArm64: TargetPredicate = Mac + Arm64
  public val WindowsAmd64: TargetPredicate = Windows + Amd64
}

/**
 * Describes information about a native target supported by Elide.
 *
 * Target info is typically implemented by a [ElideTarget] enumerated instance; such instances only exist for targets
 * supported by Elide.
 */
public interface TargetInfo {
  public val os: String
  public val arch: String
  public val libc: String

  public val tag: String get() = "$os-$arch"

  public val triple: String

  public val resources: List<String>
    get() = listOf(
      "META-INF/elide/embedded/runtime/*/*-$tag.*",
    )

  public companion object {
    private fun defaultLibcFor(os: String): String = when (os) {
      DARWIN -> APPLE
      LINUX -> GNU
      WINDOWS -> MSVC
      else -> error("Unsupported OS: $os")
    }

    /**
     * Resolve the current platform's target info.
     */
    public fun current(project: Project): TargetInfo = project.resolveTarget()

    /**
     * Resolve the current platform's target info.
     */
    public fun Project.from(
      os: String = System.getProperty("os.name").lowercase(),
      arch: String = System.getProperty("os.arch").lowercase(),
      libc: String = defaultLibcFor(os),
    ): TargetInfo = findTarget(
      os,
      arch,
      libc,
    )
  }
}

/**
 * Supported Elide target environments.
 *
 * Describes each supported native environment and its characteristics and capabilities; each instance exists at the
 * intersection of an [os], [arch], and [libc] implementation.
 *
 * Information provided by instances include:
 *
 * - [os]: The target operating system.
 * - [arch]: The target micro-architecture.
 * - [libc]: The target C standard library implementation.
 * - [tag]: A shorthand identifier for the target; this is an Elide-specific value which ends up in release tarballs.
 * - [triple]: The LLVM target triple for the target; used for Rust and C/C++ compilation.
 * - [resources]: Resources inclusions for this target with regard to final JARs and native images.
 */
public enum class ElideTarget(
  override val os: String,
  override val arch: String,
  override val libc: String,
) : TargetInfo {
  MACOS_AARCH64(DARWIN, AARCH64, APPLE),
  MACOS_AMD64(DARWIN, AMD64, APPLE),
  LINUX_AMD64_GNU(LINUX, AMD64, GNU),
  LINUX_AMD64_MUSL(LINUX, AMD64, MUSL),
  LINUX_AARCH64_GNU(LINUX, AARCH64, GNU),
  LINUX_AARCH64_MUSL(LINUX, AARCH64, MUSL),
  LINUX_AMD64_BIONIC(LINUX, AMD64, BIONIC),
  LINUX_AARCH64_BIONIC(LINUX, AARCH64, BIONIC),
  WINDOWS_AMD64(WINDOWS, AMD64, MSVC);

  override val triple: String get() = when (this) {
    LINUX_AMD64_MUSL,
    LINUX_AMD64_GNU,
    LINUX_AARCH64_GNU,
    LINUX_AARCH64_MUSL -> "${canonicalArch(arch)}-$UNKNOWN-$os-$libc"
    LINUX_AMD64_BIONIC -> ANDROID_X86_64
    LINUX_AARCH64_BIONIC -> ANDROID_ARMV7
    MACOS_AARCH64,
    MACOS_AMD64 -> "${canonicalArch(arch)}-$APPLE-$os"
    WINDOWS_AMD64 -> "${canonicalArch(arch)}-$PC-$os-$libc"
  }

  private companion object {
    private fun canonicalArch(arch: String): String = when (arch) {
      AMD64, X86_64 -> X86_64
      AARCH64, ARM64 -> AARCH64
      else -> error("Unsupported architecture: $arch")
    }
  }
}

private fun Project.resolveTargetArch(): String {
  val explicit = getProp(ELIDE_ARCH)
  val host = System.getProperty("os.arch").lowercase().trim()
  return explicit ?: host
}

private fun Project.findTarget(os: String, arch: String, libc: String): ElideTarget {
  return when (val assembled = ElideTarget.entries.find { it.os == os && it.arch == arch && it.libc == libc }) {
    // ---- 3. Then we should take after the host.
    null -> when {
      // Only AMD64 is supported on Windows.
      HostManager.hostIsMingw -> ElideTarget.WINDOWS_AMD64

      // Linux supports multiple libc implementations and architectures.
      HostManager.hostIsLinux -> when (val linuxLibc = getProp(ELIDE_LIBC) ?: GNU) {
        GNU -> when (resolveTargetArch()) {
          AARCH64 -> ElideTarget.LINUX_AARCH64_GNU
          else -> ElideTarget.LINUX_AMD64_GNU
        }
        MUSL -> when (resolveTargetArch()) {
          AARCH64 -> ElideTarget.LINUX_AARCH64_MUSL
          else -> ElideTarget.LINUX_AMD64_MUSL
        }
        BIONIC -> when (resolveTargetArch()) {
          AARCH64 -> ElideTarget.LINUX_AARCH64_BIONIC
          else -> ElideTarget.LINUX_AMD64_BIONIC
        }
        else -> error("Unsupported libc: $linuxLibc")
      }

      // macOS supports only two architectures and no variance in libc.
      HostManager.hostIsMac -> when (System.getProperty("os.arch")) {
        AMD64, X86_64 -> ElideTarget.MACOS_AMD64
        else -> ElideTarget.MACOS_AARCH64
      }

      else -> error(
        "Only macOS, Linux, and Windows are supported for Elide."
      )
    }

    else -> assembled
  }
}

/**
 * Resolve target information for native builds; this includes the target operating system, micro-architecture, and libc
 * implementation.
 *
 * If a target is specified explicitly with `elide.target`, this takes precedence. Otherwise, the target is assembled
 * from piecemeal properties listed below (and defaults, where needed). If no properties are specified at all, defaults
 * result in a host-targeted build.
 *
 * This code does not contemplate flags like `march` and `mtune` directly; for example, no consideration is made for
 * symbolic architecture targets like `native`.
 *
 * @param target The target string to resolve.
 * @return The resolved target information.
 */
public fun Project.resolveTarget(target: String? = getProp(ELIDE_TARGET)): ElideTarget = when (target) {
  // ---- 1. Is the target specified explicitly?

  LINUX_AMD64_MUSL -> ElideTarget.LINUX_AMD64_MUSL
  LINUX_AMD64_GNU -> ElideTarget.LINUX_AMD64_GNU
  LINUX_AARCH64_MUSL -> ElideTarget.LINUX_AARCH64_MUSL
  LINUX_AARCH64_GNU -> ElideTarget.LINUX_AARCH64_GNU
  DARWIN_AMD64 -> ElideTarget.MACOS_AMD64
  DARWIN_AARCH64 -> ElideTarget.MACOS_AARCH64
  WINDOWS_AMD64 -> ElideTarget.WINDOWS_AMD64

  // ---- 2. Is the target specified piecemeal?

  else -> findTarget(
    getProp(ELIDE_OS) ?: System.getProperty("os.name").lowercase(),
    getProp(ELIDE_ARCH) ?: System.getProperty("os.arch"),
    getProp(ELIDE_LIBC) ?: GNU,
  )
}

package elide.runtime.core

/**
 * Provides read-only information about the Host platform, which can be used by plugins to run platform-specific
 * code.
 */
@DelicateElideApi public data class HostPlatform(
  public val os: OperatingSystem,
  public val arch: Architecture,
) {
  /** Enumerates the Operating System families supported by the runtime */
  public enum class OperatingSystem {
    /** Linux OS family, displayed as 'linux' in name strings. */
    LINUX,

    /** MacOS family, may be displayed as 'macos' or 'darwin'. */
    DARWIN,

    /** Windows OS family, displayed as 'windows' in name strings. */
    WINDOWS,
  }

  /** Enumerates the architectures supported by the runtime. */
  public enum class Architecture {
    /** 64-bit x86 architecture family, may be displayed as 'x86_64' or 'amd64'. */
    AMD64,

    /** 64-bit ARM architecture family, may be displayed as 'aarch64' or 'arm64'. */
    ARM64,
  }

  /**
   * Returns a consistent platform name string with the format '<os>_<arch>', such as `linux_aarch64`, `windows_amd64`,
   * or `darwin_arm64`. The returned string is guaranteed to be compatible with the [parsePlatform] function.
   *
   * @return A short string representation for this platform.
   */
  public fun platformString(): String {
    return "${os.name.lowercase()}$STANDARD_SEPARATOR${arch.name.lowercase()}"
  }

  override fun toString(): String {
    // provide a more compact representation, such as 'linux_amd64'
    return platformString()
  }

  public companion object {
    private const val STANDARD_SEPARATOR = '_'

    /** Parse a generic OS name, such as the value obtained from the 'os.name' system property. */
    public fun parseOperatingSystem(name: String): OperatingSystem = when {
      name.contains("linux") -> OperatingSystem.LINUX
      name.contains("windows") -> OperatingSystem.WINDOWS
      name.contains("mac") || name.contains("darwin") -> OperatingSystem.DARWIN
      else -> error("Unrecognized Operating System family: $name")
    }

    /** Parse a generic architecture name, such as the value obtained from the 'os.arch' system property. */
    public fun parseArchitecture(name: String): Architecture = when {
      name.contains("x86_64") || name.contains("amd64") -> Architecture.AMD64
      name.contains("arm64") || name.contains("aarch64") -> Architecture.ARM64
      else -> error("Unrecognized architecture: $name")
    }

    /** Parse a well-formed [HostPlatform] value, as obtained from [HostPlatform.toString]. */
    public fun parsePlatform(name: String): HostPlatform {
      val separatorIndex = name.indexOf(STANDARD_SEPARATOR)
      require(separatorIndex != -1) { "Invalid platform name: $name" }

      return HostPlatform(
        os = parseOperatingSystem(name.substring(startIndex = 0, endIndex = separatorIndex)),
        arch = parseArchitecture(name.substring(startIndex = separatorIndex + 1)),
      )
    }

    /**
     * Resolve information about the Host, such as OS and arch. This info is used by language plugins to load
     * platform-specific resources.
     */
    public fun resolve(): HostPlatform {
      return HostPlatform(
        os = parseOperatingSystem(System.getProperty("os.name", "unknown").lowercase()),
        arch = parseArchitecture(System.getProperty("os.arch", "unknown").lowercase()),
      )
    }
  }
}

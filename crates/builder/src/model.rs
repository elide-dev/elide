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

#![forbid(unsafe_op_in_unsafe_fn, unused_unsafe, dead_code)]

/// Build-time constants describing active Elide features.
pub mod features;

/// Build-time constants and configurations for Elide's language engines.
pub mod lang;

// Constants used for OS identification.
const DARWIN: &str = "darwin";
const LINUX: &str = "linux";
const WINDOWS: &str = "windows";

// Constants used for architecture identification.
const AMD64: &str = "amd64";
const ARM64: &str = "aarch64";

// Constants used for build profiles.
const DEBUG: &str = "debug";
const RELEASE: &str = "release";

/// Minimum supported version of macOS.
pub const MACOS_MIN: &str = "14.0";

/// Enumerates the types of build profiles which Elide supports.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq)]
#[typeshare]
pub enum BuildMode {
  Debug,
  Release,
}

impl BuildMode {
  pub const fn as_str(&self) -> &'static str {
    match self {
      BuildMode::Debug => DEBUG,
      BuildMode::Release => RELEASE,
    }
  }

  pub const fn current() -> Self {
    if cfg!(debug_assertions) {
      BuildMode::Debug
    } else {
      BuildMode::Release
    }
  }
}

/// Enumerates supported operating systems.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq)]
#[typeshare]
pub enum OperatingSystem {
  Darwin,
  Linux,
  Windows,
}

impl OperatingSystem {
  pub const fn as_str(&self) -> &'static str {
    match self {
      OperatingSystem::Darwin => DARWIN,
      OperatingSystem::Linux => LINUX,
      OperatingSystem::Windows => WINDOWS,
    }
  }

  pub const fn current() -> Self {
    if cfg!(windows) {
      OperatingSystem::Windows
    } else if cfg!(target_os = "macos") {
      OperatingSystem::Darwin
    } else {
      OperatingSystem::Linux
    }
  }
}

/// Enumerates supported target architectures.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq)]
#[typeshare]
pub enum Architecture {
  Amd64,
  Arm64, // alias for aarch64 on applicable platforms
}

impl Architecture {
  pub const fn as_str(&self) -> &'static str {
    match self {
      Architecture::Amd64 => AMD64,
      Architecture::Arm64 => ARM64,
    }
  }

  pub const fn current() -> Self {
    if cfg!(target_arch = "x86_64") {
      Architecture::Amd64
    } else {
      Architecture::Arm64
    }
  }
}

/// Host Info.
///
/// Describes information about the host which Elide was built for; this information is assembled at compile-time.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq)]
#[typeshare]
pub struct HostInfo {
  /// Operating system of the host.
  pub os: OperatingSystem,

  /// Architecture of the host.
  pub arch: Architecture,
}

/// Rust Info.
///
/// Describes information about the Rust compiler used to build Elide; this information is assembled at compile-time.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq)]
#[typeshare]
pub struct RustInfo {
  /// Version of the Rust compiler used to build Elide.
  pub version: &'static str,

  /// Channel of the Rust compiler used to build Elide.
  pub channel: &'static str,
}

/// Elide Info.
///
/// Compile-time information about the current build of Elide, including the active version, target, engines, build
/// profile, and so on.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq)]
#[typeshare]
pub struct ElideInfo {
  /// Current version string for Elide.
  pub version: &'static str,

  /// Current target being built.
  pub target: &'static str,

  /// Current build profile.
  pub profile: &'static str,

  /// Information about the host OS and architecture which this build of Elide targets.
  pub host: HostInfo,

  /// Information about the Rust toolchain used to build Elide.
  pub rust: RustInfo,
}

impl ElideInfo {
  /// Create a new instance of ElideInfo.
  const fn new() -> Self {
    Self {
      version: env!("ELIDE_TARGET"),
      target: env!("ELIDE_TARGET"),
      profile: env!("ELIDE_PROFILE"),
      host: HostInfo {
        os: OperatingSystem::current(),
        arch: Architecture::current(),
      },
      rust: RustInfo {
        version: env!("RUSTC_VERSION"),
        channel: env!("RUSTC_CHANNEL"),
      },
    }
  }

  /// Retrieve the current static info about this build of Elide.
  pub const fn current() -> Self {
    ELIDE_INFO
  }
}

/// Info about the current build of Elide.
pub const ELIDE_INFO: ElideInfo = ElideInfo::new();

#[cfg(test)]
mod tests {
  use super::*;

  #[test]
  fn test_build_mode() {
    assert_eq!(BuildMode::Debug.as_str(), DEBUG);
    assert_eq!(BuildMode::Release.as_str(), RELEASE);
    assert_eq!(BuildMode::current(), BuildMode::current());
  }

  #[test]
  fn test_operating_system() {
    assert_eq!(OperatingSystem::Darwin.as_str(), DARWIN);
    assert_eq!(OperatingSystem::Linux.as_str(), LINUX);
    assert_eq!(OperatingSystem::Windows.as_str(), WINDOWS);
    assert_eq!(OperatingSystem::current(), OperatingSystem::current());
  }

  #[test]
  fn test_architecture() {
    assert_eq!(Architecture::Amd64.as_str(), AMD64);
    assert_eq!(Architecture::Arm64.as_str(), ARM64);
    assert_eq!(Architecture::current(), Architecture::current());
  }

  #[test]
  fn test_host_info() {
    let host = HostInfo {
      os: OperatingSystem::current(),
      arch: Architecture::current(),
    };

    assert_eq!(host.os, OperatingSystem::current());
    assert_eq!(host.arch, Architecture::current());
  }

  #[test]
  fn test_rust_info() {
    let rust = RustInfo {
      version: env!("RUSTC_VERSION"),
      channel: env!("RUSTC_CHANNEL"),
    };

    assert_eq!(rust.version, env!("RUSTC_VERSION"));
    assert_eq!(rust.channel, env!("RUSTC_CHANNEL"));
  }

  #[test]
  fn test_elide_info() {
    let elide = ElideInfo::new();

    assert_eq!(elide.version, env!("ELIDE_TARGET"));
    assert_eq!(elide.target, env!("ELIDE_TARGET"));
    assert_eq!(elide.profile, env!("ELIDE_PROFILE"));
    assert_eq!(elide.host.os, OperatingSystem::current());
    assert_eq!(elide.host.arch, Architecture::current());
    assert_eq!(elide.rust.version, env!("RUSTC_VERSION"));
    assert_eq!(elide.rust.channel, env!("RUSTC_CHANNEL"));
  }
}

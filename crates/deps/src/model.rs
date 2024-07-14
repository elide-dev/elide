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

use serde::{Deserialize, Serialize};
use typeshare::typeshare;

/// Enumerates recognized scopes for dependency mappings.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, Ord, PartialOrd)]
#[typeshare]
pub enum DependencyScope {
  /// The dependency is required for compiling the project.
  Compile,

  /// The dependency is required for running the project.
  Runtime,

  /// The dependency is required for development only.
  Development,

  /// The dependency is required for testing only.
  Test,

  /// The dependency is provided by the runtime environment.
  Provided,

  /// The dependency is provided by the host system.
  System,

  /// The dependency is a constraint or suite of constraints.
  Import,

  /// The dependency provides a catalog of dependencies.
  Catalog,
}

/// Enumerates recognized scopes for dependency mappings.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, Ord, PartialOrd)]
#[typeshare]
pub enum DependencyEcosystemType {
  /// The dependency originates from Maven Central or a Maven-style repository.
  Maven,

  /// The dependency originates from NPM or a NPM-style repository.
  Npm,

  /// The dependency originates from JSR or a JSR-style repository.
  Jsr,

  /// The dependency originates from PyPI or a PyPI-style repository.
  PyPi,

  /// The dependency originates from RubyGems or a RubyGems-style repository.
  RubyGems,

  /// The dependency originates from HuggingFace.
  HuggingFace,

  /// The dependency originates from Git.
  Git,
}

/// Describes a single dependency lockfile within the context of a given Elide project or dependency graph.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, Ord, PartialOrd)]
#[typeshare]
pub struct Lockfile {
  /// The local identifier for the lockfile.
  pub id: u8,

  /// The name of the lockfile.
  pub path: Option<String>,

  /// Whether this lockfile is virtual.
  pub synthetic: bool,

  /// The ecosystem type for this lockfile.
  pub ecosystem: DependencyEcosystemType,

  /// CRC64 fingerprint of this lock file's content.
  pub fingerprint: u64,

  /// Last modification time of this lockfile.
  pub modtime: u64,
}

/// Enumerates recognized protocols for accessing dependency repositories.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, Ord, PartialOrd)]
#[typeshare]
pub enum RepositoryAccessProtocol {
  /// The repository is accessed via HTTP.
  Https,

  /// The repository is accessed via Git+HTTP.
  GitHttps,

  /// The repository is accessed via SSH.
  GitSsh,

  /// The repository is accessed via S3-style storage.
  S3,

  /// The repository is located on-disk.
  Disk,
}

/// Describes top-level configuration for a single dependency repository.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, Ord, PartialOrd)]
#[typeshare]
pub struct RepositoryConfig {
  /// Whether this repository is a "default" repository for a given ecosystem.
  pub is_default: bool,

  /// Whether this repository is "built-in."
  pub is_builtin: bool,

  /// Whether this repository is local to the user's machine.
  pub is_local: bool,
}

/// Describes a single dependency repository.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, Ord, PartialOrd)]
#[typeshare]
pub struct Repository {
  /// The local identifier for the repository.
  pub id: u16,

  /// The name of the repository.
  pub name: String,

  /// The layout type employed by the repository.
  pub layout: DependencyEcosystemType,

  /// The protocol used to access the repository.
  pub protocol: RepositoryAccessProtocol,

  /// The URI of the repository.
  pub uri: String,

  /// The configuration for the repository.
  pub config: RepositoryConfig,
}

/// Describes how a given repository is employed by a dependency graph.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, Ord, PartialOrd)]
#[typeshare]
pub struct RepositoryAttachment {
  /// The local identifier for the repository.
  pub id: u16,
}

/// Describes a unique dependency version within the context of a dependency graph.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, Ord, PartialOrd)]
#[typeshare]
pub struct DependencyVersion {
  /// The local identifier for the repository.
  pub id: u32,

  /// The owning dependency ID.
  pub owner: u32,

  /// The formatted `purl` for this dependency version.
  pub purl: String,
}

/// Models the information provided within a declaration of a dependency version against a project.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, Ord, PartialOrd)]
#[typeshare]
pub struct DependencyVersionRequest {
  /// User is requesting the latest released dependency.
  pub latest: bool,

  /// User is requesting the latest stable dependency.
  pub stable: bool,

  /// User is requesting the latest snapshot dependency.
  pub snapshot: bool,

  /// Specific semver pin requested.
  pub semver: Option<String>,
}

/// Describes a dependency declared within a project.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, Ord, PartialOrd)]
#[typeshare]
pub struct DependencyRequest {
  /// The local identifier assigned to the dependency.
  pub id: u32,

  /// The semver pin declared for this dependency declaration.
  pub version: DependencyVersionRequest,
}

/// Describes a single dependency within the context of a dependency graph, ignorant of version information.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, Ord, PartialOrd)]
#[typeshare]
pub struct Dependency {
  /// The local identifier for the repository.
  pub id: u32,

  /// The formatted `purl` for this dependency.
  pub purl: String,
}

/// Describes how a given dependency is employed by a dependency graph.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, Ord, PartialOrd)]
#[typeshare]
pub struct DependencyAttachment {
  /// The local identifier for the dependency.
  pub id: u32,
}

/// Declares dependency information for a project; can be structured as a universal catalog recognizable by Elide, or a
/// file in conventional form for a given language ecosystem.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, Ord, PartialOrd)]
#[typeshare]
pub struct DependencyManifest {
  /// Unique ID assigned to this manifest.
  pub id: u8,

  /// The name declared within the manifest, as applicable; generated or provided.
  pub name: Option<String>,

  /// The version declared within the manifest, as applicable.
  pub version: Option<String>,

  /// Path on-disk to this manifest.
  pub path: Option<String>,

  /// Whether this manifest is virtual.
  pub synthetic: bool,

  /// The ecosystem type for this manifest.
  pub ecosystem: DependencyEcosystemType,

  /// Resolved lockfile which matches this manifest, if applicable/if found.
  pub lockfile: Option<Lockfile>,

  /// CRC64 fingerprint of this manifest's content.
  pub fingerprint: u64,

  /// Last modification time of this manifest.
  pub modtime: u64,

  /// Dependencies attached to this manifest.
  pub dependencies: Vec<DependencyAttachment>,
}

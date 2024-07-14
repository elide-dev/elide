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

use base::prelude::*;
use serde::{Deserialize, Serialize};
use typeshare::typeshare;

/// Defines version information for a declaration within a Gradle-style version catalog.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq)]
#[typeshare]
pub struct GradleVersionInfo {
  /// Describes a literal version specified as part of a mapping.
  pub version: Option<String>,

  /// Describes a reference to a declared version within the versions block of the catalog.
  pub reference: Option<String>,
}

/// Defines plugin dependencies within a Gradle-style version catalog.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq)]
#[typeshare]
pub struct GradlePluginDefinition {}

/// Defines library dependencies within a Gradle-style version catalog.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq)]
#[typeshare]
pub struct GradleLibraryDefinition {
    /// Describes a full Maven coordinate.
    pub module: Option<String>,

    /// Describes the group portion of a Maven coordinate.
    pub group: Option<String>,

    /// Describes the name portion of a Maven coordinate.
    pub name: Option<String>,

    /// Describes the declared version info for the dependency.
    pub version: Option<GradleVersionInfo>,
}

/// Describes the top-level structure of a Gradle-style dependency catalog.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq)]
#[typeshare]
pub struct GradleDependencyCatalog {
  /// Maps version constants to dot-nested names.
  pub versions: std::collections::HashMap<String, String>,

  /// Maps plugin definitions to dot-nested names.
  pub plugins: std::collections::HashMap<String, GradlePluginDefinition>,

  /// Maps library definitions to dot-nested names.
  pub libraries: std::collections::HashMap<String, GradleLibraryDefinition>,

  /// Maps bundle definitions to dot-nested names.
  pub bundles: std::collections::HashMap<String, Vec<String>>,
}

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

/// Versions of project config structure.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, Ord, PartialOrd)]
#[typeshare]
#[derive(Default)]
pub enum ProjectConfigVersion {
  /// Version 1: Initial version.
  #[default]
  V1,
}

/// Basic information about a project.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, Ord, PartialOrd, Default)]
#[typeshare]
pub struct ProjectInfo {
  /// Name of the project.
  pub name: Option<String>,

  /// Declared version of the project.
  pub version: Option<String>,

  /// License token/expression for this project.
  pub license: Option<String>,
}

/// Project configuration top-level structure.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, Ord, PartialOrd, Default)]
#[typeshare]
pub struct ProjectConfig {
  /// Structural version of the project configuration; defaults to latest.
  pub version: ProjectConfigVersion,

  /// Basic information about the project.
  pub project: ProjectInfo,
}

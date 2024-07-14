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

/// Enumerates known Elide features which can be controlled at build-time.
#[derive(Debug, Serialize, Deserialize, PartialEq)]
pub enum Feature {
  Baseline,
}

/// Describes metadata relating to a specific `Feature`.
#[derive(Debug, Serialize, Deserialize, PartialEq)]
pub struct FeatureInfo {
  /// CRC32 of the feature's name; used as its ID.
  pub id: u32,

  /// The name of the feature.
  pub name: &'static str,

  /// A brief description of the feature.
  pub description: &'static str,

  /// Whether the feature is enabled.
  pub enabled: bool,
}

/// The baseline feature set which is always enabled.
const BASELINE: FeatureInfo = FeatureInfo {
  id: 0x00000000,
  name: "baseline",
  description: "The baseline feature set which is always enabled.",
  enabled: true,
};

/// All known features.
const ALL_FEATURES: &[Feature] = &[Feature::Baseline];

/// All known feature info records.
const ALL_FEATURE_INFOS: &[FeatureInfo] = &[BASELINE];

impl Feature {
  /// Returns the metadata for all known `Feature`s.
  pub const fn all() -> &'static [Feature] {
    ALL_FEATURES
  }

  /// Returns the metadata for all known `Feature`s.
  pub const fn infos() -> &'static [FeatureInfo] {
    ALL_FEATURE_INFOS
  }

  /// Returns the metadata for a specific `Feature`.
  pub const fn info(&self) -> &'static FeatureInfo {
    match self {
      Feature::Baseline => &BASELINE,
    }
  }

  /// Returns the name for a specific `Feature`.
  pub const fn name(&self) -> &'static str {
    self.info().name
  }

  /// Returns the description for a specific `Feature`.
  pub const fn description(&self) -> &'static str {
    self.info().description
  }

  /// Indicates whether this feature is enabled.
  pub const fn enabled(&self) -> bool {
    self.info().enabled
  }
}

#[cfg(test)]
mod tests {
  use super::*;

  #[test]
  fn test_feature_info() {
    assert_eq!(Feature::Baseline.info().name, "baseline");
    assert_eq!(
      Feature::Baseline.info().description,
      "The baseline feature set which is always enabled."
    );
    assert!(Feature::Baseline.info().enabled);
  }

  #[test]
  fn test_feature_name() {
    assert_eq!(Feature::Baseline.name(), "baseline");
  }

  #[test]
  fn test_feature_description() {
    assert_eq!(
      Feature::Baseline.description(),
      "The baseline feature set which is always enabled."
    );
  }

  #[test]
  fn test_feature_enabled() {
    assert!(Feature::Baseline.enabled());
  }
}

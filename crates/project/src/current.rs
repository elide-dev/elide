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

pub use crate::config::ProjectConfig;
use std::path::PathBuf;
use std::sync::Arc;

/// Primary name of Elide's project configuration file.
pub const CONFIG_FILE_NAME: &str = "elide.toml";

/// Secondary name of Elide's project configuration file.
pub const CONFIG_FILE_NAME_ALT: &str = ".elide.toml";

/// Implements error codes which can surface when parsing or locating project configuration.
#[derive(Debug)]
pub enum ConfigErr {
  IoErr(std::io::Error),
  ParseErr,
}

/// Parse the project configuration file at the provided `path`.
///
/// # Arguments
/// * `path` - Path to the project configuration file.
///
/// # Returns
/// The parsed project configuration as a `Result`.
pub fn parse_config_file(path: Arc<PathBuf>) -> Result<ProjectConfig, ConfigErr> {
  let buf = path.to_path_buf();

  // read file contents
  let contents = std::fs::read_to_string(&buf).map_err(|err| ConfigErr::IoErr(err))?;
  let parsed: ProjectConfig = toml::from_str(&*contents).map_err(|_| ConfigErr::ParseErr)?;
  Ok(parsed)
}

/// Parse the project configuration file at the provided `path`.
///
/// # Arguments
/// * `seed` - Optional path where we should start looking; defaults to current working directory.
///
/// # Returns
/// The located configuration file path, or `None`.
pub fn locate_config_file(seed: Option<Arc<PathBuf>>) -> Option<PathBuf> {
  // locate the nearest `CONFIG_FILE_NAME` or `CONFIG_FILE_NAME_ALT`, starting at current working
  // directory, and moving up the tree until we hit the root.
  let mut start = seed
    .unwrap_or_else(|| Arc::new(PathBuf::from(".")))
    .to_path_buf();
  loop {
    let path = start.join(CONFIG_FILE_NAME);
    if path.exists() {
      return Some(path);
    }
    let path = start.join(CONFIG_FILE_NAME_ALT);
    if path.exists() {
      return Some(path);
    }
    if !start.pop() {
      return None; // could not locate
    }
  }
}

/// Locate and parse the project config file, or return an empty `Option`.
///
/// # Arguments
/// * `seed` - Optional path where we should start looking; defaults to current working directory.
///
/// # Returns
/// The parsed project configuration as a `Result`.
pub fn resolve_config(seed: Option<Arc<PathBuf>>) -> Result<Option<ProjectConfig>, ConfigErr> {
  match locate_config_file(seed) {
    Some(path) => parse_config_file(Arc::new(path)).and_then(|config| Ok(Some(config))),
    None => Ok(None),
  }
}

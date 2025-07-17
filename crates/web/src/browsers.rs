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

use browserslist::Error as BrowersListCoreError;
use lightningcss::targets::{Browsers, Targets};

/// Represents an error that occurred while loading or parsing a Browserslist configuration.
#[derive(Debug, Clone, PartialEq, Eq, Default)]
pub(crate) struct BrowsersListError {
  pub(crate) err: Option<BrowersListCoreError>,
  pub(crate) message: Option<String>,
}

impl From<BrowersListCoreError> for BrowsersListError {
  fn from(err: BrowersListCoreError) -> Self {
    BrowsersListError {
      err: Some(err),
      ..BrowsersListError::default()
    }
  }
}

/// Use the provided list of strings as a browsers-list parse source, or, if none are provided, delegate to the regular
/// loading protocol of Browserslist configuration; if no configuration is found (still), use default targets.
///
/// If an error is reported while loading the underlying Browserslist configuration, this will return an error, so that
/// it can be surfaced to the user (loading/parsing does not continue).
pub(crate) fn use_or_load_browserlist(
  targets: Option<Vec<String>>,
) -> Result<Targets, BrowsersListError> {
  match targets {
    // if we are given custom targets, parse them as a browserslist source
    Some(targets) => {
      if targets.is_empty() {
        Ok(Targets::default())
      } else {
        parse_browserlist(targets).map(Targets::from)
      }
    }

    // otherwise, attempt to load the browserslist configuration as normal, and fall back to defaults
    None => match Browsers::load_browserslist() {
      Ok(maybe) => match maybe {
        Some(list) => Ok(Targets::from(list)),
        None => Ok(Targets::default()),
      },

      Err(list_err) => Err(BrowsersListError {
        message: Some(list_err.to_string()),
        ..BrowsersListError::default()
      }),
    },
  }
}

/// Parse a suite of Browserslist spec strings into an instance of `Browsers`.
pub(crate) fn parse_browserlist(items: Vec<String>) -> Result<Option<Browsers>, BrowsersListError> {
  Browsers::from_browserslist(items).map_err(|case| BrowsersListError {
    message: Some(case.to_string()),
    ..BrowsersListError::default()
  })
}

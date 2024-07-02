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
#[typeshare::typeshare]
#[derive(Clone, Hash, Eq, PartialEq, Debug)]
pub enum ToolType {
    Linter,
    Formatter,
    Compiler,
    Resolver,
    EnvManager,
}

#[typeshare::typeshare]
#[derive(Clone, Hash, Eq, PartialEq, Debug)]
pub struct ToolInfo {
  pub name: &'static str,
  pub version: &'static str,
  pub experimental: bool,
  pub language: &'static [&'static str],
  pub kind: &'static [&'static ToolType],
}

// Library version of the tooling layer.
pub static LIB_VERSION: &'static str = "0.1.0";

// API version of the tooling layer.
pub static API_VERSION: &'static str = "v1";

#[cfg(feature = "uv")]
pub static UV_INFO: ToolInfo = ToolInfo {
  name: "uv",
  version: "0.1.9",
  experimental: true,
  language: &["python"],
  kind: &[&ToolType::Resolver, &ToolType::EnvManager],
};

#[cfg(feature = "ruff")]
pub static RUFF_INFO: ToolInfo = ToolInfo {
  name: "ruff",
  version: "0.4.0",
  experimental: true,
  language: &["python"],
  kind: &[&ToolType::Linter, &ToolType::Formatter],
};

#[cfg(feature = "oxc")]
pub static OXC_INFO: ToolInfo = ToolInfo {
  name: "oxc",
  version: "0.12.3",
  experimental: true,
  language: &["js"],
  kind: &[&ToolType::Compiler],
};

#[cfg(feature = "biome")]
pub static BIOME_INFO: ToolInfo = ToolInfo {
  experimental: true,
  name: "biome",
  version: "0.5.7",
  language: &["js"],
  kind: &[&ToolType::Formatter, &ToolType::Linter],
};

#[cfg(feature = "orogene")]
pub static ORO_INFO: ToolInfo = ToolInfo {
  name: "orogene",
  version: "0.3.35-elide",
  experimental: true,
  language: &["js"],
  kind: &[&ToolType::Resolver],
};

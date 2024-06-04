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

#[typeshare::typeshare]
#[derive(Clone, Hash, Eq, PartialEq, Debug, Serialize, Deserialize)]
pub enum ToolType {
    Linter,
    Compiler,
}

#[typeshare::typeshare]
#[derive(Clone, Hash, Eq, PartialEq, Debug, Serialize, Deserialize)]
pub struct ToolInfo {
    pub name: &'static str,
    pub version: &'static str,
    pub language: &'static str,
    pub experimental: bool,
    pub kind: ToolType,
}

// Library version of the tooling layer.
pub static LIB_VERSION: &'static str = "0.1.0";

// API version of the tooling layer.
pub static API_VERSION: &'static str = "v1";

pub static UV_INFO: ToolInfo = ToolInfo {
    name: "uv",
    version: "0.1.9",
    language: "python",
    experimental: true,
    kind: ToolType::Linter,
};

pub static RUFF_INFO: ToolInfo = ToolInfo {
    name: "ruff",
    version: "0.4.0",
    language: "python",
    experimental: true,
    kind: ToolType::Linter,
};

pub static OXY_INFO: ToolInfo = ToolInfo {
    name: "oxy",
    version: "0.12.3",
    language: "js",
    experimental: false,
    kind: ToolType::Compiler,
};

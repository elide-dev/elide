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

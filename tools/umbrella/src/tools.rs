use serde::{Deserialize, Serialize};

#[typeshare::typeshare]
#[derive(Clone, Hash, Eq, PartialEq, Debug, Serialize, Deserialize)]
pub struct ToolInfo {
  pub name: &'static str,
  pub version: &'static str,
  pub language: &'static str,
  pub experimental: bool,
}

use serde::{Deserialize, Serialize};

#[typeshare::typeshare]
#[derive(Clone, Hash, Eq, PartialEq, Debug, Serialize, Deserialize)]
pub enum Severity {
  Info,
  Warning,
  Error
}

#[typeshare::typeshare]
#[derive(Clone, Hash, Eq, PartialEq, Debug, Serialize, Deserialize)]
pub struct CodeLocation {
  pub file: &'static str,
  pub line: u32,
  pub column: u32
}

#[typeshare::typeshare]
#[derive(Clone, Hash, Eq, PartialEq, Debug, Serialize, Deserialize)]
pub struct DiagnosticNote {
  pub id: &'static str,
  pub tool: &'static str,
  pub code: &'static str,
  pub message: &'static str,
  pub location: CodeLocation,
  pub severity: Severity
}

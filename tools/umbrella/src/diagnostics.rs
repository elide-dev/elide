use serde::{Serialize};

#[typeshare::typeshare]
#[derive(Clone, Hash, Eq, PartialEq, Debug, Serialize)]
pub enum Severity {
  Info,
  Warning,
  Error
}

#[typeshare::typeshare]
#[derive(Clone, Hash, Eq, PartialEq, Debug, Serialize)]
pub struct CodeLocation {
  pub file: &'static str,
  pub line: u32,
  pub column: u32
}

#[typeshare::typeshare]
#[derive(Clone, Hash, Eq, PartialEq, Debug, Serialize)]
pub struct DiagnosticNote {
  pub id: &'static str,
  pub tool: &'static str,
  pub code: &'static str,
  pub message: &'static str,
  pub location: CodeLocation,
  pub severity: Severity
}

#[derive(Clone, Hash, Eq, PartialEq, Debug, Serialize)]
pub struct DiagnosticTimings {
  pub start: u64,
  pub end: u64,
}

#[derive(Clone, Hash, Eq, PartialEq, Debug, Serialize)]
pub struct DiagnosticSuite {
  pub maxSeverity: Severity,
  pub notes: Vec<DiagnosticNote>,
  pub timings: DiagnosticTimings,
}

#[derive(Clone, Hash, Eq, PartialEq, Debug, Serialize)]
pub struct DiagnosticResult {
  pub success: bool,
  pub exitCode: i32,
  pub diagnostics: Vec<DiagnosticSuite>,
}

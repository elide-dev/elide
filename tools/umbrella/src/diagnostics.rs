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
#![forbid(unsafe_code, unsafe_op_in_unsafe_fn)]
use serde::Serialize;

#[typeshare::typeshare]
#[derive(Clone, Hash, Eq, PartialEq, Debug, Serialize)]
pub enum Severity {
  Info,
  Warning,
  Error,
}

#[typeshare::typeshare]
#[derive(Clone, Hash, Eq, PartialEq, Debug, Serialize)]
pub struct CodeLocation {
  pub file: &'static str,
  pub line: u32,
  pub column: u32,
}

#[typeshare::typeshare]
#[derive(Clone, Hash, Eq, PartialEq, Debug, Serialize)]
pub struct DiagnosticNote {
  pub id: &'static str,
  pub tool: &'static str,
  pub code: &'static str,
  pub message: &'static str,
  pub location: CodeLocation,
  pub severity: Severity,
}

#[derive(Clone, Hash, Eq, PartialEq, Debug, Serialize)]
pub struct DiagnosticTimings {
  pub start: u64,
  pub end: u64,
}

#[derive(Clone, Hash, Eq, PartialEq, Debug, Serialize)]
pub struct DiagnosticSuite {
  pub max_severity: Severity,
  pub notes: Vec<DiagnosticNote>,
  pub timings: DiagnosticTimings,
}

#[derive(Clone, Hash, Eq, PartialEq, Debug, Serialize)]
pub struct DiagnosticResult {
  pub success: bool,
  pub exit_code: i32,
  pub diagnostics: Vec<DiagnosticSuite>,
}

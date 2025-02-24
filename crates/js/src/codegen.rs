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

use oxc::{
  codegen::CodegenOptions,
  diagnostics::OxcDiagnostic,
  parser::ParseOptions,
  span::{SourceType, UnknownExtension},
  transformer::TransformOptions,
};

use crate::precompiler::Precompiler;

/// Determine the `SourceType` for a given file name; if no source type can be determined, `None` is returned.
#[inline]
pub fn determine_type(name: String) -> Result<SourceType, UnknownExtension> {
  SourceType::from_path(name)
}

/// Specifies options which can be provided to the JavaScript code generator.
#[derive(Debug)]
pub struct GeneratorOptions {
  /// Type of sources to consume, if parsing is involved.
  pub source_type: Option<SourceType>,

  /// Optional suite of explicit parsing options.
  pub parser: Option<ParseOptions>,

  /// Optional suite of explicit codegen options.
  pub codegen: Option<CodegenOptions>,

  /// Code transformation options to apply during lowering.
  pub transform: Option<TransformOptions>,
}

impl Default for GeneratorOptions {
  fn default() -> Self {
    GeneratorOptions {
      source_type: None,
      parser: Some(ParseOptions::default()),
      codegen: Some(CodegenOptions::default()),
      transform: Some(TransformOptions::default()),
    }
  }
}

/// Errors which may arise during JavaScript parsing or codegen.
#[derive(Debug, PartialEq, Eq)]
pub enum CodegenError {
  /// The provided file's extension was not recognized.
  UnrecognizedExtension,

  /// Syntax errors were encountered while parsing source code.
  SyntaxErrors(Vec<OxcDiagnostic>),
}

/// Structure describing, and carrying, generated JavaScript code.
pub struct GeneratedJavaScript {
  /// Generated code.
  pub code: String,
}

/// Generate the provided AST into source-code, compliant with the provided options, if any.
pub fn lower_into(
  options: GeneratorOptions,
  name: String,
  source: &str,
) -> Result<GeneratedJavaScript, CodegenError> {
  let source_type_resolved = match options.source_type {
    Some(source_type) => Ok(source_type),
    None => determine_type(name.to_string()),
  };
  if source_type_resolved.is_err() {
    return Err(CodegenError::UnrecognizedExtension);
  }
  let source_type = source_type_resolved.unwrap();
  let pathbuf = std::path::PathBuf::from(name.to_string());

  match Precompiler::default().execute(source, source_type, name, Some(&pathbuf)) {
    Ok(code) => Ok(GeneratedJavaScript { code }),
    Err(diagnostics) => Err(CodegenError::SyntaxErrors(diagnostics)),
  }
}

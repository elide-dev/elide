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

use crate::codegen::GeneratorOptions;
use oxc::CompilerInterface;
use oxc::codegen::{CodegenOptions, CodegenReturn, LegalComment};
use oxc::diagnostics::OxcDiagnostic;
use oxc::parser::ParseOptions;
use oxc::span::SourceType;
use oxc::transformer::TransformOptions;
use std::mem;
use std::path::Path;

#[derive(Default)]
pub struct Precompiler {
  printed: String,
  errors: Vec<OxcDiagnostic>,
  config: GeneratorOptions,
}

impl CompilerInterface for Precompiler {
  fn handle_errors(&mut self, errors: Vec<OxcDiagnostic>) {
    self.errors.extend(errors);
  }

  fn parse_options(&self) -> ParseOptions {
    self
      .config
      .parser
      .or_else(|| Some(ParseOptions::default()))
      .unwrap()
  }

  fn transform_options(&self) -> Option<&TransformOptions> {
    self.config.transform.as_ref()
  }

  fn codegen_options(&self) -> Option<CodegenOptions> {
    Some(CodegenOptions {
      minify: true,
      comments: false,
      annotation_comments: true,
      legal_comments: LegalComment::None,
      ..self.config.codegen.clone()?
    })
  }

  fn after_codegen(&mut self, ret: CodegenReturn) {
    self.printed = ret.code;
  }
}

impl Precompiler {
  /// # Errors
  ///
  /// * A list of [OxcDiagnostic].
  pub fn execute(
    &mut self,
    source_text: &str,
    source_type: SourceType,
    source_name: String,
    source_path: Option<&Path>,
  ) -> Result<String, Vec<OxcDiagnostic>> {
    let source_path_effective = source_path.unwrap_or_else(|| Path::new(&source_name));
    self.compile(source_text, source_type, source_path_effective);
    if self.errors.is_empty() {
      Ok(mem::take(&mut self.printed))
    } else {
      Err(mem::take(&mut self.errors))
    }
  }
}

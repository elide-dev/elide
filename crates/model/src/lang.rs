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

/// Enumerates Elide's supported language runtimes.
#[derive(Debug, Serialize, Deserialize, PartialEq)]
pub enum LanguageEngine {
  /// Elide's JavaScript engine, powered by GraalJs.
  JavaScript,

  /// Elide's Python engine, powered by GraalPython.
  Python,

  /// Elide's Ruby engine, powered by TruffleRuby.
  Ruby,

  /// Elide's JVM engine, powered by Espresso.
  JVM,

  /// Elide's WASM engine, powered by GraalWasm.
  WASM,

  /// Elide's LLVM engine, powered by Sulong.
  LLVM,
}

/// Enumerates Elide's supported language dialects; dialects each map to a single `LanguageEngine`.
#[derive(Debug, Serialize, Deserialize, PartialEq)]
pub enum LanguageDialect {
  /// ECMA-compliant JavaScript.
  JavaScript,

  /// Standard TypeScript.
  TypeScript,

  /// Standard Python.
  Python,

  /// Standard Ruby.
  Ruby,

  /// WASI-compliant WebAssembly.
  WASM,

  /// Standard Java.
  Java,

  /// Standard Kotlin.
  Kotlin,
}

impl LanguageDialect {
  /// Returns the `LanguageEngine` which corresponds to this `LanguageDialect`.
  pub const fn engine(&self) -> LanguageEngine {
    match self {
      LanguageDialect::JavaScript => LanguageEngine::JavaScript,
      LanguageDialect::TypeScript => LanguageEngine::JavaScript,
      LanguageDialect::Python => LanguageEngine::Python,
      LanguageDialect::Ruby => LanguageEngine::Ruby,
      LanguageDialect::WASM => LanguageEngine::WASM,
      LanguageDialect::Java => LanguageEngine::JVM,
      LanguageDialect::Kotlin => LanguageEngine::JVM,
    }
  }
}

#[cfg(test)]
mod tests {
  use super::*;

  #[test]
  fn test_language_dialect_engine() {
    assert_eq!(
      LanguageDialect::JavaScript.engine(),
      LanguageEngine::JavaScript
    );
    assert_eq!(
      LanguageDialect::TypeScript.engine(),
      LanguageEngine::JavaScript
    );
    assert_eq!(LanguageDialect::Python.engine(), LanguageEngine::Python);
    assert_eq!(LanguageDialect::Ruby.engine(), LanguageEngine::Ruby);
    assert_eq!(LanguageDialect::WASM.engine(), LanguageEngine::WASM);
    assert_eq!(LanguageDialect::Java.engine(), LanguageEngine::JVM);
    assert_eq!(LanguageDialect::Kotlin.engine(), LanguageEngine::JVM);
  }
}

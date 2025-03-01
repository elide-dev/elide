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

#![forbid(unsafe_op_in_unsafe_fn, unused_unsafe)]

use crate::codegen::{CodegenError, GeneratorOptions};
use diag::{DiagnosticBuilder, create_diagnostic, report_diagnostic};
use java_native::jni;
use jni::JNIEnv;
use jni::objects::{JClass, JString};

/// Code generation tools for JavaScript; interoperates with `parser` and other exposed modules.
mod codegen;

/// Pre-compiler implementation.
mod precompiler;

#[jni("elide.runtime.lang.javascript.JavaScriptPrecompiler")]
pub fn precompile<'a>(
  mut env: JNIEnv<'a>,
  _class: JClass<'a>,
  name: JString<'a>,
  code: JString<'a>,
) -> JString<'a> {
  let str = env.get_string(&code).unwrap();
  let code = str.to_str().to_string();
  let filename = env.get_string(&name).unwrap().to_str().to_string();
  let result = codegen::lower_into(GeneratorOptions::default(), filename, code.as_str());

  match result {
    Ok(output) => env.new_string(output.code).unwrap(),

    Err(err) => match err {
      CodegenError::SyntaxErrors(diags) => {
        // with syntax errors, we return an empty string, and report to the native diagnostics receiver.
        let ret = env.new_string("").unwrap();
        for d in diags {
          let mut rec = create_diagnostic();
          rec.with_message(&d.message);
          report_diagnostic(&mut env, rec).expect("failed to report diagnostic");
        }
        ret
      }

      CodegenError::UnrecognizedExtension => {
        // throw `IllegalArgumentException`
        env
          .throw_new(
            "java/lang/IllegalArgumentException",
            "unrecognized extension",
          )
          .expect("failed to throw exception");
        unreachable!()
      }
    },
  }
}

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

use java_native::jni;
use jni::objects::{JClass, JObject, JValue};
use jni::JNIEnv;

// Class name for the diagnostics reporter.
static CLS_DIAGNOSTICS_REPORTER: &str = "elide/runtime/diag/NativeDiagnostics";

// Method signature which accepts an array of `elide/runtime/diag/DiagnosticInfo` objects.
static METHOD_SIG_REPORT_DIAGNOSTICS: &str = "(Lelide/runtime/diag/DiagnosticInfo;)V";

#[inline]
fn maybe_invoke_string_setter(
  env: &mut JNIEnv,
  value: &Option<String>,
  record: &JObject,
  method: &str,
) {
  if let Some(advice) = &value {
    let jmsg = env
      .new_string(advice)
      .expect("failed to allocate diagnostic string");
    let val = JValue::Object(&jmsg);
    let out = env.call_method(
      record,
      method,
      format!("(L{:};)V", "java/lang/String").as_str(),
      &[val],
    );
    out.unwrap_or_else(|_| panic!("failed to call {:} on diagnostic", method));
  }
}

/// Diagnostic severity.
///
/// Maps across different types of severity from various tools. Equivalent in JVM is: `elide.runtime.diag.Severity`.
#[derive(Copy, Debug, Clone, Default)]
pub enum Severity {
  #[default]
  Info = 0,
  Warn = 1,
  Error = 2,
}

#[derive(Debug)]
pub enum DiagnosticError {
  Fail = -1,
}

// Name of the class which hosts a mutable diagnostic record.
static DIAGNOSTIC_CLASS_NAME: &str = "elide/runtime/diag/MutableDiagnostic";

/// Mutable diagnostic record.
///
/// Used for building diagnostic information from native contexts; backed by a Java object.
#[derive(Debug, Default)]
pub struct MutableDiagnostic {
  /// Severity for this diagnostic.
  severity: Severity,

  /// Language which relates to this diagnostic, as applicable.
  lang: Option<String>,

  /// Tool that reported this diagnostic, if known/applicable.
  tool: Option<String>,

  /// Message for this diagnostic.
  message: Option<String>,

  /// Advice for this diagnostic.
  advice: Option<String>,
}

/// Describes the structure of a mutable diagnostic builder.
pub trait DiagnosticBuilder {
  /// Set the language tag value for this diagnostic.
  fn with_lang(&mut self, advice: &str) -> &mut Self;

  /// Set the tool tag value for this diagnostic.
  fn with_tool(&mut self, advice: &str) -> &mut Self;

  /// Set the message for this diagnostic.
  fn with_message(&mut self, message: &str) -> &mut Self;

  /// Set the advice value for this diagnostic.
  fn with_advice(&mut self, advice: &str) -> &mut Self;

  /// Set the `Severity` level for this diagnostic.
  fn with_severity(&mut self, severity: Severity) -> &mut Self;

  /// Build the final diagnostic record.
  fn build<'a>(&self, env: &mut JNIEnv<'a>) -> Result<JObject<'a>, DiagnosticError>;
}

impl DiagnosticBuilder for MutableDiagnostic {
  fn with_lang(&mut self, lang: &str) -> &mut Self {
    self.lang = Some(lang.to_string());
    self
  }

  fn with_tool(&mut self, tool: &str) -> &mut Self {
    self.tool = Some(tool.to_string());
    self
  }

  fn with_message(&mut self, message: &str) -> &mut Self {
    self.message = Some(message.to_string());
    self
  }

  fn with_advice(&mut self, advice: &str) -> &mut Self {
    self.advice = Some(advice.to_string());
    self
  }

  fn with_severity(&mut self, severity: Severity) -> &mut Self {
    self.severity = severity;
    self
  }

  fn build<'a>(&self, env: &mut JNIEnv<'a>) -> Result<JObject<'a>, DiagnosticError> {
    let cls = env
      .find_class(DIAGNOSTIC_CLASS_NAME)
      .unwrap_or_else(|_| panic!("failed to locate '{}'", DIAGNOSTIC_CLASS_NAME));
    let record = env.new_object(cls, "()V", &[]).unwrap_or_else(|_| {
      panic!(
        "failed to construct instance of '{}'",
        DIAGNOSTIC_CLASS_NAME
      )
    });
    let severity_value = JValue::Int(self.severity as i32);

    env
      .call_method(
        &record,
        "setSeverity",
        format!("({})L{:};", "I", DIAGNOSTIC_CLASS_NAME).as_str(),
        &[severity_value],
      )
      .expect("failed to call setSeverity on diagnostic");

    maybe_invoke_string_setter(env, &self.message, &record, "setMessage");
    maybe_invoke_string_setter(env, &self.advice, &record, "setAdvice");
    Ok(record)
  }
}

/// Diagnostic record.
///
/// Builder for an eventual finalized diagnostic record. Equivalent in JVM is: `elide.runtime.diag.MutableDiagnostic`.
pub fn create_diagnostic() -> MutableDiagnostic {
  MutableDiagnostic::default()
}

/// Report an un-built diagnostic record.
pub fn report_diagnostic(
  env: &mut JNIEnv,
  builder: MutableDiagnostic,
) -> Result<(), DiagnosticError> {
  let rec = builder
    .build(env)
    .expect("failed to build diagnostic record");

  let jcls = env
    .find_class(CLS_DIAGNOSTICS_REPORTER)
    .unwrap_or_else(|_| {
      panic!(
        "failed to find diagnostics reporter class '{:}'",
        CLS_DIAGNOSTICS_REPORTER
      )
    });
  let ret = env
    .call_static_method(
      jcls,
      "reportNativeDiagnostic",
      METHOD_SIG_REPORT_DIAGNOSTICS,
      &[JValue::Object(&rec)],
    )
    .or_else(|_| {
      env.exception_describe();
      Err(DiagnosticError::Fail)
    });
  match ret {
    Ok(_) => Ok(()),
    Err(e) => Err(e),
  }
}

/// JNI: Create a diagnostic record.
///
/// This method round-trips from JVM to create a mutable diagnostic record natively; mostly used for testing.
#[jni("elide.runtime.diag.NativeDiagnostics")]
pub fn createDiagnostic<'a>(mut env: JNIEnv<'a>, _class: JClass<'a>) -> JObject<'a> {
  let mut builder = create_diagnostic();
  builder.with_severity(Severity::Warn);
  builder.with_message("There was an issue");
  let diag = builder
    .build(&mut env)
    .expect("failed to build diagnostic record");
  let _ = report_diagnostic(&mut env, builder);
  diag
}

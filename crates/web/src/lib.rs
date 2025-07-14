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

/// CSS (Cascading Style Sheets) processing for the Elide toolchain.
mod css;

/// Markdown and MDX processing for the Elide toolchain.
mod md;

use crate::css::{CssBuilderError, CssBuilderErrorCase, build_css, css_options};
use java_native::jni;
use jni::JNIEnv;
use jni::objects::{JClass, JObject, JString, JValue};
use jni::sys::jboolean;
use lightningcss::stylesheet::{ParserOptions, StyleSheet};

/// Dispatch a CSS processing error to the Java side.
fn dispatch_css_error(mut env: JNIEnv, cls: JClass, message: String) {
  let jstring_err = env
    .new_string(message)
    .expect("failed to create error string");

  env
    .call_static_method(
      cls,
      "reportCssError",
      "(Ljava/lang/String;)V",
      &[JValue::Object(&jstring_err)],
    )
    .expect("failed to report CSS parser error");
}

/// Dispatch a MDX processing error to the Java side.
fn dispatch_mdx_error(mut env: JNIEnv, cls: JClass, message: String) {
  let jstring_err = env
    .new_string(message)
    .expect("failed to create error string");

  env
    .call_static_method(
      cls,
      "reportMdxError",
      "(Ljava/lang/String;)V",
      &[JValue::Object(&jstring_err)],
    )
    .expect("failed to report MDX builder error");
}

/// JNI entrypoint function to parse and then build CSS code for a given stylesheet and suite of options.
///
/// "Building" in this case involves combining or loading sources, as needed, resolving imports, fanning-out properties
/// based on browser targeting, and, finally, minification and optimization (if enabled).
///
/// CSS is exchanged as Java strings, first as source material and then as processed output.
#[jni("elide.tooling.web.css.CssNative")]
pub fn buildCss<'a>(
  mut env: JNIEnv<'a>,
  cls: JClass<'a>,
  css: JString<'a>,
  _opts: JObject<'a>,
  minify: jboolean,
  _sourceMaps: jboolean,
) -> JObject<'a> {
  let binding = env.get_string(&css).expect("failed to obtain CSS string");
  let css_code = binding.to_str();
  let parser_options = ParserOptions::default();
  match StyleSheet::parse(&css_code, parser_options).map_err(|e| CssBuilderError {
    case: CssBuilderErrorCase::Parse,
    message: e.to_string(),
  }) {
    // if we successfully parse the stylesheet, create a new wrapper to handle this object and build.
    Ok(sheet) => {
      let options = css_options(minify, None, None);

      match build_css(sheet, options) {
        Ok(built) => env
          .new_string(built)
          .expect("failed to create built CSS string")
          .into(),

        Err(err) => {
          let msg = format!("{:?}Error: {:?}", err.case, err.message);
          dispatch_css_error(env, cls, msg);
          JObject::null()
        }
      }
    }

    // if we can't parse the stylesheet, return a null result and report the error.
    Err(err) => {
      let msg = format!("{:?}: {:?}", err.case, err.message);
      dispatch_css_error(env, cls, msg);
      JObject::null()
    }
  }
}

/// JNI entrypoint function to parse and then build MDX code for a given source file.
#[jni("elide.tooling.web.mdx.MdxNative")]
pub fn buildMdx<'a>(mut env: JNIEnv<'a>, cls: JClass<'a>, jmdx: JString<'a>) -> JObject<'a> {
  let binding = env.get_string(&jmdx).expect("failed to obtain MDX string");
  let mdx_code = binding.to_str();
  match md::compile_mdx(mdx_code.to_string()) {
    Ok(mdx) => env
      .new_string(mdx)
      .expect("failed to create built MDX string")
      .into(),

    Err(err) => {
      let msg = format!("Error: {:?}", err);
      dispatch_mdx_error(env, cls, msg);
      JObject::null()
    }
  }
}

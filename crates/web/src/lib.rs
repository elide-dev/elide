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

/// CSS (Cascading Style Sheets) processing.
mod css;

/// Markdown and MDX processing.
mod md;

/// HTML minification and processing.
mod html;

/// Browserlist support utilities.
mod browsers;

use crate::browsers::use_or_load_browserlist;
use crate::css::{CssBuilderError, CssBuilderErrorCase, build_css, build_scss, css_options};
use crate::html::build_html_minify_cfg;
use java_native::jni;
use jni::JNIEnv;
use jni::objects::{JByteBuffer, JClass, JObject, JObjectArray, JString, JValue};
use jni::sys::jboolean;
use lightningcss::stylesheet::{ParserOptions, StyleSheet};
use media::images::{do_compress_jpg, do_compress_png};
use std::borrow::Cow;

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
#[cfg(feature = "mdx")]
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
  _modules: jboolean,
  _sourceMaps: jboolean,
  scss: jboolean,
  browsers: JObjectArray<'a>,
) -> JObject<'a> {
  let binding = env.get_string(&css).expect("failed to obtain CSS string");
  let code_in = binding.to_str();
  let css_code: Cow<str> = if !scss {
    code_in
  } else {
    match build_scss(&code_in, None) {
      Ok(out) => {
        // if we don't need to minify the output scss, we can return directly
        if !minify {
          return env
            .new_string(out)
            .expect("failed to create built CSS string")
            .into();
        } else {
          Cow::Owned(out)
        }
      }

      Err(err) => {
        dispatch_css_error(env, cls, format!("{:?}Error: {:?}", err.case, err.message));
        return JObject::null();
      }
    }
  };

  // begin to extract array of browsers
  let browser_count = env.get_array_length(&browsers).unwrap_or(0) as usize;
  let mut browser_list = Vec::with_capacity(browser_count);
  for i in 0..browser_count {
    let browser = env
      .get_object_array_element(&browsers, i as jni::sys::jint)
      .expect("failed to get browser from array");

    let jbrowser_str = JString::from(browser);

    let browser_name = env
      .get_string(&jbrowser_str)
      .expect("failed to get browser string");

    let owned_copy = browser_name.to_str();

    // push the browser name into the list
    browser_list.push(owned_copy.to_string());
  }

  let parser_options = ParserOptions::default();
  let browsers_list = match use_or_load_browserlist(if browser_list.is_empty() {
    None
  } else {
    Some(browser_list)
  }) {
    Ok(list) => list,
    Err(err) => {
      dispatch_css_error(env, cls, format!("Error loading Browserslist: {err:?}"));
      return JObject::null();
    }
  };
  let options = match css_options(minify, Some(browsers_list), None) {
    Ok(out) => out,
    Err(err) => {
      dispatch_css_error(env, cls, format!("Error building CSS options: {err}"));
      return JObject::null();
    }
  };

  match StyleSheet::parse(&css_code, parser_options).map_err(|e| CssBuilderError {
    case: CssBuilderErrorCase::Parse,
    message: e.to_string(),
  }) {
    // if we successfully parse the stylesheet, create a new wrapper to handle this object and build.
    Ok(sheet) => match build_css(sheet, options) {
      Ok(built) => env
        .new_string(built)
        .expect("failed to create built CSS string")
        .into(),

      Err(err) => {
        let msg = format!("{:?}Error: {:?}", err.case, err.message);
        dispatch_css_error(env, cls, msg);
        JObject::null()
      }
    },

    // if we can't parse the stylesheet, return a null result and report the error.
    Err(err) => {
      let msg = format!("{:?}: {:?}", err.case, err.message);
      dispatch_css_error(env, cls, msg);
      JObject::null()
    }
  }
}

/// JNI entrypoint function to parse and then build MDX code for a given source file.
#[cfg(feature = "mdx")]
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
      let msg = format!("Error: {err:?}");
      dispatch_mdx_error(env, cls, msg);
      JObject::null()
    }
  }
}

#[cfg(not(feature = "mdx"))]
#[jni("elide.tooling.web.mdx.MdxNative")]
pub fn buildMdx<'a>(_env: JNIEnv<'a>, _cls: JClass<'a>, _jmdx: JString<'a>) -> JObject<'a> {
  JObject::null()
}

/// JNI entrypoint for performing minification of HTML code, using the `minify-html` crate.
#[jni("in.wilsonl.minifyhtml.MinifyHtml")]
pub fn minify<'a>(
  mut env: JNIEnv<'a>,
  _class: JClass<'a>,
  input: JString<'a>,
  jcfg: JObject<'a>,
) -> JString<'a> {
  let source: String = env.get_string(&input).unwrap().into();
  let code = source.into_bytes();
  let cfg = build_html_minify_cfg(&mut env, &jcfg);
  let out_code = html::minify_html(&code, &cfg);
  env.new_string(out_code.as_str()).unwrap()
}

/// JNI entrypoint which provides image compression facilities for PNGs.
#[jni("elide.tooling.img.ImgNative")]
pub fn compressPng<'a>(
  env: JNIEnv<'a>,
  cls: JClass<'a>,
  opts: JObject<'a>,
  img: JByteBuffer<'a>,
) -> jboolean {
  do_compress_png(env, cls, opts, img)
}

/// JNI entrypoint which provides image compression facilities for JPGs.
#[jni("elide.tooling.img.ImgNative")]
pub fn compressJpg<'a>(
  env: JNIEnv<'a>,
  cls: JClass<'a>,
  opts: JObject<'a>,
  img: JByteBuffer<'a>,
) -> jboolean {
  do_compress_jpg(env, cls, opts, img)
}

/// JNI entrypoint which provides image conversion facilities to WebP.
#[jni("elide.tooling.img.ImgNative")]
pub fn convertToWebp<'a>(_env: JNIEnv<'a>, _cls: JClass<'a>) -> jboolean {
  todo!("not yet implemented")
}

/// JNI entrypoint which provides image conversion facilities to AVIF.
#[jni("elide.tooling.img.ImgNative")]
pub fn convertToAvif<'a>(_env: JNIEnv<'a>, _cls: JClass<'a>) -> jboolean {
  todo!("not yet implemented")
}

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

//
// Inlined from `minify-html-java` to allow use of `minify-html-onepass`.
//

use jni::JNIEnv;
use jni::errors::Error;
use jni::objects::{JObject, JValueOwned};
use minify_html::Cfg;
use std::str::from_utf8;

// Attempt to extract a field from the provided result, and unwrap it; then, check for exceptions, printing a message
// with the exception if one has occurred because of our field retrieval.
fn checked_unwrap<'a>(field: &str, result: Result<JValueOwned<'a>, Error>) -> JValueOwned<'a> {
  result.unwrap_or_else(|err| panic!("failed to retrieve field '{}': {:?}", field, err))
}

/// Build an HTML minification config from the provided Java object.
pub(crate) fn build_html_minify_cfg(env: &mut JNIEnv, obj: &JObject) -> Cfg {
  #[rustfmt::skip]
  // This is a statement because "attributes on expressions are experimental".
  let cfg = Cfg {
    minify_doctype: checked_unwrap(
      "minify_doctype",
      env.get_field(obj, "minify_doctype", "Z")
    ).z().unwrap(),

    allow_optimal_entities: checked_unwrap(
      "allow_optimal_entities",
      env.get_field(obj, "allow_optimal_entities", "Z")
    ).z().unwrap(),

    allow_noncompliant_unquoted_attribute_values: env.get_field(obj, "allow_noncompliant_unquoted_attribute_values", "Z").unwrap().z().unwrap(),
    keep_closing_tags: env.get_field(obj, "keep_closing_tags", "Z").unwrap().z().unwrap(),
    keep_comments: env.get_field(obj, "keep_comments", "Z").unwrap().z().unwrap(),
    keep_html_and_head_opening_tags: env.get_field(obj, "keep_html_and_head_opening_tags", "Z").unwrap().z().unwrap(),
    keep_input_type_text_attr: env.get_field(obj, "keep_input_type_text_attr", "Z").unwrap().z().unwrap(),
    allow_removing_spaces_between_attributes: env.get_field(obj, "allow_removing_spaces_between_attributes", "Z").unwrap().z().unwrap(),
    keep_ssi_comments: env.get_field(obj, "keep_ssi_comments", "Z").unwrap().z().unwrap(),
    minify_css: env.get_field(obj, "minify_css", "Z").unwrap().z().unwrap(),
    minify_js: env.get_field(obj, "minify_js", "Z").unwrap().z().unwrap(),
    preserve_brace_template_syntax: env.get_field(obj, "preserve_brace_template_syntax", "Z").unwrap().z().unwrap(),
    preserve_chevron_percent_template_syntax: env.get_field(obj, "preserve_chevron_percent_template_syntax", "Z").unwrap().z().unwrap(),
    remove_bangs: env.get_field(obj, "remove_bangs", "Z").unwrap().z().unwrap(),
    remove_processing_instructions: env.get_field(obj, "remove_processing_instructions", "Z").unwrap().z().unwrap(),
  };
  cfg
}

/// Perform HTML minification on the given byte slice of HTML code.
pub(crate) fn minify_html(code: &[u8], cfg: &Cfg) -> String {
  let out_code = minify_html::minify(code, cfg);
  from_utf8(&out_code).unwrap().to_string()
}

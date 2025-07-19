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

use browserslist::Error as BrowserslistError;
use grass::Options as ScssOptions;
use grass::from_string as compile_scss_from_string;
use lightningcss::printer::PrinterOptions;
use lightningcss::stylesheet::{MinifyOptions, StyleSheet};
use lightningcss::targets::Targets;
use parcel_sourcemap::SourceMap;

/// Options which can be specified for CSS processing.
pub(crate) struct CssBuilderOptions<'a> {
  /// Options which apply to minification of CSS.
  minify: Option<MinifyOptions>,

  /// Options which apply to printing of CSS.
  printer: Option<PrinterOptions<'a>>,
}

/// Options which can be specified for SCSS processing.
#[derive(Debug, Default)]
pub(crate) struct ScssBuilderOptions<'a> {
  pub scss_options: ScssOptions<'a>,
}

/// Error cases for the CSS builder.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum CssBuilderErrorCase {
  /// An error occurred while parsing CSS.
  Parse,

  /// An error occurred while minifying CSS.
  Minify,

  /// An error occurred while printing CSS.
  Printer,
}

/// Error cases for the SCSS builder.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum ScssBuilderErrorCase {
  /// An error occurred while printing SCSS.
  Render,
}

/// Error type for CSS builder operations.
#[derive(Debug, Clone)]
pub(crate) struct CssBuilderError {
  /// The case of the error.
  pub(crate) case: CssBuilderErrorCase,

  /// A message describing the error.
  pub(crate) message: String,
}

/// Error type for SCSS builder operations.
#[derive(Debug, Clone)]
pub(crate) struct ScssBuilderError {
  /// The case of the error.
  pub(crate) case: ScssBuilderErrorCase,

  /// A message describing the error.
  pub(crate) message: String,
}

/// Build minification options from JVM-side flags.
fn build_printer_options(
  minify: bool,
  source_map: Option<&mut SourceMap>,
  targets: Targets,
) -> Result<PrinterOptions, BrowserslistError> {
  Ok(PrinterOptions {
    minify,
    source_map,
    targets,
    ..PrinterOptions::default()
  })
}

/// Build CSS options from JVM-side flags.
pub(crate) fn css_options(
  do_minify: bool,
  use_targets: Option<Targets>,
  source_map: Option<&mut SourceMap>,
) -> Result<CssBuilderOptions, BrowserslistError> {
  let targets = use_targets.or(Some(Targets::default())).unwrap_or_default();
  let minify = match do_minify {
    true => Some(MinifyOptions {
      targets,
      ..MinifyOptions::default()
    }),
    false => None,
  };
  let printer_opts = build_printer_options(do_minify, source_map, targets)?;
  let printer = Some(printer_opts);
  Ok(CssBuilderOptions { minify, printer })
}

/// Build the underlying CSS and return it as a string.
pub(crate) fn build_css<'a>(
  mut css: StyleSheet,
  opts: CssBuilderOptions<'a>,
) -> Result<String, CssBuilderError> {
  let effective_minify_options = opts.minify.unwrap_or_default();
  let effective_printer_options = opts.printer.unwrap_or_default();

  css
    .minify(effective_minify_options)
    .map_err(|e| CssBuilderError {
      case: CssBuilderErrorCase::Minify,
      message: e.to_string(),
    })?;

  css
    .to_css(effective_printer_options)
    .map_err(|e| CssBuilderError {
      case: CssBuilderErrorCase::Printer,
      message: e.to_string(),
    })
    .map(|result| result.code)
}

/// Build the underlying SCSS and return it as a string.
pub(crate) fn build_scss<'a>(
  input: &'a str,
  opts: Option<ScssBuilderOptions<'a>>,
) -> Result<String, ScssBuilderError> {
  compile_scss_from_string(
    input,
    &(opts
      .or(Some(ScssBuilderOptions::default()))
      .unwrap_or_default()
      .scss_options),
  )
  .map_err(|e| ScssBuilderError {
    case: ScssBuilderErrorCase::Render,
    message: e.to_string(),
  })
}

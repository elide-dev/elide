@0xeb3fcfccf8735acc;

# Copyright © 2022, The Elide Framework Authors. All rights reserved.
#
# The Gust/Elide framework and tools, and all associated source or object computer code, except where otherwise noted,
# are licensed under the Zero Prosperity license, which is enclosed in this repository, in the file LICENSE.txt. Use of
# this code in object or source form requires and implies consent and agreement to that license in principle and
# practice. Source or object code not listing this header, or unless specified otherwise, remain the property of
# Elide LLC and its suppliers, if any. The intellectual and technical concepts contained herein are proprietary to
# Elide LLC and its suppliers and may be covered by U.S. and Foreign Patents, or patents in process, and are protected
# by trade secret and copyright law. Dissemination of this information, or reproduction of this material, in any form,
# is strictly forbidden except in adherence with assigned license requirements.

# Specifies structures used to define the notion of an "embedded" script asset in some foreign language, which can be
# executed at runtime by Elide to fulfill user requests.

using Timestamp = import "../std/timestamp.capnp".Timestamp;
using DataContainerRef = import "../data/data.capnp".DataContainerRef;

# Enumerates languages which are supported for embedded scripting in Elide.
enum EmbeddedScriptLanguage {
  # The language is unknown or unspecified; regular code should not use this value.
  LANGUAGE_UNSPECIFIED @0;

  # The language is a dialect of JavaScript.
  JAVASCRIPT @1;

  # The language is Python; language support for Python.
  PYTHON @2;

  # The language is Ruby; language support for Ruby.
  RUBY @3;

  # The language is supported by the JVM.
  JVM @4;

  # The language is Kotlin; language support for JVM with Kotlin compiler.
  KOTLIN @5;

  # The language is Groovy; language support for JVM with Groovy compiler.
  GROOVY @6;

  # The language is Scala; language support for JVM with Scala compiler.
  SCALA @7;
}

# Enumerates supported JavaScript language levels.
enum JsLanguageLevel {}

# Specifies JavaScript-specific script metadata.
struct JsScriptMetadata {
  # Specifies the language level of the script.
  languageLevel @1 :JsLanguageLevel;
}

# Describes embedded script-level metadata which is enclosed with the asset spec for an embedded script.
struct EmbeddedScriptMetadata {
  # Language-specific script metadata.
  metadata :union {
    # JavaScript-related metadata.
    javascript @1 :JsScriptMetadata;
  }
}

# Describes a single embedded script asset, which is embedded within an Elide application. The script is enclosed
# within the protocol buffer record, along with a digest and various metadata.
struct EmbeddedScript {
  # Module name / ID for this embedded script. Set at build time.
  module @1 :Text;

  # Filename, or some synthesized filename, for this script.
  filename @2 :Text;

  # Language of the embedded script, and expected interpreted language.
  language @3 :EmbeddedScriptLanguage;

  # Embedded script-level metadata, including language-specific metadata.
  metadata @4 :EmbeddedScriptMetadata;

  # Last-modified timestamp for the assets underlying this script.
  last_modified @5 :Timestamp;

  # Unique set of direct dependencies for this embedded script asset; expected to be other, compatible embedded scripts
  # (same language, same runtime level). Expressed as a `module` ID.
  direct_dependency @6 : List(Text);

  # Unique transitive closure of all dependencies this module relies upon; expected to be other, compatible embedded
  # scripts (same language, same runtime level). Expressed as a `module` ID.
  transitive_dependency @7 : List(Text);

  # Describes the raw data for the script content itself, plus a digest of the data for verification purposes; the
  # digest payload additionally specifies the algorithm used.
  script @8 : DataContainerRef;

  # Source-map file path for the embedded script, if generated as an external file.
  sourcemap @9 : DataContainerRef;
}

@0xf355846bbd4268e2;

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

using Java = import "/capnp/java.capnp";

$Java.package("tools.elide.data");
$Java.outerClassname("Encodings");

# Data Encoding
#
# Describes supported file encodings. By default, files are encoded with `UTF-8`.
enum Encoding {
  # The encoding is UTF-8.
  utf8 @0;

  # The encoding is UTF-16.
  utf16 @1;

  # The encoding is UTF-32.
  utf32 @2;
}

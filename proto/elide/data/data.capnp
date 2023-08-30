@0x8b072f799fa9221d;

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

# Specifies core structures related to cryptographic operations and primitives. These records and enumerates are used
# throughout the codebase as a standard base set of definitions for hashing, encryption, and more.

using Java = import "/capnp/java.capnp";
using Encoding = import "./encoding.capnp".Encoding;

$Java.package("tools.elide.data");
$Java.outerClassname("Data");


# Compression Mode
#
# Specifies compression modes that are supported by the framework for pre-compressed assets stored inline within the
# manifest. These inlined assets do not replace original source assets, which are enclosed in the resource JAR.
enum CompressionMode {
  # No compression.
  identity @0;

  # Standard gzip-based compression.
  gzip @1;

  # Brotli-based compression.
  brotli @2;

  # Snappy-based compression.
  snappy @3;

  # Deflate (zlib)-based compression.
  deflate @4;
}

# Data Container
#
#
struct DataContainer {
}

# Data Container Refernece
#
#
struct DataContainerRef {
}

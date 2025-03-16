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
using HashAlgorithm = import "../crypto/crypto.capnp".HashAlgorithm;
using Encoding = import "./encoding.capnp".Encoding;

$Java.package("elide.data");
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

# Data Fingerprint
#
# Stores a generic cryptographic fingerprint of some arbitrary data. This is a utility record, which simply gathers the
# specification of a hash algorithm with a raw data field storing the result of the hash.
struct DataFingerprint {
    # Algorithm in use when fingerprinting the associated data.
    hash @0 :HashAlgorithm;

    # Specifies the encoding for the affixed fingerprint data. If unspecified, the fingerprint is expressed in raw bytes.
    encoding @1 :Encoding;

    # Prefix values used when fingerprinting the associated data, if applicable.
    prefix @2 :List(Data);

    # Suffix values used when fingerprinting the associated data, if applicable.
    suffix @3 :List(Data);

    # Separator to use or pre-image reconstruction.
    separator @4 :Data;

    # Content of the fingerprint/checksum calculated as part of this data fingerprint.
    fingerprint @5 :Data;
}

# Data Container
#
# Specifies an arbitrary container, which is used to hold raw data, along with the optional specification of a
# cryptographic fingerprint calculated from the data.
struct DataContainer {
    # Specifies the encoding for the affixed container data. If unspecified, the data is expressed in raw bytes.
    encoding @0 :Encoding;

    # Specifies an (optional) integrity fingerprint that may be used to verify the consistency of the underlying data
    # held by this data container.
    integrity @1 :List(DataFingerprint);

    # Raw bytes for the data referenced by this container.
    raw @2 :List(Data);
}

# Compressed Data
#
# Generic container for compressed data, which simply combines an enumerated `CompressionMode` with a blob of raw bytes
# which are expected to be compressed by the specified algorithm or tool.
struct CompressedData {
    # Compression mode applied to this data. If no compression is active, `IDENTITY` may be specified as a default.
    compression @0 :CompressionMode;

    # Size of the asset data before compression. To obtain the size of the data in compressed form, simply take the
    # length of the data field itself. This value is expressed in bytes.
    size @1 :UInt64;

    # Holds an (optional) integrity fingerprint, calculated from the held data *before* compression, which maybe used to
    # verify the consistency of the data held by this container after de-compression.
    integrity @2 :List(DataFingerprint);

    # Container holding the raw compressed data, and a fingerprint of the data in compressed form. This is unmodified
    # raw bytes, aside from being compressed by the algorithm specified by `compression`.
    data @3 :DataContainer;
}

# Data Container Reference
#
# Specifies the structure of a data container which can also be a reference to a different resource or file-system
# style-asset. Only one reference value may be filled in.
struct DataContainerRef {
    # Specifies a fingerprint and path pair.
    struct FingerprintPathPair {
        # Specifies the fingerprint information associated with, or calculated for, the subject info.
        fingerprint @0 :DataFingerprint;

        # Specifies the path for this data container reference.
        path @1 :Text;
    }

    # Specifies the reference subject for this record.
    reference :union {
        # Specifies raw data which is enclosed as a value with this reference. In this case, a data blob and fingerprint
        # are enclosed inline within the protocol buffer.
        data @0 :CompressedData;

        # Specifies a reference to a file resident in the application JAR or native image.
        resource @1 :FingerprintPathPair;

        # Specifies a reference to a file resident on the filesystem outside of the built application.
        filesystem @2 :FingerprintPathPair;
    }
}

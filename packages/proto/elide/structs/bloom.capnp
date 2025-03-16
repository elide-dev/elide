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

@0x8b21b13b96557d31;
# Specifies a structure for use as a lightweight bloom-filter in protocol buffers. This message must be processed with
# special care, by runtime utilities in each language.

using Cxx = import "/capnp/c++.capnp";
using Java = import "/capnp/java.capnp";
using HashAlgorithm = import "../std/hash.capnp".HashAlgorithm;

$Cxx.namespace("elide::structs");
$Cxx.allowCancellation;
$Java.package("elide.structs");

# Specifies a layer in a multi-layer Bloom filter. If no more than 1 layer is specified, the filter is a simple single-
# layer bit set (also known as a regular Bloom filter).
struct FilterLayer {
  bitset: [uint64];
  # Raw bit sets for each layer of the filter.

  count: [uint64];
  # Count of items for each bucket in this filter layer. Only present if this is a *Counting Bloom filter*, in which
  # the bit set indicates presence for each bucket and the count indicates the value for each bucket.
}

# Defines a structure that is operable as a 1-to-n-layer Bloom filter. NIST (the National Institute of Standards and
# Technology) defines a *"Bloom filter"* as a "data structure with a probabilistic algorithm to quickly test membership
# in a large set using multiple hash functions into a single array of bits."
#
# In essence, a Bloom filter is a very compact set of data, which operates kind of like a cheese cloth. For each unique
# data fingerprint applied to the filter, you can imagine throwing colored die at the cheese cloth - what you end up
# with is some blend of colors, which is entirely unique and ephemeral, having been constituted entirely by the colors
# you chose, and the exact parameters of how you threw them. Albeit a contrived example, one could then imagine simply
# examining the result of your work to determine the presence of different colors.
#
# Bloom filters work the same way. Since their invention in by Burton Howard Bloom in 1970, many exotic forms of Bloom
# filters have shown up, a few of which this structure supports. For example, one may choose to enhance regular filters
# with the count of items considered in the filter, making it a *Counting Bloom filter*. Or, one may choose to combine
# multiple filters into one effective filter, which is referred to as a *Layered Bloom filter*.
struct BloomFilter {
  algorithm: HashAlgorithm;
  # Hash algorithm in use for this Bloom filter. This must be considered an immutable value for a constituted filter,
  # otherwise the underlying data may be rendered unusable.

  rounds: uint32;
  # Number of hash rounds to apply when adding data to this filter.

  count: uint32;
  # Number of items in the filter, across all layers.

  limit: uint32;
  # Limit setting to enforce for this Bloom filter, before re-calculating contents. Optional.

  layers: [FilterLayer];
  # Specifies each layer of this Bloom filter structure. If no more than one layer is present, the structure represents
  # a simple, single-layer regular Bloom filter. If more than one layer are present, the structure represents a multi-
  # layer (*Layered*) Bloom filter.
}

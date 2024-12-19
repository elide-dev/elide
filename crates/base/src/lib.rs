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

#![no_std]
#![feature(test)]
#![feature(const_trait_impl)]
#![forbid(unsafe_op_in_unsafe_fn, unused_unsafe, dead_code)]

extern crate alloc;

/// Base64 encoding and decoding.
pub mod b64;

/// CRC32 checksums.
pub mod crc;

/// SHA-type hashing.
pub mod sha;

/// UTF-N operations.
pub mod utf;

/// Hexadecimal encoding and decoding.
pub mod hex;

/// UUID generation and parsing.
pub mod uuid;

/// Random number generation.
pub mod rng;

/// Rust module prelude.
pub mod prelude;

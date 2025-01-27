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

#![forbid(unsafe_op_in_unsafe_fn, unused_unsafe, dead_code)]

use crc32fast::Hasher as CRC32;
use crc64fast::Digest as CRC64;

/// Compute the CRC32 checksum of the given data.
///
/// This function is a wrapper around the `crc32fast` crate.
///
/// # Arguments
/// * `data` - The data to compute the checksum of.
///
/// # Returns
/// The CRC32 checksum of the data.
///
/// # Examples
///
/// ```
/// use base::crc::crc32;
/// let data = b"hello, world!";
/// let checksum = crc32(data);
/// assert_eq!(checksum, 1486392595);
/// ```
#[inline]
pub fn crc32(data: &[u8]) -> u32 {
  let mut hasher = CRC32::new();
  hasher.update(data);
  hasher.finalize()
}

/// Compute the CRC64 checksum of the given data.
///
/// This function is a wrapper around the `crc64fast` crate.
///
/// # Arguments
/// * `data` - The data to compute the checksum of.
///
/// # Returns
/// The CRC64 checksum of the data.
///
/// # Examples
///
/// ```
/// use base::crc::crc64;
/// let data = b"hello, world!";
/// let checksum = crc64(data);
/// assert_eq!(checksum, 11638617936805349819);
/// ```
#[inline]
pub fn crc64(data: &[u8]) -> u64 {
  let mut hasher = CRC64::new();
  hasher.write(data);
  hasher.sum64()
}

#[cfg(test)]
mod tests {
  use super::*;

  #[test]
  fn crc32_basic() {
    assert_eq!(crc32(b"hello, world!"), 1486392595);
    assert_ne!(crc32(b"more data as an example"), 1486392595);
  }

  #[test]
  fn crc64_basic() {
    assert_eq!(crc64(b"hello, world!"), 11638617936805349819);
  }
}

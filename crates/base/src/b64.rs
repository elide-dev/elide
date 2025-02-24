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

use base64::{
  Engine as B64Engine, engine::general_purpose::STANDARD as BASE64_STANDARD,
  engine::general_purpose::STANDARD_NO_PAD as BASE64_STANDARD_NOPAD,
  engine::general_purpose::URL_SAFE as BASE64_URLSAFE,
  engine::general_purpose::URL_SAFE_NO_PAD as BASE64_URLSAFE_NOPAD,
};

#[cfg(feature = "alloc")]
extern crate alloc;

#[cfg(feature = "alloc")]
use {crate::prelude::*, alloc::string::String as StdString, alloc::vec::Vec};

#[cfg(feature = "simd")]
use base64_simd::{
  Out as Base64Out, STANDARD as BASE64_STANDARD_SIMD,
  STANDARD_NO_PAD as BASE64_STANDARD_NOPAD_SIMD, URL_SAFE as BASE64_URLSAFE_SIMD,
  URL_SAFE_NO_PAD as BASE64_URLSAFE_NOPAD_SIMD,
};

/// Estimate the length of a base64 encoded string.
///
/// This method is provided publicly so that callers can estimate the size of a stack-allocated output buffer.
///
/// # Arguments
///
/// * `input` - The length of the input byte-slice to encode.
/// * `padding` - Whether to account for padding in the output with `=` characters.
///
/// Returns the estimated length of the base64 encoded string.
#[inline]
pub const fn base64_estimate_encoded_len(input: usize, padding: bool) -> usize {
  base64::encoded_len(input, padding)
    .expect("integer overflow while estimating base64 encoded length")
}

/// Estimate the length of a base64 string, once decoded.
///
/// This method is provided publicly so that callers can estimate the size of a stack-allocated output buffer.
///
/// # Arguments
///
/// * `input` - The length of the subject byte-slice.
///
/// Returns the estimated length of the string once decoded.
#[inline]
pub fn base64_estimate_decoded_len(input: usize) -> usize {
  base64::decoded_len_estimate(input)
}

/// Encode a byte slice to a base64 output vector.
///
/// This method only allocates for `output` if necessary; `output` may be truncated if the output size is expected to be
/// smaller than the available space.
///
/// # Arguments
///
/// * `input` - The input byte-slice to encode.
/// * `output` - The output vector to write the base64 encoded data to.
/// * `padding` - Whether to pad the output with `=` characters.
/// * `urlsafe` - Whether to use the URL-safe base64 alphabet.
#[inline]
pub(crate) fn base64_encode_sync(input: &[u8], output: &mut Vec<u8>, padding: bool, urlsafe: bool) {
  let base64 = if urlsafe {
    if padding {
      BASE64_URLSAFE
    } else {
      BASE64_URLSAFE_NOPAD
    }
  } else if padding {
    BASE64_STANDARD
  } else {
    BASE64_STANDARD_NOPAD
  };
  let expected_len = base64::encoded_len(input.len(), padding)
    .expect("integer overflow while estimating base64 encoded length");
  output.resize(expected_len, 0);
  base64
    .encode_slice(input, output)
    .expect("base64 encoding failed");
}

/// Encode a byte slice to a base64 output vector.
///
/// This method only allocates for `output` if necessary; `output` may be truncated if the output size is expected to be
/// smaller than the available space.
///
/// # Arguments
///
/// * `input` - The input byte-slice to encode.
/// * `output` - The output vector to write the base64 encoded data to.
/// * `padding` - Whether to pad the output with `=` characters.
/// * `urlsafe` - Whether to use the URL-safe base64 alphabet.
#[inline]
pub(crate) fn base64_encode_sync_str<'a>(
  input: &'a [u8],
  output: &'a mut [u8],
  padding: bool,
  urlsafe: bool,
) {
  if urlsafe {
    if padding {
      BASE64_URLSAFE
    } else {
      BASE64_URLSAFE_NOPAD
    }
  } else if padding {
    BASE64_STANDARD
  } else {
    BASE64_STANDARD_NOPAD
  }
  .encode_slice(input, output)
  .expect("base64 encoding failed");
}

/// Encode a byte slice to a base64 output string; this variant uses standard Base64, with optional padding.
///
/// This method uses `base64_encode_sync` and then allocates into a `String`.
///
/// # Arguments
///
/// * `input` - The input byte-slice to encode.
/// * `padding` - Whether to pad the output with `=` characters.
///
/// Returns a `String` containing the base64 encoded data.
#[cfg(feature = "alloc")]
#[inline]
pub fn base64_std_encode_string_sync(input: &[u8], padding: bool) -> String {
  let mut outvec = Vec::new();
  base64_encode_sync(input, &mut outvec, padding, false);
  #[cfg(not(feature = "unsafe"))]
  let out = StdString::from_utf8(outvec).expect("base64 encoding failed");
  #[cfg(feature = "unsafe")]
  let out = unsafe { StdString::from_utf8_unchecked(outvec) };
  out.into()
}

/// Encode a byte slice to a base64 output string; this variant uses standard Base64, with optional padding.
///
/// This method uses `base64_encode_sync` and then allocates into a `String`.
///
/// # Arguments
///
/// * `input` - The input byte-slice to encode.
/// * `output`- Pre-allocated output byte array.
/// * `padding` - Whether to pad the output with `=` characters.
///
/// Returns a `&str` containing the base64 encoded data.
#[inline]
pub fn base64_std_encode_str_sync<'a>(
  input: &'a [u8],
  output: &'a mut [u8],
  padding: bool,
) -> &'a str {
  base64_encode_sync_str(input, output, padding, false);
  #[cfg(not(feature = "unsafe"))]
  let out = core::str::from_utf8_mut(output).expect("base64 encoding failed");
  #[cfg(feature = "unsafe")]
  let out = unsafe { core::str::from_utf8_unchecked_mut(output) };
  out
}

/// Encode a byte slice to a base64 output string; this variant uses URL-safe Base64, with optional padding.
///
/// This method uses `base64_encode_sync` and then allocates into a `String`.
///
/// # Arguments
///
/// * `input` - The input byte-slice to encode.
/// * `padding` - Whether to pad the output with `=` characters.
/// * Returns a `String` containing the base64 encoded data.
#[cfg(feature = "alloc")]
#[inline]
pub fn base64_url_encode_string_sync(input: &[u8], padding: bool) -> String {
  let mut outvec = Vec::new();
  base64_encode_sync(input, &mut outvec, padding, true);
  #[cfg(not(feature = "unsafe"))]
  let out = StdString::from_utf8(outvec).expect("base64 encoding failed");
  #[cfg(feature = "unsafe")]
  let out = unsafe { StdString::from_utf8_unchecked(outvec) };
  out.into()
}

/// Encode a byte slice to a base64 output string; this variant uses URL-safe Base64, with optional padding.
///
/// This method uses `base64_encode_sync` and then allocates into a `String`.
///
/// # Arguments
///
/// * `input` - The input byte-slice to encode.
/// * `output` - Pre-allocated output byte array.
/// * `padding` - Whether to pad the output with `=` characters.
///
/// Returns a `&str` containing the base64 encoded data.
#[inline]
pub fn base64_url_encode_str_sync<'a>(
  input: &'a [u8],
  output: &'a mut [u8],
  padding: bool,
) -> &'a str {
  base64_encode_sync_str(input, output, padding, true);
  #[cfg(not(feature = "unsafe"))]
  let out = core::str::from_utf8_mut(output).expect("base64 encoding failed");
  #[cfg(feature = "unsafe")]
  let out = unsafe { core::str::from_utf8_unchecked_mut(output) };
  out
}

/// Encode a byte slice to a base64 output vector, using SIMD instructions if available.
///
/// This method only allocates for `output` if necessary; `output` may be truncated if the output size is expected to be
/// smaller than the available space.
///
/// # Arguments
///
/// * `input` - The input byte-slice to encode.
/// * `output` - The output vector to write the base64 encoded data to.
/// * `padding` - Whether to pad the output with `=` characters.
/// * `urlsafe` - Whether to use the URL-safe base64 alphabet.
#[cfg(feature = "simd")]
#[inline]
pub(crate) fn base64_encode_simd<'a>(
  input: &'a [u8],
  output: Base64Out<'a, [u8]>,
  padding: bool,
  urlsafe: bool,
) -> &'a mut [u8] {
  if urlsafe {
    if padding {
      BASE64_URLSAFE_SIMD
    } else {
      BASE64_URLSAFE_NOPAD_SIMD
    }
  } else if padding {
    BASE64_STANDARD_SIMD
  } else {
    BASE64_STANDARD_NOPAD_SIMD
  }
  .encode(input, output)
}

/// Encode a byte slice to a base64 output string using SIMD instructions, if available; this variant uses standard
/// Base64, with optional padding.
///
/// This method uses `base64_encode_sync` and then allocates into a `String`.
///
/// # Arguments
///
/// * `input` - The input byte-slice to encode.
/// * `padding` - Whether to pad the output with `=` characters.
/// * Returns a `String` containing the base64 encoded data.
#[cfg(all(feature = "simd", feature = "alloc",))]
#[inline]
pub fn base64_std_encode_string_simd(input: &[u8], padding: bool) -> String {
  let mut outvec = Vec::new();
  let size = base64_estimate_encoded_len(input.len(), padding);
  outvec.resize(size, 0);
  let b64out = Base64Out::from_slice(&mut outvec);
  base64_encode_simd(input, b64out, padding, false);
  #[cfg(not(feature = "unsafe"))]
  let out = StdString::from_utf8(outvec).expect("base64 encoding failed");
  #[cfg(feature = "unsafe")]
  let out = unsafe { StdString::from_utf8_unchecked(outvec) };
  out.into()
}

/// Encode a byte slice to a base64 output string using SIMD instructions, if available; this variant uses standard
/// Base64, with optional padding.
///
/// This method uses `base64_encode_simd` without allocating.
///
/// # Arguments
///
/// * `input` - The input byte-slice to encode.
/// * `output` - Pre-allocated output byte array.
/// * `padding` - Whether to pad the output with `=` characters.
#[cfg(feature = "simd")]
#[inline]
pub fn base64_std_encode_str_simd<'a>(
  input: &'a [u8],
  output: &'a mut [u8],
  padding: bool,
) -> &'a str {
  let b64out = Base64Out::from_slice(output);
  base64_encode_simd(input, b64out, padding, false);
  #[cfg(not(feature = "unsafe"))]
  let out = core::str::from_utf8_mut(output).expect("base64 encoding failed");
  #[cfg(feature = "unsafe")]
  let out = unsafe { core::str::from_utf8_unchecked_mut(output) };
  out
}

/// Encode a byte slice to a base64 output string using SIMD instructions, if available; this variant uses URL-safe
/// Base64, with optional padding.
///
/// This method uses `base64_encode_simd` without allocating.
///
/// # Arguments
///
/// * `input` - The input byte-slice to encode.
/// * `output` - Pre-allocated output byte array.
/// * `padding` - Whether to pad the output with `=` characters.
#[cfg(feature = "simd")]
#[inline]
pub fn base64_url_encode_str_simd<'a>(
  input: &'a [u8],
  output: &'a mut [u8],
  padding: bool,
) -> &'a str {
  let b64out = Base64Out::from_slice(output);
  base64_encode_simd(input, b64out, padding, true);
  #[cfg(not(feature = "unsafe"))]
  let out = core::str::from_utf8_mut(output).expect("base64 encoding failed");
  #[cfg(feature = "unsafe")]
  let out = unsafe { core::str::from_utf8_unchecked_mut(output) };
  out
}

/// Encode a byte slice to a base64 output string using SIMD instructions, if available; this variant uses URL-safe
/// Base64, with optional padding.
///
/// This method uses `base64_encode_sync` and then allocates into a `String`.
///
/// # Arguments
///
/// * `input` - The input byte-slice to encode.
/// * `padding` - Whether to pad the output with `=` characters.
/// * Returns a `String` containing the base64 encoded data.
#[cfg(all(feature = "simd", feature = "alloc",))]
#[inline]
pub fn base64_url_encode_string_simd(input: &[u8], padding: bool) -> String {
  let mut outvec = Vec::new();
  let size = base64_estimate_encoded_len(input.len(), padding);
  outvec.resize(size, 0);
  let b64out = Base64Out::from_slice(&mut outvec);
  base64_encode_simd(input, b64out, padding, true);
  #[cfg(not(feature = "unsafe"))]
  let out = StdString::from_utf8(outvec).expect("base64 encoding failed");
  #[cfg(feature = "unsafe")]
  let out = unsafe { StdString::from_utf8_unchecked(outvec) };
  out.into()
}

#[cfg(test)]
mod tests {
  use super::*;

  const HELLO_WORLD: &[u8] = b"Hello, world!";
  const HELLO_WORLD_B64_STD_PAD: &str = "SGVsbG8sIHdvcmxkIQ==";
  const HELLO_WORLD_B64_STD_NOPAD: &str = "SGVsbG8sIHdvcmxkIQ";

  #[test]
  #[cfg(feature = "alloc")]
  fn b64_std_encode_string_sync() {
    assert_eq!(base64_std_encode_string_sync(b"", true), "");
    assert_eq!(base64_std_encode_string_sync(b"f", true), "Zg==");
    assert_eq!(base64_std_encode_string_sync(b"f", false), "Zg");
    assert_eq!(base64_std_encode_string_sync(b"fo", true), "Zm8=");
    assert_eq!(base64_std_encode_string_sync(b"fo", false), "Zm8");
    assert_eq!(base64_std_encode_string_sync(b"foo", true), "Zm9v");
    assert_eq!(
      base64_std_encode_string_sync(HELLO_WORLD, true),
      HELLO_WORLD_B64_STD_PAD
    );
    assert_eq!(
      base64_std_encode_string_sync(HELLO_WORLD, false),
      HELLO_WORLD_B64_STD_NOPAD
    );
  }

  #[test]
  fn b64_std_encode_str_sync() {
    let input = b"foo";
    let mut output = [0u8; 4];
    assert_eq!(base64_std_encode_str_sync(input, &mut output, true), "Zm9v");
  }

  #[test]
  #[cfg(feature = "alloc")]
  fn b64_url_encode_string_sync() {
    assert_eq!(base64_std_encode_string_sync(b"", true), "");
    assert_eq!(base64_std_encode_string_sync(b"f", true), "Zg==");
    assert_eq!(base64_std_encode_string_sync(b"f", false), "Zg");
    assert_eq!(base64_std_encode_string_sync(b"fo", true), "Zm8=");
    assert_eq!(base64_std_encode_string_sync(b"fo", false), "Zm8");
    assert_eq!(base64_std_encode_string_sync(b"foo", true), "Zm9v");
  }

  #[test]
  #[cfg(all(feature = "simd", feature = "alloc"))]
  fn b64_std_encode_string_simd() {
    assert_eq!(base64_std_encode_string_simd(b"", true), "");
    assert_eq!(base64_std_encode_string_simd(b"f", true), "Zg==");
    assert_eq!(base64_std_encode_string_simd(b"f", false), "Zg");
    assert_eq!(base64_std_encode_string_simd(b"fo", true), "Zm8=");
    assert_eq!(base64_std_encode_string_simd(b"fo", false), "Zm8");
    assert_eq!(base64_std_encode_string_simd(b"foo", true), "Zm9v");
    assert_eq!(
      base64_std_encode_string_sync(HELLO_WORLD, true),
      HELLO_WORLD_B64_STD_PAD
    );
    assert_eq!(
      base64_std_encode_string_sync(HELLO_WORLD, false),
      HELLO_WORLD_B64_STD_NOPAD
    );
  }

  #[test]
  #[cfg(all(feature = "simd", feature = "alloc"))]
  fn b64_url_encode_string_simd() {
    assert_eq!(base64_std_encode_string_simd(b"", true), "");
    assert_eq!(base64_std_encode_string_simd(b"f", true), "Zg==");
    assert_eq!(base64_std_encode_string_simd(b"f", false), "Zg");
    assert_eq!(base64_std_encode_string_simd(b"fo", true), "Zm8=");
    assert_eq!(base64_std_encode_string_simd(b"fo", false), "Zm8");
    assert_eq!(base64_std_encode_string_simd(b"foo", true), "Zm9v");
  }
}

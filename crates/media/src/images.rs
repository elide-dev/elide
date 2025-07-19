/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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

#![allow(unused_variables, unused_mut)]

use jni::JNIEnv;
use jni::objects::{JByteBuffer, JClass, JObject};
use jni::sys::jboolean;

#[cfg(feature = "images")]
use std::io::Cursor;

#[cfg(feature = "images")]
use image::codecs::jpeg::JpegEncoder;
#[cfg(feature = "images")]
use image::{ImageFormat, ImageReader};

#[cfg(feature = "png")]
use image::codecs::png::{CompressionType, FilterType, PngEncoder};
use jni::errors::Error;

/// Reads a JVM byte array in-place into a slice.
#[cfg(feature = "images")]
#[inline(always)]
fn read_in_place<'a>(
  env: &mut JNIEnv<'a>,
  buffer: &JByteBuffer<'a>,
) -> Result<&'a mut [u8], Error> {
  let data = env.get_direct_buffer_address(&buffer)?;
  let capacity = env.get_direct_buffer_capacity(&buffer)?;
  unsafe { Ok(std::slice::from_raw_parts_mut(data, capacity)) }
}

#[cfg(feature = "png")]
#[derive(Debug)]
pub struct PngOptions {
  pub compression: CompressionType,
  pub filter: FilterType,
}

#[cfg(feature = "jpg")]
#[derive(Debug)]
pub struct JpegOptions {
  pub quality: u8,
}

#[cfg(feature = "png")]
impl Default for PngOptions {
  fn default() -> Self {
    PngOptions {
      compression: CompressionType::Best,
      filter: FilterType::Adaptive,
    }
  }
}

#[cfg(feature = "jpg")]
impl Default for JpegOptions {
  fn default() -> Self {
    JpegOptions { quality: 90 }
  }
}

#[cfg(feature = "png")]
pub fn do_compress_png<'a>(
  mut env: JNIEnv<'a>,
  _cls: JClass<'a>,
  _opts: JObject<'a>,
  img: JByteBuffer<'a>,
) -> jboolean {
  if img.is_null() {
    return false;
  }
  let data: Result<&'a mut [u8], Error> = read_in_place(&mut env, (&img).into());
  if data.is_err() {
    return false;
  }
  let mut data = data.unwrap();
  let opts = PngOptions::default();
  let mut loaded = ImageReader::new(Cursor::new(&mut *data));
  loaded.set_format(ImageFormat::Png);
  let decoded = loaded
    .decode()
    .expect("invalid png image data as input to `compress_png`");
  let png = PngEncoder::new_with_quality(&mut *data, opts.compression, opts.filter);
  decoded
    .write_with_encoder(png)
    .expect("failed to write PNG image data");
  true
}

#[cfg(feature = "jpg")]
pub fn do_compress_jpg<'a>(
  mut env: JNIEnv<'a>,
  _cls: JClass<'a>,
  _opts: JObject<'a>,
  img: JByteBuffer<'a>,
) -> jboolean {
  if img.is_null() {
    return false;
  }
  let data: Result<&'a mut [u8], Error> = read_in_place(&mut env, (&img).into());
  if data.is_err() {
    return false;
  }
  let mut data = data.unwrap();
  let opts = JpegOptions::default();
  let mut loaded = ImageReader::new(Cursor::new(&mut *data));
  loaded.set_format(ImageFormat::Jpeg);
  let decoded = loaded
    .decode()
    .expect("invalid png image data as input to `compress_png`");
  let jpg = JpegEncoder::new_with_quality(&mut *data, opts.quality);
  decoded
    .write_with_encoder(jpg)
    .expect("failed to write PNG image data");
  true
}

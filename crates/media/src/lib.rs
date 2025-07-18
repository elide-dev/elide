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

use image::codecs::jpeg::JpegEncoder;
use image::codecs::png::{CompressionType, FilterType, PngEncoder};
use image::{ImageFormat, ImageReader};
use java_native::jni;
use jni::JNIEnv;
use jni::objects::{JByteBuffer, JClass, JObject};
use jni::sys::jboolean;
use std::io::Cursor;

/// Reads a JVM byte array in-place into a slice.
#[inline(always)]
fn read_in_place<'a>(env: &mut JNIEnv<'a>, buffer: &JByteBuffer<'a>) -> &'a mut [u8] {
  let data = env
    .get_direct_buffer_address(&buffer)
    .expect("failed to get direct buffer address");
  let capacity = env
    .get_direct_buffer_capacity(&buffer)
    .expect("failed to get direct buffer capacity");

  unsafe { std::slice::from_raw_parts_mut(data, capacity) }
}

#[derive(Debug)]
struct PngOptions {
  compression: CompressionType,
  filter: FilterType,
}

#[derive(Debug)]
struct JpegOptions {
  quality: u8,
}

impl Default for PngOptions {
  fn default() -> Self {
    PngOptions {
      compression: CompressionType::Best,
      filter: FilterType::Adaptive,
    }
  }
}

impl Default for JpegOptions {
  fn default() -> Self {
    JpegOptions { quality: 90 }
  }
}

/// JNI entrypoint which provides image compression facilities for PNGs.
#[jni("elide.tooling.img.ImgNative")]
pub fn compress_png<'a>(
  mut env: JNIEnv<'a>,
  _cls: JClass<'a>,
  _opts: JObject<'a>,
  img: JByteBuffer<'a>,
) -> jboolean {
  let data: &'a mut [u8] = read_in_place(&mut env, &img);
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

/// JNI entrypoint which provides image compression facilities for JPGs.
#[jni("elide.tooling.img.ImgNative")]
pub fn compress_jpg<'a>(
  mut env: JNIEnv<'a>,
  _cls: JClass<'a>,
  _opts: JObject<'a>,
  img: JByteBuffer<'a>,
) -> jboolean {
  let data: &'a mut [u8] = read_in_place(&mut env, &img);
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

/// JNI entrypoint which provides image conversion facilities to WebP.
#[jni("elide.tooling.img.ImgNative")]
pub fn convert_to_webp<'a>(
  mut _env: JNIEnv<'a>,
  _cls: JClass<'a>,
  _opts: JObject<'a>,
  _inplace: jboolean,
  _img: JByteBuffer<'a>,
) -> jboolean {
  false
}

/// JNI entrypoint which provides image conversion facilities to AVIF.
#[jni("elide.tooling.img.ImgNative")]
pub fn convert_to_avif<'a>(
  mut _env: JNIEnv<'a>,
  _cls: JClass<'a>,
  _opts: JObject<'a>,
  _inplace: jboolean,
  _img: JByteBuffer<'a>,
) -> jboolean {
  false
}

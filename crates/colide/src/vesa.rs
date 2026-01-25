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

//! VESA Framebuffer JNI bindings for Colide OS.
//!
#![allow(unsafe_attr_outside_unsafe)]
//! Provides direct framebuffer access on bare metal via Cosmopolitan.

use java_native::jni;
use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::jint;
use lazy_static::lazy_static;
use std::sync::Mutex;

lazy_static! {
    static ref SCREEN_WIDTH: Mutex<i32> = Mutex::new(800);
    static ref SCREEN_HEIGHT: Mutex<i32> = Mutex::new(600);
}

#[cfg(feature = "bare-metal")]
extern "C" {
    fn colide_vesa_init(width: i32, height: i32, bpp: i32) -> i32;
    fn colide_vesa_putpixel(x: i32, y: i32, color: u32);
    fn colide_vesa_fill_rect(x: i32, y: i32, w: i32, h: i32, color: u32);
    fn colide_vesa_clear(color: u32);
    fn colide_vesa_width() -> i32;
    fn colide_vesa_height() -> i32;
}

/// Initialize VESA framebuffer.
pub fn init(width: i32, height: i32, bpp: i32) -> bool {
    #[cfg(feature = "bare-metal")]
    {
        let result = unsafe { colide_vesa_init(width, height, bpp) };
        if result != 0 {
            *SCREEN_WIDTH.lock().unwrap() = width;
            *SCREEN_HEIGHT.lock().unwrap() = height;
            true
        } else {
            false
        }
    }
    #[cfg(not(feature = "bare-metal"))]
    {
        *SCREEN_WIDTH.lock().unwrap() = width;
        *SCREEN_HEIGHT.lock().unwrap() = height;
        true
    }
}

/// Get screen width.
pub fn width() -> i32 {
    #[cfg(feature = "bare-metal")]
    {
        unsafe { colide_vesa_width() }
    }
    #[cfg(not(feature = "bare-metal"))]
    {
        *SCREEN_WIDTH.lock().unwrap()
    }
}

/// Get screen height.
pub fn height() -> i32 {
    #[cfg(feature = "bare-metal")]
    {
        unsafe { colide_vesa_height() }
    }
    #[cfg(not(feature = "bare-metal"))]
    {
        *SCREEN_HEIGHT.lock().unwrap()
    }
}

/// Put a pixel at (x, y) with the given color.
#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_Vesa_putPixel(
    _env: JNIEnv,
    _class: JClass,
    x: jint,
    y: jint,
    color: jint,
) {
    #[cfg(feature = "bare-metal")]
    unsafe {
        colide_vesa_putpixel(x, y, color as u32);
    }
}

/// Fill a rectangle with the given color.
#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_Vesa_fillRect(
    _env: JNIEnv,
    _class: JClass,
    x: jint,
    y: jint,
    w: jint,
    h: jint,
    color: jint,
) {
    #[cfg(feature = "bare-metal")]
    unsafe {
        colide_vesa_fill_rect(x, y, w, h, color as u32);
    }
}

/// Clear the screen with the given color.
#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_Vesa_clear(
    _env: JNIEnv,
    _class: JClass,
    color: jint,
) {
    #[cfg(feature = "bare-metal")]
    unsafe {
        colide_vesa_clear(color as u32);
    }
}

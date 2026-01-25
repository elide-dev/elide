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

//! PS/2 Keyboard JNI bindings for Colide OS.
//!
#![allow(unsafe_attr_outside_unsafe)]
//! Provides direct keyboard input on bare metal via Intel 8042 controller.

use java_native::jni;
use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::{jboolean, jint, JNI_TRUE, JNI_FALSE};

#[cfg(feature = "bare-metal")]
extern "C" {
    fn colide_keyboard_init() -> i32;
    fn colide_keyboard_getchar() -> i32;
    fn colide_keyboard_available() -> i32;
    fn colide_keyboard_getmods() -> i32;
    fn colide_mouse_x() -> i32;
    fn colide_mouse_y() -> i32;
    fn colide_mouse_buttons() -> i32;
}

/// Initialize keyboard driver.
pub fn init() -> bool {
    #[cfg(feature = "bare-metal")]
    {
        unsafe { colide_keyboard_init() != 0 }
    }
    #[cfg(not(feature = "bare-metal"))]
    {
        true
    }
}

/// Get a character from keyboard (blocking).
#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_Keyboard_getChar(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    #[cfg(feature = "bare-metal")]
    {
        unsafe { colide_keyboard_getchar() }
    }
    #[cfg(not(feature = "bare-metal"))]
    {
        // In hosted mode, return -1 (no input)
        -1
    }
}

/// Check if keyboard input is available (non-blocking).
#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_Keyboard_available(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    #[cfg(feature = "bare-metal")]
    {
        unsafe {
            if colide_keyboard_available() != 0 { JNI_TRUE } else { JNI_FALSE }
        }
    }
    #[cfg(not(feature = "bare-metal"))]
    {
        JNI_FALSE
    }
}

/// Get modifier key state (shift, ctrl, alt).
/// Returns bitmask: bit 0 = shift, bit 1 = ctrl, bit 2 = alt
#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_Keyboard_getModifiers(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    #[cfg(feature = "bare-metal")]
    {
        unsafe { colide_keyboard_getmods() }
    }
    #[cfg(not(feature = "bare-metal"))]
    {
        0
    }
}

/// Get mouse X position.
pub fn mouse_x() -> i32 {
    #[cfg(feature = "bare-metal")]
    {
        unsafe { colide_mouse_x() }
    }
    #[cfg(not(feature = "bare-metal"))]
    {
        0
    }
}

/// Get mouse Y position.
pub fn mouse_y() -> i32 {
    #[cfg(feature = "bare-metal")]
    {
        unsafe { colide_mouse_y() }
    }
    #[cfg(not(feature = "bare-metal"))]
    {
        0
    }
}

/// Get mouse button state.
pub fn mouse_buttons() -> i32 {
    #[cfg(feature = "bare-metal")]
    {
        unsafe { colide_mouse_buttons() }
    }
    #[cfg(not(feature = "bare-metal"))]
    {
        0
    }
}

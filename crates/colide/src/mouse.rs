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

//! # PS/2 Mouse Driver
//!
//! Dedicated mouse input driver for Colide OS.
//! Separates mouse handling from keyboard for cleaner architecture.
//!
//! ## PS/2 Mouse Protocol
//! - 3-byte packets: status, X delta, Y delta
//! - Status byte: Y overflow | X overflow | Y sign | X sign | 1 | Middle | Right | Left
//! - Intellimouse: 4-byte packets with scroll wheel

#![allow(unsafe_attr_outside_unsafe)]

use java_native::jni;
use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::{jboolean, jint, JNI_TRUE, JNI_FALSE};
use std::sync::atomic::{AtomicI32, AtomicBool, Ordering};

#[cfg(feature = "bare-metal")]
extern "C" {
    fn colide_mouse_init() -> i32;
    fn colide_mouse_poll() -> i32;
    fn colide_mouse_x() -> i32;
    fn colide_mouse_y() -> i32;
    fn colide_mouse_buttons() -> i32;
    fn colide_mouse_wheel() -> i32;
    fn colide_mouse_set_bounds(max_x: i32, max_y: i32);
}

// Mouse state (for hosted mode emulation)
static MOUSE_X: AtomicI32 = AtomicI32::new(400);
static MOUSE_Y: AtomicI32 = AtomicI32::new(300);
static MOUSE_BUTTONS: AtomicI32 = AtomicI32::new(0);
static MOUSE_WHEEL: AtomicI32 = AtomicI32::new(0);
static MOUSE_INITIALIZED: AtomicBool = AtomicBool::new(false);

/// Button bit flags
pub const BUTTON_LEFT: i32 = 0x01;
pub const BUTTON_RIGHT: i32 = 0x02;
pub const BUTTON_MIDDLE: i32 = 0x04;

/// Initialize PS/2 mouse driver.
pub fn init() -> bool {
    #[cfg(feature = "bare-metal")]
    {
        let result = unsafe { colide_mouse_init() != 0 };
        MOUSE_INITIALIZED.store(result, Ordering::SeqCst);
        result
    }
    #[cfg(not(feature = "bare-metal"))]
    {
        MOUSE_INITIALIZED.store(true, Ordering::SeqCst);
        true
    }
}

/// Poll for mouse events.
/// Returns true if new data available.
pub fn poll() -> bool {
    #[cfg(feature = "bare-metal")]
    {
        unsafe { colide_mouse_poll() != 0 }
    }
    #[cfg(not(feature = "bare-metal"))]
    {
        false
    }
}

/// Get mouse X position.
pub fn get_x() -> i32 {
    #[cfg(feature = "bare-metal")]
    {
        unsafe { colide_mouse_x() }
    }
    #[cfg(not(feature = "bare-metal"))]
    {
        MOUSE_X.load(Ordering::Relaxed)
    }
}

/// Get mouse Y position.
pub fn get_y() -> i32 {
    #[cfg(feature = "bare-metal")]
    {
        unsafe { colide_mouse_y() }
    }
    #[cfg(not(feature = "bare-metal"))]
    {
        MOUSE_Y.load(Ordering::Relaxed)
    }
}

/// Get mouse button state.
/// Returns bitmask of BUTTON_LEFT, BUTTON_RIGHT, BUTTON_MIDDLE.
pub fn get_buttons() -> i32 {
    #[cfg(feature = "bare-metal")]
    {
        unsafe { colide_mouse_buttons() }
    }
    #[cfg(not(feature = "bare-metal"))]
    {
        MOUSE_BUTTONS.load(Ordering::Relaxed)
    }
}

/// Get scroll wheel delta.
pub fn get_wheel() -> i32 {
    #[cfg(feature = "bare-metal")]
    {
        unsafe { colide_mouse_wheel() }
    }
    #[cfg(not(feature = "bare-metal"))]
    {
        MOUSE_WHEEL.swap(0, Ordering::Relaxed)
    }
}

/// Set mouse movement bounds.
pub fn set_bounds(max_x: i32, max_y: i32) {
    #[cfg(feature = "bare-metal")]
    {
        unsafe { colide_mouse_set_bounds(max_x, max_y) }
    }
    #[cfg(not(feature = "bare-metal"))]
    {
        let _ = (max_x, max_y);
    }
}

/// Check if mouse is initialized.
pub fn is_initialized() -> bool {
    MOUSE_INITIALIZED.load(Ordering::SeqCst)
}

// JNI bindings

#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_Mouse_init(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    if init() { JNI_TRUE } else { JNI_FALSE }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_Mouse_poll(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    if poll() { JNI_TRUE } else { JNI_FALSE }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_Mouse_getX(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    get_x()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_Mouse_getY(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    get_y()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_Mouse_getButtons(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    get_buttons()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_Mouse_getWheel(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    get_wheel()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_Mouse_setBounds(
    _env: JNIEnv,
    _class: JClass,
    max_x: jint,
    max_y: jint,
) {
    set_bounds(max_x, max_y)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_Mouse_isInitialized(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    if is_initialized() { JNI_TRUE } else { JNI_FALSE }
}

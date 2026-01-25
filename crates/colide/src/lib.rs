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

//! # Colide OS Native Drivers
//!
//! This crate provides JNI bindings for Colide OS bare metal drivers:
//! - VESA framebuffer (graphics output)
//! - PS/2 keyboard (input)
//! - AI inference (llamafile integration)
//!
//! These drivers are designed to work on bare metal via Cosmopolitan Libc,
//! enabling Elide to run directly on hardware without an OS.

#![allow(clippy::missing_safety_doc)]

pub mod vesa;
pub mod keyboard;
pub mod ai;

use java_native::jni;
use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::{jboolean, jint, JNI_TRUE, JNI_FALSE};
use lazy_static::lazy_static;
use std::sync::Mutex;

lazy_static! {
    static ref INITIALIZED: Mutex<bool> = Mutex::new(false);
}

/// Initialize Colide native drivers.
/// Returns true if initialization succeeded.
#[no_mangle]
pub extern "system" fn Java_elide_colide_ColideNative_init(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    let mut init = INITIALIZED.lock().unwrap();
    if *init {
        return JNI_TRUE;
    }
    
    // Initialize VESA
    if !vesa::init(800, 600, 32) {
        return JNI_FALSE;
    }
    
    // Initialize keyboard
    if !keyboard::init() {
        return JNI_FALSE;
    }
    
    *init = true;
    JNI_TRUE
}

/// Check if running on bare metal (Cosmopolitan).
#[no_mangle]
pub extern "system" fn Java_elide_colide_ColideNative_isMetal(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    // In hosted mode, always returns false
    // In bare metal, the C driver will return true via IsMetal()
    #[cfg(feature = "bare-metal")]
    {
        extern "C" {
            fn colide_is_metal() -> i32;
        }
        unsafe {
            if colide_is_metal() != 0 { JNI_TRUE } else { JNI_FALSE }
        }
    }
    #[cfg(not(feature = "bare-metal"))]
    {
        JNI_FALSE
    }
}

/// Get screen width.
#[no_mangle]
pub extern "system" fn Java_elide_colide_ColideNative_screenWidth(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    vesa::width()
}

/// Get screen height.
#[no_mangle]
pub extern "system" fn Java_elide_colide_ColideNative_screenHeight(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    vesa::height()
}

// JNI_OnLoad for dynamic loading
#[no_mangle]
pub extern "system" fn JNI_OnLoad(
    _vm: *mut jni::sys::JavaVM,
    _reserved: *mut std::ffi::c_void,
) -> jint {
    jni::sys::JNI_VERSION_1_8
}

// JNI_OnLoad for static linking (GraalVM native image)
#[no_mangle]
pub extern "system" fn JNI_OnLoad_colide(
    _vm: *mut jni::sys::JavaVM,
    _reserved: *mut std::ffi::c_void,
) -> jint {
    jni::sys::JNI_VERSION_1_8
}

#[no_mangle]
pub extern "system" fn JNI_OnUnload_colide(
    _vm: *mut jni::sys::JavaVM,
    _reserved: *mut std::ffi::c_void,
) {
    // Cleanup if needed
}

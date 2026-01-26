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
#![allow(unsafe_attr_outside_unsafe)]

pub mod vesa;
pub mod keyboard;
pub mod mouse;
pub mod ai;
pub mod filesystem;
pub mod net;
pub mod exec;
pub mod usb;
pub mod pcie;
pub mod llm;
pub mod hw;
pub mod storage;

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
#[unsafe(no_mangle)]
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
#[unsafe(no_mangle)]
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
#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_ColideNative_screenWidth(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    vesa::width()
}

/// Get screen height.
#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_ColideNative_screenHeight(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    vesa::height()
}

/// Get mouse X position.
#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_ColideNative_mouseX(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    keyboard::mouse_x()
}

/// Get mouse Y position.
#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_ColideNative_mouseY(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    keyboard::mouse_y()
}

/// Get mouse button state.
#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_ColideNative_mouseButtons(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    keyboard::mouse_buttons()
}

// ============ FileSystem JNI Bindings ============

use jni::objects::JString;
use jni::objects::JObjectArray;

/// Check if file exists.
#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_fs_FileSystem_nativeFileExists(
    mut env: JNIEnv,
    _class: JClass,
    path: JString,
) -> jboolean {
    let path: String = env.get_string(&path).map(|s| s.into()).unwrap_or_default();
    if filesystem::file_exists(&path) { JNI_TRUE } else { JNI_FALSE }
}

/// Check if path is directory.
#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_fs_FileSystem_nativeFileIsDir(
    mut env: JNIEnv,
    _class: JClass,
    path: JString,
) -> jboolean {
    let path: String = env.get_string(&path).map(|s| s.into()).unwrap_or_default();
    if filesystem::file_is_dir(&path) { JNI_TRUE } else { JNI_FALSE }
}

/// Get file size.
#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_fs_FileSystem_nativeFileSize(
    mut env: JNIEnv,
    _class: JClass,
    path: JString,
) -> jni::sys::jlong {
    let path: String = env.get_string(&path).map(|s| s.into()).unwrap_or_default();
    filesystem::file_size(&path)
}

/// Read file as text.
#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_fs_FileSystem_nativeFileReadText<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass,
    path: JString,
) -> JString<'a> {
    let path: String = env.get_string(&path).map(|s| s.into()).unwrap_or_default();
    match filesystem::file_read_text(&path) {
        Some(content) => env.new_string(&content).unwrap_or_else(|_| JString::default()),
        None => JString::default(),
    }
}

/// Read file as bytes.
#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_fs_FileSystem_nativeFileReadBytes(
    mut env: JNIEnv,
    _class: JClass,
    path: JString,
) -> jni::sys::jbyteArray {
    let path: String = env.get_string(&path).map(|s| s.into()).unwrap_or_default();
    match filesystem::file_read_bytes(&path) {
        Some(bytes) => {
            let arr = env.new_byte_array(bytes.len() as i32).unwrap();
            let _ = env.set_byte_array_region(&arr, 0, unsafe {
                std::slice::from_raw_parts(bytes.as_ptr() as *const i8, bytes.len())
            });
            arr.into_raw()
        }
        None => std::ptr::null_mut(),
    }
}

/// List directory contents.
#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_fs_FileSystem_nativeListDir(
    mut env: JNIEnv,
    _class: JClass,
    path: JString,
) -> jni::sys::jobjectArray {
    let path: String = env.get_string(&path).map(|s| s.into()).unwrap_or_default();
    match filesystem::dir_list(&path) {
        Some(entries) => {
            let string_class = env.find_class("java/lang/String").unwrap();
            let arr = env.new_object_array(entries.len() as i32, &string_class, JString::default()).unwrap();
            for (i, entry) in entries.iter().enumerate() {
                let jstr = env.new_string(entry).unwrap();
                let _ = env.set_object_array_element(&arr, i as i32, jstr);
            }
            arr.into_raw()
        }
        None => std::ptr::null_mut(),
    }
}

// JNI_OnLoad for dynamic loading
#[unsafe(no_mangle)]
pub extern "system" fn JNI_OnLoad(
    _vm: *mut jni::sys::JavaVM,
    _reserved: *mut std::ffi::c_void,
) -> jint {
    jni::sys::JNI_VERSION_1_8
}

// JNI_OnLoad for static linking (GraalVM native image)
#[unsafe(no_mangle)]
pub extern "system" fn JNI_OnLoad_colide(
    _vm: *mut jni::sys::JavaVM,
    _reserved: *mut std::ffi::c_void,
) -> jint {
    jni::sys::JNI_VERSION_1_8
}

#[unsafe(no_mangle)]
pub extern "system" fn JNI_OnUnload_colide(
    _vm: *mut jni::sys::JavaVM,
    _reserved: *mut std::ffi::c_void,
) {
    // Cleanup if needed
}

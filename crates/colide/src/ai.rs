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

//! AI Inference JNI bindings for Colide OS.
//!
#![allow(unsafe_attr_outside_unsafe)]
//! Provides llamafile integration for bare metal AI inference.

use java_native::jni;
use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jint, JNI_TRUE, JNI_FALSE};
use lazy_static::lazy_static;
use std::sync::Mutex;

lazy_static! {
    static ref AI_INITIALIZED: Mutex<bool> = Mutex::new(false);
}

#[cfg(feature = "bare-metal")]
extern "C" {
    fn colide_ai_init(model_path: *const i8) -> i32;
    fn colide_ai_complete(prompt: *const i8, result: *mut i8, max_len: i32) -> i32;
    fn colide_ai_shutdown();
}

/// Initialize AI with the given model path.
#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_Ai_init(
    mut env: JNIEnv,
    _class: JClass,
    model_path: JString,
) -> jboolean {
    let mut init = AI_INITIALIZED.lock().unwrap();
    if *init {
        return JNI_TRUE;
    }
    
    #[cfg(feature = "bare-metal")]
    {
        let path: String = env.get_string(&model_path)
            .expect("Failed to get model path string")
            .into();
        let c_path = std::ffi::CString::new(path).unwrap();
        let result = unsafe { colide_ai_init(c_path.as_ptr()) };
        if result != 0 {
            *init = true;
            JNI_TRUE
        } else {
            JNI_FALSE
        }
    }
    #[cfg(not(feature = "bare-metal"))]
    {
        // In hosted mode, defer to local-ai crate
        *init = true;
        JNI_TRUE
    }
}

/// Complete a prompt using the AI model.
#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_Ai_complete<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    prompt: JString<'local>,
    max_len: jint,
) -> JString<'local> {
    #[cfg(feature = "bare-metal")]
    {
        let prompt_str: String = env.get_string(&prompt)
            .expect("Failed to get prompt string")
            .into();
        let c_prompt = std::ffi::CString::new(prompt_str).unwrap();
        
        let mut result_buf: Vec<i8> = vec![0; max_len as usize];
        let result_len = unsafe {
            colide_ai_complete(c_prompt.as_ptr(), result_buf.as_mut_ptr(), max_len)
        };
        
        if result_len > 0 {
            let result_str = unsafe {
                std::ffi::CStr::from_ptr(result_buf.as_ptr())
                    .to_string_lossy()
                    .into_owned()
            };
            env.new_string(result_str).expect("Failed to create result string")
        } else {
            env.new_string("").expect("Failed to create empty string")
        }
    }
    #[cfg(not(feature = "bare-metal"))]
    {
        // In hosted mode, return empty (defer to local-ai)
        env.new_string("").expect("Failed to create empty string")
    }
}

/// Shutdown AI and release resources.
#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_Ai_shutdown(
    _env: JNIEnv,
    _class: JClass,
) {
    let mut init = AI_INITIALIZED.lock().unwrap();
    if !*init {
        return;
    }
    
    #[cfg(feature = "bare-metal")]
    unsafe {
        colide_ai_shutdown();
    }
    
    *init = false;
}

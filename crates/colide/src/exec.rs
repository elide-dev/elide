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

//! # Tool Execution
//!
//! Provides execution of embedded tools from /zip/bin/ on bare metal.
//! Uses Cosmopolitan's native process execution capabilities.

use std::sync::atomic::{AtomicI32, Ordering};

#[cfg(feature = "bare-metal")]
extern "C" {
    fn colide_exec(path: *const i8, argv: *const *const i8, argc: i32) -> i32;
    fn colide_exec_capture(path: *const i8, argv: *const *const i8, argc: i32, 
                           output: *mut u8, max_output: usize) -> i32;
}

static LAST_EXIT_CODE: AtomicI32 = AtomicI32::new(0);

/// Execute a tool and capture its output.
pub fn execute(path: &str, args: &[&str]) -> Option<String> {
    #[cfg(feature = "bare-metal")]
    {
        use std::ffi::CString;
        
        let c_path = CString::new(path).ok()?;
        let c_args: Vec<CString> = args.iter()
            .filter_map(|s| CString::new(*s).ok())
            .collect();
        
        let mut c_argv: Vec<*const i8> = Vec::with_capacity(c_args.len() + 2);
        c_argv.push(c_path.as_ptr());
        for arg in &c_args {
            c_argv.push(arg.as_ptr());
        }
        c_argv.push(std::ptr::null());
        
        let mut output_buf = vec![0u8; 65536];
        
        let exit_code = unsafe {
            colide_exec_capture(
                c_path.as_ptr(),
                c_argv.as_ptr(),
                c_args.len() as i32 + 1,
                output_buf.as_mut_ptr(),
                output_buf.len()
            )
        };
        
        LAST_EXIT_CODE.store(exit_code, Ordering::SeqCst);
        
        let end = output_buf.iter().position(|&b| b == 0).unwrap_or(output_buf.len());
        Some(String::from_utf8_lossy(&output_buf[..end]).to_string())
    }
    
    #[cfg(not(feature = "bare-metal"))]
    {
        use std::process::Command;
        
        let output = Command::new(path)
            .args(args)
            .output()
            .ok()?;
        
        LAST_EXIT_CODE.store(output.status.code().unwrap_or(-1), Ordering::SeqCst);
        
        let stdout = String::from_utf8_lossy(&output.stdout);
        let stderr = String::from_utf8_lossy(&output.stderr);
        
        if stderr.is_empty() {
            Some(stdout.to_string())
        } else {
            Some(format!("{}{}", stdout, stderr))
        }
    }
}

/// Get the exit code from the last execution.
pub fn get_exit_code() -> i32 {
    LAST_EXIT_CODE.load(Ordering::SeqCst)
}

/// Execute a tool without capturing output.
pub fn execute_detached(path: &str, args: &[&str]) -> bool {
    #[cfg(feature = "bare-metal")]
    {
        use std::ffi::CString;
        
        let c_path = match CString::new(path) {
            Ok(p) => p,
            Err(_) => return false,
        };
        
        let c_args: Vec<CString> = args.iter()
            .filter_map(|s| CString::new(*s).ok())
            .collect();
        
        let mut c_argv: Vec<*const i8> = Vec::with_capacity(c_args.len() + 2);
        c_argv.push(c_path.as_ptr());
        for arg in &c_args {
            c_argv.push(arg.as_ptr());
        }
        c_argv.push(std::ptr::null());
        
        let result = unsafe {
            colide_exec(c_path.as_ptr(), c_argv.as_ptr(), c_args.len() as i32 + 1)
        };
        
        result == 0
    }
    
    #[cfg(not(feature = "bare-metal"))]
    {
        use std::process::Command;
        
        Command::new(path)
            .args(args)
            .spawn()
            .is_ok()
    }
}

// JNI bindings
use jni::JNIEnv;
use jni::objects::{JClass, JString, JObjectArray};
use jni::sys::jint;

#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_exec_ToolExecutor_nativeExecute<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass,
    path: JString,
    args: JObjectArray,
) -> JString<'a> {
    let path_str: String = match env.get_string(&path) {
        Ok(s) => s.into(),
        Err(_) => return JString::default(),
    };
    
    let args_len = match env.get_array_length(&args) {
        Ok(len) => len as usize,
        Err(_) => 0,
    };
    
    let mut rust_args: Vec<String> = Vec::with_capacity(args_len);
    for i in 0..args_len {
        if let Ok(obj) = env.get_object_array_element(&args, i as i32) {
            let jstr = JString::from(obj);
            if let Ok(s) = env.get_string(&jstr) {
                rust_args.push(s.into());
            }
        }
    }
    
    let arg_refs: Vec<&str> = rust_args.iter().map(|s| s.as_str()).collect();
    
    match execute(&path_str, &arg_refs) {
        Some(output) => env.new_string(&output).unwrap_or_else(|_| JString::default()),
        None => JString::default(),
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_exec_ToolExecutor_nativeGetExitCode(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    get_exit_code()
}

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

//! # Filesystem Access
//!
//! Provides file system operations for Colide OS.
//! Supports both the embedded /zip/ filesystem (via Cosmopolitan)
//! and standard file operations.

#[cfg(feature = "bare-metal")]
extern "C" {
    fn colide_file_exists(path: *const i8) -> i32;
    fn colide_file_is_dir(path: *const i8) -> i32;
    fn colide_file_size(path: *const i8) -> i64;
    fn colide_file_read(path: *const i8, buf: *mut u8, max_size: usize) -> i64;
    fn colide_dir_list(path: *const i8, buf: *mut u8, max_size: usize) -> i32;
}

use std::ffi::CString;

/// Check if a file exists.
pub fn file_exists(path: &str) -> bool {
    #[cfg(feature = "bare-metal")]
    {
        let c_path = CString::new(path).unwrap();
        unsafe { colide_file_exists(c_path.as_ptr()) != 0 }
    }
    #[cfg(not(feature = "bare-metal"))]
    {
        std::path::Path::new(path).exists()
    }
}

/// Check if path is a directory.
pub fn file_is_dir(path: &str) -> bool {
    #[cfg(feature = "bare-metal")]
    {
        let c_path = CString::new(path).unwrap();
        unsafe { colide_file_is_dir(c_path.as_ptr()) != 0 }
    }
    #[cfg(not(feature = "bare-metal"))]
    {
        std::path::Path::new(path).is_dir()
    }
}

/// Get file size.
pub fn file_size(path: &str) -> i64 {
    #[cfg(feature = "bare-metal")]
    {
        let c_path = CString::new(path).unwrap();
        unsafe { colide_file_size(c_path.as_ptr()) }
    }
    #[cfg(not(feature = "bare-metal"))]
    {
        std::fs::metadata(path).map(|m| m.len() as i64).unwrap_or(-1)
    }
}

/// Read file contents as bytes.
pub fn file_read_bytes(path: &str) -> Option<Vec<u8>> {
    #[cfg(feature = "bare-metal")]
    {
        let size = file_size(path);
        if size <= 0 {
            return None;
        }
        
        let mut buf = vec![0u8; size as usize];
        let c_path = CString::new(path).unwrap();
        let read = unsafe { colide_file_read(c_path.as_ptr(), buf.as_mut_ptr(), buf.len()) };
        
        if read > 0 {
            buf.truncate(read as usize);
            Some(buf)
        } else {
            None
        }
    }
    #[cfg(not(feature = "bare-metal"))]
    {
        std::fs::read(path).ok()
    }
}

/// Read file contents as string.
pub fn file_read_text(path: &str) -> Option<String> {
    file_read_bytes(path).and_then(|bytes| String::from_utf8(bytes).ok())
}

/// List directory contents.
/// Returns entries in format: "name|type|size|exec" separated by newlines.
pub fn dir_list(path: &str) -> Option<Vec<String>> {
    #[cfg(feature = "bare-metal")]
    {
        let mut buf = vec![0u8; 65536];
        let c_path = CString::new(path).unwrap();
        let count = unsafe { colide_dir_list(c_path.as_ptr(), buf.as_mut_ptr(), buf.len()) };
        
        if count <= 0 {
            return None;
        }
        
        let end = buf.iter().position(|&b| b == 0).unwrap_or(buf.len());
        let content = String::from_utf8_lossy(&buf[..end]);
        Some(content.lines().map(|s| s.to_string()).collect())
    }
    #[cfg(not(feature = "bare-metal"))]
    {
        let entries: Vec<String> = std::fs::read_dir(path)
            .ok()?
            .filter_map(|e| e.ok())
            .map(|entry| {
                let name = entry.file_name().to_string_lossy().to_string();
                let meta = entry.metadata().ok();
                let is_dir = meta.as_ref().map(|m| m.is_dir()).unwrap_or(false);
                let size = meta.as_ref().map(|m| m.len()).unwrap_or(0);
                let is_exec = false;
                format!(
                    "{}|{}|{}|{}",
                    name,
                    if is_dir { "d" } else { "f" },
                    size,
                    if is_exec { "x" } else { "-" }
                )
            })
            .collect();
        Some(entries)
    }
}

/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

#![allow(
  non_snake_case,
  non_camel_case_types,
  non_upper_case_globals,
  improper_ctypes
)]
#![forbid(dead_code)]

use java_native::{on_load, on_unload};
use jni::JavaVM;
use jni_sys::{JNI_VERSION_21, jint};

/// Methods to manage VM state.
pub mod vm;

/// JNI on-load hook; responsible for setting the active VM.
#[on_load]
pub fn on_load(vm: JavaVM) -> jint {
  vm::init_java_vm(vm);
  JNI_VERSION_21
}

/// JNI on-unload hook; responsible for clearing the active VM on shutdown or unload.
#[on_unload]
pub fn on_unload() {
  vm::clear_vm_state();
}

include!(concat!(env!("OUT_DIR"), "/libjvm.rs"));

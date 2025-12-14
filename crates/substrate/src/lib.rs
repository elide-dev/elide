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
  improper_ctypes,
  unnecessary_transmutes
)]
#![forbid(dead_code)]

use jni::JavaVM;

/// Methods to manage VM state.
pub mod vm;

/// Initialize the VM if needed.
pub fn init_vm(vm: JavaVM) {
  vm::init_java_vm(vm);
}

include!(concat!(env!("OUT_DIR"), "/libjvm.rs"));

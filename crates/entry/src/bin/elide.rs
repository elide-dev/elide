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
  dead_code
)]

use gvmcapi::ELIDE_PLUGIN_API_VERSION;
use std::env;
use std::env::current_exe;
use std::ffi::OsString;
use std::os::raw::c_int;
use std::process::exit;
use umbrella::run_oro_with_args;

#[cfg(any(
  all(feature = "allocator", target_env = "musl"),
  all(feature = "allocator", target_os = "windows")
))]
#[global_allocator]
static GLOBAL: mimalloc::MiMalloc = mimalloc::MiMalloc;

#[cfg(all(
  feature = "allocator",
  feature = "jemalloc",
  not(target_os = "windows"),
  not(target_os = "openbsd"),
  not(target_env = "musl"),
  any(
    target_arch = "x86_64",
    target_arch = "aarch64",
    target_arch = "powerpc64"
  )
))]
#[global_allocator]
static GLOBAL: tikv_jemallocator::Jemalloc = tikv_jemallocator::Jemalloc;

#[cfg(feature = "entry-v1")]
include!(concat!(env!("OUT_DIR"), "/libelidemain.rs"));

#[cfg(feature = "entry-v2")]
include!(concat!(env!("OUT_DIR"), "/libentry.rs"));

#[cfg(feature = "entry-v1")]
const ENTRY_INIT_SYMBOL: &[u8] = b"elide_main_init\0";
#[cfg(feature = "entry-v1")]
const ENTRY_RUN_SYMBOL: &[u8] = b"elide_main_entry\0";

#[cfg(feature = "entry-v2")]
const ENTRY_INIT_SYMBOL: &[u8] = b"elide_entry_init\0";
#[cfg(feature = "entry-v2")]
const ENTRY_RUN_SYMBOL: &[u8] = b"elide_entry_run\0";
const GRAAL_CREATE_ISOLATE_SYMBOL: &[u8] = b"graal_create_isolate\0";
const GRAAL_TEAR_DOWN_ISOLATE_SYMBOL: &[u8] = b"graal_tear_down_isolate\0";

#[cfg(feature = "entry-v1")]
fn run_entry_v1_with_args(_args: Vec<String>) -> Result<u32, Box<dyn std::error::Error>> {
  let bin_path = current_exe().unwrap();
  let lib_path = bin_path.parent().unwrap().to_path_buf();

  // Determine the correct library file extension based on platform
  #[cfg(target_os = "windows")]
  let lib_extension = "dll";
  #[cfg(target_os = "macos")]
  let lib_extension = "dylib";
  #[cfg(not(any(target_os = "windows", target_os = "macos")))]
  let lib_extension = "so";

  let lib_entry_path = lib_path.join(format!("libelidemain.{}", lib_extension));
  let lib_entry_path_str = lib_entry_path.to_str().unwrap();

  unsafe {
    let mut isolate: *mut graal_isolate_t = std::ptr::null_mut();
    let mut thread: *mut graal_isolatethread_t = std::ptr::null_mut();
    let lib = libloading::Library::new(lib_entry_path_str)?;

    // load symbol to create isolates
    let graal_create_isolate: libloading::Symbol<
      unsafe extern "C" fn(
        *mut graal_create_isolate_params_t,
        *mut *mut graal_isolate_t,
        *mut *mut graal_isolatethread_t,
      ) -> i32,
    > = lib.get(GRAAL_CREATE_ISOLATE_SYMBOL)?;

    // initialize isolate state
    if graal_create_isolate(std::ptr::null_mut(), &mut isolate, &mut thread) != 0 {
      eprintln!("isolate initialization error");
      exit(-1);
    }

    // load the init and entrypoint functions
    let elide_entry_init: libloading::Symbol<
      unsafe extern "C" fn(*mut graal_isolatethread_t) -> i32,
    > = lib.get(ENTRY_INIT_SYMBOL)?;

    let elide_entry_run: libloading::Symbol<
      unsafe extern "C" fn(*mut graal_isolatethread_t, el_entry_invocation) -> i32,
    > = lib.get(ENTRY_RUN_SYMBOL)?;

    let invoc = el_entry_invocation {
      f_apiversion: ELIDE_PLUGIN_API_VERSION as c_int,
    };
    elide_entry_init(thread);
    let run_result = elide_entry_run(thread, invoc);

    // clean up
    let graal_tear_down_isolate: libloading::Symbol<
      unsafe extern "C" fn(*mut graal_isolatethread_t) -> i32,
    > = lib.get(GRAAL_TEAR_DOWN_ISOLATE_SYMBOL)?;

    graal_tear_down_isolate(thread);
    Ok(run_result as u32)
  }
}

#[cfg(feature = "entry-v2")]
fn run_entry_v2_with_args(_args: Vec<String>) -> Result<u32, Box<dyn std::error::Error>> {
  let bin_path = current_exe().unwrap();
  let lib_path = bin_path.parent().unwrap().to_path_buf();

  // Determine the correct library file extension based on platform
  #[cfg(target_os = "windows")]
  let lib_extension = "dll";
  #[cfg(target_os = "macos")]
  let lib_extension = "dylib";
  #[cfg(not(any(target_os = "windows", target_os = "macos")))]
  let lib_extension = "so";

  let lib_entry_path = lib_path.join(format!("libentry.{}", lib_extension));
  let lib_entry_path_str = lib_entry_path.to_str().unwrap();

  unsafe {
    let mut isolate: *mut graal_isolate_t = std::ptr::null_mut();
    let mut thread: *mut graal_isolatethread_t = std::ptr::null_mut();
    let lib = libloading::Library::new(lib_entry_path_str)?;

    // load symbol to create isolates
    let graal_create_isolate: libloading::Symbol<
      unsafe extern "C" fn(
        *mut graal_create_isolate_params_t,
        *mut *mut graal_isolate_t,
        *mut *mut graal_isolatethread_t,
      ) -> i32,
    > = lib.get(GRAAL_CREATE_ISOLATE_SYMBOL)?;

    // initialize isolate state
    if graal_create_isolate(std::ptr::null_mut(), &mut isolate, &mut thread) != 0 {
      eprintln!("isolate initialization error");
      exit(-1);
    }

    // load the init and entrypoint functions
    let elide_entry_init: libloading::Symbol<
      unsafe extern "C" fn(*mut graal_isolatethread_t) -> i32,
    > = lib.get(ENTRY_INIT_SYMBOL)?;

    let elide_entry_run: libloading::Symbol<
      unsafe extern "C" fn(*mut graal_isolatethread_t) -> i32,
    > = lib.get(ENTRY_RUN_SYMBOL)?;

    elide_entry_init(thread);
    let run_result = elide_entry_run(thread);

    // clean up
    let graal_tear_down_isolate: libloading::Symbol<
      unsafe extern "C" fn(*mut graal_isolatethread_t) -> i32,
    > = lib.get(GRAAL_TEAR_DOWN_ISOLATE_SYMBOL)?;

    graal_tear_down_isolate(thread);
    Ok(run_result as u32)
  }
}

fn run_tool_with_args(tool: &str, args: Vec<OsString>) -> i32 {
  match tool {
    "oro" => match run_oro_with_args(args) {
      Ok(_) => 0,
      Err(e) => {
        eprintln!("oro error: {}", e);
        1
      }
    },
    _ => panic!("unknown tool: {}", tool),
  }
}

pub fn main() {
  let args: Vec<String> = env::args().collect();
  let empty = String::new();
  let first_or_empty = args.get(1).unwrap_or(&empty);
  if first_or_empty == "install" || first_or_empty == "i" {
    let tool = "oro";
    let oro_args = vec![OsString::from("oro"), OsString::from("apply")];
    exit(run_tool_with_args(tool, oro_args));
  }
  #[cfg(feature = "entry-v1")]
  match run_entry_v1_with_args(args) {
    Ok(code) => exit(code as i32),
    Err(e) => {
      eprintln!("error: {}", e);
      exit(1);
    }
  }
  #[cfg(feature = "entry-v2")]
  match run_entry_v2_with_args(args) {
    Ok(code) => exit(code as i32),
    Err(e) => {
      eprintln!("error: {}", e);
      exit(1);
    }
  }
}

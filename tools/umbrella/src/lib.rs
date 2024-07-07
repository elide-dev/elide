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
#![forbid(unsafe_op_in_unsafe_fn, unused_unsafe, unsafe_code)]

#[cfg(target_os = "windows")]
#[global_allocator]
static GLOBAL: mimalloc::MiMalloc = mimalloc::MiMalloc;

#[cfg(all(
  not(target_os = "windows"),
  not(target_os = "openbsd"),
  any(
    target_arch = "x86_64",
    target_arch = "aarch64",
    target_arch = "powerpc64"
  )
))]
#[global_allocator]
static GLOBAL: tikv_jemallocator::Jemalloc = tikv_jemallocator::Jemalloc;

use clap::Args as ClapArgs;
use clap::{Command, Parser};
use colored::Colorize;
use java_native::{jni, on_load, on_unload};
use jni::JNIEnv;
use jni::objects::{JClass, JObjectArray, JString};
use jni::sys::{JavaVM, jint, jobjectArray, jstring};
use jni_sys::JNI_VERSION_21;
use miette::Result;
use std::collections::HashMap;
use std::ffi::{c_void, OsStr};
use std::sync::OnceLock;
use tokio::runtime::Runtime;

pub use transport;
pub use terminal;

#[cfg(feature = "orogene")]
use orogene::Orogene;

#[cfg(feature = "ruff")]
use ruff::args::{Args};
#[cfg(feature = "ruff")]
use ruff::{ExitStatus, run};
#[cfg(feature = "uv")]
use uv::run_uv_entry_with_args;

use crate::tools::{ToolInfo, API_VERSION, LIB_VERSION};
#[cfg(feature = "biome")]
use crate::tools::BIOME_INFO;
#[cfg(feature = "orogene")]
use crate::tools::ORO_INFO;
#[cfg(feature = "oxc")]
use crate::tools::OXC_INFO;
#[cfg(feature = "ruff")]
use crate::tools::RUFF_INFO;
#[cfg(feature = "uv")]
use crate::tools::UV_INFO;

mod diagnostics;
mod tools;
mod nativetransport;

/// Obtain a mapping of tool names to their respective `ToolInfo`.
fn tool_map() -> &'static HashMap<&'static str, &'static ToolInfo> {
  static TOOL_MAP: OnceLock<HashMap<&'static str, &'static ToolInfo>> = OnceLock::new();
  TOOL_MAP.get_or_init(|| {
    let mut m = HashMap::new();
    #[cfg(feature = "biome")]
    m.insert("biome", &BIOME_INFO);
    #[cfg(feature = "orogene")]
    m.insert("orogene", &ORO_INFO);
    #[cfg(feature = "oxc")]
    m.insert("oxc", &OXC_INFO);
    #[cfg(feature = "ruff")]
    m.insert("ruff", &RUFF_INFO);
    #[cfg(feature = "uv")]
    m.insert("uv", &UV_INFO);
    m
  })
}

/// Obtain the active Tokio runtime; if one is not already initialized, a new one will be created.
fn obtain_runtime() -> &'static Runtime {
  static RUNTIME: OnceLock<Runtime> = OnceLock::new();
  RUNTIME.get_or_init(|| {
    tokio::runtime::Builder::new_multi_thread()
            .enable_all()
            .build()
            .expect("Failed building the Runtime")
  })
}

#[cfg(feature = "ruff")]
fn run_ruff_entry(args: Vec<std::ffi::OsString>) -> ExitStatus {
  let args = Args::parse_from(args);
  run(args).unwrap_or_else(|err| {
    #[allow(clippy::print_stderr)]
    {
      // This communicates that this isn't a linter error but ruff itself hard-errored for
      // some reason (e.g. failed to resolve the configuration)
      eprintln!("{}", "ruff failed".red().bold());
      // Currently we generally only see one error, but e.g. with io errors when resolving
      // the configuration it is help to chain errors ("resolving configuration failed" ->
      // "failed to read file: subdir/pyproject.toml")
      for cause in err.chain() {
        eprintln!("  {} {cause}", "Cause:".bold());
      }
    }
    ExitStatus::Error.into()
  })
}

#[cfg(feature = "orogene")]
fn run_oro_with_args(args: Vec<std::ffi::OsString>) -> Result<()> {
  let cmd = Command::new("orogene");
  let again = Orogene::augment_args(cmd);

  // Execute the future, blocking the current thread until completion
  let runtime = obtain_runtime();
  let _guard = runtime.enter();
  runtime.block_on(Orogene::init_and_run(again, args))
}

#[cfg(feature = "uv")]
fn run_uv_with_args(args: Vec<std::ffi::OsString>) -> uv::commands::ExitStatus {
  eprintln!("checkpoint(uv) args: {:?}", args);
  eprintln!("checkpoint(uv) {}", 0);
  let runtime = obtain_runtime();
  eprintln!("checkpoint(uv) {}", 1);
  let _guard = runtime.enter();
  eprintln!("checkpoint(uv) {}", 2);
  match runtime.block_on(run_uv_entry_with_args(args)) {
    Ok(_) => uv::commands::ExitStatus::Success,
    Err(e) => {
      eprintln!("Error running uv: {:?}", e);
      return uv::commands::ExitStatus::Error;
    }
  }
}

#[cfg(test)]
mod tests {
  #[cfg(feature = "uv")]
  use std::ffi::OsString;

  #[cfg(feature = "uv")]
  #[test]
  fn test_run_uv_with_args() {
    let args = vec!["uv", "--help"];
    let as_os_strs: Vec<OsString> = args.iter().map(|x| x.to_string().into()).collect();
    let result = super::run_uv_with_args(as_os_strs);
    let exit_code = match result {
      uv::commands::ExitStatus::Success => 0,
      uv::commands::ExitStatus::Failure => 1,
      uv::commands::ExitStatus::Error => 2,
    };
    assert_eq!(exit_code, 0);
  }
}

// -- Entrypoint Functions
fn all_supported_tools() -> Vec<&'static str> {
    tool_map().keys().map(|&x| x).collect()
}

// -- JNI Aliases
#[jni("dev.elide.cli.bridge.CliNativeBridge")]
pub fn libVersion(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    env.new_string(LIB_VERSION).unwrap().into_raw()
}

#[jni("dev.elide.cli.bridge.CliNativeBridge")]
pub fn apiVersion(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    env.new_string(API_VERSION).unwrap().into_raw()
}

#[jni("dev.elide.cli.bridge.CliNativeBridge")]
pub fn supportedTools<'local>(
    mut env: JNIEnv,
    _class: JClass,
) -> jobjectArray {
    let tools = all_supported_tools();
    let array = env
        .new_object_array(
            tools.len() as i32,
            "java/lang/String",
            env.new_string("").unwrap(),
        )
        .unwrap();

    for (i, tool) in tools.iter().enumerate() {
        let tool = env.new_string(*tool).unwrap();
        env.set_object_array_element(&array, i as i32, tool)
            .unwrap();
    }
    array.into_raw()
}

#[jni("dev.elide.cli.bridge.CliNativeBridge")]
pub fn relatesTo<'local>(
    mut env: JNIEnv,
    _class: JClass,
    tool: JString<'local>,
) -> jobjectArray {
  let input: String = env
          .get_string(&tool)
          .expect("Couldn't get tool string")
          .into();
  let tool_info = tool_map().get(input.as_str());
  let tool = match tool_info {
    Some(tool) => tool,
    None => panic!("Tool not found"),
  };
  let array = env
          .new_object_array(1, "java/lang/String", env.new_string("").unwrap())
          .unwrap();

  tool.language.iter().enumerate().for_each(|(_i, lang)| {
    let tool = env.new_string(lang).unwrap();
    env.set_object_array_element(&array, 0, tool).unwrap();
  });

  array.into_raw()
}

#[jni("dev.elide.cli.bridge.CliNativeBridge")]
pub fn toolVersion<'local>(
    mut env: JNIEnv,
    _class: JClass,
    tool: JString<'local>,
) -> jstring {
    let input: String = env
        .get_string(&tool)
        .expect("Couldn't get tool string")
        .into();
    let tool_info = tool_map().get(input.as_str());
    let tool = match tool_info {
        Some(tool) => tool,
        None => panic!("Tool not found"),
    };
    env.new_string(tool.version).unwrap().into_raw()
}

fn decode_tool_args(mut env: JNIEnv, args: JObjectArray) -> Vec<std::ffi::OsString> {
  let arg_count = env.get_array_length(&args).unwrap();
  let mut arg_i = 0;
  let mut tool_args: Vec<std::ffi::OsString> = vec![];
  while arg_i < arg_count {
    // fetch the arg
    let arg_entry = env.get_object_array_element(&args, arg_i)
            .expect("Failed to retrieve expected array element");

    // cast/decode as a string
    let arg_str = env.get_string(<&JString>::from(&arg_entry))
            .expect("Failed to decode string argument for Ruff");

    let arg_str = arg_str.to_str();
    let arg_os_str = OsStr::new(arg_str.as_ref());

    // insert into the arg vec
    tool_args.insert(arg_i as usize, arg_os_str.to_os_string());
    arg_i = arg_i + 1;
  }
  tool_args
}

#[jni("dev.elide.cli.bridge.CliNativeBridge")]
pub fn runOrogene<'local>(env: JNIEnv<'local>, _class: JClass<'local>, args: JObjectArray<'local>) -> jint {
  #[cfg(feature = "orogene")]
  match run_oro_with_args(decode_tool_args(env, args)) {
    Ok(_) => 0,
    Err(_) => 1,
  }
  #[cfg(not(feature = "orogene"))]
  return -1
}

#[jni("dev.elide.cli.bridge.CliNativeBridge")]
pub fn runRuff<'local>(env: JNIEnv<'local>, _class: JClass<'local>, args: JObjectArray<'local>) -> jint {
  #[cfg(feature = "ruff")]
  match run_ruff_entry(decode_tool_args(env, args)) {
    ExitStatus::Success => 0,
    ExitStatus::Failure => 1,
    ExitStatus::Error => 2,
  }
  #[cfg(not(feature = "ruff"))]
  return 0
}

#[cfg(feature = "uv")]
#[jni("dev.elide.cli.bridge.CliNativeBridge")]
pub fn runUv<'local>(env: JNIEnv<'local>, _class: JClass<'local>, args: JObjectArray<'local>) -> jint {
  match run_uv_with_args(decode_tool_args(env, args)) {
    uv::commands::ExitStatus::Success => 0,
    uv::commands::ExitStatus::Failure => 1,
    uv::commands::ExitStatus::Error => 2,
  }
}

#[cfg(not(feature = "uv"))]
#[jni("dev.elide.cli.bridge.CliNativeBridge")]
pub fn runUv<'local>(_env: JNIEnv<'local>, _class: JClass<'local>, _args: JObjectArray<'local>) -> jint {
  return -1
}

#[cfg(feature = "biome")]
#[jni("dev.elide.cli.bridge.CliNativeBridge")]
pub fn runBiomeFmt<'local>(_env: JNIEnv<'local>, _class: JClass<'local>, _args: JObjectArray<'local>) -> jint {
  return 0
}

#[cfg(not(feature = "biome"))]
#[jni("dev.elide.cli.bridge.CliNativeBridge")]
pub fn runBiomeFmt<'local>(_env: JNIEnv<'local>, _class: JClass<'local>, _args: JObjectArray<'local>) -> jint {
  return -1
}

#[on_load(umbrella)]
pub fn on_load<'local>(_vm: JavaVM, _: c_void) -> jint {
  return JNI_VERSION_21;
}

#[on_unload(umbrella)]
pub fn on_unload<'local>(_vm: JavaVM, _: c_void) {
  // nothing to do at this time
}

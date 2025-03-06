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
use std::env;
use std::ffi::OsString;
use umbrella::run_oro_with_args;

#[cfg(target_os = "windows")]
#[global_allocator]
static GLOBAL: mimalloc::MiMalloc = mimalloc::MiMalloc;

#[cfg(all(feature = "allocator", target_env = "musl"))]
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

fn run_tool_with_args(tool: &str, args: Vec<OsString>) {
  match tool {
    "oro" => {
      run_oro_with_args(args).expect("failed to run orogene");
    }
    _ => {
      panic!("unknown tool: {}", tool);
    }
  }
}

pub fn main() {
  let args: Vec<String> = env::args().collect();
  let tool = "oro";
  let args: Vec<OsString> = args.iter().map(|arg| OsString::from(arg)).collect();
  run_tool_with_args(tool, args);
}

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

use std::env::var;

extern crate rustc_version;
use rustc_version::{version as rustc_version, version_meta, Channel};

fn main() {
  // Define environment variables for build context.
  let version = var("CARGO_PKG_VERSION").expect("variable `CARGO_PKG_VERSION` is not defined");
  let target = var("TARGET").expect("variable `TARGET` is not defined");
  let profile = var("PROFILE").expect("variable `PROFILE` is not defined");
  let rustc_version = rustc_version().expect("failed to resolve rustc version");

  println!("cargo:rustc-env=ELIDE_VERSION={}", version);
  println!("cargo:rustc-env=ELIDE_TARGET={}", target);
  println!("cargo:rustc-env=ELIDE_PROFILE={}", profile);
  println!("cargo:rustc-env=RUSTC_VERSION={}", rustc_version);

  match version_meta().unwrap().channel {
    Channel::Stable => {
      println!("cargo:rustc-env=RUSTC_CHANNEL=stable");
    }
    Channel::Beta => {
      println!("cargo:rustc-env=RUSTC_CHANNEL=beta");
    }
    Channel::Nightly => {
      println!("cargo:rustc-env=RUSTC_CHANNEL=nightly");
    }
    Channel::Dev => {
      println!("cargo:rustc-env=RUSTC_CHANNEL=dev");
    }
  }
}

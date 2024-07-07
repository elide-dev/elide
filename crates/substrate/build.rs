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

use builder::{build_bindings, cargo_lib_metadata};

fn main() {
    // link against lib jvm
    cargo_lib_metadata(None);
    println!("cargo:rustc-link-lib=jvm");

    // generate rust bindings
    let bindings = bindgen::Builder::default().header("headers/substrate.h");

    build_bindings("libjvm.rs", bindings);
}

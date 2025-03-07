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

#[cfg(feature = "entry-v2")]
fn build_libentry_bindings() {
  // generate rust bindings
  let bindings = bindgen::Builder::default()
    .header("headers/elide-entry.h")
    .header("headers/graal_isolate_dynamic.h")
    .header("headers/libentry_dynamic.h")
    .allowlist_var("NO_PROTECTION_DOMAIN")
    .allowlist_var("NEW_PROTECTION_DOMAIN")
    .allowlist_type("graal_create_isolate_params_t")
    .allowlist_type("graal_isolatethread_t")
    .allowlist_type("graal_isolate_t")
    .allowlist_type("graal_create_isolate_fn_t")
    .allowlist_type("graal_attach_thread_fn_t")
    .allowlist_type("graal_get_current_thread_fn_t")
    .allowlist_type("graal_get_isolate_fn_t")
    .allowlist_type("graal_detach_thread_fn_t")
    .allowlist_type("graal_tear_down_isolate_fn_t")
    .allowlist_type("elide_entry_init_fn_t")
    .allowlist_type("elide_entry_run_fn_t")
    .allowlist_function("graal_create_isolate")
    .allowlist_function("graal_attach_thread")
    .allowlist_function("graal_get_current_thread")
    .allowlist_function("graal_get_isolate")
    .allowlist_function("graal_detach_thread")
    .allowlist_function("graal_tear_down_isolate")
    .allowlist_function("graal_detach_all_threads_and_tear_down_isolate")
    .allowlist_function("elide_entry_init")
    .allowlist_function("elide_entry_run");

  build_bindings("libentry", "libentry.rs", bindings);

  //println!("cargo::rustc-link-lib=entry");
  //println!("cargo::rustc-link-search=native=/home/sam/workspace/elide/packages/entry/build/native/nativeSharedCompile");
}

#[cfg(feature = "entry-v1")]
fn build_libmain_bindings() {
  // generate rust bindings
  let bindings = bindgen::Builder::default()
    .header("headers/elide-entry.h")
    .header("headers/graal_isolate_dynamic.h")
    .header("headers/libelidemain_dynamic.h")
    .allowlist_var("NO_PROTECTION_DOMAIN")
    .allowlist_var("NEW_PROTECTION_DOMAIN")
    .allowlist_type("graal_create_isolate_params_t")
    .allowlist_type("graal_isolatethread_t")
    .allowlist_type("graal_isolate_t")
    .allowlist_type("graal_create_isolate_fn_t")
    .allowlist_type("graal_attach_thread_fn_t")
    .allowlist_type("graal_get_current_thread_fn_t")
    .allowlist_type("graal_get_isolate_fn_t")
    .allowlist_type("graal_detach_thread_fn_t")
    .allowlist_type("graal_tear_down_isolate_fn_t")
    .allowlist_type("elide_main_init_fn_t")
    .allowlist_type("elide_main_entry_fn_t")
    .allowlist_function("graal_create_isolate")
    .allowlist_function("graal_attach_thread")
    .allowlist_function("graal_get_current_thread")
    .allowlist_function("graal_get_isolate")
    .allowlist_function("graal_detach_thread")
    .allowlist_function("graal_tear_down_isolate")
    .allowlist_function("graal_detach_all_threads_and_tear_down_isolate")
    .allowlist_function("elide_main_init")
    .allowlist_function("elide_main_entry");

  build_bindings("libelidemain", "libelidemain.rs", bindings);

  //println!("cargo::rustc-link-lib=entry");
  //println!("cargo::rustc-link-search=native=/home/sam/workspace/elide/packages/entry/build/native/nativeSharedCompile");
}

fn main() {
  // link against lib jvm
  cargo_lib_metadata(None);

  #[cfg(feature = "entry-v1")]
  build_libmain_bindings();

  #[cfg(feature = "entry-v2")]
  build_libentry_bindings();
}

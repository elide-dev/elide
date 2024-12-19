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
#![feature(const_trait_impl)]
#![forbid(unsafe_code, dead_code)]

pub use model::{Architecture as TargetArch, OperatingSystem as TargetOs};

use bindgen::Builder;
use cc::Build;
use std::env::var;
use std::env::var_os;
use std::path::{Path, PathBuf};
use std::process::Command;

/// Internal function which runs a Makefile command, possibly against a subdirectory.
///
/// # Arguments
///
/// * `subdir` - The subdirectory to run the command in.
/// * `command_line` - The command line to run.
///
/// # Examples
///
/// ```
/// use builder::do_makefile_run;
/// do_makefile_run(None, "help");
/// ```
pub fn do_makefile_run(subdir: Option<&str>, command_line: &str) {
  let env_make = var("MAKE").unwrap_or_else(|_| "make".to_string());
  let mut command = Command::new(env_make);
  let root_project_path = var("ELIDE_ROOT").unwrap();
  command.current_dir(root_project_path.clone());
  if let Some(subpath) = subdir {
    let root_subpath = format!("{}/{}", root_project_path, subpath);
    command.arg("-C").arg(root_subpath);
  }
  command.arg(command_line);
  println!("Running make: {:?}", command);
  let status = command.status().expect("Failed to execute make");
  assert!(status.success());
}

/// Run a Makefile command in the project root.
///
/// # Arguments
///
/// * `command_line` - The command line to run.
///
/// # Examples
///
/// ```
/// use builder::makefile_run;
/// makefile_run("help");
/// ```
pub fn makefile_run(command_line: &str) {
  do_makefile_run(None, command_line);
}

/// Run a Makefile command in the project root.
///
/// # Arguments
///
/// * `subdir` - The subdirectory to run the command in.
/// * `command_line` - The command line to run.
///
/// # Examples
///
/// ```
/// use builder::makefile_sub_run;
/// makefile_sub_run("third_party", "help");
/// ```
pub fn makefile_sub_run(subdir: &str, command_line: &str) {
  do_makefile_run(Some(subdir), command_line);
}

/// Run a closure if the provided path exists.
///
/// # Arguments
///
/// * `path` - The path to check for existence.
/// * `closure` - The closure to run if the path exists.
///
/// # Examples
///
/// ```
/// use builder::{if_exists, root_project_path};
/// if_exists(root_project_path("README.md").as_str(), || {
///   println!("README.md exists!");
/// });
pub fn if_exists(path: &str, closure: impl FnOnce()) {
  if Path::new(path).exists() {
    closure();
  }
}

/// Run a closure if the provided path does not exist.
///
/// # Arguments
///
/// * `path` - The path to check for existence.
/// * `closure` - The closure to run if the path does not exist.
///
/// # Examples
///
/// ```
/// use builder::{if_not_exists, root_project_path};
/// if_not_exists(root_project_path("some-other-non-readme-file-that-does-not-exist.md").as_str(), || {
///   println!("bunk example file does not exist, as expected!");
/// });
pub fn if_not_exists(path: &str, closure: impl FnOnce()) {
  if !Path::new(path).exists() {
    closure();
  }
}

/// Run a command in the project root.
///
/// # Arguments
///
/// * `command` - The command to run.
/// * `command_line` - The command line to run.
///
/// # Examples
///
/// ```
/// use builder::root_project_run;
/// root_project_run("ls", "-la", "Listing files in the project root");
/// ```
pub fn root_project_run(command: &str, command_line: &str, message: &str) {
  let status = Command::new(command)
    .arg(command_line)
    .status()
    .expect(message);
  assert!(status.success());
}

/// Make sure the Elide codebase and build environment is set up properly.
///
/// # Arguments
///
/// * `closure` - The closure to run after setup; this is where project-specific setup tasks should be run.
pub fn setup(closure: impl FnOnce()) {
  // we need to update submodules if they cannot be found on-disk
  if_not_exists(&root_project_path("third_party/sqlite/Makefile"), || {
    root_project_run(
      "git",
      "submodule update --init --recursive",
      "Updating submodules",
    );
  });

  // we need to install node modules if they are not present
  if_not_exists(&root_project_path("node_modules"), || {
    root_project_run("pnpm", "install", "Installing Node modules");
  });

  // run project-specific setup tasks
  closure();
}

/// Make sure the Elide codebase and build environment is setup properly; this variant uses no callback.
pub fn ensure_setup() {
  setup(|| {});
}

/// Build a path to a root project resource.
///
/// # Arguments
///
/// * `path` - The path to the resource.
///
/// # Examples
///
/// ```
/// use builder::root_project_path;
/// let path = root_project_path("README.md");
/// // `path` should be: `/README.md`
/// ```
pub fn root_project_path(path: &str) -> String {
  let manifest_dir = var_os("ELIDE_ROOT").expect("No ELIDE_ROOT variable set");
  let folder_path = Path::new(&manifest_dir);
  folder_path.join(path).to_str().unwrap().to_string()
}

/// Build a path to a third-party project.
///
/// # Arguments
///
/// * `name` - The name of the project.
///
/// # Examples
///
/// ```
/// use builder::third_party_project;
/// let path = third_party_project("sqlite");
/// // `path` should be: `<path to elide's project root>/third_party/sqlite`
/// ```
pub fn third_party_project(name: &str) -> String {
  root_project_path(format!("third_party/{}", name).as_str())
}

/// Build a path to a third-party source file.
///
/// # Arguments
///
/// * `project` - The name of the project.
/// * `path` - The path to the source file.
///
/// # Examples
///
/// ```
/// use builder::third_party_src_file;
/// let path = third_party_src_file("sqlite", "sqlite3.c");
/// // `path` should be: `<path to elide's project root>/third_party/sqlite/sqlite3.c`
/// ```
pub fn third_party_src_file(project: &str, path: &str) -> String {
  root_project_path(format!("third_party/{}/{}", project, path).as_str())
}

/// Build a path to a project resource.
///
/// # Arguments
///
/// * `path` -  The path to the resource.
///
/// # Examples
///
/// ```
/// use builder::project_path;
/// let path = project_path("src/main.rs");
/// // `path` should be: `<path to elide's project root>/crates/<crate>/src/main.rs`
/// ```
pub fn project_path(path: &str) -> String {
  let manifest_dir = var_os("CARGO_MANIFEST_DIR").expect("No CARGO_MANIFEST_DIR variable set");
  let folder_path = Path::new(&manifest_dir);
  folder_path.join(path).to_str().unwrap().to_string()
}

/// Build a path to a project source file resource.
///
/// # Arguments
///
/// * `path` - The path to the source file, starting at the source root; `src/` is prepended.
///
/// # Examples
///
/// ```
/// use builder::src_file;
/// let path = src_file("main.rs");
/// // `path` should be: `crates/example/src/main.rs`
/// ```
pub fn src_file(path: &str) -> String {
  project_path(format!("src/{}", path).as_str())
}

/// Build a path to a project header file resource.
///
/// # Arguments
///
/// * `path` - The path to the header file, starting at the source root; `src/` is prepended.
///
/// # Examples
///
/// ```
/// use builder::header_file;
/// let path = header_file("example.h");
/// // `path` should be: `crates/example/headers/example.h`
/// ```
pub fn header_file(path: &str) -> String {
  project_path(format!("headers/{}", path).as_str())
}

/// Return the base include paths which should always be included in native builds.
pub fn base_include_paths() -> Vec<String> {
  // get base folders and build profile
  let gvm_home = gvm_home();
  let target_base = root_project_path("target");
  let profile = var("PROFILE").expect("No PROFILE variable set");
  let project_root = var("CARGO_MANIFEST_DIR").expect("No CARGO_MANIFEST_DIR variable set");
  let project_root_path = Path::new(&project_root);
  let project_headers = project_root_path
    .join("headers")
    .to_str()
    .unwrap()
    .to_string();
  let apr_headers = root_project_path("third_party/apache/apr/include");
  let sqlite_headers = root_project_path("third_party/sqlite/install/include");
  let boringssl_headers = root_project_path("third_party/google/boringssl/include");

  // resolve os/arch from target
  let target = var("TARGET").unwrap();
  let os = if target.contains("darwin") {
    "darwin"
  } else {
    "linux"
  };

  vec![
    format!("{}/include", gvm_home),
    format!("{}/include/{}", gvm_home, os),
    format!("{}/{}/include", target_base, profile),
    project_headers,
    apr_headers,
    sqlite_headers,
    boringssl_headers,
  ]
}

/// Resolve the path to the current Java home (`JAVA_HOME`).
pub const fn java_home() -> &'static str {
  let java_home = env!("JAVA_HOME");
  java_home
}

/// Resolve the path to the current GraalVM home (`GRAALVM_HOME`).
#[allow(clippy::if_same_then_else)]
pub const fn gvm_home() -> &'static str {
  let gvm_home_var = option_env!("GRAALVM_HOME");
  let java_home = option_env!("JAVA_HOME");

  if gvm_home_var.is_none() && java_home.is_none() {
    panic!("Please set the GRAALVM_HOME or JAVA_HOME environment variable");
  } else if let Some(gvm_home) = gvm_home_var {
    gvm_home
  } else {
    env!("JAVA_HOME")
  }
}

/// Return a full suite of include paths for native builds, including any extras.
pub fn include_paths(extras: Option<Vec<String>>) -> Vec<String> {
  let mut paths = base_include_paths();
  if let Some(addl) = extras {
    paths.extend(addl);
  }
  paths
}

/// Return the base library paths which should always be included in native builds.
pub fn base_lib_paths() -> Vec<String> {
  // get base folders and build profile
  let gvm_home = gvm_home();
  let target_base = root_project_path("target");
  let profile = var("PROFILE").unwrap();
  let sqlite_libs = root_project_path("third_party/sqlite/install/lib");

  // resolve os/arch from target
  let target = var("TARGET").unwrap();
  let os = if target.contains("darwin") {
    "darwin"
  } else {
    "linux"
  };
  let arch = if target.contains("x86_64") {
    "amd64"
  } else {
    "aarch64"
  };

  vec![
    format!("{}/lib", gvm_home),
    format!("{}/lib/server", gvm_home),
    format!("{}/lib/svm/clibraries/{}-{}", gvm_home, os, arch),
    format!("{}/{}", target_base, profile),
    format!("{}/{}/lib", target_base, profile),
    sqlite_libs,
  ]
}

/// Return a full suite of library paths for native builds, including any extras.
pub fn lib_paths(extras: Option<Vec<String>>) -> Vec<String> {
  let mut paths = base_lib_paths();
  if let Some(addl) = extras {
    paths.extend(addl);
  }
  paths
}

/// Emit Cargo metadata for all built-in link paths, plus `extra_lib_paths`; the resulting `Vec<String>` contains the
/// sum of all library search paths.
pub fn cargo_lib_metadata(extra_lib_paths: Option<Vec<String>>) -> Vec<String> {
  let paths = lib_paths(extra_lib_paths);
  for path in paths.iter() {
    println!("cargo:rustc-link-search=native={}", path);
  }
  paths
}

/// Obtain a consistent value identifying the target operating system.
pub fn target_os() -> TargetOs {
  TargetOs::current()
}

/// Obtain a consistent value identifying the target CPU architecture.
pub fn target_arch() -> TargetArch {
  TargetArch::current()
}

/// Common C flags applied to all builds which use this builder interface.
const common_c_flags: [&str; 10] = [
  "-O3",
  "-fPIC",
  "-fPIE",
  "-fno-omit-frame-pointer",
  "-fstack-protector-strong",
  "-fstack-clash-protection",
  "-fno-delete-null-pointer-checks",
  "-fno-strict-overflow",
  "-fno-strict-aliasing",
  "-fexceptions",
];

/// Common ASM flags applied to all builds which use this builder interface.
const common_asm_flags: [&str; 1] = ["--noexecstack"];

/// Setup a consistent C compiler build environment.
pub fn setup_cc() -> Build {
  let profile = var("PROFILE").unwrap();
  let os = target_os();
  let arch = target_arch();
  let mut build = Build::new();

  build
    // Defines & Compiler Settings
    .pic(true)
    .static_flag(true)
    .shared_flag(true)
    .use_plt(false);

  build
    // Global Defines
    .define("ELIDE", "1")
    .define("ELIDE_GVM_STATIC", "1")
    .define("HAVE_OPENSSL", "1")
    .define("HAVE_USLEEP", "1");

  build
    // Include Paths
    .includes(include_paths(None).iter().map(|s| s.as_str()));

  build
    // General Hardening
    .flag_if_supported("-fhardened")
    .flag_if_supported("-fstrict-flex-arrays=3")
    .flag_if_supported("-fno-delete-null-pointer-checks")
    .flag_if_supported("-fno-strict-overflow")
    .flag_if_supported("-fno-strict-aliasing");

  // add cflags
  for flag in common_c_flags.iter() {
    build.flag(flag);
  }

  // add asm flags
  for flag in common_asm_flags.iter() {
    build.asm_flag(flag);
  }

  // add profile-specific flags
  match profile.as_str() {
    "debug" => build
      // Debug-only Flags
      .define("ELIDE_DEBUG", "1")
      .flag("-g"),
    "release" => build
      // Release-only Flags
      .define("ELIDE_RELEASE", "1")
      .flag("-flto"),

    _ => &mut build,
  };

  // add os-specific flags
  match os {
    TargetOs::Darwin => match arch {
      TargetArch::Amd64 => build
        // C Flags: macOS
        .flag("-mmacosx-version-min=12.3"),

      TargetArch::Arm64 => build
        // C Flags: macOS
        .flag("-mmacosx-version-min=12.3")
        .flag("-march=armv8-a+crypto+crc+simd")
        .flag("-mbranch-protection=standard")
        .define("__ARM_NEON", "1")
        .define("__ARM_FEATURE_AES", "1")
        .define("__ARM_FEATURE_SHA2", "1"),
    },

    TargetOs::Linux => &mut build,

    TargetOs::Windows => &mut build,
  };

  // always generate cargo metadata
  cargo_lib_metadata(None);
  build
}

/// Execute a prepared `Build` environment, creating both a static library and shared library; for this function to work
/// properly, `cdylib` must be removed from the crate's own targets.
pub fn build_dual_cc(
  mut build: Build,
  static_name: &str,
  shared_lib_name: &str,
  extra_static_cflags: Option<Vec<&str>>,
  extra_shared_cflags: Option<Vec<&str>>,
) {
  let os = target_os();
  let link_paths = base_lib_paths();

  // add link paths to both builds
  for path in link_paths {
    build.flag(format!("-L{}", path));
  }

  let mut static_lib = build.clone();
  let mut shared_lib = build.clone();

  static_lib
    // Sources: Native DB
    .static_flag(true)
    .shared_flag(false);

  for flag in extra_static_cflags.unwrap_or_default() {
    static_lib.flag(flag);
  }

  // manually force compilation of a shared object as well
  let objects = shared_lib.compile_intermediates();

  // compile static lib
  static_lib.compile(static_name);

  let profile = var("PROFILE").unwrap();
  let elide_root_var = var("ELIDE_ROOT").unwrap();
  let target_dir = format!("{}/target/{}", elide_root_var, profile);
  let out_dir = Path::new(&target_dir);
  let lib_tail = match os {
    TargetOs::Darwin => "dylib",
    TargetOs::Linux => "so",
    TargetOs::Windows => "dll",
  };

  let outpath = out_dir.join(format!("lib{}.{}", shared_lib_name, lib_tail));

  shared_lib.static_flag(false).shared_flag(true);

  // add extra shared c_cflags, if any
  for flag in extra_shared_cflags.unwrap_or_default() {
    shared_lib.flag(flag);
  }

  match shared_lib
    .get_compiler()
    .to_command()
    .arg("-o")
    .arg(outpath)
    .args(&objects)
    .status()
  {
    Ok(_) => {
      // nothing to do
    }

    Err(e) => {
      // crash
      panic!(
        "Failed to compile shared library {}; error: {}",
        shared_lib_name, e
      );
    }
  }
}

/// Execute a consistent C bind-gen environment.
pub fn build_bindings(lib_name: &str, gen_name: &str, builder: Builder) {
  // formulate include paths
  println!("Building bindings for {}", lib_name);
  let gvm_home = gvm_home();
  let os = target_os();
  let jvm_include = format!("{}/include", gvm_home);
  let jvm_include_native = format!("{}/include/{}", gvm_home, os.as_str());
  let profile = var("PROFILE").unwrap();
  let target_headers_gen = root_project_path(format!("target/{}/include", profile).as_str());
  let target_headers_profile = root_project_path(format!("target/{}/include", profile).as_str());
  let target_headers_apr = root_project_path("third_party/apache/apr/include");
  let project_headers = project_path("headers");

  let bindings = builder
    // Generate Bindings
    .clang_arg(format!("-I{}", jvm_include))
    .clang_arg(format!("-I{}", jvm_include_native))
    .clang_arg(format!("-I{}", project_headers))
    .clang_arg(format!("-I{}", target_headers_gen))
    .clang_arg(format!("-I{}", target_headers_profile))
    .clang_arg(format!("-I{}", target_headers_apr))
    .parse_callbacks(Box::new(bindgen::CargoCallbacks::new()))
    .generate()
    .expect("Unable to generate bindings");

  let out_path = PathBuf::from(var("OUT_DIR").unwrap());

  bindings
    .write_to_file(out_path.join(gen_name))
    .expect("Couldn't write bindings!");
}

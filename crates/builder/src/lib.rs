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
#![forbid(unsafe_code, dead_code)]

use bindgen::Builder;
use cc::Build;
use std::env;
use std::env::var;
use std::env::var_os;
use std::path::{Path, PathBuf};

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
    let manifest_dir = var_os("PWD").unwrap();
    let folder_path = Path::new(&manifest_dir);
    folder_path.join(path).to_str().unwrap().to_string()
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
/// // `path` should be: `crates/example/src/main.rs`
/// ```
pub fn project_path(path: &str) -> String {
    let manifest_dir = var_os("CARGO_MANIFEST_DIR").unwrap();
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
    let profile = var("PROFILE").unwrap();
    let project_root = var("CARGO_MANIFEST_DIR").unwrap();
    let project_root_path = Path::new(&project_root);
    let project_root_headers = project_root_path
        .join("headers")
        .to_str()
        .unwrap()
        .to_string();

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
        format!("{}/{}/include/apr-2", target_base, profile),
        project_root_headers,
    ]
}

/// Resolve the path to the current Java home (`JAVA_HOME`).
pub fn java_home() -> String {
    let java_home = var("JAVA_HOME");
    if java_home.is_err() {
        panic!("Please set the JAVA_HOME environment variable");
    }
    java_home.unwrap()
}

/// Resolve the path to the current GraalVM home (`GRAALVM_HOME`).
pub fn gvm_home() -> String {
    let gvm_home_var = var("GRAALVM_HOME");
    let java_home = var("JAVA_HOME");

    if gvm_home_var.is_err() && java_home.is_err() {
        panic!("Please set the GRAALVM_HOME or JAVA_HOME environment variable");
    }

    let gvm_home_val = gvm_home_var.unwrap_or_default();
    let java_home = java_home.unwrap_or_default();
    if gvm_home_val.is_empty() {
        java_home
    } else {
        gvm_home_val
    }
}

/// Return a full suite of include paths for native builds, including any extras.
pub fn include_paths(extras: Option<Vec<String>>) -> Vec<String> {
    let mut paths = base_include_paths();
    if extras.is_some() {
        paths.extend(extras.unwrap());
    }
    paths
}

/// Return the base library paths which should always be included in native builds.
pub fn base_lib_paths() -> Vec<String> {
    // get base folders and build profile
    let gvm_home = gvm_home();
    let target_base = root_project_path("target");
    let profile = var("PROFILE").unwrap();

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
    ]
}

/// Return a full suite of library paths for native builds, including any extras.
pub fn lib_paths(extras: Option<Vec<String>>) -> Vec<String> {
    let mut paths = base_lib_paths();
    if extras.is_some() {
        paths.extend(extras.unwrap());
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

/// Enumerates supported operating systems.
pub enum TargetOs {
    Darwin,
    Linux,
    Windows,
}

impl TargetOs {
    pub fn as_str(&self) -> &'static str {
        match self {
            TargetOs::Darwin => "darwin",
            TargetOs::Linux => "linux",
            TargetOs::Windows => "windows",
        }
    }
}

/// Enumerates supported target architectures.
pub enum TargetArch {
    Amd64,
    Arm64, // alias for aarch64 on applicable platforms
}

impl TargetArch {
    pub fn as_str(&self) -> &'static str {
        match self {
            TargetArch::Amd64 => "amd64",
            TargetArch::Arm64 => "aarch64",
        }
    }
}

/// Obtain a consistent value identifying the target operating system.
pub fn target_os() -> TargetOs {
    let target = env::var("TARGET").unwrap();
    if target.contains("darwin") {
        TargetOs::Darwin
    } else if target.contains("linux") {
        TargetOs::Linux
    } else if target.contains("windows") {
        TargetOs::Windows
    } else {
        panic!("Unsupported target: {}", target);
    }
}

/// Obtain a consistent value identifying the target CPU architecture.
pub fn target_arch() -> TargetArch {
    let target = env::var("TARGET").unwrap();
    if target.contains("x86_64") {
        TargetArch::Amd64
    } else if target.contains("aarch64") {
        TargetArch::Arm64
    } else if target.contains("arm64") {
        TargetArch::Arm64
    } else {
        panic!("Unsupported target: {}", target);
    }
}

/// Common C flags applied to all builds which use this builder interface.
const common_c_flags: [&str; 8] = [
    "-O3",
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
        .std("c11")
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

    // add base includes
    build.includes(include_paths(None).iter().map(|s| s.as_str()));

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
            .flag("-flto=thin"),

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

    // always link against jvm for jni
    println!("cargo:rustc-link-lib=jvm");
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
    let shared_flag = match os {
        TargetOs::Darwin => "-dynamiclib",
        TargetOs::Linux => "-shared",
        TargetOs::Windows => "-shared",
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
        .args([shared_flag, "-o"])
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
pub fn build_bindings(lib_name: &str, builder: Builder) {
    // formulate include paths
    let gvm_home = gvm_home();
    let os = target_os();
    let jvm_include = format!("{}/include", gvm_home);
    let jvm_include_native = format!("{}/include/{}", gvm_home, os.as_str());
    let project_headers = project_path("headers");

    let bindings = builder
        // Generate Bindings
        .clang_arg(format!("-I{}", jvm_include))
        .clang_arg(format!("-I{}", jvm_include_native))
        .clang_arg(format!("-I{}", project_headers))
        .parse_callbacks(Box::new(bindgen::CargoCallbacks::new()))
        .generate()
        .expect("Unable to generate bindings");

    let out_path = PathBuf::from(var("OUT_DIR").unwrap());

    bindings
        .write_to_file(out_path.join(lib_name))
        .expect("Couldn't write bindings!");
}

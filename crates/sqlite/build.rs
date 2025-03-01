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

use bindgen::Builder;
use builder::{
  build_bindings, build_dual_cc, header_file, if_not_exists, makefile_sub_run, setup, setup_cc,
  src_file, third_party_project, third_party_src_file,
};

fn main() {
  let profile = std::env::var("PROFILE").expect("No profile variable set");
  let profile_val = profile.as_str();
  let cmd_args = match profile_val {
    "release" => "RELEASE=yes",
    _ => "RELEASE=no",
  };

  setup(|| {
    // we need to build the sqlite amalgamation if it is not present
    if_not_exists(third_party_src_file("sqlite", "sqlite3.c").as_str(), || {
      makefile_sub_run("third_party", format!("sqlite {}", cmd_args).as_str());
    });
  });

  let mut build = setup_cc();

  let sqlite_path = third_party_project("sqlite/install");
  let sqlite_include = format!("-I{}/include", sqlite_path);
  let include_binding = sqlite_include.clone();
  let extra_args = vec![include_binding.as_str()];

  build
    // Build Hardening & Warning Suppression
    .flag("-w")
    .flag("-fPIC")
    .flag("-fstack-protector-strong")
    .flag("-fstack-clash-protection")
    .flag("-Wl,-z,relro,-z,now")
    .flag("-Wl,-z,noexecstack")
    .flag("-Wl,-z,separate-code")
    .flag("-Wa,--noexecstack");

  build.flag(sqlite_include.clone());

  build
    // Defines & Compiler Settings
    .define("SQLITE_GVM_STATIC", "1")
    .define("SQLITE_CORE", "1")
    .define("SQLITE_DEFAULT_FILE_PERMISSIONS", "0666")
    .define("SQLITE_DEFAULT_MEMSTATUS", "0")
    .define("SQLITE_DISABLE_PAGECACHE_OVERFLOW_STATS", "1")
    .define("SQLITE_ENABLE_API_ARMOR", "1")
    .define("SQLITE_ENABLE_COLUMN_METADATA", "1")
    .define("SQLITE_ENABLE_DBSTAT_VTAB", "1")
    .define("SQLITE_ENABLE_FTS3", "1")
    .define("SQLITE_ENABLE_FTS3_PARENTHESIS", "1")
    .define("SQLITE_ENABLE_FTS5", "1")
    .define("SQLITE_ENABLE_LOAD_EXTENSION", "1")
    .define("SQLITE_ENABLE_MATH_FUNCTIONS", "0")
    .define("SQLITE_ENABLE_RTREE", "1")
    .define("SQLITE_ENABLE_STAT4", "1")
    .define("SQLITE_HAVE_ISNAN", "1")
    .define("SQLITE_MAX_ATTACHED", "125")
    .define("SQLITE_MAX_COLUMN", "32767")
    .define("SQLITE_MAX_FUNCTION_ARG", "127")
    .define("SQLITE_MAX_LENGTH", "2147483647")
    .define("SQLITE_MAX_MMAP_SIZE", "1099511627776")
    .define("SQLITE_MAX_PAGE_COUNT", "4294967294")
    .define("SQLITE_MAX_SQL_LENGTH", "1073741824")
    .define("SQLITE_MAX_VARIABLE_NUMBER", "250000")
    .define("SQLITE_THREADSAFE", "1");

  build
    // Source Files
    .file(src_file("NativeDB.c"))
    .file(third_party_src_file("sqlite", "sqlite3.c"));

  build_dual_cc(
    build,
    "sqlitejdbccore",
    "sqlitejdbc",
    Some(extra_args.clone()),
    Some(extra_args.clone()),
    None,
  );

  build_bindings(
    "sqlite",
    "libsqlitejdbc.rs",
    Builder::default()
      .clang_arg(sqlite_include.clone())
      .header(header_file("NativeDB.h")),
  );
}

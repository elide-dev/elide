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
#![feature(exit_status_error)]

use bindgen::Builder;
use builder::{build_bindings, build_dual_cc, header_file, setup_cc, src_file};

fn main() {
  let mut build = setup_cc();

  // link against `libjvm` and backing `sqlite3`, which should be in the lib path
  println!("cargo:rustc-link-lib=static=sqlite3");

  build
          // Warnings
          .flag("-Werror")
          .flag("-Wno-unused-variable")
          .flag("-Wno-unused-parameter")
          .flag("-Wno-unused-command-line-argument")
          .flag("-Wno-deprecated-declarations")
          .flag("-Wno-implicit-function-declaration");

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
          .define("SQLITE_ENABLE_MATH_FUNCTIONS", "1")
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
          .file(src_file("NativeDB.c"));

  let extra_cflags = vec![
    "-lsqlite3",
  ];

  build_dual_cc(
    build,
    "sqlitejdbccore",
    "sqlitejdbc",
    None,
    Some(extra_cflags));

  build_bindings(
    "libsqlitejdbc.rs",
    Builder::default()
          .header(header_file("NativeDB.h")))
}

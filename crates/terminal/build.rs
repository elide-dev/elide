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
  build_bindings,
  build_dual_cc,
  header_file,
  setup_cc,
  src_file,
  target_os,
  TargetOs,
};

fn main() {
  // decide if we are on darwin
  let os = target_os();
  let mut build = setup_cc();

  build
    // Warnings
    .flag("-Werror")
    .flag("-Wno-unused-parameter")
    .flag("-Wno-unused-command-line-argument")
    .flag("-Wno-sign-compare")
    // Sources: Jansi & JLine
    .file(src_file("clibrary.c"))
    .file(src_file("jansi.c"))
    .file(src_file("jansi_isatty.c"))
    .file(src_file("jansi_structs.c"))
    .file(src_file("jansi_ttyname.c"))
    .file(src_file("jlinenative.c"))
    .file(src_file("terminaljni.c"));

  let bindings_builder: Builder = match os {
    TargetOs::Darwin => Builder::default()
      .header(header_file("jansi.h"))
      .header(header_file("jansi_structs.h"))
      .header(header_file("jlinenative.h")),

    TargetOs::Linux => Builder::default()
      .header(header_file("jansi.h"))
      .header(header_file("jansi_structs.h"))
      .header(header_file("jlinenative.h")),

    TargetOs::Windows => {
      build
        // Windows C Sources
        .file(src_file("kernel32.c"));

      Builder::default()
        .header(header_file("jansi.h"))
        .header(header_file("jansi_structs.h"))
        .header(header_file("jlinenative.h"))
    }
  };

  build_dual_cc(build, "terminalcore", "terminal", None, None);
  build_bindings("terminal", "libterminal.rs", bindings_builder);
}

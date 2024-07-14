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
  let os = target_os();
  let mut build = setup_cc();

  build
    // Compiler Settings
    .std("gnu99")
    .flag("-w");

  build
    // Defines & Compiler Settings
    .define("TCN_BUILD_STATIC", "1")
    .define("NETTY_BUILD_STATIC", "1")
    .define("NETTY_BUILD_GRAALVM", "1")
    .define("NETTY_GVM_STATIC", "1");

  let bindings_builder: Builder = match os {
    TargetOs::Darwin => {
      build
        // C Flags: macOS
        .flag("-mmacosx-version-min=12.3");

      build
        // Sources: Netty KQueue
        .file(src_file("netty_kqueue_bsdsocket.c"))
        .file(src_file("netty_kqueue_eventarray.c"))
        .file(src_file("netty_kqueue_native.c"));

      Builder::default()
        .header(header_file("netty_jni_util.h"))
        .header(header_file("netty_kqueue_bsdsocket.h"))
        .header(header_file("netty_kqueue_eventarray.h"))
        .header(header_file("ssl.h"))
    }

    TargetOs::Linux => {
      build
        // Sources: Netty EPoll
        .file(src_file("netty_epoll_linuxsocket.c"))
        .file(src_file("netty_epoll_native.c"));

      build
        // Sources: Netty IOUring
        .file(src_file("netty_io_uring_linuxsocket.c"))
        .file(src_file("netty_io_uring_native.c"))
        .file(src_file("syscall.c"));

      Builder::default().header("headers/netty_jni_util.h")
    }

    TargetOs::Windows => Builder::default().header("headers/netty_jni_util.h"),
  };

  build
    // Sources: Tomcat Native / APR
    .file(src_file("bb.c"))
    .file(src_file("cert_compress.c"))
    .file(src_file("error.c"))
    .file(src_file("jnilib.c"))
    .file(src_file("native_constants.c"))
    .file(src_file("ssl.c"))
    .file(src_file("sslcontext.c"))
    .file(src_file("sslsession.c"))
    .file(src_file("sslutils.c"));

  build
    // Sources: Netty JNI / Unix Commons
    .file(src_file("netty_jni_util.c"))
    .file(src_file("netty_unix.c"))
    .file(src_file("netty_unix_buffer.c"))
    .file(src_file("netty_unix_errors.c"))
    .file(src_file("netty_unix_filedescriptor.c"))
    .file(src_file("netty_unix_limits.c"))
    .file(src_file("netty_unix_socket.c"))
    .file(src_file("netty_unix_util.c"));

  let shared_cflags = vec!["-lssl", "-lcrypto", "-lapr-2", "-lz", "-lc++"];

  build_dual_cc(
    build,
    "transportcore",
    "transport",
    None,
    Some(shared_cflags),
  );

  build_bindings("transport", "libtransport.rs", bindings_builder);
}

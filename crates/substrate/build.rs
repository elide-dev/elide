use std::env;
use std::path::PathBuf;

fn main() {
  // resolve java home / gvm home variables
  let gvm_home_var = env::var("GRAALVM_HOME");
  let java_home = env::var("JAVA_HOME");

  // resolve java home as gvm home if needed
  if gvm_home_var.is_err() && java_home.is_err() {
    panic!("Please set the GRAALVM_HOME or JAVA_HOME environment variable");
  }
  let gvm_home_val = gvm_home_var.unwrap_or_default();
  let java_home = java_home.unwrap_or_default();
  let gvm_home = if gvm_home_val.is_empty() { java_home } else { gvm_home_val };

  // decide if we are on darwin
  let target = env::var("TARGET").unwrap();
  let os = if target.contains("darwin") { "darwin" } else { "linux" };
  let arch = if target.contains("x86_64") { "amd64" } else { "aarch64" };

  // formulate lib paths
  let lib_path = format!("{}/lib", gvm_home);
  let lib_path_server = format!("{}/lib/server", gvm_home);
  let lib_path_native = format!("{}/lib/svm/clibraries/{}-{}", gvm_home, os, arch);

  // add lib paths
  println!("cargo:rustc-link-search={}", lib_path);
  println!("cargo:rustc-link-search={}", lib_path_server);
  println!("cargo:rustc-link-search={}", lib_path_native);

  // link against `libjvm`
  println!("cargo:rustc-link-lib=jvm");

  // generate rust bindings
  let bindings = bindgen::Builder::default()
          .header("headers/substrate.h")
          .parse_callbacks(Box::new(bindgen::CargoCallbacks::new()))
          .generate()
          .expect("Unable to generate bindings");

  let out_path = PathBuf::from(env::var("OUT_DIR").unwrap());

  bindings
          .write_to_file(out_path.join("libjvm.rs"))
          .expect("Couldn't write bindings!");
}

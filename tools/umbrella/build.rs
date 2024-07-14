use builder::{target_os, TargetOs};

fn main() {
  println!("cargo::rustc-link-lib=static=sqlite3");
  println!("cargo::rustc-link-lib=dylib=sqlite3");

  match target_os() {
    TargetOs::Linux => {
      println!("cargo:rustc-link-lib=dylib=stdc++");
      println!("cargo:rustc-link-lib=dylib=ssl");
      println!("cargo:rustc-link-lib=dylib=crypto");
      println!("cargo:rustc-link-lib=static=ssl");
      println!("cargo:rustc-link-lib=static=crypto");
    }
    TargetOs::Darwin => {
      println!("cargo:rustc-link-lib=dylib=c++");
    }
    _ => {}
  }
}

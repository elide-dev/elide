use builder::{target_os, TargetOs};

fn main() {
  println!("cargo::rustc-link-lib=static=sqlite3");
  println!("cargo::rustc-link-lib=dylib=sqlite3");

  match target_os() {
    TargetOs::Linux => {
      println!("cargo:rustc-link-lib=dylib=stdc++");
    }
    TargetOs::Darwin => {
      println!("cargo:rustc-link-lib=dylib=c++");
    }
    _ => {}
  }
}

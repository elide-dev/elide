use builder::{TargetOs, target_os};

fn main() {
  match target_os() {
    TargetOs::Linux => {
      println!("cargo:rustc-link-lib=dylib=stdc++");
      println!("cargo:rustc-link-lib=dylib=ssl");
      println!("cargo:rustc-link-lib=dylib=z");
      println!("cargo:rustc-link-lib=dylib=crypto");
    }
    TargetOs::Darwin => {
      println!("cargo:rustc-link-lib=dylib=c++");
    }
    _ => {}
  }
}

use builder::{TargetOs, target_os};

fn main() {
  match target_os() {
    TargetOs::Linux => {
      println!("cargo:rustc-link-lib=dylib=stdc++");
      println!("cargo:rustc-link-lib=static=ssl");
      println!("cargo:rustc-link-lib=static=crypto");
    }
    TargetOs::Darwin => {
      println!("cargo:rustc-link-lib=dylib=c++");
    }
    _ => {}
  }
}

use builder::{OperatingSystem, target_os};

fn main() {
  match target_os() {
    OperatingSystem::Linux => {
      println!("cargo:rustc-link-lib=dylib=stdc++");
      println!("cargo:rustc-link-lib=dylib=ssl");
      println!("cargo:rustc-link-lib=dylib=z");
      println!("cargo:rustc-link-lib=dylib=crypto");
    }
    OperatingSystem::Darwin => {
      println!("cargo:rustc-link-lib=dylib=c++");
    }
    _ => {}
  }
}

use builder::{OperatingSystem, target_os};

fn main() {
  match target_os() {
    OperatingSystem::Linux => {
      println!("cargo:rustc-link-lib=dylib=gomp");
      println!("cargo:rustc-link-lib=dylib=m");
      println!("cargo:rustc-link-lib=dylib=z");
    }
    OperatingSystem::Darwin => {
      println!("cargo:rustc-link-lib=dylib=c++");
      println!("cargo:rustc-link-lib=dylib=m");
      println!("cargo:rustc-link-lib=dylib=z");
    }
    _ => {}
  }
}

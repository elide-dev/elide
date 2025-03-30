fn main() {
  println!("cargo:rustc-link-lib=dylib=gomp");
  println!("cargo:rustc-link-lib=dylib=m");
  println!("cargo:rustc-link-lib=dylib=z");
}

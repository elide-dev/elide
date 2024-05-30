#![allow(non_snake_case)]
use jni::JNIEnv;
use jni::objects::{JClass, JString};

enum Language {
  V4,
  V6,
}

#[no_mangle]
pub extern "system" fn Java_dev_elide_cli_bridge_CliNativeBridge_hello<'local>(mut env: JNIEnv, class: JClass) {
  println!("Hello from the Rust world!");
}

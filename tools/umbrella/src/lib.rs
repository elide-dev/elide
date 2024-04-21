use std::io::{self, Write};

use j4rs::{InvocationArg, JavaClass};
use j4rs::prelude::*;
use j4rs_derive::*;
use serde::{Deserialize, Serialize};

enum Language {
  V4,
  V6,
}

#[call_from_java("dev.elide.cli.bridge.CliNativeBridge.hello")]
fn hello_impl() {
  println!("Hello from the Rust world!");
}

#[call_from_java("dev.elide.cli.bridge.CliNativeBridge.lint")]
fn run_lint(language: Language) {
  println!("Hello from the Rust world!");
}

//
// #[no_mangle]
// pub extern "system" fn Java_HelloWorld_hello<'local>(mut env: JNIEnv<'local>,
//                                                      _class: JClass<'local>,
//                                                      input: JString<'local>)
//                                                      -> jstring {
//   let input: String =
//           env.get_string(&input).expect("Couldn't get java string!").into();
//   let output = env.new_string(format!("Hello, {}!", input))
//           .expect("Couldn't create java string!");
//   output.into_raw()
// }

pub fn say_hello() {
  println!("Hello world!");
  io::stdout().flush().unwrap();
}

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
fn run_lint(vm: Instance) {
  println!("Hello from the Rust world!");
}

pub fn say_hello() {
  println!("Hello world!");
  io::stdout().flush().unwrap();
}

/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */
use jni::{InitArgsBuilder, JNIVersion, JavaVM};
use lazy_static::lazy_static;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{Arc, Mutex};

lazy_static! {
  static ref ACTIVE_JVM: Arc<Mutex<Option<NativeJVM>>> = Arc::new(Mutex::new(None));
}

// Atomic flips when a JVM is initialized.
static JVM_INITIALIZED: AtomicBool = AtomicBool::new(false);

/// Native Java Virtual Machine
///
/// Manages state with regard to a running in-process JVM.
#[derive(Clone, Debug)]
#[allow(dead_code)]
pub struct NativeJVM {
  vm: JavaVM,
}

impl NativeJVM {
  #[allow(dead_code)]
  fn java_vm(&self) -> &JavaVM {
    &self.vm
  }
}

/// Create JVM
///
/// Prepares and launches a native JVM which can then be obtained and manipulated through module-level functions.
fn create_jvm(args: Vec<String>) -> NativeJVM {
  let mut jvm_args_builder = InitArgsBuilder::new().version(JNIVersion::V21);

  for arg in args.iter() {
    let cstr = arg.as_str();
    jvm_args_builder = jvm_args_builder.option(cstr);
  }

  let finalized_args = jvm_args_builder
    .build()
    .expect("failed to build jvm init args");

  // Create a new VM
  NativeJVM {
    vm: JavaVM::new(finalized_args).expect("failed to create jvm instance"),
  }
}

/// Create or use JVM
///
/// Initializes a JVM if none is running, using the provided `args`, and then returns a reference to the JVM instance.
pub fn create_or_use_jvm(args: Vec<String>) -> NativeJVM {
  let mut engine = ACTIVE_JVM.lock().unwrap();
  if engine.is_none() {
    let runtime = create_jvm(args);
    *engine = Some(runtime);
    JVM_INITIALIZED.store(true, Ordering::SeqCst);
  }
  let mut jvm = ACTIVE_JVM.lock().unwrap();
  if let Some(runtime) = jvm.take() {
    runtime
  } else {
    panic!("JVM was not initialized, but is expected to be.");
  }
}

/// Obtain JVM
///
/// Obtain a reference to the running JVM instance, if any.
pub fn obtain_jvm() -> Option<NativeJVM> {
  match ACTIVE_JVM.lock() {
    Ok(engine) => engine.clone(),
    Err(_) => None, // If the lock is poisoned, return None
  }
}

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

#![forbid(unsafe_op_in_unsafe_fn, unused_unsafe)]

use java_native::{jni, on_load, on_unload};
use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::{JNI_VERSION_21, jint};
use lazy_static::lazy_static;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{Arc, Mutex};
use std::time::Duration;
use tokio::runtime::{Builder, Runtime};

lazy_static! {
  static ref ASYNC_ENGINE: Arc<Mutex<Option<Runtime>>> = Arc::new(Mutex::new(None));
}

// Whether to emit simple debug logs.
const ENGINE_DEBUG_LOG: bool = true;

// Atomic flips when engine is initialized.
static ENGINE_INITIALIZED: AtomicBool = AtomicBool::new(false);

fn debug_log(msg: &str) {
  if ENGINE_DEBUG_LOG {
    eprintln!("[native:engine] {}", msg);
  }
}

/// Initialize the shared async engine.
fn init_engine() {
  if ENGINE_INITIALIZED.load(Ordering::SeqCst) {
    return;
  }
  debug_log("initializing engine");

  let mut engine = ASYNC_ENGINE.lock().unwrap();
  if engine.is_none() {
    let runtime = Builder::new_multi_thread().enable_all().build().unwrap();
    *engine = Some(runtime);
    ENGINE_INITIALIZED.store(true, Ordering::SeqCst);
  }
}

/// Gracefully shutdown the async engine.
fn shutdown_engine_graceful() {
  if !ENGINE_INITIALIZED.load(Ordering::SeqCst) {
    return;
  }

  let mut engine = ASYNC_ENGINE.lock().unwrap();
  if let Some(runtime) = engine.take() {
    runtime.shutdown_timeout(Duration::from_millis(100));
    ENGINE_INITIALIZED.store(false, Ordering::SeqCst);
  }
}

/// Bind methods and perform other lib-init tasks.
#[on_load]
pub fn did_load(_env: JNIEnv) -> jint {
  JNI_VERSION_21
}

/// Perform cleanup.
#[on_unload]
pub fn did_unload(_env: JNIEnv) {
  shutdown_engine_graceful();
}

/// Initialize the native execution layer.
#[jni("elide.exec.Execution")]
pub fn initialize<'a>(_env: JNIEnv<'a>, _class: JClass<'a>) -> jint {
  // deferred until first use
  0
}

/// Shutdown the native execution layer.
#[jni("elide.exec.Execution")]
pub fn shutdown<'a>(_env: JNIEnv<'a>, _class: JClass<'a>) -> jint {
  shutdown_engine_graceful();
  0
}

/// Return a reference to the current async engine.
pub fn async_engine() -> Runtime {
  init_engine();
  let mut engine = ASYNC_ENGINE.lock().unwrap();
  if let Some(runtime) = engine.take() {
    return runtime;
  }
  panic!("elide's async engine has not initialized");
}

/// Return a reference to the current async engine.
pub fn async_engine_safe() -> Option<Runtime> {
  init_engine();
  let mut engine = ASYNC_ENGINE.lock().unwrap();
  engine.take()
}

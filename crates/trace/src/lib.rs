/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
use crate::ringbuf::{register_handlers, shutdown_consumer_thread, start_consumer_thread};
use java_native::jni;
use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jint;
use log::{Level, debug, error, info, trace, warn};

/// Import preludes for easy use of `trace` and `tracing`.
pub mod prelude;

/// In-memory channel and buffer implementation for JVM/JNI tracing.
pub mod channel;

/// Ring buffer used for trace and log delivery to the JVM.
mod ringbuf;

/// Determine a known log level for the provided name.
pub fn level_for_name(name: &str) -> Option<Level> {
  match name {
    "TRACE" => Some(Level::Trace),
    "DEBUG" => Some(Level::Debug),
    "INFO" => Some(Level::Info),
    "WARN" => Some(Level::Warn),
    "ERROR" => Some(Level::Error),
    _ => None,
  }
}

/// Determine a consistent string name for the provided log level.
pub fn name_for_level(level: Level) -> &'static str {
  match level {
    Level::Trace => "TRACE",
    Level::Debug => "DEBUG",
    Level::Info => "INFO",
    Level::Warn => "WARN",
    Level::Error => "ERROR",
  }
}

/// Initialize the native tracing layer.
#[jni("elide.exec.Tracing")]
pub fn initialize<'a>(_env: JNIEnv<'a>, _class: JClass<'a>) -> jint {
  start_consumer_thread();
  register_handlers();
  0
}

/// Trigger a flush of events held by the native tracing/log layer.
#[jni("elide.exec.Tracing")]
pub fn flush<'a>(_env: JNIEnv<'a>, _class: JClass<'a>) -> jint {
  ringbuf::flush();
  0
}

/// Trigger delivery of a native log message back to the JVM via the event buffer.
#[jni("elide.exec.Tracing")]
pub fn nativeLog<'a>(
  mut env: JNIEnv<'a>,
  _class: JClass<'a>,
  level: JString<'a>,
  msg: JString<'a>,
) -> jint {
  let levelStr: String = match env.get_string(&level) {
    Ok(str) => str.to_str().to_string(),
    Err(_) => return 1,
  };
  let msgStr: String = match env.get_string(&msg) {
    Ok(str) => str.to_str().to_string(),
    Err(_) => return 1,
  };
  let knownLevel = level_for_name(levelStr.as_str());
  let level = match knownLevel {
    Some(level) => level,
    None => return 2,
  };
  match level {
    Level::Info => info!("[native:info] {}", msgStr),
    Level::Debug => debug!("[native:debug] {}", msgStr),
    Level::Trace => trace!("[native:trace] {}", msgStr),
    Level::Warn => warn!("[native:warn] {}", msgStr),
    Level::Error => error!("[native:error] {}", msgStr),
  }
  0
}

/// Trigger delivery of a native trace message back to the JVM via the event buffer.
#[jni("elide.exec.Tracing")]
pub fn nativeTrace<'a>(
  _env: JNIEnv<'a>,
  _class: JClass<'a>,
  _level: JString<'a>,
  _msg: JString<'a>,
) -> jint {
  // @TODO
  0
}

/// Shutdown the native tracing layer.
#[jni("elide.exec.Tracing")]
pub fn shutdown<'a>(_env: JNIEnv<'a>, _class: JClass<'a>) {
  shutdown_consumer_thread();
}

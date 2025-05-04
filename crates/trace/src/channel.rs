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

use crate::ringbuf::{deliver_log, deliver_trace};
use jni::objects::JValue;
use log::Log;
use substrate::vm::active_java_vm;
use tracing::span::{Attributes, Record};
use tracing::{Event, Id, Metadata, Subscriber};

const TRACE_UTILS_CLASS: &str = "elide/exec/TraceNative";
const LOOKUP_LOGGER_METHOD: &str = "lookupLoggerEnabled";

/// Defines the type for an ID assigned to a trace channel.
pub type VmTraceChannelId = u32;

/// # VM Trace Channel
///
/// Defines the native portion of a "trace channel," which is created as a scope under which trace and log information
/// may be tracked and emitted; trace channels have a corresponding JVM implementation, with a ring buffer queue between
/// that delivers events back and forth.
///
/// VM trace channels are used throughout Elide's native code, via the `tracing` and `log` crates. Those facades report
/// to these channels, which deliver data and events on to the VM when it is ready to consume them.
///
/// The JVM side of `VmTraceChannel` is implemented via `elide.exec.TraceChannel`.
#[derive(Default, Debug)]
pub struct VmTraceChannel {
  /// Unique ID corresponding to this channel; equal to the channel's hash code on the JVM side.
  pub id: VmTraceChannelId,

  /// Whether logging facilities are enabled.
  pub logging: bool,

  /// Whether tracing facilities are enabled.
  pub tracing: bool,
}

impl VmTraceChannel {
  pub(crate) fn new() -> VmTraceChannel {
    VmTraceChannel {
      id: 0,
      logging: true,
      tracing: true,
    }
  }
}

/// Implements logging facilities for VM channels.
impl Log for VmTraceChannel {
  fn enabled(&self, metadata: &log::Metadata) -> bool {
    if !self.logging {
      return false;
    }
    let jvm = active_java_vm();
    let mut env = jvm
      .attach_current_thread()
      .expect("failed to attach to JVM");
    let logger_name = metadata.target();
    let logger_name = env
      .new_string(logger_name)
      .expect("Failed to create Java string for logger name");
    let utils_cls = env
      .find_class(TRACE_UTILS_CLASS)
      .expect("failed to locate native trace utils");

    env
      .call_static_method(
        utils_cls,
        LOOKUP_LOGGER_METHOD,
        "(Ljava/lang/String;)Z",
        &[JValue::Object(logger_name.as_ref())],
      )
      .expect("Failed to call lookupLoggerEnabled")
      .z()
      .expect("Failed to get boolean result from lookupLoggerEnabled")
  }

  fn log(&self, record: &log::Record) {
    deliver_log(record);
  }

  fn flush(&self) {
    crate::ringbuf::flush()
  }
}

/// Implements trace subscription facilities for VM channels.
impl Subscriber for VmTraceChannel {
  fn enabled(&self, _metadata: &Metadata<'_>) -> bool {
    self.tracing // @TODO some configuration here?
  }

  fn new_span(&self, _span: &Attributes<'_>) -> Id {
    todo!()
  }

  fn record(&self, _span: &Id, _values: &Record<'_>) {
    todo!()
  }

  fn record_follows_from(&self, _span: &Id, _follows: &Id) {
    todo!()
  }

  fn event(&self, event: &Event<'_>) {
    deliver_trace(event);
  }

  fn enter(&self, _span: &Id) {
    todo!()
  }

  fn exit(&self, _span: &Id) {
    todo!()
  }
}

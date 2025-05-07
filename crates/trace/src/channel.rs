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
use crate::ringbuf::deliver_log;
use log::Log;
use std::cell::RefCell;
use std::sync::atomic::AtomicUsize;
use std::thread;
use tracing::Id;

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
#[derive(Debug)]
pub struct VmTraceChannel {
  /// Unique ID corresponding to this channel; equal to the channel's hash code on the JVM side.
  pub id: VmTraceChannelId,

  /// The next ID of the span to be created on this channel.
  pub span_id_tip: AtomicUsize,

  /// Thread-local span tracking.
  pub current: CurrentSpanPerThread,

  /// Whether logging facilities are enabled.
  pub logging: bool,

  /// Whether tracing facilities are enabled.
  pub tracing: bool,
}

/// Implements logging facilities for VM channels.
impl Log for VmTraceChannel {
  fn enabled(&self, _metadata: &log::Metadata) -> bool {
    if !self.logging {
      return false;
    }
    true
  }

  fn log(&self, record: &log::Record) {
    deliver_log(record);
  }

  fn flush(&self) {
    crate::ringbuf::flush()
  }
}

/// Tracks the currently executing span on a per-thread basis.
#[derive(Clone, Debug)]
pub struct CurrentSpanPerThread {
  current: &'static thread::LocalKey<RefCell<Vec<Id>>>,
}

impl Default for CurrentSpanPerThread {
  fn default() -> Self {
    Self::new()
  }
}

impl CurrentSpanPerThread {
  pub fn new() -> Self {
    thread_local! {
      static CURRENT: RefCell<Vec<Id>> = const { RefCell::new(Vec::new()) };
    }
    Self { current: &CURRENT }
  }

  /// Returns the [`Id`](::Id) of the span in which the current thread is
  /// executing, or `None` if it is not inside a span.
  pub fn id(&self) -> Option<Id> {
    self
      .current
      .with(|current| current.borrow().last().cloned())
  }

  pub fn enter(&self, span: Id) {
    self.current.with(|current| {
      current.borrow_mut().push(span);
    })
  }

  pub fn exit(&self) {
    self.current.with(|current| {
      let _ = current.borrow_mut().pop();
    })
  }
}

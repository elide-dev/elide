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

use crate::channel::VmTraceChannel;
use jni::objects::{JObject, JValue};
use jni::sys::jlong;
use once_cell::sync::Lazy;
use std::collections::VecDeque;
use std::sync::{Arc, Condvar, Mutex};
use std::thread;
use std::time::{SystemTime, UNIX_EPOCH};
use substrate::vm::active_java_vm;
use tracing_core::field::Visit;
use tracing_core::{Event, Field, Subscriber};
use tracing_subscriber::Layer;
use tracing_subscriber::layer::{Context, SubscriberExt};

const USE_JVM_LOG_INTEGRATION: bool = false;
const TRACE_UTILS_CLASS: &str = "elide/exec/TraceNative";
const DELIVER_LOG_METHOD: &str = "deliverNativeLog";
const DELIVER_LOG_SIG: &str =
  "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V";
const DELIVER_TRACE_METHOD: &str = "deliverNativeTrace";
const DELIVER_TRACE_SIG: &str =
  "(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V";

// Flips to false when the consumer should exit on the next iteration.
static SHOULD_KEEP_DELIVERING: Lazy<Arc<Mutex<bool>>> = Lazy::new(|| Arc::new(Mutex::new(true)));

/// In-memory structure for buffered log events.
#[derive(Debug, Clone)]
struct LogEvent {
  level: String,
  target: String,
  message: String,
  thread_name: String,
}

/// In-memory structure for buffered trace events.
#[derive(Debug)]
struct TraceEvent {
  timestamp: u64,
  level: String,
  message: String,
}

/// Container which holds buffers for log and trace events, as well as methods to deliver them.
struct VmEventBuffer {
  logs: VecDeque<LogEvent>,
  traces: VecDeque<TraceEvent>,
  capacity: usize,
}

impl VmEventBuffer {
  fn new(capacity: usize) -> Self {
    VmEventBuffer {
      logs: VecDeque::with_capacity(capacity),
      traces: VecDeque::with_capacity(capacity),
      capacity,
    }
  }

  fn push_log(&mut self, event: LogEvent) {
    if self.logs.len() == self.capacity {
      // if buffer is full, remove oldest element
      self.logs.pop_front();
    }
    self.logs.push_back(event);
  }

  fn push_trace(&mut self, event: TraceEvent) {
    if self.traces.len() == self.capacity {
      // if buffer is full, remove oldest element
      self.traces.pop_front();
    }
    self.traces.push_back(event);
  }

  fn drain_logs(&mut self) -> Vec<LogEvent> {
    let mut events = Vec::with_capacity(self.logs.len());
    while let Some(event) = self.logs.pop_front() {
      events.push(event);
    }
    events
  }

  fn drain_traces(&mut self) -> Vec<TraceEvent> {
    let mut events = Vec::with_capacity(self.traces.len());
    while let Some(event) = self.traces.pop_front() {
      events.push(event);
    }
    events
  }

  fn is_empty(&self) -> bool {
    self.logs.is_empty() && self.traces.is_empty()
  }
}

/// Static shared-state for the VM event buffer.
struct VmEventBufferState {
  buffer: VmEventBuffer,
  shutdown: bool,
}

/// Static VM event buffer state; initialized on first-use.
static BUFFER_STATE: Lazy<(Arc<Mutex<VmEventBufferState>>, Arc<Condvar>)> = Lazy::new(|| {
  (
    Arc::new(Mutex::new(VmEventBufferState {
      buffer: VmEventBuffer::new(1000),
      shutdown: false,
    })),
    Arc::new(Condvar::new()),
  )
});

struct ElideTracingLayer;

impl ElideTracingLayer {
  fn new() -> ElideTracingLayer {
    ElideTracingLayer {}
  }
}

// Implements a tracing subscriber as an installable layer.
impl<S> Layer<S> for ElideTracingLayer
where
  S: Subscriber,
{
  fn on_event(&self, event: &Event<'_>, _ctx: Context<'_, S>) {
    // Extract fields from the event
    let mut visitor = EventVisitor::default();
    event.record(&mut visitor);

    let metadata = event.metadata();
    let timestamp = SystemTime::now()
      .duration_since(UNIX_EPOCH)
      .unwrap_or_default()
      .as_millis() as u64;

    let trace_event = TraceEvent {
      timestamp,
      level: metadata.level().to_string(),
      message: visitor.message.unwrap_or_default(),
    };

    // deliver event
    let (state_ref, condvar_ref) = &*BUFFER_STATE;
    let mut state = state_ref.lock().unwrap();
    let condvar = Arc::clone(condvar_ref);
    state.buffer.push_trace(trace_event);
    condvar.notify_one();
  }
}

#[derive(Default)]
struct EventVisitor {
  message: Option<String>,
}

impl Visit for EventVisitor {
  fn record_str(&mut self, field: &Field, value: &str) {
    if field.name() == "message" {
      self.message = Some(value.to_string());
    }
  }

  fn record_debug(&mut self, field: &Field, value: &dyn std::fmt::Debug) {
    if field.name() == "message" {
      self.message = Some(format!("{:?}", value));
    }
  }
}

/// Trigger a flush of log and trace data, as applicable.
pub(crate) fn flush() {
  // trigger a flush
  if !USE_JVM_LOG_INTEGRATION {
    return;
  }

  // let the consumer know it should flush
  let (_, condvar_ref) = &*BUFFER_STATE;
  let condvar = Arc::clone(condvar_ref);
  condvar.notify_one();
}

/// Acquire the shared buffer state and deliver a log record.
pub(crate) fn deliver_log(record: &log::Record) {
  if !USE_JVM_LOG_INTEGRATION {
    return;
  }
  let (state_ref, condvar_ref) = &*BUFFER_STATE;
  let mut state = state_ref.lock().unwrap();
  let condvar = Arc::clone(condvar_ref);

  // init log event, then push it to the buffer
  state.buffer.push_log(LogEvent {
    level: record.level().to_string(),
    target: record.target().to_string(),
    message: record.args().to_string(),
    thread_name: thread::current().name().unwrap_or("unknown").to_string(),
  });

  // notify the consumer that it has work
  condvar.notify_one();
}

/// Acquire the shared buffer state and deliver a trace record.
pub(crate) fn deliver_trace(event: &Event<'_>) {
  if !USE_JVM_LOG_INTEGRATION {
    return;
  }
  let (state_ref, condvar_ref) = &*BUFFER_STATE;
  let mut state = state_ref.lock().unwrap();
  let condvar = Arc::clone(condvar_ref);
  let metadata = event.metadata();
  let level = metadata.level();
  let level_str = level.to_string();
  // let message = metadata.message();
  let message_str = "sample".to_string();
  let timestamp = SystemTime::now()
    .duration_since(UNIX_EPOCH)
    .unwrap_or_default()
    .as_millis() as u64;

  // init trace event, then push it to the buffer
  state.buffer.push_trace(TraceEvent {
    timestamp,
    level: level_str,
    message: message_str,
  });

  // notify the consumer that it has work
  condvar.notify_one();
}

/// Flips a flag which causes the consumer flag to exit on the next iteration.
pub(crate) fn shutdown_consumer_thread() {
  if !USE_JVM_LOG_INTEGRATION {
    return;
  }
  {
    let mut should_deliver = SHOULD_KEEP_DELIVERING.lock().unwrap();
    *should_deliver = false;
  }
  let (state, condvar) = &*BUFFER_STATE;
  {
    let mut guard = state.lock().unwrap();
    guard.shutdown = true;
  }
  condvar.notify_all();
}

/// Install the tracing subscriber.
pub(crate) fn register_tracing_subscriber() {
  let subscriber = VmTraceChannel::new().with(ElideTracingLayer::new());

  match tracing::subscriber::set_global_default(subscriber) {
    Ok(_) => {}
    Err(err) => {
      eprintln!("failed to initialize native tracing subscriber: {}", err)
    }
  }
}

/// Install the log handler for bridging to JVM.
pub(crate) fn register_log_handler() {
  // nothing yet
}

/// Start the event buffer consumer thread; this will begin consuming events, and delivering them to the active JVM.
pub(crate) fn start_consumer_thread() {
  if !USE_JVM_LOG_INTEGRATION {
    return;
  }
  let (state_ref, condvar_ref) = &*BUFFER_STATE;
  let state = Arc::clone(state_ref);
  let condvar = Arc::clone(condvar_ref);
  let keep_delivering = Arc::clone(&SHOULD_KEEP_DELIVERING);

  // Spawn only one thread to handle the consumer loop
  thread::spawn(move || {
    // Get JVM reference
    let jvm = active_java_vm();

    // Attach this thread to the JVM
    let mut env = match jvm.attach_current_thread() {
      Ok(env) => env,
      Err(e) => {
        eprintln!("Failed to attach thread to JVM: {:?}", e);
        return;
      }
    };

    // Find the receiver class
    let receiver_class = env.find_class(TRACE_UTILS_CLASS).unwrap_or_else(|_| {
      panic!(
        "failed to locate event receiver class `{}`",
        TRACE_UTILS_CLASS
      )
    });

    // Cache method IDs for better performance
    let _log_delivery = env
      .get_static_method_id(&receiver_class, DELIVER_LOG_METHOD, DELIVER_LOG_SIG)
      .expect("failed to resolve log delivery method");

    let _trace_delivery = env
      .get_static_method_id(&receiver_class, DELIVER_TRACE_METHOD, DELIVER_TRACE_SIG)
      .expect("failed to resolve trace delivery method");

    loop {
      // check exit flag
      if !*keep_delivering.lock().unwrap() {
        break;
      }
      let mut guard = state.lock().unwrap();
      while guard.buffer.is_empty() && !guard.shutdown {
        guard = condvar.wait(guard).unwrap();
      }
      if guard.shutdown && guard.buffer.is_empty() {
        break;
      }

      // drain events
      let logs = guard.buffer.drain_logs();
      let traces = guard.buffer.drain_traces();
      drop(guard);

      // deliver logs
      for event in logs {
        let j_level = env
          .new_string(&event.level)
          .unwrap_or_else(|_| JObject::null().into());
        let j_target = env
          .new_string(&event.target)
          .unwrap_or_else(|_| JObject::null().into());
        let j_message = env
          .new_string(&event.message)
          .unwrap_or_else(|_| JObject::null().into());
        let j_thread = env
          .new_string(&event.thread_name)
          .unwrap_or_else(|_| JObject::null().into());

        let log_result = env.call_static_method(
          &receiver_class,
          DELIVER_LOG_METHOD,
          DELIVER_LOG_SIG,
          &[
            JValue::Object(j_level.as_ref()),
            JValue::Object(j_target.as_ref()),
            JValue::Object(j_message.as_ref()),
            JValue::Object(j_thread.as_ref()),
          ],
        );

        if let Err(e) = log_result {
          eprintln!("Failed to deliver log to JVM: {:?}", e);
        }
      }

      // deliver traces
      for trace in traces {
        let j_level = env
          .new_string(&trace.level)
          .unwrap_or_else(|_| JObject::null().into());
        let j_message = env
          .new_string(&trace.message)
          .unwrap_or_else(|_| JObject::null().into());
        let j_timestamp = trace.timestamp as jlong;

        let trace_result = env.call_static_method(
          &receiver_class,
          DELIVER_TRACE_METHOD,
          DELIVER_TRACE_SIG,
          &[
            JValue::Long(j_timestamp),
            JValue::Object(j_level.as_ref()),
            JValue::Object(j_message.as_ref()),
          ],
        );

        if let Err(e) = trace_result {
          eprintln!("Failed to deliver trace to JVM: {:?}", e);
        }
      }
    }
  });
}

/*
jvm log flush:

    let jvm = active_java_vm();
    let mut env = jvm
      .attach_current_thread()
      .expect("failed to attach to JVM");
    let utils_cls = env
      .find_class(TRACE_UTILS_CLASS)
      .expect("failed to locate native trace utils");

    env
      .call_static_method(utils_cls, FLUSH_LOGS_METHOD, "()Z", &[])
      .expect("Failed to call lookupLoggerEnabled")
      .z()
      .expect("Failed to get boolean result from lookupLoggerEnabled");

*/

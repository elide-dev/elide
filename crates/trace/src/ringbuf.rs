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

use jni::objects::{JObject, JValue};
use jni::signature::Primitive::Boolean;
use jni::signature::ReturnType;
use jni::sys::{jint, jlong};
use log::Level;
use once_cell::sync::Lazy;
use std::collections::VecDeque;
use std::sync::{Arc, Condvar, Mutex};
use std::thread;
use std::time::{SystemTime, UNIX_EPOCH};
use substrate::vm::active_java_vm;
use tracing_core::field::Visit;
use tracing_core::span::{Attributes, Id};
use tracing_core::{Event, Field, LevelFilter, Metadata, Subscriber};
use tracing_subscriber::Layer;
use tracing_subscriber::layer::{Context, SubscriberExt};
use tracing_subscriber::util::SubscriberInitExt;

const DEBUG_LOGS: bool = false;
const TRACE_LOGS: bool = false;
const USE_JVM_LOG_INTEGRATION: bool = true;
const USE_PRELOADED_METHODS: bool = true;
const MAX_LOG_LEVEL: log::LevelFilter = log::LevelFilter::Info;
const MAX_EVENT_LEVEL: log::Level = log::Level::Info;
const TRACE_UTILS_CLASS: &str = "elide/exec/TraceNative";
const DELIVER_LOG_METHOD: &str = "deliverNativeLog";
const DELIVER_LOG_SIG: &str =
  "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z";
const DELIVER_TRACE_METHOD: &str = "deliverNativeTrace";
const DELIVER_TRACE_SIG: &str = "(JJIILjava/lang/String;)Z";

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

/// Indicates the type of trace event.
#[derive(Debug, Clone, Default)]
enum TraceEventType {
  #[default]
  Event = 0,
  Enter = 1,
  Exit = 2,
}

/// In-memory structure for buffered trace events.
#[derive(Debug, Clone, Default)]
struct TraceEvent {
  timestamp: u64,
  kind: TraceEventType,
  id: Option<u64>,
  level: Option<Level>,
  message: Option<String>,
}

/// Converts a tracing level to a log level.
fn trace_level_to_log_level(level: &tracing_core::metadata::Level) -> Level {
  match *level {
    tracing_core::metadata::Level::TRACE => Level::Trace,
    tracing_core::metadata::Level::DEBUG => Level::Debug,
    tracing_core::metadata::Level::INFO => Level::Info,
    tracing_core::metadata::Level::WARN => Level::Warn,
    tracing_core::metadata::Level::ERROR => Level::Error,
  }
}

/// Return an integer code indicating the severity level.
fn code_for_level(level: Option<Level>) -> jint {
  match level {
    Some(value) => match value {
      Level::Error => 1,
      Level::Warn => 2,
      Level::Info => 3,
      Level::Debug => 4,
      Level::Trace => 5,
    },
    None => 0,
  }
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
    debug_log(format!("pushing log event: {:?}", event).as_str());
    if self.logs.len() == self.capacity {
      // if buffer is full, remove oldest element
      self.logs.pop_front();
    }
    self.logs.push_back(event);
  }

  fn push_trace(&mut self, event: TraceEvent) {
    let level = event.level;
    if level.is_some() {
      let level = level.unwrap();
      if level > MAX_EVENT_LEVEL {
        trace_log(format!("skipping event (not severe enough): {:?}", event).as_str());
        return;
      }
    }
    debug_log(format!("pushing trace event: {:?}", event).as_str());
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
      buffer: VmEventBuffer::new(200),
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

// High-res debug logs util function to use before logging has initialized.
pub(crate) fn trace_log(msg: &str) {
  if DEBUG_LOGS && TRACE_LOGS {
    eprintln!("[native:trace] {}", msg);
  }
}

// Simple debug log fn to use before logging has initialized.
pub(crate) fn debug_log(msg: &str) {
  if DEBUG_LOGS {
    eprintln!("[native:trace] {}", msg);
  }
}

fn deliver_trace_event(event: TraceEvent) {
  // deliver event
  let (state_ref, condvar_ref) = &*BUFFER_STATE;
  let mut state = state_ref.lock().unwrap();
  let condvar = Arc::clone(condvar_ref);
  state.buffer.push_trace(event);
  condvar.notify_one();
}

// Implements a tracing subscriber as an installable layer.
impl<S> Layer<S> for ElideTracingLayer
where
  S: Subscriber,
{
  fn enabled(&self, metadata: &Metadata<'_>, ctx: Context<'_, S>) -> bool {
    true
  }

  fn on_new_span(&self, _attrs: &Attributes<'_>, id: &Id, _ctx: Context<'_, S>) {
    trace_log(format!("layer: new span (id={})", id.into_u64()).as_str());
  }

  fn event_enabled(&self, _event: &Event<'_>, _ctx: Context<'_, S>) -> bool {
    true
  }

  fn on_event(&self, event: &Event<'_>, _ctx: Context<'_, S>) {
    trace_log("layer: tracing event");
    let mut visitor = EventVisitor::default();
    event.record(&mut visitor);

    let metadata = event.metadata();
    let timestamp = SystemTime::now()
      .duration_since(UNIX_EPOCH)
      .unwrap_or_default()
      .as_millis() as u64;

    // deliver event
    deliver_trace_event(TraceEvent {
      timestamp,
      id: None,
      kind: TraceEventType::Event,
      level: Some(trace_level_to_log_level(metadata.level())),
      message: Some(visitor.message.unwrap_or_default()),
    });
  }

  fn on_enter(&self, id: &Id, _ctx: Context<'_, S>) {
    trace_log("layer: tracing enter");
    deliver_trace_enter(id.into_u64());
  }

  fn on_exit(&self, id: &Id, _ctx: Context<'_, S>) {
    trace_log("layer: tracing exit");
    deliver_trace_exit(id.into_u64());
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
  debug_log("delivering native log");
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

/// Acquire the shared buffer state and deliver a trace record of type 'enter'.
pub(crate) fn deliver_trace_enter(id: u64) {
  if !USE_JVM_LOG_INTEGRATION {
    return;
  }
  debug_log("delivering native trace enter");
  let (state_ref, condvar_ref) = &*BUFFER_STATE;
  let mut state = state_ref.lock().unwrap();
  let condvar = Arc::clone(condvar_ref);
  let timestamp = SystemTime::now()
    .duration_since(UNIX_EPOCH)
    .unwrap_or_default()
    .as_millis() as u64;

  state.buffer.push_trace(TraceEvent {
    timestamp,
    id: Some(id),
    kind: TraceEventType::Enter,
    level: None,
    message: None,
  });

  condvar.notify_one();
}

/// Acquire the shared buffer state and deliver a trace record of type 'exit'.
pub(crate) fn deliver_trace_exit(id: u64) {
  if !USE_JVM_LOG_INTEGRATION {
    return;
  }
  debug_log("delivering native trace exit");
  let (state_ref, condvar_ref) = &*BUFFER_STATE;
  let mut state = state_ref.lock().unwrap();
  let condvar = Arc::clone(condvar_ref);
  let timestamp = SystemTime::now()
    .duration_since(UNIX_EPOCH)
    .unwrap_or_default()
    .as_millis() as u64;

  state.buffer.push_trace(TraceEvent {
    timestamp,
    id: Some(id),
    kind: TraceEventType::Exit,
    level: None,
    message: None,
  });

  condvar.notify_one();
}

/// Acquire the shared buffer state and deliver a trace record.
pub(crate) fn deliver_trace(event: &Event<'_>) {
  if !USE_JVM_LOG_INTEGRATION {
    return;
  }
  debug_log("delivering native trace");
  let (state_ref, condvar_ref) = &*BUFFER_STATE;
  let mut state = state_ref.lock().unwrap();
  let condvar = Arc::clone(condvar_ref);
  let metadata = event.metadata();
  let level = metadata.level();
  let timestamp = SystemTime::now()
    .duration_since(UNIX_EPOCH)
    .unwrap_or_default()
    .as_millis() as u64;

  // init trace event, then push it to the buffer
  state.buffer.push_trace(TraceEvent {
    timestamp,
    id: None,
    kind: TraceEventType::Event,
    level: Some(trace_level_to_log_level(level)),
    message: None,
  });

  // notify the consumer that it has work
  condvar.notify_one();
}

/// Flips a flag which causes the consumer flag to exit on the next iteration.
pub(crate) fn shutdown_consumer_thread() {
  if !USE_JVM_LOG_INTEGRATION {
    return;
  }

  debug_log("shutting down log consumer");
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

// Main log subscriber.
// static MAIN_SUBSCRIBER: LazyLock<VmTraceChannel> = LazyLock::new(VmTraceChannel::new);

// Flag indicating initialization has already occurred for logging/tracing handlers.
static INITIALIZED: Lazy<Mutex<bool>> = Lazy::new(|| Mutex::new(false));

/// Install the tracing and log subscriber.
pub(crate) fn register_handlers() {
  // don't register repeatedly
  let mut initialized = INITIALIZED.lock().unwrap();
  if *initialized {
    debug_log("handlers already registered; skipping");
    return;
  }
  *initialized = true;

  debug_log("building trace subscriber registry");
  let builder = tracing_subscriber::registry();

  debug_log("registering trace subscriber");
  builder
    .with(
      tracing_subscriber::fmt::layer()
        .without_time()
        .with_target(true)
        .with_filter(LevelFilter::from_level(tracing_core::Level::TRACE)),
    )
    .with(ElideTracingLayer::new())
    .try_init()
    .expect("failed to register elide's tracing layer");

  tracing::trace!("tracing initialized")
}

/// Start the event buffer consumer thread; this will begin consuming events, and delivering them to the active JVM.
pub(crate) fn start_consumer_thread() {
  if !USE_JVM_LOG_INTEGRATION {
    return;
  }
  debug_log("starting consumer thread");
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
    debug_log("locating trace native receiver");
    let receiver_class = env.find_class(TRACE_UTILS_CLASS).unwrap_or_else(|_| {
      panic!(
        "failed to locate event receiver class `{}`",
        TRACE_UTILS_CLASS
      )
    });

    // Cache method IDs for better performance
    debug_log("plucking log delivery method");
    let log_delivery = env
      .get_static_method_id(&receiver_class, DELIVER_LOG_METHOD, DELIVER_LOG_SIG)
      .expect("failed to resolve log delivery method");

    debug_log("plucking trace delivery method");
    let trace_delivery = env
      .get_static_method_id(&receiver_class, DELIVER_TRACE_METHOD, DELIVER_TRACE_SIG)
      .expect("failed to resolve trace delivery method");

    loop {
      // check exit flag
      trace_log("(consumer) tick");
      if !*keep_delivering.lock().unwrap() {
        debug_log("instructed to halt; breaking consumer loop");
        break;
      }
      trace_log("(consumer) no halt");
      let mut guard = state.lock().unwrap();
      while guard.buffer.is_empty() && !guard.shutdown {
        trace_log("(consumer) wait - empty");
        guard = condvar.wait(guard).unwrap();
      }
      if guard.shutdown && guard.buffer.is_empty() {
        trace_log("(consumer) exit - empty + shutdown");
        break;
      }

      // drain events
      debug_log("draining events");
      let logs = guard.buffer.drain_logs();
      let traces = guard.buffer.drain_traces();
      drop(guard);

      // deliver logs
      for event in logs {
        trace_log("draining log event");
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

        let log_result = if USE_PRELOADED_METHODS {
          unsafe {
            env.call_static_method_unchecked(
              &receiver_class,
              log_delivery,
              ReturnType::Primitive(Boolean),
              &[
                JValue::Object(j_level.as_ref()).as_jni(),
                JValue::Object(j_target.as_ref()).as_jni(),
                JValue::Object(j_message.as_ref()).as_jni(),
                JValue::Object(j_thread.as_ref()).as_jni(),
              ],
            )
          }
        } else {
          env.call_static_method(
            &receiver_class,
            DELIVER_LOG_METHOD,
            DELIVER_LOG_SIG,
            &[
              JValue::Object(j_level.as_ref()),
              JValue::Object(j_target.as_ref()),
              JValue::Object(j_message.as_ref()),
              JValue::Object(j_thread.as_ref()),
            ],
          )
        };

        if let Err(e) = log_result {
          eprintln!("Failed to deliver log to JVM: {:?}", e);
        } else {
          debug_log("log event delivered");
        }
      }

      // deliver traces
      for trace in traces {
        trace_log("draining trace event");
        let j_level = code_for_level(trace.level);

        let j_message = match trace.message {
          Some(msg) => env
            .new_string(&msg)
            .unwrap_or_else(|_| JObject::null().into()),

          _ => JObject::null().into(),
        };

        let j_id = match trace.id {
          Some(trace_id) => trace_id as jlong,
          None => 0,
        };
        let j_timestamp = trace.timestamp as jlong;

        let j_type: jint = trace.kind as jint;
        let trace_result = if USE_PRELOADED_METHODS {
          unsafe {
            env.call_static_method_unchecked(
              &receiver_class,
              trace_delivery,
              ReturnType::Primitive(Boolean),
              &[
                JValue::Long(j_timestamp).as_jni(),
                JValue::Long(j_id).as_jni(),
                JValue::Int(j_type).as_jni(),
                JValue::Int(j_level).as_jni(),
                JValue::Object(j_message.as_ref()).as_jni(),
              ],
            )
          }
        } else {
          env.call_static_method(
            &receiver_class,
            DELIVER_TRACE_METHOD,
            DELIVER_TRACE_SIG,
            &[
              JValue::Long(j_timestamp),
              JValue::Long(j_id),
              JValue::Int(j_type),
              JValue::Int(j_level),
              JValue::Object(j_message.as_ref()),
            ],
          )
        };

        if let Err(e) = trace_result {
          eprintln!("Failed to deliver trace to JVM: {:?}", e);
        } else {
          debug_log("trace event delivered");
        }
      }
    }
    debug_log("consumer thread exiting");
  });
}

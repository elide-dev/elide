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

#![allow(non_snake_case)]
#![forbid(unsafe_op_in_unsafe_fn, unused_unsafe, dead_code)]

use java_native::jni;
use jni::objects::{JClass, JString};
use jni::sys::jint;
use jni::JNIEnv;
use jni_sys::jlong;
use sysinfo::{Pid, Signal, System};

#[cfg(not(target_os = "windows"))]
use rustix::process::{getpriority_process, setpriority_process, Pid as PosixPid, RawPid};

/// Resolve a signal from its `name`.
fn resolve_signal(name: &str) -> Signal {
  match name {
    "SIGKILL" => Signal::Kill,
    "SIGTERM" => Signal::Term,
    "SIGSTOP" => Signal::Stop,
    _ => Signal::Kill,
  }
}

/// Kill a process at a specific PID with a specific signal.
fn kill_with_signal(pid: u64, signal: &str) -> i8 {
  let s = System::new_all();
  if let Some(process) = s.process(Pid::from(pid as usize)) {
    let signal = resolve_signal(signal);
    process.kill_with(signal).expect("failed to kill process");
    process.wait();
    return 0;
  }
  -1
}

/// Retrieve the priority of the current process, via Posix APIs.
#[cfg(not(target_os = "windows"))]
fn current_process_priority() -> i32 {
  getpriority_process(None).expect("failed to retrieve process priority")
}

/// Retrieve the priority of the current process, via Windows APIs.
#[cfg(target_os = "windows")]
fn current_process_priority() -> i32 {
  0 // not yet supported on Windows
}

/// Retrieve the priority of a process at a specific PID, using Posix APIs.
#[cfg(not(target_os = "windows"))]
fn process_priority(pid: u64) -> i32 {
  let target = Some(PosixPid::from_raw(pid as RawPid).expect("not a valid pid"));
  getpriority_process(target).expect("failed to retrieve process priority")
}

/// Retrieve the priority of a process at a specific PID, using Windows APIs.
#[cfg(target_os = "windows")]
fn process_priority(_pid: u64) -> i32 {
  0 // not yet supported on Windows
}

/// Set the priority of the current process.
#[cfg(not(target_os = "windows"))]
fn set_current_process_priority(prio: i32) -> i32 {
  setpriority_process(None, prio).expect("failed to set proc priority");
  prio
}

/// Set the priority of a process at a specific PID.
#[cfg(not(target_os = "windows"))]
fn set_process_priority(pid: u64, prio: i32) -> i32 {
  let target = Some(PosixPid::from_raw(pid as RawPid).expect("not a valid pid"));
  setpriority_process(target, prio).expect("failed to set proc priority");
  prio
}

/// Set the priority of the current process.
#[cfg(target_os = "windows")]
fn set_current_process_priority(_prio: i32) -> i32 {
  -1 // not yet supported on Windows
}

/// Set the priority of a process at a specific PID.
#[cfg(target_os = "windows")]
fn set_process_priority(_pid: u64, _prio: i32) -> i32 {
  -1 // not yet supported on Windows
}

#[jni("elide.runtime.node.childProcess.ChildProcessNative")]
pub fn currentProcessPriority(_env: JNIEnv, _class: JClass) -> jint {
  current_process_priority()
}

#[jni("elide.runtime.node.childProcess.ChildProcessNative")]
pub fn getProcessPriority(_env: JNIEnv, _class: JClass, pid: jlong) -> jint {
  process_priority(pid as u64)
}

#[jni("elide.runtime.node.childProcess.ChildProcessNative")]
pub fn setCurrentProcessPriority(_env: JNIEnv, _class: JClass, prio: jint) -> jint {
  set_current_process_priority(prio)
}

#[jni("elide.runtime.node.childProcess.ChildProcessNative")]
pub fn setProcessPriority(_env: JNIEnv, _class: JClass, pid: jlong, prio: jint) -> jint {
  set_process_priority(pid as u64, prio)
}

#[jni("elide.runtime.node.childProcess.ChildProcessNative")]
pub fn killWith(mut env: JNIEnv, _class: JClass, pid: jlong, signal: JString<'_>) -> jint {
  let signal_name: String = env
    .get_string(&signal)
    .expect("failed to decode signal name")
    .into();
  kill_with_signal(pid as u64, signal_name.as_str()).into()
}

// add tests block
#[cfg(test)]
mod tests {
  use super::*;

  #[test]
  fn test_resolve_signal_sigkill() {
    let signal = resolve_signal("SIGKILL");
    assert_eq!(signal, Signal::Kill);
  }

  #[test]
  fn test_resolve_signal_sigterm() {
    let signal = resolve_signal("SIGTERM");
    assert_eq!(signal, Signal::Term);
  }

  #[test]
  fn test_resolve_signal_sigstop() {
    let signal = resolve_signal("SIGSTOP");
    assert_eq!(signal, Signal::Stop);
  }

  #[test]
  fn test_resolve_signal_unknown() {
    let signal = resolve_signal("unknown");
    assert_eq!(signal, Signal::Kill);
  }

  #[test]
  fn test_subproc_invalid_pid() {
    let result = kill_with_signal(999999, "SIGKILL");
    assert_eq!(result, -1);
  }

  #[test]
  fn test_subproc_killwith_sigkill() {
    // start a subprocess calling into `sleep`
    let child = std::process::Command::new("sleep")
      .arg("60")
      .spawn()
      .expect("failed to start subprocess");

    // get the pid of the subprocess
    let pid = child.id();

    // make sure child is running
    assert!(System::new_all().process(Pid::from(pid as usize)).is_some());

    // send a kill signal
    let result = kill_with_signal(pid as u64, "SIGKILL");
    assert_eq!(result, 0);

    // make sure child is not running anymore
    #[cfg(target_os = "macos")]
    {
      let proc = System::new_all();
      let reproc = proc.process(Pid::from(pid as usize));
      let is_none = reproc.is_none();
      assert!(is_none);
    }
  }

  #[test]
  fn test_subproc_killwith_sigterm() {
    // start a subprocess calling into `sleep`
    let child = std::process::Command::new("sleep")
      .arg("60")
      .spawn()
      .expect("failed to start subprocess");

    // get the pid of the subprocess
    let pid = child.id();

    // make sure child is running
    assert!(System::new_all().process(Pid::from(pid as usize)).is_some());

    // send a kill signal
    let result = kill_with_signal(pid as u64, "SIGTERM");
    assert_eq!(result, 0);

    // make sure child is not running anymore
    #[cfg(target_os = "macos")]
    {
      let proc = System::new_all();
      let reproc = proc.process(Pid::from(pid as usize));
      let is_none = reproc.is_none();
      assert!(is_none);
    }
  }
}

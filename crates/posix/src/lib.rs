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
#![feature(test)]
#![feature(const_option)]
#![feature(const_trait_impl)]
#![allow(non_snake_case)]
#![forbid(unsafe_op_in_unsafe_fn, unused_unsafe, dead_code)]

use java_native::jni;
use jni::objects::{JClass, JString};
use jni::sys::jint;
use jni::JNIEnv;
use sysinfo::{Pid, Signal, System};

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
fn kill_with_signal(pid: u32, signal: &str) -> i8 {
  let s = System::new_all();
  if let Some(process) = s.process(Pid::from(pid as usize)) {
    let signal = resolve_signal(signal);
    process.kill_with(signal).expect("failed to kill process");
    process.wait();
    return 0;
  }
  -1
}

#[jni("elide.runtime.gvm.internals.node.childProcess.ChildProcessNative")]
pub fn killWith(mut env: JNIEnv, _class: JClass, pid: jint, signal: JString<'_>) -> jint {
  let signal_name: String = env.get_string(&signal).expect("failed to decode signal name").into();
  kill_with_signal(pid as u32, signal_name.as_str()).into()
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
    let result = kill_with_signal(pid, "SIGKILL");
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
    let result = kill_with_signal(pid, "SIGTERM");
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

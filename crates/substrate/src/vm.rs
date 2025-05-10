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

use jni::JavaVM;
use once_cell::sync::OnceCell;

/// Active Java VM for this process; initialized early in the boot cycle, during JNI `on_load`.
static JAVA_VM: OnceCell<JavaVM> = OnceCell::new();

/// Determine whether the Java VM has been initialized for this process.
pub fn has_java_vm() -> bool {
  JAVA_VM.get().is_some()
}

/// Initialize the active Java VM with the provided VM instance.
pub fn init_java_vm(vm: JavaVM) {
  // don't let the static overwrite
  JAVA_VM
    .set(vm)
    .expect("failed to set java vm: already initialized");
}

/// Retrieve the Java VM without checking presence.
pub fn active_java_vm() -> &'static JavaVM {
  active_java_vm_safe().expect("java vm is not initialized")
}

/// Retrieve the Java VM without checking presence.
pub fn active_java_vm_safe() -> Option<&'static JavaVM> {
  JAVA_VM.get()
}

/// Clear the active VM; this is called at process shutdown by `on_unload`.
pub fn clear_vm_state() {
  // nothing to do at this time
}

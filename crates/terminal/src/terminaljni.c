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
#include "jni.h"

#ifdef ELIDE_GVM_STATIC
JNIEXPORT jint JNICALL JNI_OnLoad_jansi(JavaVM *vm, void *reserved) {
  return JNI_VERSION_1_8;
}
JNIEXPORT jint JNICALL JNI_OnLoad_jlinenative(JavaVM *vm, void *reserved) {
  return JNI_VERSION_1_8;
}
JNIEXPORT jint JNICALL JNI_OnLoad_terminal(JavaVM *vm, void *reserved) {
  return JNI_VERSION_1_8;
}
JNIEXPORT void JNICALL JNI_OnUnload_jansi(JavaVM *vm, void *reserved) {
  // nothing to do
}
JNIEXPORT void JNICALL JNI_OnUnload_jlinenative(JavaVM *vm, void *reserved) {
  // nothing to do
}
JNIEXPORT void JNICALL JNI_OnUnload_terminal(JavaVM *vm, void *reserved) {
  // nothing to do
}
#else
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
  return JNI_VERSION_1_8;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
  // nothing to do
}
#endif /* ELIDE_GVM_STATIC */

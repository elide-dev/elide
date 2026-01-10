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

#![allow(
  non_snake_case,
  non_camel_case_types,
  non_upper_case_globals,
  improper_ctypes,
  dead_code,
  clippy::missing_safety_doc,
  clippy::type_complexity,
  unnecessary_transmutes
)]

use core::ffi::c_void;
use java_native::{jni, on_load, on_unload};

include!(concat!(env!("OUT_DIR"), "/libsqlitejdbc.rs"));

#[on_load("sqlitejdbc")]
pub unsafe fn on_load_static(vm: *mut JavaVM, reserved: *mut c_void) -> jint {
  unsafe { sqlite_on_load(vm, reserved) }
}

#[on_unload("sqlitejdbc")]
pub unsafe fn on_unload_static(vm: *mut JavaVM, reserved: *mut c_void) {
  unsafe { sqlite_on_unload(vm, reserved) }
}

/// Determine if the SQLite library is operating in static JNI mode.
#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn isStatic(env: *mut JNIEnv, this: *mut c_void) -> jint {
  unsafe { sqlite_isStatic(env, this) }
}

/// Initialize the SQLite library.
#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn initializeStatic(env: *mut JNIEnv, this: *mut c_void) -> jint {
  unsafe { sqlite_initializeStatic(env, this) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    _open_utf8
//  * Signature: ([BI)V
//  */
// JNIEXPORT void JNICALL sqlite__1open_1utf8
//   (JNIEnv *, jobject, jbyteArray, jint);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn openUtf8(env: *mut JNIEnv, this: jobject, jfilename: jbyteArray, flags: jint) {
  unsafe { sqlite__1open_1utf8(env, this, jfilename, flags) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    _close
//  * Signature: ()V
//  */
// JNIEXPORT void JNICALL sqlite__1close
//   (JNIEnv *, jobject);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn _close(env: *mut JNIEnv, this: jobject) {
  unsafe { sqlite__1close(env, this) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    _exec_utf8
//  * Signature: ([B)I
//  */
// JNIEXPORT jint JNICALL sqlite__1exec_1utf8
//   (JNIEnv *, jobject, jbyteArray);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn _exec_utf8(env: *mut JNIEnv, this: jobject, jsql: jbyteArray) -> jint {
  unsafe { sqlite__1exec_1utf8(env, this, jsql) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    shared_cache
//  * Signature: (Z)I
//  */
// JNIEXPORT jint JNICALL sqlite_shared_1cache
//   (JNIEnv *, jobject, jboolean);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn shared_cache(env: *mut JNIEnv, this: jobject, enable: jboolean) -> jint {
  unsafe { sqlite_shared_1cache(env, this, enable) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    enable_load_extension
//  * Signature: (Z)I
//  */
// JNIEXPORT jint JNICALL sqlite_enable_1load_1extension
//   (JNIEnv *, jobject, jboolean);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn enable_load_extension(env: *mut JNIEnv, this: jobject, enable: jboolean) -> jint {
  unsafe { sqlite_enable_1load_1extension(env, this, enable) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    interrupt
//  * Signature: ()V
//  */
// JNIEXPORT void JNICALL sqlite_interrupt
//   (JNIEnv *, jobject);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn interrupt(env: *mut JNIEnv, this: jobject) {
  unsafe { sqlite_interrupt(env, this) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    busy_timeout
//  * Signature: (I)V
//  */
// JNIEXPORT void JNICALL sqlite_busy_1timeout
//   (JNIEnv *, jobject, jint);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn busy_timeout(env: *mut JNIEnv, this: jobject, ms: jint) {
  unsafe { sqlite_busy_1timeout(env, this, ms) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    busy_handler
//  * Signature: (Lorg/sqlite/BusyHandler;)V
//  */
// JNIEXPORT void JNICALL sqlite_busy_1handler
//   (JNIEnv *, jobject, jobject);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn busy_handler(env: *mut JNIEnv, this: jobject, handler: jobject) {
  unsafe { sqlite_busy_1handler(env, this, handler) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    prepare_utf8
//  * Signature: ([B)J
//  */
// JNIEXPORT jlong JNICALL sqlite_prepare_1utf8
//   (JNIEnv *, jobject, jbyteArray);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn prepare_utf8(env: *mut JNIEnv, this: jobject, jsql: jbyteArray) -> jlong {
  unsafe { sqlite_prepare_1utf8(env, this, jsql) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    errmsg_utf8
//  * Signature: ()Ljava/nio/ByteBuffer;
//  */
// JNIEXPORT jobject JNICALL sqlite_errmsg_1utf8
//   (JNIEnv *, jobject);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn errmsg_utf8(env: *mut JNIEnv, this: jobject) -> jobject {
  unsafe { sqlite_errmsg_1utf8(env, this) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    libversion_utf8
//  * Signature: ()Ljava/nio/ByteBuffer;
//  */
// JNIEXPORT jobject JNICALL sqlite_libversion_1utf8
//   (JNIEnv *, jobject);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn libversion_utf8(env: *mut JNIEnv, this: jobject) -> jobject {
  unsafe { sqlite_libversion_1utf8(env, this) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    changes
//  * Signature: ()J
//  */
// JNIEXPORT jlong JNICALL sqlite_changes
//   (JNIEnv *, jobject);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn changes(env: *mut JNIEnv, this: jobject) -> jlong {
  unsafe { sqlite_changes(env, this) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    total_changes
//  * Signature: ()J
//  */
// JNIEXPORT jlong JNICALL sqlite_total_1changes
//   (JNIEnv *, jobject);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn total_changes(env: *mut JNIEnv, this: jobject) -> jlong {
  unsafe { sqlite_total_1changes(env, this) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    finalize
//  * Signature: (J)I
//  */
// JNIEXPORT jint JNICALL sqlite_finalize
//   (JNIEnv *, jobject, jlong);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn finalize(env: *mut JNIEnv, this: jobject, stmt: jlong) -> jint {
  unsafe { sqlite_finalize(env, this, stmt) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    step
//  * Signature: (J)I
//  */
// JNIEXPORT jint JNICALL sqlite_step
//   (JNIEnv *, jobject, jlong);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn step(env: *mut JNIEnv, this: jobject, stmt: jlong) -> jint {
  unsafe { sqlite_step(env, this, stmt) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    reset
//  * Signature: (J)I
//  */
// JNIEXPORT jint JNICALL sqlite_reset
//   (JNIEnv *, jobject, jlong);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn reset(env: *mut JNIEnv, this: jobject, stmt: jlong) -> jint {
  unsafe { sqlite_reset(env, this, stmt) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    clear_bindings
//  * Signature: (J)I
//  */
// JNIEXPORT jint JNICALL sqlite_clear_1bindings
//   (JNIEnv *, jobject, jlong);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn clear_bindings(env: *mut JNIEnv, this: jobject, stmt: jlong) -> jint {
  unsafe { sqlite_clear_1bindings(env, this, stmt) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    bind_parameter_count
//  * Signature: (J)I
//  */
// JNIEXPORT jint JNICALL sqlite_bind_1parameter_1count
//   (JNIEnv *, jobject, jlong);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn bind_parameter_count(env: *mut JNIEnv, this: jobject, stmt: jlong) -> jint {
  unsafe { sqlite_bind_1parameter_1count(env, this, stmt) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    column_count
//  * Signature: (J)I
//  */
// JNIEXPORT jint JNICALL sqlite_column_1count
//   (JNIEnv *, jobject, jlong);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn column_count(env: *mut JNIEnv, this: jobject, stmt: jlong) -> jint {
  unsafe { sqlite_column_1count(env, this, stmt) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    column_type
//  * Signature: (JI)I
//  */
// JNIEXPORT jint JNICALL sqlite_column_1type
//   (JNIEnv *, jobject, jlong, jint);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn column_type(env: *mut JNIEnv, this: jobject, stmt: jlong, col: jint) -> jint {
  unsafe { sqlite_column_1type(env, this, stmt, col) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    column_decltype_utf8
//  * Signature: (JI)Ljava/nio/ByteBuffer;
//  */
// JNIEXPORT jobject JNICALL sqlite_column_1decltype_1utf8
//   (JNIEnv *, jobject, jlong, jint);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn column_decltype_utf8(
  env: *mut JNIEnv,
  this: jobject,
  stmt: jlong,
  col: jint,
) -> jobject {
  unsafe { sqlite_column_1decltype_1utf8(env, this, stmt, col) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    column_table_name_utf8
//  * Signature: (JI)Ljava/nio/ByteBuffer;
//  */
// JNIEXPORT jobject JNICALL sqlite_column_1table_1name_1utf8
//   (JNIEnv *, jobject, jlong, jint);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn column_table_name_utf8(
  env: *mut JNIEnv,
  this: jobject,
  stmt: jlong,
  col: jint,
) -> jobject {
  unsafe { sqlite_column_1table_1name_1utf8(env, this, stmt, col) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    column_name_utf8
//  * Signature: (JI)Ljava/nio/ByteBuffer;
//  */
// JNIEXPORT jobject JNICALL sqlite_column_1name_1utf8
//   (JNIEnv *, jobject, jlong, jint);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn column_name_utf8(env: *mut JNIEnv, this: jobject, stmt: jlong, col: jint) -> jobject {
  unsafe { sqlite_column_1name_1utf8(env, this, stmt, col) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    column_text_utf8
//  * Signature: (JI)Ljava/nio/ByteBuffer;
//  */
// JNIEXPORT jobject JNICALL sqlite_column_1text_1utf8
//   (JNIEnv *, jobject, jlong, jint);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn column_text_utf8(env: *mut JNIEnv, this: jobject, stmt: jlong, col: jint) -> jobject {
  unsafe { sqlite_column_1text_1utf8(env, this, stmt, col) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    column_blob
//  * Signature: (JI)[B
//  */
// JNIEXPORT jbyteArray JNICALL sqlite_column_1blob
//   (JNIEnv *, jobject, jlong, jint);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn column_blob(env: *mut JNIEnv, this: jobject, stmt: jlong, col: jint) -> jbyteArray {
  unsafe { sqlite_column_1blob(env, this, stmt, col) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    column_double
//  * Signature: (JI)D
//  */
// JNIEXPORT jdouble JNICALL sqlite_column_1double
//   (JNIEnv *, jobject, jlong, jint);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn column_double(env: *mut JNIEnv, this: jobject, stmt: jlong, col: jint) -> jdouble {
  unsafe { sqlite_column_1double(env, this, stmt, col) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    column_long
//  * Signature: (JI)J
//  */
// JNIEXPORT jlong JNICALL sqlite_column_1long
//   (JNIEnv *, jobject, jlong, jint);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn column_long(env: *mut JNIEnv, this: jobject, stmt: jlong, col: jint) -> jlong {
  unsafe { sqlite_column_1long(env, this, stmt, col) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    column_int
//  * Signature: (JI)I
//  */
// JNIEXPORT jint JNICALL sqlite_column_1int
//   (JNIEnv *, jobject, jlong, jint);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn column_int(env: *mut JNIEnv, this: jobject, stmt: jlong, col: jint) -> jint {
  unsafe { sqlite_column_1int(env, this, stmt, col) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    bind_null
//  * Signature: (JI)I
//  */
// JNIEXPORT jint JNICALL sqlite_bind_1null
//   (JNIEnv *, jobject, jlong, jint);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn bind_null(env: *mut JNIEnv, this: jobject, stmt: jlong, pos: jint) -> jint {
  unsafe { sqlite_bind_1null(env, this, stmt, pos) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    bind_int
//  * Signature: (JII)I
//  */
// JNIEXPORT jint JNICALL sqlite_bind_1int
//   (JNIEnv *, jobject, jlong, jint, jint);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn bind_int(env: *mut JNIEnv, this: jobject, stmt: jlong, pos: jint, val: jint) -> jint {
  unsafe { sqlite_bind_1int(env, this, stmt, pos, val) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    bind_long
//  * Signature: (JIJ)I
//  */
// JNIEXPORT jint JNICALL sqlite_bind_1long
//   (JNIEnv *, jobject, jlong, jint, jlong);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn bind_long(
  env: *mut JNIEnv,
  this: jobject,
  stmt: jlong,
  pos: jint,
  val: jlong,
) -> jint {
  unsafe { sqlite_bind_1long(env, this, stmt, pos, val) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    bind_double
//  * Signature: (JID)I
//  */
// JNIEXPORT jint JNICALL sqlite_bind_1double
//   (JNIEnv *, jobject, jlong, jint, jdouble);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn bind_double(
  env: *mut JNIEnv,
  this: jobject,
  stmt: jlong,
  pos: jint,
  val: jdouble,
) -> jint {
  unsafe { sqlite_bind_1double(env, this, stmt, pos, val) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    bind_text_utf8
//  * Signature: (JI[B)I
//  */
// JNIEXPORT jint JNICALL sqlite_bind_1text_1utf8
//   (JNIEnv *, jobject, jlong, jint, jbyteArray);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn bind_text_utf8(
  env: *mut JNIEnv,
  this: jobject,
  stmt: jlong,
  pos: jint,
  val: jbyteArray,
) -> jint {
  unsafe { sqlite_bind_1text_1utf8(env, this, stmt, pos, val) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    bind_blob
//  * Signature: (JI[B)I
//  */
// JNIEXPORT jint JNICALL sqlite_bind_1blob
//   (JNIEnv *, jobject, jlong, jint, jbyteArray);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn bind_blob(
  env: *mut JNIEnv,
  this: jobject,
  stmt: jlong,
  pos: jint,
  val: jbyteArray,
) -> jint {
  unsafe { sqlite_bind_1blob(env, this, stmt, pos, val) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    result_null
//  * Signature: (J)V
//  */
// JNIEXPORT void JNICALL sqlite_result_1null
//   (JNIEnv *, jobject, jlong);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn result_null(env: *mut JNIEnv, this: jobject, stmt: jlong) {
  unsafe { sqlite_result_1null(env, this, stmt) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    result_text_utf8
//  * Signature: (J[B)V
//  */
// JNIEXPORT void JNICALL sqlite_result_1text_1utf8
//   (JNIEnv *, jobject, jlong, jbyteArray);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn result_text_utf8(env: *mut JNIEnv, this: jobject, stmt: jlong, val: jbyteArray) {
  unsafe { sqlite_result_1text_1utf8(env, this, stmt, val) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    result_blob
//  * Signature: (J[B)V
//  */
// JNIEXPORT void JNICALL sqlite_result_1blob
//   (JNIEnv *, jobject, jlong, jbyteArray);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn result_blob(env: *mut JNIEnv, this: jobject, stmt: jlong, val: jbyteArray) {
  unsafe { sqlite_result_1blob(env, this, stmt, val) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    result_double
//  * Signature: (JD)V
//  */
// JNIEXPORT void JNICALL sqlite_result_1double
//   (JNIEnv *, jobject, jlong, jdouble);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn result_double(env: *mut JNIEnv, this: jobject, stmt: jlong, val: jdouble) {
  unsafe { sqlite_result_1double(env, this, stmt, val) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    result_long
//  * Signature: (JJ)V
//  */
// JNIEXPORT void JNICALL sqlite_result_1long
//   (JNIEnv *, jobject, jlong, jlong);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn result_long(env: *mut JNIEnv, this: jobject, stmt: jlong, val: jlong) {
  unsafe { sqlite_result_1long(env, this, stmt, val) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    result_int
//  * Signature: (JI)V
//  */
// JNIEXPORT void JNICALL sqlite_result_1int
//   (JNIEnv *, jobject, jlong, jint);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn result_int(env: *mut JNIEnv, this: jobject, stmt: jlong, val: jint) {
  unsafe { sqlite_result_1int(env, this, stmt, val) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    result_error_utf8
//  * Signature: (J[B)V
//  */
// JNIEXPORT void JNICALL sqlite_result_1error_1utf8
//   (JNIEnv *, jobject, jlong, jbyteArray);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn result_error_utf8(env: *mut JNIEnv, this: jobject, stmt: jlong, val: jbyteArray) {
  unsafe { sqlite_result_1error_1utf8(env, this, stmt, val) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    value_text_utf8
//  * Signature: (Lorg/sqlite/Function;I)Ljava/nio/ByteBuffer;
//  */
// JNIEXPORT jobject JNICALL sqlite_value_1text_1utf8
//   (JNIEnv *, jobject, jobject, jint);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn value_text_utf8(
  env: *mut JNIEnv,
  this: jobject,
  func: jobject,
  arg: jint,
) -> jobject {
  unsafe { sqlite_value_1text_1utf8(env, this, func, arg) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    value_blob
//  * Signature: (Lorg/sqlite/Function;I)[B
//  */
// JNIEXPORT jbyteArray JNICALL sqlite_value_1blob
//   (JNIEnv *, jobject, jobject, jint);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn value_blob(env: *mut JNIEnv, this: jobject, func: jobject, arg: jint) -> jbyteArray {
  unsafe { sqlite_value_1blob(env, this, func, arg) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    value_double
//  * Signature: (Lorg/sqlite/Function;I)D
//  */
// JNIEXPORT jdouble JNICALL sqlite_value_1double
//   (JNIEnv *, jobject, jobject, jint);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn value_double(env: *mut JNIEnv, this: jobject, func: jobject, arg: jint) -> jdouble {
  unsafe { sqlite_value_1double(env, this, func, arg) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    value_long
//  * Signature: (Lorg/sqlite/Function;I)J
//  */
// JNIEXPORT jlong JNICALL sqlite_value_1long
//   (JNIEnv *, jobject, jobject, jint);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn value_long(env: *mut JNIEnv, this: jobject, func: jobject, arg: jint) -> jlong {
  unsafe { sqlite_value_1long(env, this, func, arg) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    value_int
//  * Signature: (Lorg/sqlite/Function;I)I
//  */
// JNIEXPORT jint JNICALL sqlite_value_1int
//   (JNIEnv *, jobject, jobject, jint);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn value_int(env: *mut JNIEnv, this: jobject, func: jobject, arg: jint) -> jint {
  unsafe { sqlite_value_1int(env, this, func, arg) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    value_type
//  * Signature: (Lorg/sqlite/Function;I)I
//  */
// JNIEXPORT jint JNICALL sqlite_value_1type
//   (JNIEnv *, jobject, jobject, jint);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn value_type(env: *mut JNIEnv, this: jobject, func: jobject, arg: jint) -> jint {
  unsafe { sqlite_value_1type(env, this, func, arg) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    create_function_utf8
//  * Signature: ([BLorg/sqlite/Function;II)I
//  */
// JNIEXPORT jint JNICALL sqlite_create_1function_1utf8
//   (JNIEnv *, jobject, jbyteArray, jobject, jint, jint);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn create_function_utf8(
  env: *mut JNIEnv,
  this: jobject,
  jname: jbyteArray,
  func: jobject,
  nargs: jint,
  flags: jint,
) -> jint {
  unsafe { sqlite_create_1function_1utf8(env, this, jname, func, nargs, flags) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    destroy_function_utf8
//  * Signature: ([B)I
//  */
// JNIEXPORT jint JNICALL sqlite_destroy_1function_1utf8
//   (JNIEnv *, jobject, jbyteArray);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn destroy_function_utf8(env: *mut JNIEnv, this: jobject, jname: jbyteArray) -> jint {
  unsafe { sqlite_destroy_1function_1utf8(env, this, jname) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    create_collation_utf8
//  * Signature: ([BLorg/sqlite/Collation;)I
//  */
// JNIEXPORT jint JNICALL sqlite_create_1collation_1utf8
//   (JNIEnv *, jobject, jbyteArray, jobject);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn create_collation_utf8(
  env: *mut JNIEnv,
  this: jobject,
  jname: jbyteArray,
  collation: jobject,
) -> jint {
  unsafe { sqlite_create_1collation_1utf8(env, this, jname, collation) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    destroy_collation_utf8
//  * Signature: ([B)I
//  */
// JNIEXPORT jint JNICALL sqlite_destroy_1collation_1utf8
//   (JNIEnv *, jobject, jbyteArray);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn destroy_collation_utf8(env: *mut JNIEnv, this: jobject, jname: jbyteArray) -> jint {
  unsafe { sqlite_destroy_1collation_1utf8(env, this, jname) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    limit
//  * Signature: (II)I
//  */
// JNIEXPORT jint JNICALL sqlite_limit
//   (JNIEnv *, jobject, jint, jint);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn limit(env: *mut JNIEnv, this: jobject, id: jint, newVal: jint) -> jint {
  unsafe { sqlite_limit(env, this, id, newVal) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    backup
//  * Signature: ([B[BLorg/sqlite/core/DB/ProgressObserver;III)I
//  */
// JNIEXPORT jint JNICALL sqlite_backup
//   (JNIEnv *, jobject, jbyteArray, jbyteArray, jobject, jint, jint, jint);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn backup(
  env: *mut JNIEnv,
  this: jobject,
  jfilename: jbyteArray,
  jdest: jbyteArray,
  observer: jobject,
  pages: jint,
  sleep: jint,
  step: jint,
) -> jint {
  unsafe { sqlite_backup(env, this, jfilename, jdest, observer, pages, sleep, step) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    restore
//  * Signature: ([B[BLorg/sqlite/core/DB/ProgressObserver;III)I
//  */
// JNIEXPORT jint JNICALL sqlite_restore
//   (JNIEnv *, jobject, jbyteArray, jbyteArray, jobject, jint, jint, jint);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn restore(
  env: *mut JNIEnv,
  this: jobject,
  jfilename: jbyteArray,
  jdest: jbyteArray,
  observer: jobject,
  pages: jint,
  sleep: jint,
  step: jint,
) -> jint {
  unsafe { sqlite_restore(env, this, jfilename, jdest, observer, pages, sleep, step) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    column_metadata
//  * Signature: (J)[[Z
//  */
// JNIEXPORT jobjectArray JNICALL sqlite_column_1metadata
//   (JNIEnv *, jobject, jlong);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn column_metadata(env: *mut JNIEnv, this: jobject, stmt: jlong) -> jobjectArray {
  unsafe { sqlite_column_1metadata(env, this, stmt) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    set_commit_listener
//  * Signature: (Z)V
//  */
// JNIEXPORT void JNICALL sqlite_set_1commit_1listener
//   (JNIEnv *, jobject, jboolean);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn set_commit_listener(env: *mut JNIEnv, this: jobject, enable: jboolean) {
  unsafe { sqlite_set_1commit_1listener(env, this, enable) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    set_update_listener
//  * Signature: (Z)V
//  */
// JNIEXPORT void JNICALL sqlite_set_1update_1listener
//   (JNIEnv *, jobject, jboolean);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn set_update_listener(env: *mut JNIEnv, this: jobject, enable: jboolean) {
  unsafe { sqlite_set_1update_1listener(env, this, enable) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    register_progress_handler
//  * Signature: (ILorg/sqlite/ProgressHandler;)V
//  */
// JNIEXPORT void JNICALL sqlite_register_1progress_1handler
//   (JNIEnv *, jobject, jint, jobject);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn register_progress_handler(
  env: *mut JNIEnv,
  this: jobject,
  steps: jint,
  handler: jobject,
) {
  unsafe { sqlite_register_1progress_1handler(env, this, steps, handler) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    clear_progress_handler
//  * Signature: ()V
//  */
// JNIEXPORT void JNICALL sqlite_clear_1progress_1handler
//   (JNIEnv *, jobject);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn clear_progress_handler(env: *mut JNIEnv, this: jobject) {
  unsafe { sqlite_clear_1progress_1handler(env, this) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    serialize
//  * Signature: (Ljava/lang/String;)[B
//  */
// jbyteArray JNICALL sqlite_serialize
//   (JNIEnv *, jobject, jstring);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn serialize(env: *mut JNIEnv, this: jobject, db: jstring) -> jbyteArray {
  unsafe { sqlite_serialize(env, this, db) }
}

// /*
//  * Class:     org_sqlite_core_NativeDB
//  * Method:    deserialize
//  * Signature: (Ljava/lang/String;[B)V
//  */
// void JNICALL sqlite_deserialize
//   (JNIEnv *, jobject, jstring, jbyteArray);

#[jni("org.sqlite.core.NativeDB")]
pub unsafe fn deserialize(env: *mut JNIEnv, this: jobject, jschema: jstring, jbuff: jbyteArray) {
  unsafe { sqlite_deserialize(env, this, jschema, jbuff) }
}

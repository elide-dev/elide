use std::ffi::{c_void, CStr, CString};
use std::os::raw::{c_char, c_int};
use std::sync::Once;

use jni::objects::{JClass, JObject, JString, JValue};
use jni::strings::JNIString;
use jni::sys::{jboolean, jbyteArray, jdouble, jint, jlong, jobject, jobjectArray, jsize, jstring, JavaVM, JNI_ERR, JNI_FALSE, JNI_TRUE, JNI_VERSION_1_8};
use jni::{JNIEnv};
use jni::sys::JNI_OK;

use libsqlite3_sys::{sqlite3, sqlite3_backup, sqlite3_backup_finish, sqlite3_backup_init, sqlite3_backup_pagecount, sqlite3_backup_remaining, sqlite3_backup_step, sqlite3_bind_blob, sqlite3_bind_double, sqlite3_bind_int, sqlite3_bind_int64, sqlite3_bind_null, sqlite3_bind_parameter_count, sqlite3_bind_text, sqlite3_busy_handler, sqlite3_busy_timeout, sqlite3_changes, sqlite3_clear_bindings, sqlite3_close, sqlite3_column_blob, sqlite3_column_bytes, sqlite3_column_count, sqlite3_column_decltype, sqlite3_column_double, sqlite3_column_int, sqlite3_column_int64, sqlite3_column_name, sqlite3_column_table_name, sqlite3_column_text, sqlite3_column_type, sqlite3_commit_hook, sqlite3_create_collation_v2, sqlite3_create_function_v2, sqlite3_create_window_function, sqlite3_deserialize, sqlite3_enable_load_extension, sqlite3_enable_shared_cache, sqlite3_errcode, sqlite3_errmsg, sqlite3_exec, sqlite3_extended_errcode, sqlite3_extended_result_codes, sqlite3_file_control, sqlite3_finalize, sqlite3_free, sqlite3_interrupt, sqlite3_libversion, sqlite3_limit, sqlite3_malloc64, sqlite3_open_v2, sqlite3_prepare_v2, sqlite3_progress_handler, sqlite3_reset, sqlite3_result_blob, sqlite3_result_double, sqlite3_result_error, sqlite3_result_error_nomem, sqlite3_result_int, sqlite3_result_int64, sqlite3_result_null, sqlite3_result_text, sqlite3_rollback_hook, sqlite3_serialize, sqlite3_sleep, sqlite3_step, sqlite3_stmt, sqlite3_strnicmp, sqlite3_table_column_metadata, sqlite3_total_changes, sqlite3_update_hook, sqlite3_user_data, sqlite3_value, sqlite3_value_blob, sqlite3_value_bytes, sqlite3_value_double, sqlite3_value_int, sqlite3_value_int64, sqlite3_value_text, sqlite3_value_type, SQLITE_ABORT, SQLITE_BUSY, SQLITE_DESERIALIZE_FREEONCLOSE, SQLITE_DESERIALIZE_RESIZEABLE, SQLITE_DONE, SQLITE_ERROR, SQLITE_FCNTL_SIZE_LIMIT, SQLITE_INTERNAL, SQLITE_LOCKED, SQLITE_MISUSE, SQLITE_NOMEM, SQLITE_OK, SQLITE_OPEN_CREATE, SQLITE_OPEN_READONLY, SQLITE_OPEN_READWRITE, SQLITE_OPEN_URI, SQLITE_SERIALIZE_NOCOPY, SQLITE_TRANSIENT, SQLITE_UTF16, SQLITE_UTF8};

static STATIC_SQLITE_LIB_INITIALIZATION: Once = Once::new();
const DEBUG_LOGS: bool = false;

// Java class variables and method references initialized on library load.
// These classes are weak references to that if the classloader is no longer referenced (garbage)
// It can be garbage collected. The weak references are freed on unload.
// These should not be static variables within methods because assignment in C is not
// guaranteed to be atomic, meaning that one thread may have half initialized a reference
// while another thread reads this half reference resulting in a crash.

static mut DID_INITIALIZE: bool = false;

#[must_use]
#[allow(non_snake_case)]
unsafe fn loadStaticSqliteLib(env: *mut JNIEnv) -> jint {
  if DID_INITIALIZE {
    return JNI_VERSION_1_8;
  }

  debugLog("SQLITE3: loadStaticSqliteLib checkpoint 1");
  if env.is_null() {
    debugLog("SQLITE3: env is NULL");
  } else {
    debugLog("SQLITE3: env is present");
  }

  // Initialize Java class variables, method IDs, and field IDs here
  // ...

  DID_INITIALIZE = true;
  JNI_VERSION_1_8
}

fn debugLog(message: &str) {
  if DEBUG_LOGS {
    eprintln!("{}", message);
  }
}

// Add remaining utility functions
// ...

// INITIALISATION

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn JNI_OnLoad(vm: *mut JavaVM, _reserved: *mut c_void) -> jint {
  let mut env: *mut JNIEnv = std::ptr::null_mut();
  if (**vm).GetEnv.unwrap()(vm, &mut env as *mut *mut JNIEnv as *mut *mut c_void, JNI_VERSION_1_8) != JNI_OK {
    return JNI_ERR;
  }
  if env.is_null() {
    debugLog("SQLITE3: env is NULL");
    return JNI_ERR;
  }
  debugLog("SQLITE3: JNI_OnLoad");
  loadStaticSqliteLib(env)
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_org_sqlite_core_NativeDB_initializeStatic(_env: *mut JNIEnv, _reserved: *mut c_void) -> jint {
  debugLog("SQLITE3: initializing static");
  loadStaticSqliteLib(_env)
}

// FINALIZATION

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn JNI_OnUnload(vm: *mut JavaVM, _reserved: *mut c_void) {
  let mut env: *mut JNIEnv = std::ptr::null_mut();
  if (**vm).GetEnv.unwrap()(vm, &mut env as *mut *mut JNIEnv as *mut *mut c_void, JNI_VERSION_1_8) != JNI_OK {
    return;
  }

  // Delete weak global references to Java classes and reset variables here
  // ...

  DID_INITIALIZE = false;
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_org_sqlite_core_NativeDB_unload(_vm: *mut JavaVM, _reserved: *mut c_void) {
  JNI_OnUnload(_vm, _reserved);
}

// WRAPPERS for sqlite_* functions - translate to Rust
// ...

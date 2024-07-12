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
    dead_code
)]
#![forbid(unsafe_code)]

use jni::objects::{JClass, JObject, JString};
use jni::sys::{jboolean, jbyteArray, jdouble, jint, jlong, jobject, jstring};
use jni::JNIEnv;
use rusqlite::{Connection, NO_PARAMS};

struct NativeDB {
    conn: Connection,
}

impl NativeDB {
    fn new() -> Self {
        NativeDB {
            conn: Connection::open_in_memory().unwrap(),
        }
    }

    fn open_utf8(&mut self, env: JNIEnv, file: jbyteArray, flags: jint) {
        let file_bytes = env.convert_byte_array(file).unwrap();
        let file_str = String::from_utf8(file_bytes).unwrap();
        self.conn = Connection::open_with_flags(file_str, flags).unwrap();
    }

    fn close(&mut self) {
        self.conn.close().unwrap();
    }

    fn exec_utf8(&self, env: JNIEnv, sql: jbyteArray) -> jint {
        let sql_bytes = env.convert_byte_array(sql).unwrap();
        let sql_str = String::from_utf8(sql_bytes).unwrap();
        self.conn.execute_batch(&sql_str).unwrap() as jint
    }

    fn shared_cache(&self, enable: jboolean) -> jint {
        rusqlite::config::Config::shared_cache(enable != 0).unwrap() as jint
    }

    fn enable_load_extension(&self, enable: jboolean) -> jint {
        self.conn
            .set_load_extension(enable != 0)
            .unwrap() as jint
    }

    fn interrupt(&self) {
        self.conn.interrupt();
    }

    fn busy_timeout(&self, ms: jint) {
        self.conn.busy_timeout(std::time::Duration::from_millis(ms as u64));
    }

    fn prepare_utf8(&self, env: JNIEnv, sql: jbyteArray) -> jlong {
        let sql_bytes = env.convert_byte_array(sql).unwrap();
        let sql_str = String::from_utf8(sql_bytes).unwrap();
        let stmt = self.conn.prepare(&sql_str).unwrap();
        Box::into_raw(Box::new(stmt)) as jlong
    }

    fn errmsg_utf8(&self, env: JNIEnv) -> jobject {
        let errmsg = self.conn.errmsg().unwrap_or_default();
        let errmsg_bytes = errmsg.as_bytes();
        env.byte_array_from_slice(errmsg_bytes).unwrap()
    }

    fn libversion_utf8(&self, env: JNIEnv) -> jobject {
        let version = rusqlite::version();
        let version_bytes = version.as_bytes();
        env.byte_array_from_slice(version_bytes).unwrap()
    }

    fn changes(&self) -> jlong {
        self.conn.changes() as jlong
    }

    fn total_changes(&self) -> jlong {
        self.conn.total_changes() as jlong
    }

    fn finalize(&self, stmt: jlong) -> jint {
        let stmt = unsafe { Box::from_raw(stmt as *mut rusqlite::Statement) };
        stmt.finalize().unwrap() as jint
    }

    fn step(&self, stmt: jlong) -> jint {
        let stmt = unsafe { &mut *(stmt as *mut rusqlite::Statement) };
        stmt.step().unwrap() as jint
    }

    fn reset(&self, stmt: jlong) -> jint {
        let stmt = unsafe { &mut *(stmt as *mut rusqlite::Statement) };
        stmt.reset().unwrap() as jint
    }

    fn clear_bindings(&self, stmt: jlong) -> jint {
        let stmt = unsafe { &mut *(stmt as *mut rusqlite::Statement) };
        stmt.clear_bindings().unwrap() as jint
    }

    fn bind_parameter_count(&self, stmt: jlong) -> jint {
        let stmt = unsafe { &*(stmt as *mut rusqlite::Statement) };
        stmt.bind_parameter_count() as jint
    }

    fn column_count(&self, stmt: jlong) -> jint {
        let stmt = unsafe { &*(stmt as *mut rusqlite::Statement) };
        stmt.column_count() as jint
    }

    fn column_type(&self, stmt: jlong, col: jint) -> jint {
        let stmt = unsafe { &*(stmt as *mut rusqlite::Statement) };
        stmt.column_type(col as usize) as jint
    }

    fn column_decltype_utf8(&self, env: JNIEnv, stmt: jlong, col: jint) -> jobject {
        let stmt = unsafe { &*(stmt as *mut rusqlite::Statement) };
        let decltype = stmt.column_decltype(col as usize).unwrap_or_default();
        let decltype_bytes = decltype.as_bytes();
        env.byte_array_from_slice(decltype_bytes).unwrap()
    }

    fn column_table_name_utf8(&self, env: JNIEnv, stmt: jlong, col: jint) -> jobject {
        let stmt = unsafe { &*(stmt as *mut rusqlite::Statement) };
        let table_name = stmt.column_table_name(col as usize).unwrap_or_default();
        let table_name_bytes = table_name.as_bytes();
        env.byte_array_from_slice(table_name_bytes).unwrap()
    }

    fn column_name_utf8(&self, env: JNIEnv, stmt: jlong, col: jint) -> jobject {
        let stmt = unsafe { &*(stmt as *mut rusqlite::Statement) };
        let name = stmt.column_name(col as usize).unwrap_or_default();
        let name_bytes = name.as_bytes();
        env.byte_array_from_slice(name_bytes).unwrap()
    }

    fn column_text_utf8(&self, env: JNIEnv, stmt: jlong, col: jint) -> jobject {
        let stmt = unsafe { &*(stmt as *mut rusqlite::Statement) };
        let text = stmt.column_text(col as usize).unwrap_or_default();
        let text_bytes = text.as_bytes();
        env.byte_array_from_slice(text_bytes).unwrap()
    }

    fn column_blob(&self, env: JNIEnv, stmt: jlong, col: jint) -> jbyteArray {
        let stmt = unsafe { &*(stmt as *mut rusqlite::Statement) };
        let blob = stmt.column_blob(col as usize).unwrap_or_default();
        env.byte_array_from_slice(blob).unwrap()
    }

    fn column_double(&self, stmt: jlong, col: jint) -> jdouble {
        let stmt = unsafe { &*(stmt as *mut rusqlite::Statement) };
        stmt.column_double(col as usize) as jdouble
    }

    fn column_long(&self, stmt: jlong, col: jint) -> jlong {
        let stmt = unsafe { &*(stmt as *mut rusqlite::Statement) };
        stmt.column_long(col as usize) as jlong
    }

    fn column_int(&self, stmt: jlong, col: jint) -> jint {
        let stmt = unsafe { &*(stmt as *mut rusqlite::Statement) };
        stmt.column_int(col as usize) as jint
    }

    fn bind_null(&self, stmt: jlong, pos: jint) -> jint {
        let stmt = unsafe { &mut *(stmt as *mut rusqlite::Statement) };
        stmt.bind_null(pos as usize).unwrap() as jint
    }

    fn bind_int(&self, stmt: jlong, pos: jint, v: jint) -> jint {
        let stmt = unsafe { &mut *(stmt as *mut rusqlite::Statement) };
        stmt.bind_int(pos as usize, v).unwrap() as jint
    }

    fn bind_long(&self, stmt: jlong, pos: jint, v: jlong) -> jint {
        let stmt = unsafe { &mut *(stmt as *mut rusqlite::Statement) };
        stmt.bind_long(pos as usize, v).unwrap() as jint
    }

    fn bind_double(&self, stmt: jlong, pos: jint, v: jdouble) -> jint {
        let stmt = unsafe { &mut *(stmt as *mut rusqlite::Statement) };
        stmt.bind_double(pos as usize, v).unwrap() as jint
    }

    fn bind_text_utf8(&self, env: JNIEnv, stmt: jlong, pos: jint, v: jbyteArray) -> jint {
        let stmt = unsafe { &mut *(stmt as *mut rusqlite::Statement) };
        let v_bytes = env.convert_byte_array(v).unwrap();
        let v_str = String::from_utf8(v_bytes).unwrap();
        stmt.bind_text(pos as usize, &v_str).unwrap() as jint
    }

    fn bind_blob(&self, env: JNIEnv, stmt: jlong, pos: jint, v: jbyteArray) -> jint {
        let stmt = unsafe { &mut *(stmt as *mut rusqlite::Statement) };
        let v_bytes = env.convert_byte_array(v).unwrap();
        stmt.bind_blob(pos as usize, &v_bytes).unwrap() as jint
    }

    fn result_null(&self, context: jlong) {
        let context = unsafe { &mut *(context as *mut rusqlite::Context) };
        context.result_null();
    }

    fn result_text_utf8(&self, env: JNIEnv, context: jlong, val: jbyteArray) {
        let context = unsafe { &mut *(context as *mut rusqlite::Context) };
        let val_bytes = env.convert_byte_array(val).unwrap();
        let val_str = String::from_utf8(val_bytes).unwrap();
        context.result_text(&val_str);
    }

    fn result_blob(&self, env: JNIEnv, context: jlong, val: jbyteArray) {
        let context = unsafe { &mut *(context as *mut rusqlite::Context) };
        let val_bytes = env.convert_byte_array(val).unwrap();
        context.result_blob(&val_bytes);
    }

    fn result_double(&self, context: jlong, val: jdouble) {
        let context = unsafe { &mut *(context as *mut rusqlite::Context) };
        context.result_double(val);
    }

    fn result_long(&self, context: jlong, val: jlong) {
        let context = unsafe { &mut *(context as *mut rusqlite::Context) };
        context.result_long(val);
    }

    fn result_int(&self, context: jlong, val: jint) {
        let context = unsafe { &mut *(context as *mut rusqlite::Context) };
        context.result_int(val);
    }

    fn result_error_utf8(&self, env: JNIEnv, context: jlong, err: jbyteArray) {
        let context = unsafe { &mut *(context as *mut rusqlite::Context) };
        let err_bytes = env.convert_byte_array(err).unwrap();
        let err_str = String::from_utf8(err_bytes).unwrap();
        context.result_error(&err_str);
    }

    fn value_text_utf8(&self, env: JNIEnv, f: JObject, arg: jint) -> jobject {
        let value = self.value_text(f, arg);
        let value_bytes = value.as_bytes();
        env.byte_array_from_slice(value_bytes).unwrap()
    }

    fn value_blob(&self, env: JNIEnv, f: JObject, arg: jint) -> jbyteArray {
        let value = self.value_blob(f, arg);
        env.byte_array_from_slice(&value).unwrap()
    }

    fn value_double(&self, f: JObject, arg: jint) -> jdouble {
        self.value_double(f, arg) as jdouble
    }

    fn value_long(&self, f: JObject, arg: jint) -> jlong {
        self.value_long(f, arg) as jlong
    }

    fn value_int(&self, f: JObject, arg: jint) -> jint {
        self.value_int(f, arg) as jint
    }

    fn value_type(&self, f: JObject, arg: jint) -> jint {
        self.value_type(f, arg) as jint
    }

    fn create_function_utf8(
        &self,
        env: JNIEnv,
        name: jbyteArray,
        func: JObject,
        n_args: jint,
        flags: jint,
    ) -> jint {
        let name_bytes = env.convert_byte_array(name).unwrap();
        let name_str = String::from_utf8(name_bytes).unwrap();
        self.conn
            .create_scalar_function(
                &name_str,
                n_args as usize,
                flags as u32,
                move |ctx| {
                    let args: Vec<_> = (0..ctx.len())
                        .map(|i| ctx.get::<String>(i).unwrap())
                        .collect();
                    let result = func.call_method(env, "xFunc", &args).unwrap();
                    ctx.set_result(result);
                },
            )
            .unwrap() as jint
    }

    fn destroy_function_utf8(&self, env: JNIEnv, name: jbyteArray) -> jint {
        let name_bytes = env.convert_byte_array(name).unwrap();
        let name_str = String::from_utf8(name_bytes).unwrap();
        self.conn
            .create_scalar_function(&name_str, 0, 0, |_| {})
            .unwrap() as jint
    }

    fn create_collation_utf8(&self, env: JNIEnv, name: jbyteArray, coll: JObject) -> jint {
        let name_bytes = env.convert_byte_array(name).unwrap();
        let name_str = String::from_utf8(name_bytes).unwrap();
        self.conn
            .create_collation(&name_str, move |a, b| {
                let a_str = String::from_utf8_lossy(a);
                let b_str = String::from_utf8_lossy(b);
                coll.call_method(env, "xCompare", &[a_str, b_str])
                    .unwrap()
                    .i()
                    .unwrap()
            })
            .unwrap() as jint
    }

    fn destroy_collation_utf8(&self, env: JNIEnv, name: jbyteArray) -> jint {
        let name_bytes = env.convert_byte_array(name).unwrap();
        let name_str = String::from_utf8(name_bytes).unwrap();
        self.conn
            .create_collation(&name_str, |_, _| 0)
            .unwrap() as jint
    }

    fn limit(&self, id: jint, value: jint) -> jint {
        self.conn.limit(id as i32, value as i32) as jint
    }

    fn backup(
        &self,
        env: JNIEnv,
        db_name: jbyteArray,
        dest_file_name: jbyteArray,
        observer: JObject,
        sleep_time_millis: jint,
        n_timeouts: jint,
        pages_per_step: jint,
    ) -> jint {
        let db_name_bytes = env.convert_byte_array(db_name).unwrap();
        let db_name_str = String::from_utf8(db_name_bytes).unwrap();
        let dest_file_name_bytes = env.convert_byte_array(dest_file_name).unwrap();
        let dest_file_name_str = String::from_utf8(dest_file_name_bytes).unwrap();
        let observer = observer.into_inner();
        self.conn
            .backup(
                &db_name_str,
                &dest_file_name_str,
                |remaining, page_count| {
                    observer
                        .call_method(
                            env,
                            "progress",
                            "(II)V",
                            &[remaining.into(), page_count.into()],
                        )
                        .unwrap();
                },
                sleep_time_millis as u32,
                n_timeouts as u32,
                pages_per_step as u32,
            )
            .unwrap() as jint
    }

    fn restore(
        &self,
        env: JNIEnv,
        db_name: jbyteArray,
        source_file_name: jbyteArray,
        observer: JObject,
        sleep_time_millis: jint,
        n_timeouts: jint,
        pages_per_step: jint,
    ) -> jint {
        let db_name_bytes = env.convert_byte_array(db_name).unwrap();
        let db_name_str = String::from_utf8(db_name_bytes).unwrap();
        let source_file_name_bytes = env.convert_byte_array(source_file_name).unwrap();
        let source_file_name_str = String::from_utf8(source_file_name_bytes).unwrap();
        let observer = observer.into_inner();
        self.conn
            .restore(
                &db_name_str,
                &source_file_name_str,
                |remaining, page_count| {
                    observer
                        .call_method(
                            env,
                            "progress",
                            "(II)V",
                            &[remaining.into(), page_count.into()],
                        )
                        .unwrap();
                },
                sleep_time_millis as u32,
                n_timeouts as u32,
                pages_per_step as u32,
            )
            .unwrap() as jint
    }

    fn column_metadata(&self, env: JNIEnv, stmt: jlong) -> jobject {
        let stmt = unsafe { &*(stmt as *mut rusqlite::Statement) };
        let col_count = stmt.column_count();
        let array = env.new_object_array(col_count as i32, "java/lang/Object", JObject::null()).unwrap();
        for col in 0..col_count {
            let not_null = stmt.column_nullable(col).unwrap_or(false);
            let primary_key = stmt.column_is_primary_key(col).unwrap_or(false);
            let autoinc = stmt.column_is_autoincrement(col).unwrap_or(false);
            let col_data = env.new_boolean_array(3).unwrap();
            let col_data_raw = [not_null as jboolean, primary_key as jboolean, autoinc as jboolean];
            env.set_boolean_array_region(col_data, 0, &col_data_raw).unwrap();
            env.set_object_array_element(array, col as i32, col_data).unwrap();
        }
        array
    }

    fn set_commit_listener(&self, env: JNIEnv, enabled: jboolean) {
        if enabled != 0 {
            self.conn.set_commit_listener(Some(Box::new(move || {
                env.call_method(JObject::null(), "onCommit", "(Z)V", &[true.into()])
                    .unwrap();
                true
            })));
        } else {
            self.conn.set_commit_listener(None);
        }
    }

    fn set_update_listener(&self, env: JNIEnv, enabled: jboolean) {
        if enabled != 0 {
            self.conn.set_update_listener(Some(Box::new(move |_, _, _| {
                env.call_method(JObject::null(), "onUpdate", "(ILjava/lang/String;Ljava/lang/String;J)V", &[])
                    .unwrap();
            })));
        } else {
            self.conn.set_update_listener(None);
        }
    }

    fn register_progress_handler(
        &self,
        env: JNIEnv,
        vm_calls: jint,
        progress_handler: JObject,
    ) {
        let progress_handler = progress_handler.into_inner();
        self.conn
            .set_progress_handler(Some(Box::new(move || {
                progress_handler
                    .call_method(env, "progress", "()I", &[])
                    .unwrap()
                    .i()
                    .unwrap()
            })), vm_calls as i32);
    }

    fn clear_progress_handler(&self) {
        self.conn.set_progress_handler(None, 0);
    }

    fn serialize(&self, env: JNIEnv, schema: JString) -> jbyteArray {
        let schema_str = env.get_string(schema).unwrap().into();
        let serialized = self.conn.serialize(&schema_str).unwrap();
        env.byte_array_from_slice(&serialized).unwrap()
    }

    fn deserialize(&self, env: JNIEnv, schema: JString, buff: jbyteArray) {
        let schema_str = env.get_string(schema).unwrap().into();
        let buff_bytes = env.convert_byte_array(buff).unwrap();
        self.conn.deserialize(&schema_str, &buff_bytes).unwrap();
    }
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB__1open_1utf8(
    env: JNIEnv,
    obj: JObject,
    file: jbyteArray,
    flags: jint,
) {
    let mut db = NativeDB::new();
    db.open_utf8(env, file, flags);
    env.set_rust_field(obj, "nativeHandle", db).unwrap();
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB__1close(env: JNIEnv, obj: JObject) {
    let mut db: NativeDB = env.take_rust_field(obj, "nativeHandle").unwrap();
    db.close();
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB__1exec_1utf8(
    env: JNIEnv,
    obj: JObject,
    sql: jbyteArray,
) -> jint {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.exec_utf8(env, sql)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_shared_1cache(
    env: JNIEnv,
    obj: JObject,
    enable: jboolean,
) -> jint {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.shared_cache(enable)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_enable_1load_1extension(
    env: JNIEnv,
    obj: JObject,
    enable: jboolean,
) -> jint {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.enable_load_extension(enable)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_interrupt(env: JNIEnv, obj: JObject) {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.interrupt();
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_busy_1timeout(
    env: JNIEnv,
    obj: JObject,
    ms: jint,
) {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.busy_timeout(ms);
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_prepare_1utf8(
    env: JNIEnv,
    obj: JObject,
    sql: jbyteArray,
) -> jlong {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.prepare_utf8(env, sql)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_errmsg_1utf8(
    env: JNIEnv,
    obj: JObject,
) -> jobject {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.errmsg_utf8(env)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_libversion_1utf8(
    env: JNIEnv,
    obj: JObject,
) -> jobject {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.libversion_utf8(env)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_changes(env: JNIEnv, obj: JObject) -> jlong {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.changes()
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_total_1changes(
    env: JNIEnv,
    obj: JObject,
) -> jlong {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.total_changes()
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_finalize(
    env: JNIEnv,
    obj: JObject,
    stmt: jlong,
) -> jint {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.finalize(stmt)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_step(env: JNIEnv, obj: JObject, stmt: jlong) -> jint {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.step(stmt)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_reset(env: JNIEnv, obj: JObject, stmt: jlong) -> jint {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.reset(stmt)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_clear_1bindings(env: JNIEnv, obj: JObject, stmt: jlong) -> jint {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.clear_bindings(stmt)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_bind_1parameter_1count(env: JNIEnv, obj: JObject, stmt: jlong) -> jint {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.bind_parameter_count(stmt)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_column_1count(env: JNIEnv, obj: JObject, stmt: jlong) -> jint {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.column_count(stmt)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_column_1type(env: JNIEnv, obj: JObject, stmt: jlong, col: jint) -> jint {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.column_type(stmt, col)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_column_1decltype_1utf8(env: JNIEnv, obj: JObject, stmt: jlong, col: jint) -> jobject {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.column_decltype_utf8(env, stmt, col)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_column_1table_1name_1utf8(env: JNIEnv, obj: JObject, stmt: jlong, col: jint) -> jobject {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.column_table_name_utf8(env, stmt, col)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_column_1name_1utf8(env: JNIEnv, obj: JObject, stmt: jlong, col: jint) -> jobject {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.column_name_utf8(env, stmt, col)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_column_1text_1utf8(env: JNIEnv, obj: JObject, stmt: jlong, col: jint) -> jobject {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.column_text_utf8(env, stmt, col)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_column_1blob(env: JNIEnv, obj: JObject, stmt: jlong, col: jint) -> jbyteArray {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.column_blob(env, stmt, col)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_column_1double(env: JNIEnv, obj: JObject, stmt: jlong, col: jint) -> jdouble {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.column_double(stmt, col)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_column_1long(env: JNIEnv, obj: JObject, stmt: jlong, col: jint) -> jlong {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.column_long(stmt, col)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_column_1int(env: JNIEnv, obj: JObject, stmt: jlong, col: jint) -> jint {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.column_int(stmt, col)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_bind_1null(env: JNIEnv, obj: JObject, stmt: jlong, pos: jint) -> jint {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.bind_null(stmt, pos)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_bind_1int(env: JNIEnv, obj: JObject, stmt: jlong, pos: jint, v: jint) -> jint {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.bind_int(stmt, pos, v)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_bind_1long(env: JNIEnv, obj: JObject, stmt: jlong, pos: jint, v: jlong) -> jint {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.bind_long(stmt, pos, v)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_bind_1double(env: JNIEnv, obj: JObject, stmt: jlong, pos: jint, v: jdouble) -> jint {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.bind_double(stmt, pos, v)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_bind_1text_1utf8(env: JNIEnv, obj: JObject, stmt: jlong, pos: jint, v: jbyteArray) -> jint {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.bind_text_utf8(env, stmt, pos, v)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_bind_1blob(env: JNIEnv, obj: JObject, stmt: jlong, pos: jint, v: jbyteArray) -> jint {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.bind_blob(env, stmt, pos, v)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_result_1null(env: JNIEnv, obj: JObject, context: jlong) {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.result_null(context)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_result_1text_1utf8(env: JNIEnv, obj: JObject, context: jlong, val: jbyteArray) {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.result_text_utf8(env, context, val)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_result_1blob(env: JNIEnv, obj: JObject, context: jlong, val: jbyteArray) {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.result_blob(env, context, val)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_result_1double(env: JNIEnv, obj: JObject, context: jlong, val: jdouble) {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.result_double(context, val)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_result_1long(env: JNIEnv, obj: JObject, context: jlong, val: jlong) {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.result_long(context, val)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_result_1int(env: JNIEnv, obj: JObject, context: jlong, val: jint) {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.result_int(context, val)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_result_1error_1utf8(env: JNIEnv, obj: JObject, context: jlong, err: jbyteArray) {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.result_error_utf8(env, context, err)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_value_1text_1utf8(env: JNIEnv, obj: JObject, f: JObject, arg: jint) -> jobject {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.value_text_utf8(env, f, arg)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_value_1blob(env: JNIEnv, obj: JObject, f: JObject, arg: jint) -> jbyteArray {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.value_blob(env, f, arg)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_value_1double(env: JNIEnv, obj: JObject, f: JObject, arg: jint) -> jdouble {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.value_double(f, arg)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_value_1long(env: JNIEnv, obj: JObject, f: JObject, arg: jint) -> jlong {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.value_long(f, arg)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_value_1int(env: JNIEnv, obj: JObject, f: JObject, arg: jint) -> jint {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.value_int(f, arg)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_value_1type(env: JNIEnv, obj: JObject, f: JObject, arg: jint) -> jint {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.value_type(f, arg)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_create_1function_1utf8(env: JNIEnv, obj: JObject, name: jbyteArray, func: JObject, n_args: jint, flags: jint) -> jint {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.create_function_utf8(env, name, func, n_args, flags)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_destroy_1function_1utf8(env: JNIEnv, obj: JObject, name: jbyteArray) -> jint {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.destroy_function_utf8(env, name)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_create_1collation_1utf8(env: JNIEnv, obj: JObject, name: jbyteArray, coll: JObject) -> jint {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.create_collation_utf8(env, name, coll)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_destroy_1collation_1utf8(env: JNIEnv, obj: JObject, name: jbyteArray) -> jint {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.destroy_collation_utf8(env, name)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_limit(env: JNIEnv, obj: JObject, id: jint, value: jint) -> jint {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.limit(id, value)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_backup(env: JNIEnv, obj: JObject, db_name: jbyteArray, dest_file_name: jbyteArray, observer: JObject, sleep_time_millis: jint, n_timeouts: jint, pages_per_step: jint) -> jint {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.backup(env, db_name, dest_file_name, observer, sleep_time_millis, n_timeouts, pages_per_step)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_restore(env: JNIEnv, obj: JObject, db_name: jbyteArray, source_file_name: jbyteArray, observer: JObject, sleep_time_millis: jint, n_timeouts: jint, pages_per_step: jint) -> jint {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.restore(env, db_name, source_file_name, observer, sleep_time_millis, n_timeouts, pages_per_step)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_column_1metadata(env: JNIEnv, obj: JObject, stmt: jlong) -> jobject {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.column_metadata(env, stmt)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_set_1commit_1listener(env: JNIEnv, obj: JObject, enabled: jboolean) {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.set_commit_listener(env, enabled)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_set_1update_1listener(env: JNIEnv, obj: JObject, enabled: jboolean) {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.set_update_listener(env, enabled)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_register_1progress_1handler(env: JNIEnv, obj: JObject, vm_calls: jint, progress_handler: JObject) {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.register_progress_handler(env, vm_calls, progress_handler)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_clear_1progress_1handler(env: JNIEnv, obj: JObject) {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.clear_progress_handler()
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_serialize(env: JNIEnv, obj: JObject, schema: JString) -> jbyteArray {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.serialize(env, schema)
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_deserialize(env: JNIEnv, obj: JObject, schema: JString, buff: jbyteArray) {
    let db: &NativeDB = env.get_rust_field(obj, "nativeHandle").unwrap();
    db.deserialize(env, schema, buff)
}

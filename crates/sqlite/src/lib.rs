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

use rusqlite::{Connection, Result, NO_PARAMS};
use jni::JNIEnv;
use jni::objects::{JClass, JObject, JString};
use jni::sys::{jint, jlong};

pub struct NativeDB {
    conn: Connection,
}

impl NativeDB {
    pub fn new() -> Self {
        NativeDB {
            conn: Connection::open_in_memory().unwrap(),
        }
    }

    pub fn open(&mut self, path: &str) -> Result<()> {
        self.conn = Connection::open(path)?;
        Ok(())
    }

    pub fn close(&mut self) -> Result<()> {
        self.conn.close().map_err(|(_, err)| err)
    }

    pub fn exec(&self, sql: &str) -> Result<()> {
        self.conn.execute_batch(sql)
    }

    pub fn changes(&self) -> i32 {
        self.conn.changes() as i32
    }

    pub fn total_changes(&self) -> i32 {
        self.conn.total_changes() as i32
    }
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB__1open_1utf8(
    env: JNIEnv,
    _class: JClass,
    path: JString,
) -> jlong {
    let path: String = env.get_string(path).expect("Couldn't get java string!").into();
    let mut db = NativeDB::new();
    db.open(&path).expect("Failed to open database!");
    Box::into_raw(Box::new(db)) as jlong
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB__1close(
    _env: JNIEnv,
    _class: JClass,
    db_ptr: jlong,
) {
    let db = unsafe { &mut *(db_ptr as *mut NativeDB) };
    db.close().expect("Failed to close database!");
    unsafe {
        Box::from_raw(db_ptr as *mut NativeDB);
    }
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB__1exec_1utf8(
    env: JNIEnv,
    _class: JClass,
    db_ptr: jlong,
    sql: JString,
) -> jint {
    let sql: String = env.get_string(sql).expect("Couldn't get java string!").into();
    let db = unsafe { &*(db_ptr as *mut NativeDB) };
    match db.exec(&sql) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_changes(
    _env: JNIEnv,
    _class: JClass,
    db_ptr: jlong,
) -> jint {
    let db = unsafe { &*(db_ptr as *mut NativeDB) };
    db.changes()
}

#[no_mangle]
pub extern "system" fn Java_org_sqlite_core_NativeDB_total_1changes(
    _env: JNIEnv,
    _class: JClass,
    db_ptr: jlong,
) -> jint {
    let db = unsafe { &*(db_ptr as *mut NativeDB) };
    db.total_changes()
}

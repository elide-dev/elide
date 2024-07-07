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
 * Copyright (c) 2007 David Crawshaw <david@zentus.com>
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

#include <jni.h>
#include "sqlite3.h"

/* Header for class org_sqlite_core_NativeDB */

#ifndef _Included_org_sqlite_core_NativeDB
#define _Included_org_sqlite_core_NativeDB
#ifdef __cplusplus
extern "C" {
#endif
#undef org_sqlite_core_NativeDB_DEFAULT_BACKUP_BUSY_SLEEP_TIME_MILLIS
#define org_sqlite_core_NativeDB_DEFAULT_BACKUP_BUSY_SLEEP_TIME_MILLIS 100L
#undef org_sqlite_core_NativeDB_DEFAULT_BACKUP_NUM_BUSY_BEFORE_FAIL
#define org_sqlite_core_NativeDB_DEFAULT_BACKUP_NUM_BUSY_BEFORE_FAIL 3L
#undef org_sqlite_core_NativeDB_DEFAULT_PAGES_PER_BACKUP_STEP
#define org_sqlite_core_NativeDB_DEFAULT_PAGES_PER_BACKUP_STEP 100L
/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    _open_utf8
 * Signature: ([BI)V
 */
JNIEXPORT void JNICALL Java_org_sqlite_core_NativeDB__1open_1utf8
  (JNIEnv *, jobject, jbyteArray, jint);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    _close
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_sqlite_core_NativeDB__1close
  (JNIEnv *, jobject);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    _exec_utf8
 * Signature: ([B)I
 */
JNIEXPORT jint JNICALL Java_org_sqlite_core_NativeDB__1exec_1utf8
  (JNIEnv *, jobject, jbyteArray);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    shared_cache
 * Signature: (Z)I
 */
JNIEXPORT jint JNICALL Java_org_sqlite_core_NativeDB_shared_1cache
  (JNIEnv *, jobject, jboolean);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    enable_load_extension
 * Signature: (Z)I
 */
JNIEXPORT jint JNICALL Java_org_sqlite_core_NativeDB_enable_1load_1extension
  (JNIEnv *, jobject, jboolean);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    interrupt
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_sqlite_core_NativeDB_interrupt
  (JNIEnv *, jobject);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    busy_timeout
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_sqlite_core_NativeDB_busy_1timeout
  (JNIEnv *, jobject, jint);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    busy_handler
 * Signature: (Lorg/sqlite/BusyHandler;)V
 */
JNIEXPORT void JNICALL Java_org_sqlite_core_NativeDB_busy_1handler
  (JNIEnv *, jobject, jobject);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    prepare_utf8
 * Signature: ([B)J
 */
JNIEXPORT jlong JNICALL Java_org_sqlite_core_NativeDB_prepare_1utf8
  (JNIEnv *, jobject, jbyteArray);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    errmsg_utf8
 * Signature: ()Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_org_sqlite_core_NativeDB_errmsg_1utf8
  (JNIEnv *, jobject);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    libversion_utf8
 * Signature: ()Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_org_sqlite_core_NativeDB_libversion_1utf8
  (JNIEnv *, jobject);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    changes
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_org_sqlite_core_NativeDB_changes
  (JNIEnv *, jobject);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    total_changes
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_org_sqlite_core_NativeDB_total_1changes
  (JNIEnv *, jobject);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    finalize
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_sqlite_core_NativeDB_finalize
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    step
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_sqlite_core_NativeDB_step
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    reset
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_sqlite_core_NativeDB_reset
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    clear_bindings
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_sqlite_core_NativeDB_clear_1bindings
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    bind_parameter_count
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_sqlite_core_NativeDB_bind_1parameter_1count
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    column_count
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_sqlite_core_NativeDB_column_1count
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    column_type
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_org_sqlite_core_NativeDB_column_1type
  (JNIEnv *, jobject, jlong, jint);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    column_decltype_utf8
 * Signature: (JI)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_org_sqlite_core_NativeDB_column_1decltype_1utf8
  (JNIEnv *, jobject, jlong, jint);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    column_table_name_utf8
 * Signature: (JI)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_org_sqlite_core_NativeDB_column_1table_1name_1utf8
  (JNIEnv *, jobject, jlong, jint);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    column_name_utf8
 * Signature: (JI)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_org_sqlite_core_NativeDB_column_1name_1utf8
  (JNIEnv *, jobject, jlong, jint);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    column_text_utf8
 * Signature: (JI)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_org_sqlite_core_NativeDB_column_1text_1utf8
  (JNIEnv *, jobject, jlong, jint);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    column_blob
 * Signature: (JI)[B
 */
JNIEXPORT jbyteArray JNICALL Java_org_sqlite_core_NativeDB_column_1blob
  (JNIEnv *, jobject, jlong, jint);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    column_double
 * Signature: (JI)D
 */
JNIEXPORT jdouble JNICALL Java_org_sqlite_core_NativeDB_column_1double
  (JNIEnv *, jobject, jlong, jint);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    column_long
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_org_sqlite_core_NativeDB_column_1long
  (JNIEnv *, jobject, jlong, jint);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    column_int
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_org_sqlite_core_NativeDB_column_1int
  (JNIEnv *, jobject, jlong, jint);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    bind_null
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_org_sqlite_core_NativeDB_bind_1null
  (JNIEnv *, jobject, jlong, jint);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    bind_int
 * Signature: (JII)I
 */
JNIEXPORT jint JNICALL Java_org_sqlite_core_NativeDB_bind_1int
  (JNIEnv *, jobject, jlong, jint, jint);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    bind_long
 * Signature: (JIJ)I
 */
JNIEXPORT jint JNICALL Java_org_sqlite_core_NativeDB_bind_1long
  (JNIEnv *, jobject, jlong, jint, jlong);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    bind_double
 * Signature: (JID)I
 */
JNIEXPORT jint JNICALL Java_org_sqlite_core_NativeDB_bind_1double
  (JNIEnv *, jobject, jlong, jint, jdouble);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    bind_text_utf8
 * Signature: (JI[B)I
 */
JNIEXPORT jint JNICALL Java_org_sqlite_core_NativeDB_bind_1text_1utf8
  (JNIEnv *, jobject, jlong, jint, jbyteArray);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    bind_blob
 * Signature: (JI[B)I
 */
JNIEXPORT jint JNICALL Java_org_sqlite_core_NativeDB_bind_1blob
  (JNIEnv *, jobject, jlong, jint, jbyteArray);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    result_null
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_sqlite_core_NativeDB_result_1null
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    result_text_utf8
 * Signature: (J[B)V
 */
JNIEXPORT void JNICALL Java_org_sqlite_core_NativeDB_result_1text_1utf8
  (JNIEnv *, jobject, jlong, jbyteArray);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    result_blob
 * Signature: (J[B)V
 */
JNIEXPORT void JNICALL Java_org_sqlite_core_NativeDB_result_1blob
  (JNIEnv *, jobject, jlong, jbyteArray);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    result_double
 * Signature: (JD)V
 */
JNIEXPORT void JNICALL Java_org_sqlite_core_NativeDB_result_1double
  (JNIEnv *, jobject, jlong, jdouble);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    result_long
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_org_sqlite_core_NativeDB_result_1long
  (JNIEnv *, jobject, jlong, jlong);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    result_int
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_org_sqlite_core_NativeDB_result_1int
  (JNIEnv *, jobject, jlong, jint);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    result_error_utf8
 * Signature: (J[B)V
 */
JNIEXPORT void JNICALL Java_org_sqlite_core_NativeDB_result_1error_1utf8
  (JNIEnv *, jobject, jlong, jbyteArray);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    value_text_utf8
 * Signature: (Lorg/sqlite/Function;I)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_org_sqlite_core_NativeDB_value_1text_1utf8
  (JNIEnv *, jobject, jobject, jint);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    value_blob
 * Signature: (Lorg/sqlite/Function;I)[B
 */
JNIEXPORT jbyteArray JNICALL Java_org_sqlite_core_NativeDB_value_1blob
  (JNIEnv *, jobject, jobject, jint);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    value_double
 * Signature: (Lorg/sqlite/Function;I)D
 */
JNIEXPORT jdouble JNICALL Java_org_sqlite_core_NativeDB_value_1double
  (JNIEnv *, jobject, jobject, jint);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    value_long
 * Signature: (Lorg/sqlite/Function;I)J
 */
JNIEXPORT jlong JNICALL Java_org_sqlite_core_NativeDB_value_1long
  (JNIEnv *, jobject, jobject, jint);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    value_int
 * Signature: (Lorg/sqlite/Function;I)I
 */
JNIEXPORT jint JNICALL Java_org_sqlite_core_NativeDB_value_1int
  (JNIEnv *, jobject, jobject, jint);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    value_type
 * Signature: (Lorg/sqlite/Function;I)I
 */
JNIEXPORT jint JNICALL Java_org_sqlite_core_NativeDB_value_1type
  (JNIEnv *, jobject, jobject, jint);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    create_function_utf8
 * Signature: ([BLorg/sqlite/Function;II)I
 */
JNIEXPORT jint JNICALL Java_org_sqlite_core_NativeDB_create_1function_1utf8
  (JNIEnv *, jobject, jbyteArray, jobject, jint, jint);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    destroy_function_utf8
 * Signature: ([B)I
 */
JNIEXPORT jint JNICALL Java_org_sqlite_core_NativeDB_destroy_1function_1utf8
  (JNIEnv *, jobject, jbyteArray);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    create_collation_utf8
 * Signature: ([BLorg/sqlite/Collation;)I
 */
JNIEXPORT jint JNICALL Java_org_sqlite_core_NativeDB_create_1collation_1utf8
  (JNIEnv *, jobject, jbyteArray, jobject);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    destroy_collation_utf8
 * Signature: ([B)I
 */
JNIEXPORT jint JNICALL Java_org_sqlite_core_NativeDB_destroy_1collation_1utf8
  (JNIEnv *, jobject, jbyteArray);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    limit
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_org_sqlite_core_NativeDB_limit
  (JNIEnv *, jobject, jint, jint);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    backup
 * Signature: ([B[BLorg/sqlite/core/DB/ProgressObserver;III)I
 */
JNIEXPORT jint JNICALL Java_org_sqlite_core_NativeDB_backup
  (JNIEnv *, jobject, jbyteArray, jbyteArray, jobject, jint, jint, jint);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    restore
 * Signature: ([B[BLorg/sqlite/core/DB/ProgressObserver;III)I
 */
JNIEXPORT jint JNICALL Java_org_sqlite_core_NativeDB_restore
  (JNIEnv *, jobject, jbyteArray, jbyteArray, jobject, jint, jint, jint);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    column_metadata
 * Signature: (J)[[Z
 */
JNIEXPORT jobjectArray JNICALL Java_org_sqlite_core_NativeDB_column_1metadata
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    set_commit_listener
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_org_sqlite_core_NativeDB_set_1commit_1listener
  (JNIEnv *, jobject, jboolean);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    set_update_listener
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_org_sqlite_core_NativeDB_set_1update_1listener
  (JNIEnv *, jobject, jboolean);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    register_progress_handler
 * Signature: (ILorg/sqlite/ProgressHandler;)V
 */
JNIEXPORT void JNICALL Java_org_sqlite_core_NativeDB_register_1progress_1handler
  (JNIEnv *, jobject, jint, jobject);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    clear_progress_handler
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_sqlite_core_NativeDB_clear_1progress_1handler
  (JNIEnv *, jobject);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    serialize
 * Signature: (Ljava/lang/String;)[B
 */
JNIEXPORT jbyteArray JNICALL Java_org_sqlite_core_NativeDB_serialize
  (JNIEnv *, jobject, jstring);

/*
 * Class:     org_sqlite_core_NativeDB
 * Method:    deserialize
 * Signature: (Ljava/lang/String;[B)V
 */
JNIEXPORT void JNICALL Java_org_sqlite_core_NativeDB_deserialize
  (JNIEnv *, jobject, jstring, jbyteArray);

#ifdef __cplusplus
}
#endif
#endif

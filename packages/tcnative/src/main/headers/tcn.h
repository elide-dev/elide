/*
 * Copyright 2016 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef TCN_H
#define TCN_H

// Start includes
#include <jni.h>
#include "netty_jni_util.h"

#include "apr.h"
#include "apr_pools.h"

#ifndef APR_HAS_THREADS
#error "Missing APR_HAS_THREADS support from APR."
#endif

#include <stdio.h>
#include <stdlib.h>
#if defined(_WIN32) && !defined(__CYGWIN__)
#include <process.h>
#else
#include <unistd.h>
#endif

#if defined(_DEBUG) || defined(DEBUG)
#include <assert.h>
#define TCN_ASSERT(x)  assert((x))
#else
#define TCN_ASSERT(x) (void)0
#endif
// End includes

#ifdef _WIN32
#define LLT(X) (X)
#else
#define LLT(X) ((long)(X))
#endif
#define P2J(P)          ((jlong)LLT(P))
#define J2P(P, T)       ((T)LLT((jlong)P))
/* On stack buffer size */
#define TCN_BUFFER_SZ   8192
#define TCN_STDARGS     JNIEnv *e, jobject o

#define STR(V) #V

#define TCN_FUNCTION_NAME(CL, FN)  \
    netty_internal_tcnative_##CL##_##FN

#define TCN_IMPLEMENT_CALL(RT, CL, FN)  \
    static RT TCN_FUNCTION_NAME(CL, FN)

#define TCN_METHOD_TABLE_ENTRY(ME, SI, CL) \
    STR(ME), STR(SI), (void *) TCN_FUNCTION_NAME(CL, ME)

/* Private helper functions */
void            tcn_Throw(JNIEnv *, const char *, ...);
void            tcn_ThrowException(JNIEnv *, const char *);
void            tcn_ThrowNullPointerException(JNIEnv *, const char *);
void            tcn_ThrowIllegalArgumentException(JNIEnv *, const char *);
void            tcn_ThrowAPRException(JNIEnv *, apr_status_t);
void            tcn_throwOutOfMemoryError(JNIEnv *, const char *);

jstring         tcn_new_string(JNIEnv *, const char *);
jstring         tcn_new_stringn(JNIEnv *, const char *, size_t);

#define J2S(V)  c##V
#define J2L(V)  p##V

#define TCN_ALLOC_CSTRING(V)     \
    const char *c##V = V ? (const char *)((*e)->GetStringUTFChars(e, V, JNI_FALSE)) : NULL

#define TCN_FREE_CSTRING(V)      \
    if (c##V) (*e)->ReleaseStringUTFChars(e, V, c##V)

#define AJP_TO_JSTRING(V)   (*e)->NewStringUTF((e), (V))

#define TCN_CHECK_NULL(V, M, R)                      \
    NETTY_JNI_UTIL_BEGIN_MACRO                       \
        if (V == NULL) {                             \
            tcn_ThrowNullPointerException(e, #M);    \
            return R;                                \
        }                                            \
    NETTY_JNI_UTIL_END_MACRO

#define TCN_FREE_JSTRING(V)      \
    NETTY_JNI_UTIL_BEGIN_MACRO   \
        if (c##V)                \
            free(c##V);          \
    NETTY_JNI_UTIL_END_MACRO

#define TCN_THROW_IF_ERR(x, r)                  \
    NETTY_JNI_UTIL_BEGIN_MACRO                  \
        apr_status_t R = (x);                   \
        if (R != APR_SUCCESS) {                 \
            tcn_ThrowAPRException(e, R);        \
            (r) = 0;                            \
            goto cleanup;                       \
        }                                       \
    NETTY_JNI_UTIL_END_MACRO

#define TCN_MIN(a, b) ((a) < (b) ? (a) : (b))

#define TCN_REASSIGN(V1, V2)                  \
    NETTY_JNI_UTIL_BEGIN_MACRO                \
        free(V1);                             \
        V1 = V2;                              \
        V2 = NULL;                            \
    NETTY_JNI_UTIL_END_MACRO


/* Return global String class
 */
jclass tcn_get_string_class(void);

jclass tcn_get_byte_array_class();

/* Get current thread JNIEnv
 */
jint tcn_get_java_env(JNIEnv **);

#endif /* TCN_H */

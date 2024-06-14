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

#include "tcn.h"
#include "apr_strings.h"

static jclass exceptionClass;
static jclass nullPointerExceptionClass;
static jclass illegalArgumentExceptionClass;
static jclass oomeClass;


/*
 * Convenience function to help throw an java.lang.Exception.
 */
void tcn_ThrowException(JNIEnv *env, const char *msg)
{
    (*env)->ThrowNew(env, exceptionClass, msg);
}

void tcn_ThrowNullPointerException(JNIEnv *env, const char *msg)
{
    (*env)->ThrowNew(env, nullPointerExceptionClass, msg);
}

void tcn_ThrowIllegalArgumentException(JNIEnv *env, const char *msg)
{
    (*env)->ThrowNew(env, illegalArgumentExceptionClass, msg);
}

void tcn_Throw(JNIEnv *env, const char *fmt, ...)
{
    char msg[TCN_BUFFER_SZ] = {'\0'};
    va_list ap;

    va_start(ap, fmt);
    apr_vsnprintf(msg, TCN_BUFFER_SZ, fmt, ap);
    tcn_ThrowException(env, msg);
    va_end(ap);
}

void tcn_ThrowAPRException(JNIEnv *e, apr_status_t err)
{
    char serr[512] = {0};

    apr_strerror(err, serr, 512);
    tcn_ThrowException(e, serr);
}

void tcn_throwOutOfMemoryError(JNIEnv* env, const char *msg)
{
    (*env)->ThrowNew(env, oomeClass, msg);
}

// IMPORTANT: If you add any NETTY_JNI_UTIL_LOAD_CLASS or NETTY_JNI_UTIL_FIND_CLASS calls you also need to update
//            Library to reflect that.
jint netty_internal_tcnative_Error_JNI_OnLoad(JNIEnv* env, const char* packagePrefix) {
    NETTY_JNI_UTIL_LOAD_CLASS(env, exceptionClass, "java/lang/Exception", error);
    NETTY_JNI_UTIL_LOAD_CLASS(env, nullPointerExceptionClass, "java/lang/NullPointerException", error);
    NETTY_JNI_UTIL_LOAD_CLASS(env, illegalArgumentExceptionClass, "java/lang/IllegalArgumentException", error);
    NETTY_JNI_UTIL_LOAD_CLASS(env, oomeClass, "java/lang/OutOfMemoryError", error);
    return NETTY_JNI_UTIL_JNI_VERSION;
error:
    return JNI_ERR;
}

void netty_internal_tcnative_Error_JNI_OnUnLoad(JNIEnv* env, const char* packagePrefix) {
     NETTY_JNI_UTIL_UNLOAD_CLASS(env, exceptionClass);
     NETTY_JNI_UTIL_UNLOAD_CLASS(env, nullPointerExceptionClass);
     NETTY_JNI_UTIL_UNLOAD_CLASS(env, illegalArgumentExceptionClass);
     NETTY_JNI_UTIL_UNLOAD_CLASS(env, oomeClass);
 }

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

#define LIBRARY_CLASSNAME "io/netty/internal/tcnative/Library"

#ifdef _WIN32
#define MAX_DLL_PATH_LEN 2048
#endif

#ifdef TCN_BUILD_STATIC
#define NETTY_JNI_UTIL_BUILD_STATIC 
#endif

#include "tcn.h"
#include "apr_version.h"
#include "apr_atomic.h"
#include "apr_strings.h"
#include "bb.h"
#include "native_constants.h"
#include "ssl.h"
#include "sslcontext.h"
#include "sslsession.h"
#include "error.h"

apr_pool_t *tcn_global_pool = NULL;
static JavaVM     *tcn_global_vm = NULL;

static jclass    jString_class;
static jmethodID jString_init;
static jmethodID jString_getBytes;
static jclass    byteArrayClass;
static char const* staticPackagePrefix = NULL;

jstring tcn_new_stringn(JNIEnv *env, const char *str, size_t l)
{
    jstring result = NULL;
    jbyteArray bytes = 0;

    if (!str) {
        return NULL;
    }
    if ((*env)->EnsureLocalCapacity(env, 2) < 0) {
        return NULL; /* out of memory error */
    }
    bytes = (*env)->NewByteArray(env, l);
    if (bytes != NULL) {
        (*env)->SetByteArrayRegion(env, bytes, 0, l, (jbyte *)str);
        result = (*env)->NewObject(env, jString_class, jString_init, bytes);
        NETTY_JNI_UTIL_DELETE_LOCAL(env, bytes);
        return result;
    } /* else fall through */
    return NULL;
}

jstring tcn_new_string(JNIEnv *env, const char *str)
{
    if (!str) {
        return NULL;
    }
    return (*env)->NewStringUTF(env, str);
}

TCN_IMPLEMENT_CALL(jboolean, Library, initialize0)(TCN_STDARGS)
{

    if (!tcn_global_pool) {
        apr_initialize();
        if (apr_pool_create(&tcn_global_pool, NULL) != APR_SUCCESS) {
            return JNI_FALSE;
        }
        apr_atomic_init(tcn_global_pool);
    }
    return JNI_TRUE;
}

TCN_IMPLEMENT_CALL(jint, Library, aprMajorVersion)(TCN_STDARGS)
{
    apr_version_t apv;

    apr_version(&apv);
    return apv.major;
}

TCN_IMPLEMENT_CALL(jstring, Library, aprVersionString)(TCN_STDARGS)
{
    return AJP_TO_JSTRING(apr_version_string());
}

TCN_IMPLEMENT_CALL(jboolean, Library, aprHasThreads)(TCN_STDARGS)
{
#if APR_HAS_THREADS
    return JNI_TRUE;
#else
    return JNI_FALSE;
#endif
}

jclass tcn_get_string_class()
{
    return jString_class;
}

jclass tcn_get_byte_array_class()
{
    return byteArrayClass;
}

jint tcn_get_java_env(JNIEnv **env)
{
    return (*tcn_global_vm)->GetEnv(tcn_global_vm, (void **)env, NETTY_JNI_UTIL_JNI_VERSION);
}

// JNI Method Registration Table Begin
static const JNINativeMethod method_table[] = {
  { TCN_METHOD_TABLE_ENTRY(initialize0, ()Z, Library) },
  { TCN_METHOD_TABLE_ENTRY(aprMajorVersion, ()I, Library) },
  { TCN_METHOD_TABLE_ENTRY(aprVersionString, ()Ljava/lang/String;, Library) },
  { TCN_METHOD_TABLE_ENTRY(aprHasThreads, ()Z, Library) },
};

static const jint method_table_size = sizeof(method_table) / sizeof(method_table[0]);
// JNI Method Registration Table End

// IMPORTANT: If you add any NETTY_JNI_UTIL_LOAD_CLASS or NETTY_JNI_UTIL_FIND_CLASS calls you also need to update
//            Library to reflect that.
static jint netty_internal_tcnative_Library_JNI_OnLoad(JNIEnv* env, char const* packagePrefix) {
    int errorOnLoadCalled = 0;
    int bufferOnLoadCalled = 0;
    int jniMethodsOnLoadCalled = 0;
    int sessionOnLoadCalled = 0;
    int sslOnLoadCalled = 0;
    int contextOnLoadCalled = 0;

    if (netty_jni_util_register_natives(env, packagePrefix, LIBRARY_CLASSNAME, method_table, method_table_size) != 0) {
        goto error;
    }

    // Load all c modules that we depend upon
    if (netty_internal_tcnative_Error_JNI_OnLoad(env, packagePrefix) == JNI_ERR) {
        goto error;
    }
    errorOnLoadCalled = 1;

    if (netty_internal_tcnative_Buffer_JNI_OnLoad(env, packagePrefix) == JNI_ERR) {
        goto error;
    }
    bufferOnLoadCalled = 1;

    if (netty_internal_tcnative_NativeStaticallyReferencedJniMethods_JNI_OnLoad(env, packagePrefix) == JNI_ERR) {
        goto error;
    }
    jniMethodsOnLoadCalled = 1;

    if (netty_internal_tcnative_SSL_JNI_OnLoad(env, packagePrefix) == JNI_ERR) {
        goto error;
    }
    sslOnLoadCalled = 1;

    if (netty_internal_tcnative_SSLContext_JNI_OnLoad(env, packagePrefix) == JNI_ERR) {
        goto error;
    }
    contextOnLoadCalled = 1;

    if (netty_internal_tcnative_SSLSession_JNI_OnLoad(env, packagePrefix) == JNI_ERR) {
        goto error;
    }
    sessionOnLoadCalled = 1;

    apr_version_t apv;
    int apvn;

    /* Before doing anything else check if we have a valid
     * APR version.
     */
    apr_version(&apv);
    apvn = apv.major * 1000 + apv.minor * 100 + apv.patch;
    if (apvn < 1201) {
        tcn_Throw(env, "Unsupported APR version (%s)",
                  apr_version_string());
        goto error;
    }


    /* Initialize global java.lang.String class */
    NETTY_JNI_UTIL_LOAD_CLASS(env, jString_class, "java/lang/String", error);

    NETTY_JNI_UTIL_GET_METHOD(env, jString_class, jString_init,
                   "<init>", "([B)V", error);
    NETTY_JNI_UTIL_GET_METHOD(env, jString_class, jString_getBytes,
                   "getBytes", "()[B", error);

    NETTY_JNI_UTIL_LOAD_CLASS(env, byteArrayClass, "[B", error);
    staticPackagePrefix = packagePrefix;
    return NETTY_JNI_UTIL_JNI_VERSION;
error:
    if (tcn_global_pool != NULL) {
        NETTY_JNI_UTIL_UNLOAD_CLASS(env, jString_class);
        apr_terminate();
        tcn_global_pool = NULL;
    }

    NETTY_JNI_UTIL_UNLOAD_CLASS(env, byteArrayClass);

    netty_jni_util_unregister_natives(env, packagePrefix, LIBRARY_CLASSNAME);

    // Call unload methods if needed to ensure we correctly release any resources.
    if (errorOnLoadCalled == 1) {
        netty_internal_tcnative_Error_JNI_OnUnLoad(env, packagePrefix);
    }
    if (bufferOnLoadCalled == 1) {
        netty_internal_tcnative_Buffer_JNI_OnUnLoad(env, packagePrefix);
    }
    if (jniMethodsOnLoadCalled == 1) {
        netty_internal_tcnative_NativeStaticallyReferencedJniMethods_JNI_OnUnLoad(env, packagePrefix);
    }
    if (sslOnLoadCalled == 1) {
        netty_internal_tcnative_SSL_JNI_OnUnLoad(env, packagePrefix);
    }
    if (contextOnLoadCalled == 1) {
        netty_internal_tcnative_SSLContext_JNI_OnUnLoad(env, packagePrefix);
    }
    if (sessionOnLoadCalled == 1) {
        netty_internal_tcnative_SSLSession_JNI_OnUnLoad(env, packagePrefix);
    }
    return JNI_ERR;
}

static void netty_internal_tcnative_Library_JNI_OnUnload(JNIEnv* env) {
    if (tcn_global_pool != NULL) {
        NETTY_JNI_UTIL_UNLOAD_CLASS(env, jString_class);
        apr_terminate();
        tcn_global_pool = NULL;
    }

    NETTY_JNI_UTIL_UNLOAD_CLASS(env, byteArrayClass);
    netty_internal_tcnative_Error_JNI_OnUnLoad(env, staticPackagePrefix);
    netty_internal_tcnative_Buffer_JNI_OnUnLoad(env, staticPackagePrefix);
    netty_internal_tcnative_NativeStaticallyReferencedJniMethods_JNI_OnUnLoad(env, staticPackagePrefix);
    netty_internal_tcnative_SSL_JNI_OnUnLoad(env, staticPackagePrefix);
    netty_internal_tcnative_SSLContext_JNI_OnUnLoad(env, staticPackagePrefix);
    netty_internal_tcnative_SSLSession_JNI_OnUnLoad(env, staticPackagePrefix);
    free((void *) staticPackagePrefix);
    staticPackagePrefix = NULL;
}

// As we build with -fvisibility=hidden we need to ensure we mark the entry load and unload functions used by the
// JVM as visible.
//
// It's important to note that we will only export functions that are prefixed with JNI_ so if we ever need to export
// more we need to ensure we add the prefix. This is enforced by the TCN_CHECK_STATIC function in tcnative.m4.

// Invoked by the JVM when statically linked
JNIEXPORT jint JNI_OnLoad_netty_tcnative(JavaVM* vm, void* reserved) {
    tcn_global_vm = vm;
    jint ret = netty_jni_util_JNI_OnLoad(vm, reserved, "netty_tcnative", netty_internal_tcnative_Library_JNI_OnLoad);
    if (ret == JNI_ERR) {
        tcn_global_vm = NULL;
    }
    return ret;
}

// Invoked by the JVM when statically linked
JNIEXPORT void JNI_OnUnload_netty_tcnative(JavaVM* vm, void* reserved) {
    netty_jni_util_JNI_OnUnload(vm, reserved, netty_internal_tcnative_Library_JNI_OnUnload);
    tcn_global_vm = NULL;
}

#ifndef TCN_BUILD_STATIC
JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    tcn_global_vm = vm;
    jint ret = netty_jni_util_JNI_OnLoad(vm, reserved, "netty_tcnative", netty_internal_tcnative_Library_JNI_OnLoad);
    if (ret == JNI_ERR) {
        tcn_global_vm = NULL;
    }
    return ret;
}

JNIEXPORT void JNI_OnUnload(JavaVM* vm, void* reserved) {
    netty_jni_util_JNI_OnUnload(vm, reserved, netty_internal_tcnative_Library_JNI_OnUnload);
    tcn_global_vm = NULL;
}
#endif /* TCN_BUILD_STATIC */

JNIEXPORT jboolean JNICALL _Java_io_netty_internal_tcnative_Native_isStatic(JNIEnv *env, jclass *clazz) {
#ifdef ELIDE_GVM_STATIC
  return JNI_TRUE;
#else
  return JNI_FALSE;
#endif
}

JNIEXPORT jint JNICALL _Java_io_netty_internal_tcnative_Native_initializeStatic(JNIEnv *env, jclass *reserved) {
#ifdef ELIDE_GVM_STATIC
  JavaVM *vm;
  if (JNI_OK != (*env)->GetJavaVM(env, vm)) {
    return JNI_ERR;
  }

  tcn_global_vm = vm;
  jint ret = netty_jni_util_JNI_OnLoad(vm, reserved, "netty_tcnative", netty_internal_tcnative_Library_JNI_OnLoad);
  if (ret == JNI_ERR) {
      tcn_global_vm = NULL;
  }
  return ret;
#else
  return JNI_ERR;
#endif
}

/*
 * Copyright 2020 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

#ifndef NETTY_JNI_UTIL_H_
#define NETTY_JNI_UTIL_H_

#include <jni.h>

#ifndef NETTY_JNI_UTIL_JNI_VERSION
#define NETTY_JNI_UTIL_JNI_VERSION JNI_VERSION_1_8
#endif

#define NETTY_JNI_UTIL_BEGIN_MACRO     if (1) {
#define NETTY_JNI_UTIL_END_MACRO       } else (void)(0)

#define NETTY_JNI_UTIL_FIND_CLASS(E, C, N, R)       \
    NETTY_JNI_UTIL_BEGIN_MACRO                      \
        C = (*(E))->FindClass((E), N);              \
        if (C == NULL) {                            \
            (*(E))->ExceptionClear((E));            \
            goto R;                                 \
        }                                           \
    NETTY_JNI_UTIL_END_MACRO

#define NETTY_JNI_UTIL_LOAD_CLASS(E, C, N, R)       \
    NETTY_JNI_UTIL_BEGIN_MACRO                      \
        jclass _##C = (*(E))->FindClass((E), N);    \
        if (_##C == NULL) {                         \
            (*(E))->ExceptionClear((E));            \
            goto R;                                 \
        }                                           \
        C = (*(E))->NewGlobalRef((E), _##C);        \
        (*(E))->DeleteLocalRef((E), _##C);          \
        if (C == NULL) {                            \
            goto R;                                 \
        }                                           \
    NETTY_JNI_UTIL_END_MACRO

#define NETTY_JNI_UTIL_UNLOAD_CLASS(E, C)           \
    NETTY_JNI_UTIL_BEGIN_MACRO                      \
        if (C != NULL) {                            \
            (*(E))->DeleteGlobalRef((E), (C));      \
            C = NULL;                               \
        }                                           \
    NETTY_JNI_UTIL_END_MACRO

#define NETTY_JNI_UTIL_LOAD_CLASS_WEAK(E, C, N, R)  \
    NETTY_JNI_UTIL_BEGIN_MACRO                      \
        jclass _##C = (*(E))->FindClass((E), N);    \
        if (_##C == NULL) {                         \
            (*(E))->ExceptionClear((E));            \
            goto R;                                 \
        }                                           \
        C = (*(E))->NewWeakGlobalRef((E), _##C);    \
        (*(E))->DeleteLocalRef((E), _##C);          \
        if (C == NULL) {                            \
            goto R;                                 \
        }                                           \
    NETTY_JNI_UTIL_END_MACRO

#define NETTY_JNI_UTIL_UNLOAD_CLASS_WEAK(E, C)      \
    NETTY_JNI_UTIL_BEGIN_MACRO                      \
        if (C != NULL) {                            \
            (*(E))->DeleteWeakGlobalRef((E), (C));  \
            C = NULL;                               \
        }                                           \
    NETTY_JNI_UTIL_END_MACRO


#define NETTY_JNI_UTIL_NEW_LOCAL_FROM_WEAK(E, C, W, R)          \
    NETTY_JNI_UTIL_BEGIN_MACRO                                  \
        C = (*(E))->NewLocalRef((E), W);                        \
        if ((*(E))->IsSameObject((E), C, NULL) || C == NULL) {  \
            goto R;                                             \
        }                                                       \
    NETTY_JNI_UTIL_END_MACRO

#define NETTY_JNI_UTIL_DELETE_LOCAL(E, L)   \
    NETTY_JNI_UTIL_BEGIN_MACRO              \
        if (L != NULL) {                    \
            (*(E))->DeleteLocalRef((E), L); \
        }                                   \
    NETTY_JNI_UTIL_END_MACRO

#define NETTY_JNI_UTIL_GET_METHOD(E, C, M, N, S, R) \
    NETTY_JNI_UTIL_BEGIN_MACRO                      \
        M = (*(E))->GetMethodID((E), C, N, S);      \
        if (M == NULL) {                            \
            goto R;                                 \
        }                                           \
    NETTY_JNI_UTIL_END_MACRO

#define NETTY_JNI_UTIL_GET_FIELD(E, C, F, N, S, R)  \
    NETTY_JNI_UTIL_BEGIN_MACRO                      \
        F = (*(E))->GetFieldID((E), C, N, S);       \
        if (F == NULL) {                            \
            goto R;                                 \
        }                                           \
    NETTY_JNI_UTIL_END_MACRO

#define NETTY_JNI_UTIL_TRY_GET_FIELD(E, C, F, N, S) \
    NETTY_JNI_UTIL_BEGIN_MACRO                      \
        F = (*(E))->GetFieldID((E), C, N, S);       \
        if (F == NULL) {                            \
            (*(E))->ExceptionClear((E));            \
        }                                           \
    NETTY_JNI_UTIL_END_MACRO

#define NETTY_JNI_UTIL_PREPEND(P, S, N, R)                  \
    NETTY_JNI_UTIL_BEGIN_MACRO                              \
        if ((N = netty_jni_util_prepend(P, S)) == NULL) {   \
            goto R;                                         \
        }                                                   \
    NETTY_JNI_UTIL_END_MACRO

/**
 * Return a new string (caller must free this string) which is equivalent to <pre>prefix + str</pre>.
 *
 * Caller must free the return value!
 */
char* netty_jni_util_prepend(const char* prefix, const char* str);

char* netty_jni_util_rstrstr(char* s1rbegin, const char* s1rend, const char* s2);

/**
 * The expected format of the library name is "lib<>$libraryName" where the <> portion is what we will return.
 * If status != JNI_ERR then the caller MUST call free on the return value.
 */
char* netty_jni_util_parse_package_prefix(const char* libraryPathName, const char* libraryName, jint* status);

/**
 * Return type is as defined in https://docs.oracle.com/javase/7/docs/technotes/guides/jni/spec/functions.html#wp5833.
 */
jint netty_jni_util_register_natives(JNIEnv* env, const char* packagePrefix, const char* className, const JNINativeMethod* methods, jint numMethods);
jint netty_jni_util_unregister_natives(JNIEnv* env, const char* packagePrefix, const char* className);

/**
 * Free a dynamic allocated methods table
 */
void netty_jni_util_free_dynamic_methods_table(JNINativeMethod* dynamicMethods, jint fixedMethodTableSize, jint fullMethodTableSize);
/**
 * Free dynamic allocated name
 */
void netty_jni_util_free_dynamic_name(char** dynamicName);

/**
 * Function should be called when the native library is loaded. load_function takes ownership of packagePrefix.
 */
jint netty_jni_util_JNI_OnLoad(JavaVM* vm, void* reserved, const char* libname, jint (*load_function)(JNIEnv* env, const char* packagePrefix));

/**
 * Function should be called when the native library is unloaded
 */
void netty_jni_util_JNI_OnUnload(JavaVM* vm, void* reserved, void (*unload_function)(JNIEnv* env));

#endif /* NETTY_JNI_UTIL_H_ */

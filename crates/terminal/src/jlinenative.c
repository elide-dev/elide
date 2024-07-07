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
/*******************************************************************************
 * Copyright (C) 2023 the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *******************************************************************************/
#include "jlinenative.h"

#define JLineLibrary_NATIVE(func) Java_org_jline_nativ_JLineLibrary_##func

JNIEXPORT jobject JNICALL JLineLibrary_NATIVE(newFileDescriptor)(JNIEnv *env, jclass that, int fd)
{
    jfieldID field_fd;
    jmethodID const_fdesc;
    jclass class_fdesc;
    jobject ret;

    class_fdesc = (*env)->FindClass(env, "java/io/FileDescriptor");
    if (class_fdesc == NULL) return NULL;

    // construct a new FileDescriptor
    const_fdesc = (*env)->GetMethodID(env, class_fdesc, "<init>", "()V");
    if (const_fdesc == NULL) return NULL;
    ret = (*env)->NewObject(env, class_fdesc, const_fdesc);

    // poke the "fd" field with the file descriptor
    field_fd = (*env)->GetFieldID(env, class_fdesc, "fd", "I");
    if (field_fd == NULL) return NULL;
    (*env)->SetIntField(env, ret, field_fd, fd);

    // and return it
    return ret;
}

JNIEXPORT jobject JNICALL JLineLibrary_NATIVE(newRedirectPipe)(JNIEnv *env, jclass that, jobject fd)
{
    jfieldID field_fd;
    jmethodID const_rpi;
    jclass class_rpi;
    jobject ret;

    class_rpi = (*env)->FindClass(env, "java/lang/ProcessBuilder$RedirectPipeImpl");
    if (class_rpi == NULL) return NULL;

    // construct a new RedirectPipeImpl
    const_rpi = (*env)->GetMethodID(env, class_rpi, "<init>", "()V");
    if (const_rpi == NULL) return NULL;
    ret = (*env)->NewObject(env, class_rpi, const_rpi);

    // poke the "fd" field with the file descriptor
    field_fd = (*env)->GetFieldID(env, class_rpi, "fd", "Ljava/io/FileDescriptor;");
    if (field_fd == NULL) return NULL;
    (*env)->SetObjectField(env, ret, field_fd, fd);

    // and return it
    return ret;
}

/*
 * Copyright 2020 The Netty Project
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

#ifndef _WIN32
// It's important to have #define _GNU_SOURCE before any other include as otherwise it will not work.
// See http://stackoverflow.com/questions/7296963/gnu-source-and-use-gnu
#define _GNU_SOURCE
#include <dlfcn.h>
#else
#define MAX_DLL_PATH_LEN 2048
#include <windows.h>
#endif // _WIN32

#include <stdlib.h>
#include <string.h>
#include "netty_jni_util.h"

void netty_jni_util_free_dynamic_methods_table(JNINativeMethod* dynamicMethods, jint fixedMethodTableSize, jint fullMethodTableSize) {
    if (dynamicMethods != NULL) {
        jint i = fixedMethodTableSize;
        for (; i < fullMethodTableSize; ++i) {
            free(dynamicMethods[i].signature);
        }
        free(dynamicMethods);
    }
}

void netty_jni_util_free_dynamic_name(char** dynamicName) {
    if (dynamicName != NULL && *dynamicName != NULL) {
        free(*dynamicName);
        *dynamicName = NULL;
    }
}

char* netty_jni_util_prepend(const char* prefix, const char* str) {
    if (str == NULL) {
        // If str is NULL we should just return NULL as passing NULL to strlen is undefined behavior.
        return NULL;
    }
    if (prefix == NULL) {
        char* result = (char*) malloc(sizeof(char) * (strlen(str) + 1));
        if (result == NULL) {
            return NULL;
        }
        strcpy(result, str);
        return result;
    }
    char* result = (char*) malloc(sizeof(char) * (strlen(prefix) + strlen(str) + 1));
    if (result == NULL) {
        return NULL;
    }
    strcpy(result, prefix);
    strcat(result, str);
    return result;
}

jint netty_jni_util_register_natives(JNIEnv* env, const char* packagePrefix, const char* className, const JNINativeMethod* methods, jint numMethods) {
    char* nettyClassName = NULL;
    int retValue = JNI_ERR;

    NETTY_JNI_UTIL_PREPEND(packagePrefix, className, nettyClassName, done);

    jclass nativeCls = (*env)->FindClass(env, nettyClassName);
    if (nativeCls != NULL) {
        retValue = (*env)->RegisterNatives(env, nativeCls, methods, numMethods);
    }
done:
    free(nettyClassName);
    return retValue;
}

jint netty_jni_util_unregister_natives(JNIEnv* env, const char* packagePrefix, const char* className) {
    char* nettyClassName = NULL;
    int retValue = JNI_ERR;

    NETTY_JNI_UTIL_PREPEND(packagePrefix, className, nettyClassName, done);

    jclass nativeCls = (*env)->FindClass(env, nettyClassName);
    if (nativeCls != NULL) {
        retValue = (*env)->UnregisterNatives(env, nativeCls);
    }
done:
    free(nettyClassName);
    return retValue;
}

#ifndef NETTY_JNI_UTIL_BUILD_STATIC

char* netty_jni_util_rstrstr(char* s1rbegin, const char* s1rend, const char* s2) {
    if (s1rbegin == NULL || s1rend == NULL || s2 == NULL) {
        // Return NULL if any of the parameters is NULL to not risk a segfault
        return NULL;
    }
    size_t s2len = strlen(s2);
    char *s = s1rbegin - s2len;

    for (; s >= s1rend; --s) {
        if (strncmp(s, s2, s2len) == 0) {
            return s;
        }
    }
    return NULL;
}

#ifdef _WIN32
static char* netty_jni_util_rstrchar(char* s1rbegin, const char* s1rend, const char c2) {
    if (s1rbegin == NULL || s1rend == NULL || c2 == NULL) {
        // Return NULL if any of the parameters is NULL to not risk a segfault
        return NULL;
    }
    for (; s1rbegin >= s1rend; --s1rbegin) {
        if (*s1rbegin == c2) {
            return s1rbegin;
        }
    }
    return NULL;
}
#endif // _WIN32

static char* netty_jni_util_strstr_last(const char* haystack, const char* needle) {
    if (haystack == NULL || needle == NULL) {
        // calling strstr with NULL is undefined behavior. Better just return NULL and not risk a crash.
        return NULL;
    }

    char* prevptr = NULL;
    char* ptr = (char*) haystack;

    while ((ptr = strstr(ptr, needle)) != NULL) {
        // Just store the ptr and continue searching.
        prevptr = ptr;
        ++ptr;
    }
    return prevptr;
}

/**
 * The expected format of the library name is "lib<>libname" on non windows platforms and "<>libname" on windows,
 *  where the <> portion is what we will return.
 */
static char* parsePackagePrefix(const char* libraryPathName, const char* libname, jint* status) {
    char* packageNameEnd = netty_jni_util_strstr_last(libraryPathName, libname);
    if (packageNameEnd == NULL) {
        *status = JNI_ERR;
        return NULL;
    }
#ifdef _WIN32
    // on windows there is no lib prefix so we instead look for the previous path separator or the beginning of the string.
    char* packagePrefix = netty_jni_util_rstrchar(packageNameEnd, libraryPathName, '\\');
    if (packagePrefix == NULL) {
        // The string does not have to specify a path [1].
        // [1] https://msdn.microsoft.com/en-us/library/windows/desktop/ms683200(v=vs.85).aspx
        packagePrefix = libraryPathName;
    } else {
        packagePrefix += 1;
    }
#else
    char* packagePrefix = netty_jni_util_rstrstr(packageNameEnd, libraryPathName, "lib");
    if (packagePrefix == NULL) {
        *status = JNI_ERR;
        return NULL;
    }
    packagePrefix += 3;
#endif // _WIN32

    if (packagePrefix == packageNameEnd) {
        return NULL;
    }

    // packagePrefix length is > 0
    // Make a copy so we can modify the value without impacting libraryPathName.
    size_t packagePrefixLen = packageNameEnd - packagePrefix;
    char* newPackagePrefix = malloc(packagePrefixLen + 2); // +1 for trailing slash and +1 for \0
    if (newPackagePrefix == NULL) {
        *status = JNI_ERR;
        return NULL;
    }

    // Unmangle the package name, by translating:
    // - `_1` to `_`
    // - `_` to `/`
    //
    // Note that we don't unmangle `_0xxxx` because it's extremely unlikely to have a non-ASCII character
    // in a package name. For more information, see:
    // - JNI specification: https://docs.oracle.com/javase/8/docs/technotes/guides/jni/spec/design.html#resolving_native_method_names
    // - `NativeLibraryLoader.load()` that mangles a package name.
    size_t i;
    size_t j = 0;
    for (i = 0; i < packagePrefixLen; ++i) {
        char ch = packagePrefix[i];
        if (ch != '_') {
            newPackagePrefix[j++] = ch;
            continue;
        }

        char nextCh = packagePrefix[i + 1];
        if (nextCh < '0' || nextCh > '9') {
            // No digit after `_`; translate to `/`.
            newPackagePrefix[j++] = '/';
            continue;
        }

        if (nextCh == '1') {
            i++;
            newPackagePrefix[j++] = '_';
        } else {
            // We don't support _0, _2 .. _9.
            fprintf(stderr,
                    "FATAL: Unsupported escape pattern '_%c' in library name '%s'\n",
                    nextCh, packagePrefix);
            fflush(stderr);
            free(newPackagePrefix);
            *status = JNI_ERR;
            return NULL;
        }
    }

    // Make sure the prefix ends with `/`.
    if (newPackagePrefix[j - 1] != '/') {
        newPackagePrefix[j++] = '/';
    }

    // Terminate with `\0`.
    newPackagePrefix[j++] = '\0';
    return newPackagePrefix;
}

#endif /* NETTY_JNI_UTIL_BUILD_STATIC */

/* Fix missing Dl_info & dladdr in AIX
 * The code is taken from netbsd.org (src/crypto/external/bsd/openssl/dist/crypto/dso/dso_dlfcn.c)
 * except strlcpy & strlcat (those are taken from openbsd.org (src/lib/libc/string))
 */
#ifdef _AIX
/*-
 * See IBM's AIX Version 7.2, Technical Reference:
 *  Base Operating System and Extensions, Volume 1 and 2
 *  https://www.ibm.com/support/knowledgecenter/ssw_aix_72/com.ibm.aix.base/technicalreferences.htm
 */
#include <sys/ldr.h>
#include <errno.h>

/* strlcpy:
 * Copy string src to buffer dst of size dsize.  At most dsize-1
 * chars will be copied.  Always NUL terminates (unless dsize == 0).
 * Returns strlen(src); if retval >= dsize, truncation occurred.
 */
size_t strlcpy(char *dst, const char *src, size_t dsize)
{
    const char *osrc = src;
    size_t nleft = dsize;

    /* Copy as many bytes as will fit. */
    if (nleft != 0) {
        while (--nleft != 0) {
            if ((*dst++ = *src++) == '\0') {
                break;
            }
        }
    }

    /* Not enough room in dst, add NUL and traverse rest of src. */
    if (nleft == 0) {
        if (dsize != 0) {
            *dst = '\0';		/* NUL-terminate dst */
        }
        while (*src++) {
            ;
        }
    }

    return src - osrc - 1;	/* count does not include NUL */
}

/* strlcat:
 * Appends src to string dst of size dsize (unlike strncat, dsize is the
 * full size of dst, not space left).  At most dsize-1 characters
 * will be copied.  Always NUL terminates (unless dsize <= strlen(dst)).
 * Returns strlen(src) + MIN(dsize, strlen(initial dst)).
 * If retval >= dsize, truncation occurred.
 */
size_t strlcat(char *dst, const char *src, size_t dsize)
{
    const char *odst = dst;
    const char *osrc = src;
    size_t n = dsize;
    size_t dlen;

    /* Find the end of dst and adjust bytes left but don't go past end. */
    while (n-- != 0 && *dst != '\0') {
        dst++;
    }
    dlen = dst - odst;
    n = dsize - dlen;

    if (n-- == 0) {
        return dlen + strlen(src);
    }
    while (*src != '\0') {
        if (n != 0) {
            *dst++ = *src;
            n--;
        }
        src++;
    }
    *dst = '\0';

    return dlen + src - osrc;	/* count does not include NUL */
}

/* ~ 64 * (sizeof(struct ld_info) + _XOPEN_PATH_MAX + _XOPEN_NAME_MAX) */
#  define DLFCN_LDINFO_SIZE 86976
typedef struct Dl_info {
    const char *dli_fname;
} Dl_info;
/*
 * This dladdr()-implementation will also find the ptrgl (Pointer Glue) virtual
 * address of a function, which is just located in the DATA segment instead of
 * the TEXT segment.
 */
static int dladdr(void *ptr, Dl_info *dl)
{
    uintptr_t addr = (uintptr_t)ptr;
    struct ld_info *ldinfos;
    struct ld_info *next_ldi;
    struct ld_info *this_ldi;

    if ((ldinfos = malloc(DLFCN_LDINFO_SIZE)) == NULL) {
        dl->dli_fname = NULL;
        return 0;
    }

    if ((loadquery(L_GETINFO, (void *)ldinfos, DLFCN_LDINFO_SIZE)) < 0) {
        /*-
         * Error handling is done through errno and dlerror() reading errno:
         *  ENOMEM (ldinfos buffer is too small),
         *  EINVAL (invalid flags),
         *  EFAULT (invalid ldinfos ptr)
         */
        free((void *)ldinfos);
        dl->dli_fname = NULL;
        return 0;
    }
    next_ldi = ldinfos;

    do {
        this_ldi = next_ldi;
        if (((addr >= (uintptr_t)this_ldi->ldinfo_textorg)
             && (addr < ((uintptr_t)this_ldi->ldinfo_textorg +
                         this_ldi->ldinfo_textsize)))
            || ((addr >= (uintptr_t)this_ldi->ldinfo_dataorg)
                && (addr < ((uintptr_t)this_ldi->ldinfo_dataorg +
                            this_ldi->ldinfo_datasize)))) {
            char *buffer = NULL;
            char *member = NULL;
            size_t buffer_sz;
            size_t member_len;

            buffer_sz = strlen(this_ldi->ldinfo_filename) + 1;
            member = this_ldi->ldinfo_filename + buffer_sz;
            if ((member_len = strlen(member)) > 0) {
                buffer_sz += 1 + member_len + 1;
            }
            if ((buffer = malloc(buffer_sz)) != NULL) {
                strlcpy(buffer, this_ldi->ldinfo_filename, buffer_sz);
                if (member_len > 0) {
                    /*
                     * Need to respect a possible member name and not just
                     * returning the path name in this case. See docs:
                     * sys/ldr.h, loadquery() and dlopen()/RTLD_MEMBER.
                     */
                    strlcat(buffer, "(", buffer_sz);
                    strlcat(buffer, member, buffer_sz);
                    strlcat(buffer, ")", buffer_sz);
                }
                dl->dli_fname = buffer;
            }
            break;
        } else {
            next_ldi = (struct ld_info *)((uintptr_t)this_ldi +
                                          this_ldi->ldinfo_next);
        }
    } while (this_ldi->ldinfo_next);
    free((void *)ldinfos);
    return dl->dli_fname != NULL;
}
#endif /* _AIX */

jint netty_jni_util_JNI_OnLoad(JavaVM* vm, void* reserved, const char* libname, jint (*load_function)(JNIEnv*, const char*)) {
    JNIEnv* env = NULL;
    if ((*vm)->GetEnv(vm, (void**) &env, NETTY_JNI_UTIL_JNI_VERSION) != JNI_OK) {
        fprintf(stderr, "FATAL: JNI version mismatch");
        fflush(stderr);
        return JNI_ERR;
    }

#ifndef NETTY_JNI_UTIL_BUILD_STATIC
    jint status = 0;
    const char* name = NULL;
#ifndef _WIN32
    Dl_info dlinfo;
    // We need to use an address of a function that is uniquely part of this library, so choose a static
    // function. See https://github.com/netty/netty/issues/4840.
    if (!dladdr((void*) parsePackagePrefix, &dlinfo)) {
        fprintf(stderr, "FATAL: %s JNI call to dladdr failed!\n", libname);
        fflush(stderr);
        return JNI_ERR;
    }
    name = dlinfo.dli_fname;
#else
    HMODULE module = NULL;
    if (GetModuleHandleExA(GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS | GET_MODULE_HANDLE_EX_FLAG_UNCHANGED_REFCOUNT, (void*) parsePackagePrefix, &module) == 0){
        fprintf(stderr, "FATAL: %s JNI call to GetModuleHandleExA failed!\n", libname);
        fflush(stderr);
        return JNI_ERR;
    }

    // add space for \0 termination as this is not automatically included for windows XP
    // See https://msdn.microsoft.com/en-us/library/windows/desktop/ms683197(v=vs.85).aspx
    char dllPath[MAX_DLL_PATH_LEN + 1];
    int dllPathLen = GetModuleFileNameA(module, dllPath, MAX_DLL_PATH_LEN);
    if (dllPathLen == 0) {
        fprintf(stderr, "FATAL: %s JNI call to GetModuleFileNameA failed!\n", libname);
        fflush(stderr);
        return JNI_ERR;
    } else {
        // ensure we null terminate as this is not automatically done on windows xp
        dllPath[dllPathLen] = '\0';
    }

    name = dllPath;
#endif
    char* packagePrefix = parsePackagePrefix(name, libname, &status);

    if (status == JNI_ERR) {
        fprintf(stderr, "FATAL: %s encountered unexpected library path: %s\n", name, libname);
        fflush(stderr);
        return JNI_ERR;
    }
#else
    char* packagePrefix = NULL;
#endif /* NETTY_JNI_UTIL_BUILD_STATIC */

    jint ret = load_function(env, packagePrefix);
    return ret;
}

void netty_jni_util_JNI_OnUnload(JavaVM* vm, void* reserved, void (*unload_function)(JNIEnv*)) {
    JNIEnv* env = NULL;
    if ((*vm)->GetEnv(vm, (void**) &env, NETTY_JNI_UTIL_JNI_VERSION) != JNI_OK) {
        fprintf(stderr, "FATAL: JNI version mismatch");
        fflush(stderr);
        // Something is wrong but nothing we can do about this :(
        return;
    }
    unload_function(env);
}

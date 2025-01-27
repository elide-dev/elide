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
 * Copyright 2016 The Netty Project
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

#ifndef NETTY_IO_URING_LINUXSOCKET_H_
#define NETTY_IO_URING_LINUXSOCKET_H_

#include <jni.h>

// JNI initialization hooks. Users of this file are responsible for calling these in the JNI_OnLoad and JNI_OnUnload methods.
jint netty_io_uring_linuxsocket_JNI_OnLoad(JNIEnv* env, const char* packagePrefix);
void netty_io_uring_linuxsocket_JNI_OnUnLoad(JNIEnv* env, const char* packagePrefix);

// Invoked by the JVM when statically linked
JNIEXPORT jint JNI_OnLoad_netty_transport_native_io_uring(JavaVM* vm, void* reserved);

// Invoked by the JVM when statically linked
JNIEXPORT void JNI_OnUnload_netty_transport_native_io_uring(JavaVM* vm, void* reserved);

#ifndef NETTY_IO_URING_BUILD_STATIC
JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved);

JNIEXPORT void JNI_OnUnload(JavaVM* vm, void* reserved);
#endif /* NETTY_IO_URING_BUILD_STATIC */

#ifdef NETTY_BUILD_STATIC
#ifdef NETTY_BUILD_GRAALVM

JNIEXPORT jint Java_io_netty_incubator_channel_uring_Native_ioUringProbe(JNIEnv *env, jclass clazz, jint ring_fd, jintArray ops);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_Native_kernelVersion(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_Native_cmsghdrData(JNIEnv* env, jclass clazz, jlong cmsghdrAddr);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_Native_registerUnix(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_Native_createFile(JNIEnv *env, jclass class, jstring filename);

JNIEXPORT jobjectArray Java_io_netty_incubator_channel_uring_Native_ioUringSetup(JNIEnv *env, jclass clazz, jint entries);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_Native_ioUringEnter(JNIEnv *env, jclass class1, jint ring_fd, jint to_submit,
                                 jint min_complete, jint flags);

JNIEXPORT void Java_io_netty_incubator_channel_uring_Native_ioUringExit(JNIEnv *env, jclass clazz,
        jlong submissionQueueArrayAddress, jint submissionQueueRingEntries, jlong submissionQueueRingAddress, jint submissionQueueRingSize,
        jlong completionQueueRingAddress, jint completionQueueRingSize, jint ringFd);

JNIEXPORT void Java_io_netty_incubator_channel_uring_Native_eventFdWrite(JNIEnv* env, jclass clazz, jint fd, jlong value);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_Native_blockingEventFd(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_sockNonblock(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_sockCloexec(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_afInet(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_afInet6(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_sizeofSockaddrIn(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_sizeofSockaddrIn6(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_sockaddrInOffsetofSinFamily(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_sockaddrInOffsetofSinPort(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_sockaddrInOffsetofSinAddr(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_inAddressOffsetofSAddr(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_sockaddrIn6OffsetofSin6Family(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_sockaddrIn6OffsetofSin6Port(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_sockaddrIn6OffsetofSin6Flowinfo(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_sockaddrIn6OffsetofSin6Addr(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_sockaddrIn6OffsetofSin6ScopeId(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_in6AddressOffsetofS6Addr(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_sizeofSockaddrStorage(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_sizeofSizeT(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_sizeofIovec(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_iovecOffsetofIovBase(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_iovecOffsetofIovLen(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_sizeofMsghdr(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_msghdrOffsetofMsgName(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_msghdrOffsetofMsgNamelen(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_msghdrOffsetofMsgIov(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_msghdrOffsetofMsgIovlen(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_msghdrOffsetofMsgControl(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_msghdrOffsetofMsgControllen(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_msghdrOffsetofMsgFlags(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_etime(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_ecanceled(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_pollin(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_pollout(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_pollrdhup(JNIEnv* env, jclass clazz);

JNIEXPORT jbyte Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_ioringOpWritev(JNIEnv* env, jclass clazz);

JNIEXPORT jbyte Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_ioringOpPollAdd(JNIEnv* env, jclass clazz);

JNIEXPORT jbyte Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_ioringOpPollRemove(JNIEnv* env, jclass clazz);

JNIEXPORT jbyte Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_ioringOpTimeout(JNIEnv* env, jclass clazz);

JNIEXPORT jbyte Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_ioringOpTimeoutRemove(JNIEnv* env, jclass clazz);

JNIEXPORT jbyte Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_ioringOpAccept(JNIEnv* env, jclass clazz);

JNIEXPORT jbyte Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_ioringOpRead(JNIEnv* env, jclass clazz);

JNIEXPORT jbyte Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_ioringOpWrite(JNIEnv* env, jclass clazz);

JNIEXPORT jbyte Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_ioringOpRecv(JNIEnv* env, jclass clazz);

JNIEXPORT jbyte Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_ioringOpSend(JNIEnv* env, jclass clazz);

JNIEXPORT jbyte Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_ioringOpConnect(JNIEnv* env, jclass clazz);

JNIEXPORT jbyte Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_ioringOpClose(JNIEnv* env, jclass clazz);

JNIEXPORT jbyte Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_ioringOpSendmsg(JNIEnv* env, jclass clazz);

JNIEXPORT jbyte Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_ioringOpRecvmsg(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_ioringEnterGetevents(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_iosqeAsync(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_msgDontwait(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_msgFastopen(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_cmsgSpace(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_cmsgLen(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_solUdp(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_udpSegment(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_cmsghdrOffsetofCmsgLen(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_cmsghdrOffsetofCmsgLevel(JNIEnv* env, jclass clazz);

JNIEXPORT jint Java_io_netty_incubator_channel_uring_NativeStaticallyReferencedJniMethods_cmsghdrOffsetofCmsgType(JNIEnv* env, jclass clazz);

#endif
#endif

#endif

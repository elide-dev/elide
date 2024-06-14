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
#include "tcn.h"
#include "ssl_private.h"
#include "sslsession.h"

#define SSLSESSION_CLASSNAME "io/netty/internal/tcnative/SSLSession"


TCN_IMPLEMENT_CALL(jlong, SSLSession, getTime)(TCN_STDARGS, jlong session)
{
    SSL_SESSION *session_ = J2P(session, SSL_SESSION *);

    TCN_CHECK_NULL(session_, session, 0);

    return SSL_SESSION_get_time(session_);
}

TCN_IMPLEMENT_CALL(jlong, SSLSession, getTimeout)(TCN_STDARGS, jlong session)
{
    SSL_SESSION *session_ = J2P(session, SSL_SESSION *);

    TCN_CHECK_NULL(session_, session, 0);

    return SSL_SESSION_get_timeout(session_);
}

TCN_IMPLEMENT_CALL(jlong, SSLSession, setTimeout)(TCN_STDARGS, jlong session, jlong seconds)
{ 
    SSL_SESSION *session_ = J2P(session, SSL_SESSION *);

    TCN_CHECK_NULL(session_, session, 0);

    return SSL_SESSION_set_timeout(session_, seconds);
}

TCN_IMPLEMENT_CALL(jbyteArray, SSLSession, getSessionId)(TCN_STDARGS, jlong session)
{
    unsigned int len;
    const unsigned char *session_id = NULL;
    jbyteArray bArray = NULL;
    SSL_SESSION *session_ = J2P(session, SSL_SESSION *);

    TCN_CHECK_NULL(session_, session, NULL);

    session_id = SSL_SESSION_get_id(session_, &len);
    if (len == 0 || session_id == NULL) {
        return NULL;
    }
    
    if ((bArray = (*e)->NewByteArray(e, len)) == NULL) {
        return NULL;
    }
    (*e)->SetByteArrayRegion(e, bArray, 0, len, (jbyte*) session_id);
    return bArray;
}

TCN_IMPLEMENT_CALL(jboolean, SSLSession, upRef)(TCN_STDARGS, jlong session) {
    SSL_SESSION *session_ = J2P(session, SSL_SESSION *);

    TCN_CHECK_NULL(session_, session, JNI_FALSE);

    // Only supported with GCC
    #if !defined(OPENSSL_IS_BORINGSSL) && (defined(__GNUC__) || defined(__GNUG__))
        if (!SSL_SESSION_up_ref) {
            return JNI_FALSE;
        }
    #endif

    // We can only support it when either use openssl version >= 1.1.0 or GCC as this way we can use weak linking
#if OPENSSL_VERSION_NUMBER >= 0x10100000L || defined(__GNUC__) || defined(__GNUG__)
    return SSL_SESSION_up_ref(session_) == 1 ? JNI_TRUE : JNI_FALSE;
#else
    return JNI_FALSE;
#endif // OPENSSL_VERSION_NUMBER >= 0x10100000L || defined(__GNUC__) || defined(__GNUG__)
}

TCN_IMPLEMENT_CALL(void, SSLSession, free)(TCN_STDARGS, jlong session) {
    SSL_SESSION *session_ = J2P(session, SSL_SESSION *);

    TCN_CHECK_NULL(session_, session, /* void */);

    SSL_SESSION_free(session_);
}

TCN_IMPLEMENT_CALL(jboolean, SSLSession, shouldBeSingleUse)(TCN_STDARGS, jlong session) {
// Only supported by BoringSSL atm
#ifdef OPENSSL_IS_BORINGSSL
    SSL_SESSION *session_ = J2P(session, SSL_SESSION *);
    TCN_CHECK_NULL(session_, session, JNI_FALSE);
    return SSL_SESSION_should_be_single_use(session_) == 0 ? JNI_FALSE : JNI_TRUE;
#else 
    return JNI_FALSE;
#endif // OPENSSL_IS_BORINGSSL
}

// JNI Method Registration Table Begin
static const JNINativeMethod method_table[] = {
  { TCN_METHOD_TABLE_ENTRY(getTime, (J)J, SSLSession) },
  { TCN_METHOD_TABLE_ENTRY(getTimeout, (J)J, SSLSession) },
  { TCN_METHOD_TABLE_ENTRY(setTimeout, (JJ)J, SSLSession) },
  { TCN_METHOD_TABLE_ENTRY(getSessionId, (J)[B, SSLSession) },
  { TCN_METHOD_TABLE_ENTRY(free, (J)V, SSLSession) },
  { TCN_METHOD_TABLE_ENTRY(upRef, (J)Z, SSLSession) },
  { TCN_METHOD_TABLE_ENTRY(shouldBeSingleUse, (J)Z, SSLSession) }
};

static const jint method_table_size = sizeof(method_table) / sizeof(method_table[0]);

// JNI Method Registration Table End

// IMPORTANT: If you add any NETTY_JNI_UTIL_LOAD_CLASS or NETTY_JNI_UTIL_FIND_CLASS calls you also need to update
//            Library to reflect that.
jint netty_internal_tcnative_SSLSession_JNI_OnLoad(JNIEnv* env, const char* packagePrefix) {
    if (netty_jni_util_register_natives(env,
             packagePrefix,
             SSLSESSION_CLASSNAME,
             method_table, method_table_size) != 0) {
        return JNI_ERR;
    }
    return NETTY_JNI_UTIL_JNI_VERSION;
}

void netty_internal_tcnative_SSLSession_JNI_OnUnLoad(JNIEnv* env, const char* packagePrefix) {
    netty_jni_util_unregister_natives(env,packagePrefix, SSLSESSION_CLASSNAME);
}

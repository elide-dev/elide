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

#include "apr_thread_rwlock.h"
#include "apr_atomic.h"

#include "ssl_private.h"
#include <stdint.h>
#include "sslcontext.h"
#include "cert_compress.h"

#define SSLCONTEXT_CLASSNAME "io/netty/internal/tcnative/SSLContext"

static jweak    sslTask_class_weak;
static jfieldID  sslTask_returnValue;
static jfieldID  sslTask_complete;

static jweak    certificateCallbackTask_class_weak;
static jmethodID certificateCallbackTask_init;

static jweak    certificateVerifierTask_class_weak;
static jmethodID certificateVerifierTask_init;

static jweak    sslPrivateKeyMethodTask_class_weak;
static jfieldID  sslPrivateKeyMethodTask_resultBytes;

static jweak    sslPrivateKeyMethodSignTask_class_weak;
static jmethodID sslPrivateKeyMethodSignTask_init;

static jweak    sslPrivateKeyMethodDecryptTask_class_weak;
static jmethodID sslPrivateKeyMethodDecryptTask_init;

static const char* staticPackagePrefix = NULL;

extern apr_pool_t *tcn_global_pool;

static apr_status_t ssl_context_cleanup(void *data)
{
    tcn_ssl_ctxt_t *c = (tcn_ssl_ctxt_t *)data;
    JNIEnv *e = NULL;

    if (c != NULL) {
        SSL_CTX_free(c->ctx); // this function is safe to call with NULL
        c->ctx = NULL;

        tcn_get_java_env(&e);

#ifdef OPENSSL_IS_BORINGSSL
        if (c->ssl_private_key_method != NULL) {
            if (e != NULL) {
                (*e)->DeleteGlobalRef(e, c->ssl_private_key_method);
            }
            c->ssl_private_key_method = NULL;
        }
        if (c->ssl_cert_compression_zlib_algorithm != NULL) {
            if (e != NULL) {
                (*e)->DeleteGlobalRef(e, c->ssl_cert_compression_zlib_algorithm);
            }
            c->ssl_cert_compression_zlib_algorithm = NULL;
        }
        if (c->ssl_cert_compression_brotli_algorithm != NULL) {
            if (e != NULL) {
                (*e)->DeleteGlobalRef(e, c->ssl_cert_compression_brotli_algorithm);
            }
            c->ssl_cert_compression_brotli_algorithm = NULL;
        }
        if (c->ssl_cert_compression_zstd_algorithm != NULL) {
            if (e != NULL) {
                (*e)->DeleteGlobalRef(e, c->ssl_cert_compression_zstd_algorithm);
            }
            c->ssl_cert_compression_zstd_algorithm = NULL;
        }
        if (c->keylog_callback != NULL) {
            if (e != NULL) {
                (*e)->DeleteGlobalRef(e, c->keylog_callback);
            }
            c->keylog_callback = NULL;
        }
        c->keylog_callback_method = NULL;
#endif // OPENSSL_IS_BORINGSSL

        if (c->ssl_session_cache != NULL) {
            if (e != NULL) {
                (*e)->DeleteGlobalRef(e, c->ssl_session_cache);
            }
            c->ssl_session_cache = NULL;
        }
        c->ssl_session_cache_creation_method = NULL;
        c->ssl_session_cache_get_method = NULL;

        if (c->verifier != NULL) {
            if (e != NULL) {
                (*e)->DeleteGlobalRef(e, c->verifier);
            }
            c->verifier = NULL;
        }
        c->verifier_method = NULL;

        if (c->cert_requested_callback != NULL) {
            if (e != NULL) {
                (*e)->DeleteGlobalRef(e, c->cert_requested_callback);
            }
            c->cert_requested_callback = NULL;
        }
        c->cert_requested_callback_method = NULL;

        if (c->certificate_callback != NULL) {
            if (e != NULL) {
                (*e)->DeleteGlobalRef(e, c->certificate_callback);
            }
            c->certificate_callback = NULL;
        }
        c->certificate_callback_method = NULL;

        if (c->sni_hostname_matcher != NULL) {
            if (e != NULL) {
                (*e)->DeleteGlobalRef(e, c->sni_hostname_matcher);
            }
            c->sni_hostname_matcher = NULL;
        }
        c->sni_hostname_matcher_method = NULL;

        if (c->next_proto_data != NULL) {
            OPENSSL_free(c->next_proto_data);
            c->next_proto_data = NULL;
        }
        c->next_proto_len = 0;

        if (c->alpn_proto_data != NULL) {
            OPENSSL_free(c->alpn_proto_data);
            c->alpn_proto_data = NULL;
        }
        c->alpn_proto_len = 0;

        apr_thread_rwlock_destroy(c->mutex);

        if (c->ticket_keys != NULL) {
            OPENSSL_free(c->ticket_keys);
            c->ticket_keys = NULL;
        }
        c->ticket_keys_len = 0;

        if (c->password != NULL) {
            // Just use free(...) as we used strdup(...) to create the stored password.
            free(c->password);
            c->password = NULL;
        }
    }
    return APR_SUCCESS;
}

/* Initialize server context */
TCN_IMPLEMENT_CALL(jlong, SSLContext, make)(TCN_STDARGS, jint protocol, jint mode)
{
    apr_pool_t *p = NULL;
    tcn_ssl_ctxt_t *c = NULL;
    SSL_CTX *ctx = NULL;

#ifdef OPENSSL_IS_BORINGSSL
    // When using BoringSSL we want to use CRYPTO_BUFFER to reduce memory usage and minimize overhead as we do not need
    // X509* at all and just need the raw bytes of the certificates to construct our Java X509Certificate.
    //
    // See https://github.com/google/boringssl/blob/chromium-stable/PORTING.md#crypto_buffer
    ctx = SSL_CTX_new(TLS_with_buffers_method());

    // Needed in BoringSSL to be able to use TLSv1.3
    //
    // See http://hg.nginx.org/nginx/rev/7ad0f4ace359
    SSL_CTX_set_max_proto_version(ctx, TLS1_3_VERSION);

#elif OPENSSL_VERSION_NUMBER >= 0x10100000L && !defined(LIBRESSL_VERSION_NUMBER)
    // TODO this is very hacky as io.netty.handler.ssl.OpenSsl#doesSupportProtocol also uses this method to test for supported protocols. Furthermore
    // in OpenSSL 1.1.0 the way protocols are enable/disabled changes
    // (SSL_OP_NO_SSLv3,... are deprecated and you should use: https://www.openssl.org/docs/man1.1.0/ssl/SSL_CTX_set_max_proto_version.html)
    if (mode == SSL_MODE_CLIENT) {
        ctx = SSL_CTX_new(TLS_client_method());
    } else if (mode == SSL_MODE_SERVER) {
        ctx = SSL_CTX_new(TLS_server_method());
    } else {
        ctx = SSL_CTX_new(TLS_method());
    }
#else
    switch (protocol) {
    case SSL_PROTOCOL_TLS:
    case SSL_PROTOCOL_ALL:
        if (mode == SSL_MODE_CLIENT) {
            ctx = SSL_CTX_new(SSLv23_client_method());
        } else if (mode == SSL_MODE_SERVER) {
            ctx = SSL_CTX_new(SSLv23_server_method());
        } else {
            ctx = SSL_CTX_new(SSLv23_method());
        }
        break;
    case SSL_PROTOCOL_TLSV1_2:
#ifndef OPENSSL_NO_TLS1
        if (mode == SSL_MODE_CLIENT) {
            ctx = SSL_CTX_new(TLSv1_2_client_method());
        } else if (mode == SSL_MODE_SERVER) {
            ctx = SSL_CTX_new(TLSv1_2_server_method());
        } else {
            ctx = SSL_CTX_new(TLSv1_2_method());
        }
#endif
        break;
    case SSL_PROTOCOL_TLSV1_1:
#ifndef OPENSSL_NO_TLS1
        if (mode == SSL_MODE_CLIENT) {
            ctx = SSL_CTX_new(TLSv1_1_client_method());
        } else if (mode == SSL_MODE_SERVER) {
            ctx = SSL_CTX_new(TLSv1_1_server_method());
        } else {
            ctx = SSL_CTX_new(TLSv1_1_method());
        }
#endif
        break;
    case SSL_PROTOCOL_TLSV1:
#ifndef OPENSSL_NO_TLS1
        if (mode == SSL_MODE_CLIENT) {
            ctx = SSL_CTX_new(TLSv1_client_method());
        } else if (mode == SSL_MODE_SERVER) {
            ctx = SSL_CTX_new(TLSv1_server_method());
        } else {
            ctx = SSL_CTX_new(TLSv1_method());
        }
#endif
        break;
    case SSL_PROTOCOL_SSLV3:
#ifndef OPENSSL_NO_SSL3
        if (mode == SSL_MODE_CLIENT) {
            ctx = SSL_CTX_new(SSLv3_client_method());
        } else if (mode == SSL_MODE_SERVER) {
            ctx = SSL_CTX_new(SSLv3_server_method());
        } else {
            ctx = SSL_CTX_new(SSLv3_method());
        }
#endif
        break;
    case SSL_PROTOCOL_SSLV2:
#ifndef OPENSSL_NO_SSL2
        if (mode == SSL_MODE_CLIENT) {
            ctx = SSL_CTX_new(SSLv2_client_method());
        } else if (mode == SSL_MODE_SERVER) {
            ctx = SSL_CTX_new(SSLv2_server_method());
        } else {
            ctx = SSL_CTX_new(SSLv2_method());
        }
#endif
        break;
    default:
        // Try to give the user the highest supported protocol.
#ifndef OPENSSL_NO_TLS1
        if (protocol & SSL_PROTOCOL_TLSV1_2) {
            if (mode == SSL_MODE_CLIENT) {
                ctx = SSL_CTX_new(TLSv1_2_client_method());
            } else if (mode == SSL_MODE_SERVER) {
                ctx = SSL_CTX_new(TLSv1_2_server_method());
            } else {
                ctx = SSL_CTX_new(TLSv1_2_method());
            }
            break;
        } else if (protocol & SSL_PROTOCOL_TLSV1_1) {
            if (mode == SSL_MODE_CLIENT) {
                ctx = SSL_CTX_new(TLSv1_1_client_method());
            } else if (mode == SSL_MODE_SERVER) {
                ctx = SSL_CTX_new(TLSv1_1_server_method());
            } else {
                ctx = SSL_CTX_new(TLSv1_1_method());
            }
            break;
        } else if (protocol & SSL_PROTOCOL_TLSV1) {
            if (mode == SSL_MODE_CLIENT) {
                ctx = SSL_CTX_new(TLSv1_client_method());
            } else if (mode == SSL_MODE_SERVER) {
                ctx = SSL_CTX_new(TLSv1_server_method());
            } else {
                ctx = SSL_CTX_new(TLSv1_method());
            }
            break;
        }
#endif
#ifndef OPENSSL_NO_SSL3
        if (protocol & SSL_PROTOCOL_SSLV3) {
            if (mode == SSL_MODE_CLIENT) {
                ctx = SSL_CTX_new(SSLv3_client_method());
            } else if (mode == SSL_MODE_SERVER) {
                ctx = SSL_CTX_new(SSLv3_server_method());
            } else {
                ctx = SSL_CTX_new(SSLv3_method());
            }
            break;
        }
#endif
#ifndef OPENSSL_NO_SSL2
        if (protocol & SSL_PROTOCOL_SSLV2) {
            if (mode == SSL_MODE_CLIENT) {
                ctx = SSL_CTX_new(SSLv2_client_method());
            } else if (mode == SSL_MODE_SERVER) {
                ctx = SSL_CTX_new(SSLv2_server_method());
            } else {
                ctx = SSL_CTX_new(SSLv2_method());
            }
            break;
        }
#endif
        tcn_Throw(e, "Unsupported SSL protocol (%d)", protocol);
        goto cleanup;
    }
#endif /* OPENSSL_IS_BORINGSSL */

    if (ctx == NULL) {
        char err[ERR_LEN];
        ERR_error_string_n(ERR_get_error(), err, ERR_LEN);
        tcn_Throw(e, "Failed to initialize SSL_CTX (%s)", err);
        goto cleanup;
    }

    TCN_THROW_IF_ERR(apr_pool_create(&p, tcn_global_pool), p);

    if ((c = apr_pcalloc(p, sizeof(tcn_ssl_ctxt_t))) == NULL) {
        tcn_ThrowAPRException(e, apr_get_os_error());
        goto cleanup;
    }

    c->protocol = protocol;
    c->mode     = mode;
    c->ctx      = ctx;
    c->pool     = p;

    if (!(protocol & SSL_PROTOCOL_SSLV2)) {
        SSL_CTX_set_options(c->ctx, SSL_OP_NO_SSLv2);
    }
    if (!(protocol & SSL_PROTOCOL_SSLV3)) {
        SSL_CTX_set_options(c->ctx, SSL_OP_NO_SSLv3);
    }
    if (!(protocol & SSL_PROTOCOL_TLSV1)) {
        SSL_CTX_set_options(c->ctx, SSL_OP_NO_TLSv1);
    }
#ifdef SSL_OP_NO_TLSv1_1
    if (!(protocol & SSL_PROTOCOL_TLSV1_1)) {
        SSL_CTX_set_options(c->ctx, SSL_OP_NO_TLSv1_1);
    }
#endif
#ifdef SSL_OP_NO_TLSv1_2
    if (!(protocol & SSL_PROTOCOL_TLSV1_2)) {
        SSL_CTX_set_options(c->ctx, SSL_OP_NO_TLSv1_2);
    }
#endif
    /*
     * Configure additional context ingredients
     */
    SSL_CTX_set_options(c->ctx, SSL_OP_SINGLE_DH_USE);
#ifdef HAVE_ECC
    SSL_CTX_set_options(c->ctx, SSL_OP_SINGLE_ECDH_USE);
#endif

    SSL_CTX_set_options(c->ctx, SSL_OP_NO_COMPRESSION);

    /*
     * Disallow a session from being resumed during a renegotiation,
     * so that an acceptable cipher suite can be negotiated.
     */
    SSL_CTX_set_options(c->ctx, SSL_OP_NO_SESSION_RESUMPTION_ON_RENEGOTIATION);
    /**
     * These options may be set by default but can be dangerous in practice [1].
     * [1] https://www.openssl.org/docs/man1.0.1/ssl/SSL_CTX_set_options.html
     */
    SSL_CTX_clear_options(c->ctx, SSL_OP_ALLOW_UNSAFE_LEGACY_RENEGOTIATION | SSL_OP_LEGACY_SERVER_CONNECT);

    /*
     * - Release idle buffers to the SSL_CTX free list
     * - Always do retries (which is also the default in BoringSSL) to fix various possible bugs.
     *   See:
     *     - https://github.com/openssl/openssl/issues/6234
     *     - https://github.com/apple/swift-nio-ssl/pull/14
     */
    SSL_CTX_set_mode(c->ctx, SSL_MODE_RELEASE_BUFFERS | SSL_MODE_AUTO_RETRY);

    /* Default session context id and cache size */
    SSL_CTX_sess_set_cache_size(c->ctx, SSL_DEFAULT_CACHE_SIZE);

    /* Session cache is disabled by default */
    SSL_CTX_set_session_cache_mode(c->ctx, SSL_SESS_CACHE_OFF);
    /* Longer session timeout */
    SSL_CTX_set_timeout(c->ctx, 14400);
    EVP_Digest((const unsigned char *)SSL_DEFAULT_VHOST_NAME,
               (unsigned long)((sizeof SSL_DEFAULT_VHOST_NAME) - 1),
               &(c->context_id[0]), NULL, EVP_sha1(), NULL);
    if (mode) {
#if defined(HAVE_ECC) && (OPENSSL_VERSION_NUMBER < 0x10100000L || defined(LIBRESSL_VERSION_NUMBER))
        /* Set default (nistp256) elliptic curve for ephemeral ECDH keys */
        EC_KEY *ecdh = EC_KEY_new_by_curve_name(NID_X9_62_prime256v1);
        SSL_CTX_set_tmp_ecdh(c->ctx, ecdh);
        EC_KEY_free(ecdh);
#endif

#if OPENSSL_VERSION_NUMBER < 0x30000000L
        SSL_CTX_set_tmp_dh_callback(c->ctx,  tcn_SSL_callback_tmp_DH);
#else
        SSL_CTX_set_dh_auto(c->ctx, 1);
#endif
    }

    // Default depth is 100 and disabled according to https://www.openssl.org/docs/man1.0.2/ssl/SSL_set_verify.html.
    c->verify_config.verify_depth  = 100;
    c->verify_config.verify_mode   = SSL_CVERIFY_NONE;

    /* Set default password callback */
    SSL_CTX_set_default_passwd_cb(c->ctx, (pem_password_cb *) tcn_SSL_password_callback);
    SSL_CTX_set_default_passwd_cb_userdata(c->ctx, (void *) c->password);

#if defined(OPENSSL_IS_BORINGSSL)
    if (mode != SSL_MODE_SERVER) {
        // Set this to make the behaviour consistent with openssl / libressl
        SSL_CTX_set_allow_unknown_alpn_protos(ctx, 1);
    }
#endif
    apr_thread_rwlock_create(&c->mutex, p);
    /*
     * Let us cleanup the ssl context when the pool is destroyed
     */
    apr_pool_cleanup_register(p, (const void *)c,
                              ssl_context_cleanup,
                              apr_pool_cleanup_null);

    tcn_SSL_CTX_set_app_state(c->ctx, c);
    return P2J(c);
cleanup:
    if (p != NULL) {
        apr_pool_destroy(p);
    }
    SSL_CTX_free(ctx); // this function is safe to call with NULL.
    return 0;
}

TCN_IMPLEMENT_CALL(jint, SSLContext, free)(TCN_STDARGS, jlong ctx)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, 0);

    /* Run and destroy the cleanup callback */
    int result = apr_pool_cleanup_run(c->pool, c, ssl_context_cleanup);
    apr_pool_destroy(c->pool);
    return result;
}

TCN_IMPLEMENT_CALL(void, SSLContext, setContextId)(TCN_STDARGS, jlong ctx,
                                                   jstring id)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, /* void */);

    TCN_ALLOC_CSTRING(id);
    if (J2S(id)) {
        EVP_Digest((const unsigned char *)J2S(id),
                   (unsigned long)strlen(J2S(id)),
                   &(c->context_id[0]), NULL, EVP_sha1(), NULL);
    }
    TCN_FREE_CSTRING(id);
}

TCN_IMPLEMENT_CALL(void, SSLContext, setOptions)(TCN_STDARGS, jlong ctx,
                                                 jint opt)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, /* void */);

    SSL_CTX_set_options(c->ctx, opt);
}

TCN_IMPLEMENT_CALL(jint, SSLContext, getOptions)(TCN_STDARGS, jlong ctx)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, 0);

    return SSL_CTX_get_options(c->ctx);
}

TCN_IMPLEMENT_CALL(void, SSLContext, clearOptions)(TCN_STDARGS, jlong ctx,
                                                   jint opt)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, /* void */);

    SSL_CTX_clear_options(c->ctx, opt);
}

TCN_IMPLEMENT_CALL(jboolean, SSLContext, setCipherSuite)(TCN_STDARGS, jlong ctx,
                                                         jstring ciphers, jboolean tlsv13)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);
    jboolean rv = JNI_TRUE;

    TCN_CHECK_NULL(c, ctx, JNI_FALSE);

#ifdef OPENSSL_NO_TLS1_3
    if (tlsv13 == JNI_TRUE) {
        tcn_Throw(e, "TLSv1.3 not supported");
        return JNI_FALSE;
    }
#endif

    if (ciphers == NULL || (*e)->GetStringUTFLength(e, ciphers) == 0) {
        return JNI_FALSE;
    }

    TCN_ALLOC_CSTRING(ciphers);
    if (!J2S(ciphers)) {
        return JNI_FALSE;
    }

#ifdef OPENSSL_NO_TLS1_3
    rv = SSL_CTX_set_cipher_list(c->ctx, J2S(ciphers)) == 0 ? JNI_FALSE : JNI_TRUE;
#else

    if (tlsv13 == JNI_TRUE) {
#ifdef OPENSSL_IS_BORINGSSL
        // BoringSSL does not support setting TLSv1.3 cipher suites explicit for now.
        rv = JNI_TRUE;
#else
        rv = SSL_CTX_set_ciphersuites(c->ctx, J2S(ciphers)) == 0 ? JNI_FALSE : JNI_TRUE;
#endif // OPENSSL_IS_BORINGSSL

    } else {
        rv = SSL_CTX_set_cipher_list(c->ctx, J2S(ciphers)) == 0 ? JNI_FALSE : JNI_TRUE;
    }
#endif // OPENSSL_NO_TLS1_3
    if (rv == JNI_FALSE) {
        char err[ERR_LEN];
        ERR_error_string_n(ERR_get_error(), err, ERR_LEN);
        tcn_Throw(e, "Unable to configure permitted SSL ciphers (%s)", err);
    }
    TCN_FREE_CSTRING(ciphers);
    return rv;
}

TCN_IMPLEMENT_CALL(jboolean, SSLContext, setCertificateChainFile)(TCN_STDARGS, jlong ctx,
                                                                  jstring file,
                                                                  jboolean skipfirst)
{
#ifdef OPENSSL_IS_BORINGSSL
    tcn_Throw(e, "Not supported using BoringSSL");
    return JNI_FALSE;
#else

    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);
    jboolean rv = JNI_FALSE;

    TCN_CHECK_NULL(c, ctx, JNI_FALSE);

    TCN_ALLOC_CSTRING(file);

    if (!J2S(file)) {
        return JNI_FALSE;
    }
    if (tcn_SSL_CTX_use_certificate_chain(c->ctx, J2S(file), skipfirst) > 0) {
        rv = JNI_TRUE;
    }
    TCN_FREE_CSTRING(file);
    return rv;
#endif // OPENSSL_IS_BORINGSSL
}

TCN_IMPLEMENT_CALL(jboolean, SSLContext, setCertificateChainBio)(TCN_STDARGS, jlong ctx,
                                                                  jlong chain,
                                                                  jboolean skipfirst)
{
#ifdef OPENSSL_IS_BORINGSSL
    tcn_Throw(e, "Not supported using BoringSSL");
    return JNI_FALSE;
#else
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);
    BIO *b = J2P(chain, BIO *);

    TCN_CHECK_NULL(c, ctx, JNI_FALSE);

    if (b == NULL) {
        return JNI_FALSE;
    }
    if (tcn_SSL_CTX_use_certificate_chain_bio(c->ctx, b, skipfirst) > 0)  {
        return JNI_TRUE;
    }
    return JNI_FALSE;
#endif // OPENSSL_IS_BORINGSSL
}

TCN_IMPLEMENT_CALL(jboolean, SSLContext, setCACertificateBio)(TCN_STDARGS, jlong ctx, jlong certs)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, JNI_FALSE);

    BIO *b = J2P(certs, BIO *);

    return b != NULL && c->mode != SSL_MODE_CLIENT && tcn_SSL_CTX_use_client_CA_bio(c->ctx, b) > 0 ? JNI_TRUE : JNI_FALSE;
}


TCN_IMPLEMENT_CALL(jboolean, SSLContext, setNumTickets)(TCN_STDARGS, jlong ctx, jint num)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, JNI_FALSE);

#ifdef OPENSSL_IS_BORINGSSL
    // Not supported by BoringSSL 
    return JNI_FALSE;
#else
    // Only supported with GCC
    #if defined(__GNUC__) || defined(__GNUG__)
        if (!SSL_CTX_set_num_tickets) {
            return JNI_FALSE;
        }
    #endif

    // We can only support it when either use openssl version >= 1.1.1 or GCC as this way we can use weak linking
    #if OPENSSL_VERSION_NUMBER >= 0x10101000L  || defined(__GNUC__) || defined(__GNUG__)
        return SSL_CTX_set_num_tickets(c->ctx, num) > 0 ? JNI_TRUE : JNI_FALSE;
    #else
        return JNI_FALSE;
    #endif
#endif
}

TCN_IMPLEMENT_CALL(void, SSLContext, setTmpDHLength)(TCN_STDARGS, jlong ctx, jint length)
{
#if OPENSSL_VERSION_NUMBER < 0x30000000L
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, /* void */);

    switch (length) {
        case 512:
            SSL_CTX_set_tmp_dh_callback(c->ctx, tcn_SSL_callback_tmp_DH_512);
            return;
        case 1024:
            SSL_CTX_set_tmp_dh_callback(c->ctx, tcn_SSL_callback_tmp_DH_1024);
            return;
        case 2048:
            SSL_CTX_set_tmp_dh_callback(c->ctx, tcn_SSL_callback_tmp_DH_2048);
            return;
        case 4096:
            SSL_CTX_set_tmp_dh_callback(c->ctx, tcn_SSL_callback_tmp_DH_4096);
            return;
        default:
            tcn_Throw(e, "Unsupported length %s", length);
            return;
    }
#endif // OPENSSL_VERSION_NUMBER < 0x30000000L
}

#ifndef OPENSSL_IS_BORINGSSL
static EVP_PKEY *load_pem_key(tcn_ssl_ctxt_t *c, const char *file)
{
    BIO *bio = NULL;
    EVP_PKEY *key = NULL;

    if ((bio = BIO_new(BIO_s_file())) == NULL) {
        return NULL;
    }
    if (BIO_read_filename(bio, file) <= 0) {
        BIO_free(bio);
        return NULL;
    }

    key = PEM_read_bio_PrivateKey(bio, NULL, (pem_password_cb *) tcn_SSL_password_callback, (void *)c->password);

    BIO_free(bio);
    return key;
}

static X509 *load_pem_cert(tcn_ssl_ctxt_t *c, const char *file)
{
    BIO *bio = NULL;
    X509 *cert = NULL;

    if ((bio = BIO_new(BIO_s_file())) == NULL) {
        return NULL;
    }
    if (BIO_read_filename(bio, file) <= 0) {
        BIO_free(bio);
        return NULL;
    }
    cert = PEM_read_bio_X509_AUX(bio, NULL,
                (pem_password_cb *) tcn_SSL_password_callback,
                (void *)c->password);
    if (cert == NULL &&
       (ERR_GET_REASON(ERR_peek_last_error()) == PEM_R_NO_START_LINE)) {
        ERR_clear_error();
        BIO_ctrl(bio, BIO_CTRL_RESET, 0, NULL);
        cert = d2i_X509_bio(bio, NULL);
    }
    BIO_free(bio);
    return cert;
}

static int ssl_load_pkcs12(tcn_ssl_ctxt_t *c, const char *file,
                           EVP_PKEY **pkey, X509 **cert, STACK_OF(X509) **ca)
{
    const char *pass = NULL;
    char        buff[PEM_BUFSIZE];
    int         len, rc = 0;
    PKCS12     *p12 = NULL;
    BIO        *in = NULL;

    if ((in = BIO_new(BIO_s_file())) == 0) {
        return 0;
    }
    if (BIO_read_filename(in, file) <= 0) {
        BIO_free(in);
        return 0;
    }
    p12 = d2i_PKCS12_bio(in, 0);
    if (p12 == 0) {
        /* Error loading PKCS12 file */
        goto cleanup;
    }
    /* See if an empty password will do */
    if (PKCS12_verify_mac(p12, "", 0) || PKCS12_verify_mac(p12, 0, 0)) {
        pass = "";
    } else {
        len = tcn_SSL_password_callback(buff, PEM_BUFSIZE, 0, (void *) c->password);
        if (len < 0) {
            /* Passpharse callback error */
            goto cleanup;
        }
        if (!PKCS12_verify_mac(p12, buff, len)) {
            /* Mac verify error (wrong password?) in PKCS12 file */
            goto cleanup;
        }
        pass = buff;
    }
    rc = PKCS12_parse(p12, pass, pkey, cert, ca);
cleanup:
    if (p12 != 0) {
        PKCS12_free(p12);
    }
    BIO_free(in);
    return rc;
}

static void free_and_reset_pass(tcn_ssl_ctxt_t *c, char* old_password, const jboolean rv) {
    if (!rv) {
        if (c->password != NULL) {
            free(c->password);
            c->password = NULL;
        }
        // Restore old password
        c->password = old_password;
    } else if (old_password != NULL) {
        free(old_password);
    }
}

#endif // OPENSSL_IS_BORINGSSL

TCN_IMPLEMENT_CALL(jboolean, SSLContext, setCertificate)(TCN_STDARGS, jlong ctx,
                                                         jstring cert, jstring key,
                                                         jstring password)
{
#ifdef OPENSSL_IS_BORINGSSL
    tcn_Throw(e, "Not supported using BoringSSL");
    return JNI_FALSE;
#else
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, JNI_FALSE);

    jboolean rv = JNI_TRUE;
    TCN_ALLOC_CSTRING(cert);
    TCN_ALLOC_CSTRING(key);
    TCN_ALLOC_CSTRING(password);
    EVP_PKEY *pkey = NULL;
    X509 *xcert = NULL;
    const char *key_file = NULL;
    const char *cert_file = NULL;
    const char *p = NULL;
    char *old_password = NULL;
    char err[ERR_LEN];

    if (J2S(password)) {
        old_password = c->password;

        c->password = strdup(cpassword);
        if (c->password == NULL) {
            rv = JNI_FALSE;
            goto cleanup;
        }
    }
    key_file  = J2S(key);
    cert_file = J2S(cert);
    if (!key_file) {
        key_file = cert_file;
    }
    if (!key_file || !cert_file) {
        tcn_Throw(e, "No Certificate file specified or invalid file format");
        rv = JNI_FALSE;
        goto cleanup;
    }
    if ((p = strrchr(cert_file, '.')) != NULL && strcmp(p, ".pkcs12") == 0) {
        if (!ssl_load_pkcs12(c, cert_file, &pkey, &xcert, 0)) {
            ERR_error_string_n(ERR_get_error(), err, ERR_LEN);
            tcn_Throw(e, "Unable to load certificate %s (%s)",
                      cert_file, err);
            rv = JNI_FALSE;
            goto cleanup;
        }
    } else {
        if ((pkey = load_pem_key(c, key_file)) == NULL) {
            ERR_error_string_n(ERR_get_error(), err, ERR_LEN);
            tcn_Throw(e, "Unable to load certificate key %s (%s)",
                      key_file, err);
            rv = JNI_FALSE;
            goto cleanup;
        }
        if ((xcert = load_pem_cert(c, cert_file)) == NULL) {
            ERR_error_string_n(ERR_get_error(), err, ERR_LEN);
            tcn_Throw(e, "Unable to load certificate %s (%s)",
                      cert_file, err);
            rv = JNI_FALSE;
            goto cleanup;
        }
    }
    if (SSL_CTX_use_certificate(c->ctx, xcert) <= 0) {
        ERR_error_string_n(ERR_get_error(), err, ERR_LEN);
        tcn_Throw(e, "Error setting certificate (%s)", err);
        rv = JNI_FALSE;
        goto cleanup;
    }
    if (SSL_CTX_use_PrivateKey(c->ctx, pkey) <= 0) {
        ERR_error_string_n(ERR_get_error(), err, ERR_LEN);
        tcn_Throw(e, "Error setting private key (%s)", err);
        rv = JNI_FALSE;
        goto cleanup;
    }
    if (SSL_CTX_check_private_key(c->ctx) <= 0) {
        ERR_error_string_n(ERR_get_error(), err, ERR_LEN);
        tcn_Throw(e, "Private key does not match the certificate public key (%s)",
                  err);
        rv = JNI_FALSE;
        goto cleanup;
    }
cleanup:
    TCN_FREE_CSTRING(cert);
    TCN_FREE_CSTRING(key);
    TCN_FREE_CSTRING(password);
    EVP_PKEY_free(pkey); // this function is safe to call with NULL
    X509_free(xcert); // this function is safe to call with NULL
    free_and_reset_pass(c, old_password, rv);
    return rv;
#endif // OPENSSL_IS_BORINGSSL
}

TCN_IMPLEMENT_CALL(jboolean, SSLContext, setCertificateBio)(TCN_STDARGS, jlong ctx,
                                                         jlong cert, jlong key,
                                                         jstring password)
{
#ifdef OPENSSL_IS_BORINGSSL
    tcn_Throw(e, "Not supported using BoringSSL");
    return JNI_FALSE;
#else
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, JNI_FALSE);

    BIO *cert_bio = J2P(cert, BIO *);
    BIO *key_bio = J2P(key, BIO *);
    EVP_PKEY *pkey = NULL;
    X509 *xcert = NULL;

    jboolean rv = JNI_TRUE;
    TCN_ALLOC_CSTRING(password);
    char *old_password = NULL;
    char err[ERR_LEN];

    if (J2S(password)) {
        old_password = c->password;

        c->password = strdup(cpassword);
        if (c->password == NULL) {
            rv = JNI_FALSE;
            goto cleanup;
        }
    }

    if (!key) {
        key = cert;
    }
    if (!cert || !key) {
        tcn_Throw(e, "No Certificate file specified or invalid file format");
        rv = JNI_FALSE;
        goto cleanup;
    }

    if ((pkey = tcn_load_pem_key_bio(c->password, key_bio)) == NULL) {
        ERR_error_string_n(ERR_get_error(), err, ERR_LEN);
        ERR_clear_error();
        tcn_Throw(e, "Unable to load certificate key (%s)",err);
        rv = JNI_FALSE;
        goto cleanup;
    }
    if ((xcert = tcn_load_pem_cert_bio(c->password, cert_bio)) == NULL) {
        ERR_error_string_n(ERR_get_error(), err, ERR_LEN);
        ERR_clear_error();
        tcn_Throw(e, "Unable to load certificate (%s) ", err);
        rv = JNI_FALSE;
        goto cleanup;
    }

    if (SSL_CTX_use_certificate(c->ctx, xcert) <= 0) {
        ERR_error_string_n(ERR_get_error(), err, ERR_LEN);
        ERR_clear_error();
        tcn_Throw(e, "Error setting certificate (%s)", err);
        rv = JNI_FALSE;
        goto cleanup;
    }
    if (SSL_CTX_use_PrivateKey(c->ctx, pkey) <= 0) {
        ERR_error_string_n(ERR_get_error(), err, ERR_LEN);
        ERR_clear_error();
        tcn_Throw(e, "Error setting private key (%s)", err);
        rv = JNI_FALSE;
        goto cleanup;
    }
    if (SSL_CTX_check_private_key(c->ctx) <= 0) {
        ERR_error_string_n(ERR_get_error(), err, ERR_LEN);
        ERR_clear_error();

        tcn_Throw(e, "Private key does not match the certificate public key (%s)",
                  err);
        rv = JNI_FALSE;
        goto cleanup;
    }
cleanup:
    TCN_FREE_CSTRING(password);
    EVP_PKEY_free(pkey); // this function is safe to call with NULL
    X509_free(xcert); // this function is safe to call with NULL
    free_and_reset_pass(c, old_password, rv);
    return rv;
#endif // OPENSSL_IS_BORINGSSL
}

TCN_IMPLEMENT_CALL(void, SSLContext, setNpnProtos0)(TCN_STDARGS, jlong ctx, jbyteArray next_protos,
        jint selectorFailureBehavior)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, /* void */);

    if (next_protos != NULL) {
        OPENSSL_free(c->next_proto_data);

        int next_protos_len = (*e)->GetArrayLength(e, next_protos);
        c->next_proto_data = OPENSSL_malloc(next_protos_len);
        c->next_proto_len = next_protos_len;
        (*e)->GetByteArrayRegion(e, next_protos, 0, next_protos_len, (jbyte*) c->next_proto_data);

        // depending on if it's client mode or not we need to call different functions.
        if (c->mode == SSL_MODE_CLIENT)  {
            SSL_CTX_set_next_proto_select_cb(c->ctx, tcn_SSL_callback_select_next_proto, (void *)c);
        } else {
            SSL_CTX_set_next_protos_advertised_cb(c->ctx, tcn_SSL_callback_next_protos, (void *)c);
        }
    }
}

TCN_IMPLEMENT_CALL(void, SSLContext, setAlpnProtos0)(TCN_STDARGS, jlong ctx, jbyteArray alpn_protos,
        jint selectorFailureBehavior)
{
    // Only supported with GCC
    #if !defined(OPENSSL_IS_BORINGSSL) && (defined(__GNUC__) || defined(__GNUG__))
        if (!SSL_CTX_set_alpn_protos || !SSL_CTX_set_alpn_select_cb) {
            return;
        }
    #endif

    // We can only support it when either use openssl version >= 1.0.2 or GCC as this way we can use weak linking
    #if OPENSSL_VERSION_NUMBER >= 0x10002000L || defined(__GNUC__) || defined(__GNUG__)
        tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

        TCN_CHECK_NULL(c, ctx, /* void */);

        if (alpn_protos != NULL) {
            OPENSSL_free(c->alpn_proto_data);

            int alpn_protos_len = (*e)->GetArrayLength(e, alpn_protos);
            c->alpn_proto_data = OPENSSL_malloc(alpn_protos_len);
            c->alpn_proto_len = alpn_protos_len;
            (*e)->GetByteArrayRegion(e, alpn_protos, 0, alpn_protos_len, (jbyte*) c->alpn_proto_data);


            // depending on if it's client mode or not we need to call different functions.
            if (c->mode == SSL_MODE_CLIENT)  {
                SSL_CTX_set_alpn_protos(c->ctx, c->alpn_proto_data, c->alpn_proto_len);
            } else {
                SSL_CTX_set_alpn_select_cb(c->ctx, tcn_SSL_callback_alpn_select_proto, (void *) c);
            }
        }
    #endif
}

TCN_IMPLEMENT_CALL(jlong, SSLContext, setSessionCacheMode)(TCN_STDARGS, jlong ctx, jlong mode)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, 0);

    return SSL_CTX_set_session_cache_mode(c->ctx, mode);
}

TCN_IMPLEMENT_CALL(jlong, SSLContext, getSessionCacheMode)(TCN_STDARGS, jlong ctx)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, 0);

    return SSL_CTX_get_session_cache_mode(c->ctx);
}

TCN_IMPLEMENT_CALL(jlong, SSLContext, setSessionCacheTimeout)(TCN_STDARGS, jlong ctx, jlong timeout)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, 0);

    jlong rv = SSL_CTX_set_timeout(c->ctx, timeout);
    return rv;
}

TCN_IMPLEMENT_CALL(jlong, SSLContext, getSessionCacheTimeout)(TCN_STDARGS, jlong ctx)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, 0);

    return SSL_CTX_get_timeout(c->ctx);
}

TCN_IMPLEMENT_CALL(jlong, SSLContext, setSessionCacheSize)(TCN_STDARGS, jlong ctx, jlong size)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, 0);

    jlong rv = 0;

    // Also allow size of 0 which is unlimited
    if (size >= 0) {
      SSL_CTX_set_session_cache_mode(c->ctx, SSL_SESS_CACHE_SERVER);
      rv = SSL_CTX_sess_set_cache_size(c->ctx, size);
    }

    return rv;
}

TCN_IMPLEMENT_CALL(jlong, SSLContext, getSessionCacheSize)(TCN_STDARGS, jlong ctx)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, 0);

    return SSL_CTX_sess_get_cache_size(c->ctx);
}

TCN_IMPLEMENT_CALL(jlong, SSLContext, sessionNumber)(TCN_STDARGS, jlong ctx)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, 0);

    jlong rv = SSL_CTX_sess_number(c->ctx);
    return rv;
}

TCN_IMPLEMENT_CALL(jlong, SSLContext, sessionConnect)(TCN_STDARGS, jlong ctx)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, 0);

    jlong rv = SSL_CTX_sess_connect(c->ctx);
    return rv;
}

TCN_IMPLEMENT_CALL(jlong, SSLContext, sessionConnectGood)(TCN_STDARGS, jlong ctx)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, 0);

    jlong rv = SSL_CTX_sess_connect_good(c->ctx);
    return rv;
}

TCN_IMPLEMENT_CALL(jlong, SSLContext, sessionConnectRenegotiate)(TCN_STDARGS, jlong ctx)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, 0);

    jlong rv = SSL_CTX_sess_connect_renegotiate(c->ctx);
    return rv;
}

TCN_IMPLEMENT_CALL(jlong, SSLContext, sessionAccept)(TCN_STDARGS, jlong ctx)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, 0);

    jlong rv = SSL_CTX_sess_accept(c->ctx);
    return rv;
}

TCN_IMPLEMENT_CALL(jlong, SSLContext, sessionAcceptGood)(TCN_STDARGS, jlong ctx)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, 0);

    jlong rv = SSL_CTX_sess_accept_good(c->ctx);
    return rv;
}

TCN_IMPLEMENT_CALL(jlong, SSLContext, sessionAcceptRenegotiate)(TCN_STDARGS, jlong ctx)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, 0);

    jlong rv = SSL_CTX_sess_accept_renegotiate(c->ctx);
    return rv;
}

TCN_IMPLEMENT_CALL(jlong, SSLContext, sessionHits)(TCN_STDARGS, jlong ctx)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, 0);

    jlong rv = SSL_CTX_sess_hits(c->ctx);
    return rv;
}

TCN_IMPLEMENT_CALL(jlong, SSLContext, sessionCbHits)(TCN_STDARGS, jlong ctx)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, 0);

    jlong rv = SSL_CTX_sess_cb_hits(c->ctx);
    return rv;
}

TCN_IMPLEMENT_CALL(jlong, SSLContext, sessionMisses)(TCN_STDARGS, jlong ctx)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, 0);

    jlong rv = SSL_CTX_sess_misses(c->ctx);
    return rv;
}

TCN_IMPLEMENT_CALL(jlong, SSLContext, sessionTimeouts)(TCN_STDARGS, jlong ctx)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, 0);

    jlong rv = SSL_CTX_sess_timeouts(c->ctx);
    return rv;
}

TCN_IMPLEMENT_CALL(jlong, SSLContext, sessionCacheFull)(TCN_STDARGS, jlong ctx)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, 0);

    jlong rv = SSL_CTX_sess_cache_full(c->ctx);
    return rv;
}

TCN_IMPLEMENT_CALL(jlong, SSLContext, sessionTicketKeyNew)(TCN_STDARGS, jlong ctx)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, 0);

    jlong rv = apr_atomic_read32(&c->ticket_keys_new);
    return rv;
}

TCN_IMPLEMENT_CALL(jlong, SSLContext, sessionTicketKeyResume)(TCN_STDARGS, jlong ctx)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, 0);

    jlong rv = apr_atomic_read32(&c->ticket_keys_resume);
    return rv;
}

TCN_IMPLEMENT_CALL(jlong, SSLContext, sessionTicketKeyRenew)(TCN_STDARGS, jlong ctx)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, 0);

    jlong rv = apr_atomic_read32(&c->ticket_keys_renew);
    return rv;
}

TCN_IMPLEMENT_CALL(jlong, SSLContext, sessionTicketKeyFail)(TCN_STDARGS, jlong ctx)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, 0);

    jlong rv = apr_atomic_read32(&c->ticket_keys_fail);
    return rv;
}

static int current_session_key(tcn_ssl_ctxt_t *c, tcn_ssl_ticket_key_t *key) {
    int result = JNI_FALSE;
    apr_thread_rwlock_rdlock(c->mutex);
    if (c->ticket_keys_len > 0) {
        *key = c->ticket_keys[0];
        result = JNI_TRUE;
    }
    apr_thread_rwlock_unlock(c->mutex);
    return result;
}

static int find_session_key(tcn_ssl_ctxt_t *c, unsigned char key_name[16], tcn_ssl_ticket_key_t *key, int *is_current_key) {
    int result = JNI_FALSE;
    int i;

    apr_thread_rwlock_rdlock(c->mutex);
    for (i = 0; i < c->ticket_keys_len; ++i) {
        // Check if we have a match for tickets.
        if (memcmp(c->ticket_keys[i].key_name, key_name, 16) == 0) {
            *key = c->ticket_keys[i];
            result = JNI_TRUE;
            *is_current_key = (i == 0);
            break;
        }
    }
    apr_thread_rwlock_unlock(c->mutex);
    return result;
}

static int ssl_tlsext_ticket_key_cb(SSL *s,
                                    unsigned char key_name[16],
                                    unsigned char *iv,
                                    EVP_CIPHER_CTX *ctx,
#if OPENSSL_VERSION_NUMBER < 0x30000000L
                                    HMAC_CTX *hmac_ctx,
#else
                                    EVP_MAC_CTX *mac_ctx,
#endif
                                    int enc) {
     tcn_ssl_ctxt_t *c = NULL;
     tcn_ssl_ticket_key_t key;
     int is_current_key;

     TCN_GET_SSL_CTX(s, c);
     if (c == NULL) {
         return 0;
     }
     if (enc) { /* create new session */
         if (current_session_key(c, &key)) {
             if (RAND_bytes(iv, EVP_MAX_IV_LENGTH) <= 0) {
                 return -1; /* insufficient random */
             }

             memcpy(key_name, key.key_name, 16);

             EVP_EncryptInit_ex(ctx, EVP_aes_128_cbc(), NULL, key.aes_key, iv);
#if OPENSSL_VERSION_NUMBER < 0x30000000L
             HMAC_Init_ex(hmac_ctx, key.hmac_key, 16, EVP_sha256(), NULL);
#else
             EVP_MAC_CTX_set_params(mac_ctx, key.mac_params);
#endif
             apr_atomic_inc32(&c->ticket_keys_new);
             return 1;
         }
         // No ticket configured
         return 0;
     } else { /* retrieve session */
         if (find_session_key(c, key_name, &key, &is_current_key)) {
#if OPENSSL_VERSION_NUMBER < 0x30000000L
             HMAC_Init_ex(hmac_ctx, key.hmac_key, 16, EVP_sha256(), NULL);
#else
             EVP_MAC_CTX_set_params(mac_ctx, key.mac_params);
#endif
             EVP_DecryptInit_ex(ctx, EVP_aes_128_cbc(), NULL, key.aes_key, iv );
             if (!is_current_key) {
                 // The ticket matched a key in the list, and we want to upgrade it to the current
                 // key.
                 apr_atomic_inc32(&c->ticket_keys_renew);
                 return 2;
             }
             // The ticket matched the current key.
             apr_atomic_inc32(&c->ticket_keys_resume);
             return 1;
         }
         // No matching ticket.
         apr_atomic_inc32(&c->ticket_keys_fail);
         return 0;
     }
}

TCN_IMPLEMENT_CALL(void, SSLContext, setSessionTicketKeys0)(TCN_STDARGS, jlong ctx, jbyteArray keys)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, /* void */);

    jbyte* b = NULL;
    jbyte* key = NULL;
    tcn_ssl_ticket_key_t* ticket_keys = NULL;
    int i;
    int cnt;

    cnt = (*e)->GetArrayLength(e, keys) / SSL_SESSION_TICKET_KEY_SIZE;
    if ((ticket_keys = OPENSSL_malloc(sizeof(tcn_ssl_ticket_key_t) * cnt)) == NULL) {
        tcn_ThrowException(e, "OPENSSL_malloc() returned null");
        return;
    }

    if ((b = (*e)->GetByteArrayElements(e, keys, NULL)) == NULL) {
      tcn_ThrowException(e, "GetByteArrayElements() returned null");
      return;
    }

    for (i = 0; i < cnt; ++i) {
        key = b + (SSL_SESSION_TICKET_KEY_SIZE * i);
        memcpy(ticket_keys[i].key_name, key, 16);
#if OPENSSL_VERSION_NUMBER < 0x30000000L
        memcpy(ticket_keys[i].hmac_key, key + 16, 16);
#else
        ticket_keys[i].mac_params[0] = OSSL_PARAM_construct_octet_string(OSSL_MAC_PARAM_KEY, key + 16, 16);
        ticket_keys[i].mac_params[1] = OSSL_PARAM_construct_utf8_string(OSSL_MAC_PARAM_DIGEST, "sha256", 0);
        ticket_keys[i].mac_params[2] = OSSL_PARAM_construct_end();
#endif
        memcpy(ticket_keys[i].aes_key, key + 32, 16);
    }
    (*e)->ReleaseByteArrayElements(e, keys, b, 0);

    apr_thread_rwlock_wrlock(c->mutex);
    if (c->ticket_keys) {
        OPENSSL_free(c->ticket_keys);
    }
    c->ticket_keys_len = cnt;
    c->ticket_keys = ticket_keys;
    apr_thread_rwlock_unlock(c->mutex);

#if OPENSSL_VERSION_NUMBER < 0x30000000L
    SSL_CTX_set_tlsext_ticket_key_cb(c->ctx, ssl_tlsext_ticket_key_cb);
#else
    SSL_CTX_set_tlsext_ticket_key_evp_cb(c->ctx, ssl_tlsext_ticket_key_cb);
#endif
}

static const char* authentication_method(const SSL* ssl) {
{
    const STACK_OF(SSL_CIPHER) *ciphers = NULL;

    switch (SSL_version(ssl))
        {
        case SSL2_VERSION:
            return SSL_TXT_RSA;
        default:
            ciphers = SSL_get_ciphers(ssl);
            if (ciphers == NULL || sk_SSL_CIPHER_num(ciphers) <= 0) {
                // No cipher available so return UNKNOWN.
                return TCN_UNKNOWN_AUTH_METHOD;
            }
            return tcn_SSL_cipher_authentication_method(sk_SSL_CIPHER_value(ciphers, 0));
        }
    }
}

tcn_ssl_task_t* tcn_ssl_task_new(JNIEnv* e, jobject task) {
    if (task == NULL) {
        // task was NULL which most likely means we did run out of memory when calling NewObject(...). Signal a failure back by returning NULL.
        return NULL;
    }
    tcn_ssl_task_t* sslTask = (tcn_ssl_task_t*) OPENSSL_malloc(sizeof(tcn_ssl_task_t));
    if (sslTask == NULL) {
        return NULL;
    }
    
    if ((sslTask->task = (*e)->NewGlobalRef(e, task)) == NULL) {
        // NewGlobalRef failed because we ran out of memory, free what we malloc'ed and fail the handshake.
        OPENSSL_free(sslTask);
        return NULL;
    }
    sslTask->consumed = JNI_FALSE;
    return sslTask;
}

void tcn_ssl_task_free(JNIEnv* e, tcn_ssl_task_t* sslTask) {
    if (sslTask == NULL) {
        return;
    }

    if (sslTask->task != NULL) {
        // As we created a Global reference before we need to delete the reference as otherwise we will leak memory.
        (*e)->DeleteGlobalRef(e, sslTask->task);
        sslTask->task = NULL;
    }

    // The task was malloc'ed before, free it and clear it from the SSL storage.
    OPENSSL_free(sslTask);
}

/* Android end */
#if OPENSSL_VERSION_NUMBER < 0x10100000L || (defined(LIBRESSL_VERSION_NUMBER) && LIBRESSL_VERSION_NUMBER < 0x2070000fL)
static STACK_OF(X509)* X509_STORE_CTX_get0_untrusted(X509_STORE_CTX *ctx) {
    return ctx->untrusted;
}
#endif

#ifdef OPENSSL_IS_BORINGSSL
static jbyteArray get_certs(JNIEnv *e, SSL* ssl, const STACK_OF(CRYPTO_BUFFER)* chain) {
    CRYPTO_BUFFER *cert = NULL;
    const int totalQueuedLength = sk_CRYPTO_BUFFER_num(chain);
#else
static jbyteArray get_certs(JNIEnv *e, SSL* ssl, STACK_OF(X509)* chain) {
    X509 *cert = NULL;
    unsigned char *buf = NULL;
    const int totalQueuedLength = sk_X509_num(chain);
#endif // OPENSSL_IS_BORINGSSL

    tcn_ssl_state_t* state = tcn_SSL_get_app_state(ssl);
    TCN_ASSERT(state != NULL);

    // SSL_CTX_set_verify_depth() and SSL_set_verify_depth() set the limit up to which depth certificates in a chain are
    // used during the verification procedure. If the certificate chain is longer than allowed, the certificates above
    // the limit are ignored. Error messages are generated as if these certificates would not be present,
    // most likely a X509_V_ERR_UNABLE_TO_GET_ISSUER_CERT_LOCALLY will be issued.
    // https://www.openssl.org/docs/man1.0.2/ssl/SSL_set_verify.html
    int len = TCN_MIN(state->verify_config.verify_depth, totalQueuedLength);
    unsigned i;
    int length;

    jbyteArray array = NULL;
    jbyteArray bArray = NULL;
    jclass byteArrayClass = tcn_get_byte_array_class();

    // Create the byte[][] array that holds all the certs
    if ((array = (*e)->NewObjectArray(e, len, byteArrayClass, NULL)) == NULL) {
        return NULL;
    }

    for(i = 0; i < len; i++) {

#ifdef OPENSSL_IS_BORINGSSL
        cert = sk_CRYPTO_BUFFER_value(chain, i);
        length = CRYPTO_BUFFER_len(cert);
#else
        cert = sk_X509_value(chain, i);
        length = i2d_X509(cert, &buf);
#endif // OPENSSL_IS_BORINGSSL

        if (length <= 0 || (bArray = (*e)->NewByteArray(e, length)) == NULL) {
            NETTY_JNI_UTIL_DELETE_LOCAL(e, array);
            array = NULL;
            goto complete;
        }

#ifdef OPENSSL_IS_BORINGSSL
        (*e)->SetByteArrayRegion(e, bArray, 0, length, (jbyte*) CRYPTO_BUFFER_data(cert));
#else
        (*e)->SetByteArrayRegion(e, bArray, 0, length, (jbyte*) buf);

        OPENSSL_free(buf);
        buf = NULL;
#endif // OPENSSL_IS_BORINGSSL
        (*e)->SetObjectArrayElement(e, array, i, bArray);

        // Delete the local reference as we not know how long the chain is and local references are otherwise
        // only freed once jni method returns.
        NETTY_JNI_UTIL_DELETE_LOCAL(e, bArray);
        bArray = NULL;
    }

complete:

#ifndef OPENSSL_IS_BORINGSSL
    // We need to delete the local references so we not leak memory as this method is called via callback.
    OPENSSL_free(buf);
#endif // OPENSSL_IS_BORINGSSL

    // Delete the local reference as we not know how long the chain is and local references are otherwise
    // only freed once jni method returns.
    NETTY_JNI_UTIL_DELETE_LOCAL(e, bArray);
    return array;
}

#ifndef OPENSSL_IS_BORINGSSL
// See https://www.openssl.org/docs/man1.0.2/man3/SSL_CTX_set_cert_verify_callback.html for return values.
static int SSL_cert_verify(X509_STORE_CTX *ctx, void *arg) {
    /* Get Apache context back through OpenSSL context */
    SSL *ssl = X509_STORE_CTX_get_ex_data(ctx, SSL_get_ex_data_X509_STORE_CTX_idx());
    TCN_ASSERT(ssl != NULL);
    tcn_ssl_ctxt_t *c = NULL;
    TCN_GET_SSL_CTX(ssl, c);
    TCN_ASSERT(c != NULL);

    STACK_OF(X509) *sk = NULL;
    JNIEnv *e = NULL;
    jstring authMethodString = NULL;
    int ret = 0;
#ifdef X509_V_ERR_UNSPECIFIED
    jint result = X509_V_ERR_UNSPECIFIED;
#else
    jint result = X509_V_ERR_CERT_REJECTED;
#endif // X509_V_ERR_UNSPECIFIED
    jint len;
    jbyteArray array = NULL;

    if (tcn_get_java_env(&e) != JNI_OK) {
        goto complete;
    }

    // Get a stack of all certs in the chain
    if ((sk = X509_STORE_CTX_get0_untrusted(ctx)) == NULL) {
        goto complete;
    }

    // Create the byte[][] array that holds all the certs
    if ((array = get_certs(e, ssl, sk)) == NULL) {
        goto complete;
    }

    len = (*e)->GetArrayLength(e, array);

    if ((authMethodString = (*e)->NewStringUTF(e, authentication_method(ssl))) == NULL) {
        goto complete;
    }

    result = (*e)->CallIntMethod(e, c->verifier, c->verifier_method, P2J(ssl), array, authMethodString);

    if ((*e)->ExceptionCheck(e)) {
         // We always need to set the error as stated in the SSL_CTX_set_cert_verify_callback manpage, so set the result
         // to the correct value.
#ifdef X509_V_ERR_UNSPECIFIED
        result = X509_V_ERR_UNSPECIFIED;
#else
        result = X509_V_ERR_CERT_REJECTED;
#endif  // X509_V_ERR_UNSPECIFIED
        goto complete;
    }

#ifdef X509_V_ERR_UNSPECIFIED
    // If we failed to verify for an unknown reason (currently this happens if we can't find a common root) then we should
    // fail with the same status as recommended in the OpenSSL docs https://www.openssl.org/docs/man1.0.2/ssl/SSL_set_verify.html
    if (result == X509_V_ERR_UNSPECIFIED && len < sk_X509_num(sk)) {
        result = X509_V_ERR_UNABLE_TO_GET_ISSUER_CERT_LOCALLY;
    }
#else
    // HACK!
    // LibreSSL 2.4.x doesn't support the X509_V_ERR_UNSPECIFIED so we introduce a work around to make sure a supported alert is used.
    // This should be reverted when we support LibreSSL 2.5.x (which does support X509_V_ERR_UNSPECIFIED).
    if (result == TCN_X509_V_ERR_UNSPECIFIED) {
        result = len < sk_X509_num(sk) ? X509_V_ERR_UNABLE_TO_GET_ISSUER_CERT_LOCALLY : X509_V_ERR_CERT_REJECTED;
    }
#endif // X509_V_ERR_UNSPECIFIED


    // TODO(scott): if verify_config.verify_depth == SSL_CVERIFY_OPTIONAL we have the option to let the handshake
    // succeed for some of the "informational" error messages (e.g. X509_V_ERR_EMAIL_MISMATCH ?)

complete:
    // We need to delete the local references so we not leak memory as this method is called via callback.
    NETTY_JNI_UTIL_DELETE_LOCAL(e, authMethodString);
    NETTY_JNI_UTIL_DELETE_LOCAL(e, array);

    X509_STORE_CTX_set_error(ctx, result);

    ret = result == X509_V_OK ? 1 : 0;
    return ret;
}
#else // OPENSSL_IS_BORINGSSL

enum ssl_verify_result_t tcn_SSL_cert_custom_verify(SSL* ssl, uint8_t *out_alert) {
    enum ssl_verify_result_t ret = ssl_verify_invalid;
    tcn_ssl_state_t *state = tcn_SSL_get_app_state(ssl);
    const STACK_OF(CRYPTO_BUFFER) *chain = NULL;
    jstring authMethodString = NULL;
    jint result = X509_V_ERR_UNSPECIFIED;
    jint len = 0;
    jbyteArray array = NULL;
    jclass certificateVerifierTask_class = NULL;
    JNIEnv *e = NULL;

    if (state == NULL || state->ctx == NULL) {
        goto complete;
    }

    if (tcn_get_java_env(&e) != JNI_OK) {
        goto complete;
    }

    // Let's check if we retried the operation and so have stored a sslTask that runs the certificiate callback.
    if (state->ssl_task  != NULL) {
        // Check if the task complete yet. If not the complete field will be still false.
        if ((*e)->GetBooleanField(e, state->ssl_task->task, sslTask_complete) == JNI_FALSE) {
            // Not done yet, try again later.
            ret = ssl_verify_retry;
            goto complete;
        }

        // The task is complete, retrieve the return value that should be signaled back.
        result = (*e)->GetIntField(e, state->ssl_task->task, sslTask_returnValue);

        tcn_ssl_task_free(e, state->ssl_task);
        state->ssl_task = NULL;

        TCN_ASSERT(result >= 0);

        // If we failed to verify for an unknown reason (currently this happens if we can't find a common root) then we should
        // fail with the same status as recommended in the OpenSSL docs https://www.openssl.org/docs/man1.0.2/ssl/SSL_set_verify.html
        if (result == X509_V_ERR_UNSPECIFIED && len < sk_CRYPTO_BUFFER_num(chain)) {
            result = X509_V_ERR_UNABLE_TO_GET_ISSUER_CERT_LOCALLY;
        }
        goto complete;
    }

    if ((chain = SSL_get0_peer_certificates(ssl)) == NULL) {
        goto complete;
    }

    // Create the byte[][] array that holds all the certs
    if ((array = get_certs(e, ssl, chain)) == NULL) {
        goto complete;
    }

    len = (*e)->GetArrayLength(e, array);

    if ((authMethodString = (*e)->NewStringUTF(e, authentication_method(ssl))) == NULL) {
        goto complete;
    }

    if (state->ctx->verifier == NULL) {
        // If we failed to verify for an unknown reason (currently this happens if we can't find a common root) then we should
        // fail with the same status as recommended in the OpenSSL docs https://www.openssl.org/docs/man1.0.2/ssl/SSL_set_verify.html
        if (len < sk_CRYPTO_BUFFER_num(chain)) {
            result = X509_V_ERR_UNABLE_TO_GET_ISSUER_CERT_LOCALLY;
        }
        goto complete;
    }

    // Let's check if we should provide the certificate callback as task that can be run on another Thread.
    if (state->ctx->use_tasks != 0) {
        // Lets create the CertificateCallbackTask and store it on the SSL object. We then later retrieve it via
        // SSL.getTask(ssl) and run it.
        NETTY_JNI_UTIL_NEW_LOCAL_FROM_WEAK(e, certificateVerifierTask_class, certificateVerifierTask_class_weak, complete);
        jobject task = (*e)->NewObject(e, certificateVerifierTask_class, certificateVerifierTask_init, P2J(ssl), array, authMethodString, state->ctx->verifier);

        if ((state->ssl_task = tcn_ssl_task_new(e, task)) == NULL) {
            goto complete;
        }

         // Signal back that we want to suspend the handshake.
        ret = ssl_verify_retry;
        goto complete;
    } else {
        // Execute the java callback
        result = (*e)->CallIntMethod(e, state->ctx->verifier, state->ctx->verifier_method, P2J(ssl), array, authMethodString);

        if ((*e)->ExceptionCheck(e) == JNI_TRUE) {
            result = X509_V_ERR_UNSPECIFIED;
            goto complete;
        }

        // If we failed to verify for an unknown reason (currently this happens if we can't find a common root) then we should
        // fail with the same status as recommended in the OpenSSL docs https://www.openssl.org/docs/man1.0.2/ssl/SSL_set_verify.html
        if (result == X509_V_ERR_UNSPECIFIED && len < sk_CRYPTO_BUFFER_num(chain)) {
            result = X509_V_ERR_UNABLE_TO_GET_ISSUER_CERT_LOCALLY;
        }

        // TODO(scott): if verify_config.verify_depth == SSL_CVERIFY_OPTIONAL we have the option to let the handshake
        // succeed for some of the "informational" error messages (e.g. X509_V_ERR_EMAIL_MISMATCH ?)
    }
complete:

    // We need to delete the local references so we not leak memory as this method is called via callback.
    NETTY_JNI_UTIL_DELETE_LOCAL(e, authMethodString);
    NETTY_JNI_UTIL_DELETE_LOCAL(e, array);
    NETTY_JNI_UTIL_DELETE_LOCAL(e, certificateVerifierTask_class);

    if (ret != ssl_verify_retry) {
        if (result == X509_V_OK) {
            ret = ssl_verify_ok;
        } else {
            ret = ssl_verify_invalid;
            *out_alert = SSL_alert_from_verify_result(result);
        }
    }
    return ret;
}
#endif // OPENSSL_IS_BORINGSSL


TCN_IMPLEMENT_CALL(void, SSLContext, setVerify)(TCN_STDARGS, jlong ctx, jint level, jint depth)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, /* void */);

    int mode = tcn_set_verify_config(&c->verify_config, level, depth);
#ifdef OPENSSL_IS_BORINGSSL
    if (c->verifier != NULL) {
        SSL_CTX_set_custom_verify(c->ctx, mode, tcn_SSL_cert_custom_verify);
    }
#else
    // No need to set the callback for SSL_CTX_set_verify because we override the default certificate verification via SSL_CTX_set_cert_verify_callback.
    SSL_CTX_set_verify(c->ctx, mode, NULL);
    SSL_CTX_set_verify_depth(c->ctx, c->verify_config.verify_depth);
#endif // OPENSSL_IS_BORINGSSL
}

TCN_IMPLEMENT_CALL(void, SSLContext, setCertVerifyCallback)(TCN_STDARGS, jlong ctx, jobject verifier)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, /* void */);

    jobject oldVerifier = c->verifier;
    if (verifier == NULL) {
        c->verifier = NULL;
        c->verifier_method = NULL;
#ifdef OPENSSL_IS_BORINGSSL
        SSL_CTX_set_custom_verify(c->ctx, SSL_VERIFY_NONE, NULL);
#else
        SSL_CTX_set_cert_verify_callback(c->ctx, NULL, NULL);
#endif // OPENSSL_IS_BORINGSSL
    } else {
        jclass verifier_class = (*e)->GetObjectClass(e, verifier);
        jmethodID method = (*e)->GetMethodID(e, verifier_class, "verify", "(J[[BLjava/lang/String;)I");

        if (method == NULL) {
            tcn_ThrowIllegalArgumentException(e, "Unable to retrieve verify method");
            return;
        }
        jobject v = (*e)->NewGlobalRef(e, verifier);
        if (v == NULL) {
            tcn_throwOutOfMemoryError(e, "Unable to allocate memory for global reference");
            return;
        }

        c->verifier = v;
        c->verifier_method = method;

#ifdef OPENSSL_IS_BORINGSSL
        SSL_CTX_set_custom_verify(c->ctx, tcn_set_verify_config(&c->verify_config, c->verify_config.verify_mode,
                 c->verify_config.verify_depth), tcn_SSL_cert_custom_verify);
#else
        SSL_CTX_set_cert_verify_callback(c->ctx, SSL_cert_verify, NULL);
#endif // OPENSSL_IS_BORINGSSL

        // Delete the reference to the previous specified verifier if needed.
        if (oldVerifier != NULL) {
            (*e)->DeleteGlobalRef(e, oldVerifier);
        }
    }
}

#ifndef LIBRESSL_VERSION_NUMBER
static jbyteArray keyTypes(JNIEnv* e, SSL* ssl) {
    jbyte* ctype_bytes = NULL;
    jbyteArray types = NULL;
    int ctype_num = tcn_SSL_get0_certificate_types(ssl, (const uint8_t **) &ctype_bytes);
    if (ctype_num <= 0) {
        // No idea what we should use... Let the caller handle it.
        return NULL;
    }
    if ((types = (*e)->NewByteArray(e, ctype_num)) == NULL) {
        return NULL;
    }
    (*e)->SetByteArrayRegion(e, types, 0, ctype_num, ctype_bytes);
    return types;
}

/**
 * Returns an array containing all the X500 principal's bytes.
 *
 * Partly based on code from conscrypt:
 * https://android.googlesource.com/platform/external/conscrypt/+/master/src/main/native/org_conscrypt_NativeCrypto.cpp
 */
#ifdef OPENSSL_IS_BORINGSSL
static jobjectArray principalBytes(JNIEnv* e, const STACK_OF(CRYPTO_BUFFER)* names) {
    CRYPTO_BUFFER* principal = NULL;
#else
static jobjectArray principalBytes(JNIEnv* e, const STACK_OF(X509_NAME)* names) {
    unsigned char *buf = NULL;
    X509_NAME* principal = NULL;
#endif // OPENSSL_IS_BORINGSSL
    jobjectArray array = NULL;
    jbyteArray bArray = NULL;;
    int i;
    int count;
    int length;

    jclass byteArrayClass = tcn_get_byte_array_class();

    if (names == NULL) {
        return NULL;
    }

#ifdef OPENSSL_IS_BORINGSSL
    count = sk_CRYPTO_BUFFER_num(names);
#else
    count = sk_X509_NAME_num(names);
#endif // OPENSSL_IS_BORINGSSL

    if (count <= 0) {
        return NULL;
    }

    if ((array = (*e)->NewObjectArray(e, count, byteArrayClass, NULL)) == NULL) {
        return NULL;
    }

    for (i = 0; i < count; i++) {
#ifdef OPENSSL_IS_BORINGSSL
        principal = sk_CRYPTO_BUFFER_value(names, i);
        length = CRYPTO_BUFFER_len(principal);
#else
        principal = sk_X509_NAME_value(names, i);
        length = i2d_X509_NAME(principal, &buf);
        if (length < 0) {
            if (buf != NULL) {
                // We need to delete the local references so we not leak memory as this method is called via callback.
                OPENSSL_free(buf);
            }
            // In case of error just return an empty byte[][]
            return (*e)->NewObjectArray(e, 0, byteArrayClass, NULL);
        }
#endif // OPENSSL_IS_BORINGSSL

        bArray = (*e)->NewByteArray(e, length);

#ifdef OPENSSL_IS_BORINGSSL
         if (bArray == NULL) {
             return NULL;
         }
        (*e)->SetByteArrayRegion(e, bArray, 0, length, (jbyte*) CRYPTO_BUFFER_data(principal));
#else
        if (bArray == NULL) {
            OPENSSL_free(buf);
            return NULL;
        }
        (*e)->SetByteArrayRegion(e, bArray, 0, length, (jbyte*) buf);
        OPENSSL_free(buf);
        buf = NULL;
#endif // OPENSSL_IS_BORINGSSL

        (*e)->SetObjectArrayElement(e, array, i, bArray);

        // Delete the local reference as we not know how long the chain is and local references are otherwise
        // only freed once jni method returns.
        NETTY_JNI_UTIL_DELETE_LOCAL(e, bArray);
    }

    return array;
}
#endif // LIBRESSL_VERSION_NUMBER

#ifndef OPENSSL_IS_BORINGSSL
static int cert_requested(SSL* ssl, X509** x509Out, EVP_PKEY** pkeyOut) {
#if defined(LIBRESSL_VERSION_NUMBER)
    // Not supported with LibreSSL
    return -1;
#else
    tcn_ssl_ctxt_t *c = NULL;
    jobjectArray issuers = NULL;
    JNIEnv *e = NULL;
    jbyteArray types = NULL;

    TCN_GET_SSL_CTX(ssl, c);

    if (c == NULL || tcn_get_java_env(&e) != JNI_OK) {
        return -1;
    }

    types = keyTypes(e, ssl);

    issuers = principalBytes(e, SSL_get_client_CA_list(ssl));

    // Execute the java callback
    (*e)->CallVoidMethod(e, c->cert_requested_callback, c->cert_requested_callback_method,
             P2J(ssl), P2J(x509Out), P2J(pkeyOut), types, issuers);

    // Check if java threw an exception and if so signal back that we should not continue with the handshake.
    if ((*e)->ExceptionCheck(e)) {
        return -1;
    }

    if ((*x509Out) == NULL) {
        // No certificate provided.
        return 0;
    }

    // Everything good...
    return 1;
#endif /* defined(LIBRESSL_VERSION_NUMBER) */
}
#endif // OPENSSL_IS_BORINGSSL

TCN_IMPLEMENT_CALL(void, SSLContext, setCertRequestedCallback)(TCN_STDARGS, jlong ctx, jobject callback)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, /* void */);

#ifdef OPENSSL_IS_BORINGSSL
    tcn_Throw(e, "Not supported using BoringSSL");
#else
    jobject oldCallback = c->cert_requested_callback;
    if (callback == NULL) {
        c->cert_requested_callback = NULL;
        c->cert_requested_callback_method = NULL;

        SSL_CTX_set_client_cert_cb(c->ctx, NULL);
    } else {
        jclass callback_class = (*e)->GetObjectClass(e, callback);
        jmethodID method = (*e)->GetMethodID(e, callback_class, "requested", "(JJJ[B[[B)V");

        if (method == NULL) {
            tcn_ThrowIllegalArgumentException(e, "Unable to retrieve requested method");
            return;
        }
        jobject cb = (*e)->NewGlobalRef(e, callback);
        if (cb == NULL) {
            tcn_throwOutOfMemoryError(e, "Unable to allocate memory for global reference");
            return;
        }
       
        c->cert_requested_callback = cb;
        c->cert_requested_callback_method = method;

        SSL_CTX_set_client_cert_cb(c->ctx, cert_requested);
    }
    // Delete the reference to the previous specified verifier if needed.
    if (oldCallback != NULL) {
        (*e)->DeleteGlobalRef(e, oldCallback);
    }
#endif
}

#ifndef LIBRESSL_VERSION_NUMBER

// See https://www.openssl.org/docs/man1.0.2/man3/SSL_set_cert_cb.html for return values.
static int certificate_cb(SSL* ssl, void* arg) {
    tcn_ssl_state_t *state = tcn_SSL_get_app_state(ssl);
    if (state == NULL || state->ctx == NULL) {
        // Signal back that we want to fail the handshake
        return 0;
    }

    jobjectArray issuers = NULL;
    JNIEnv *e = NULL;
    jbyteArray types = NULL;
    jclass certificateCallbackTask_class = NULL;

    if (tcn_get_java_env(&e) != JNI_OK) {
        return 0;
    }

    // Let's check if we retried the operation and so have stored a sslTask that runs the certificiate callback.
    if (state->ssl_task != NULL) {
        // Check if the task complete yet. If not the complete field will be still false.
        if ((*e)->GetBooleanField(e, state->ssl_task->task, sslTask_complete) == JNI_FALSE) {
            // Not done yet, try again later.
            return -1;
        }

        // The task is complete, retrieve the return value that should be signaled back.
        jint ret = (*e)->GetIntField(e, state->ssl_task->task, sslTask_returnValue);

        tcn_ssl_task_free(e, state->ssl_task);
        state->ssl_task = NULL;

        TCN_ASSERT(ret >= 0);

        return ret;
    }

    if (state->ctx->mode == SSL_MODE_SERVER) {
        // TODO: Consider filling these somehow.
        types = NULL;
        issuers = NULL;
    } else {
        types = keyTypes(e, ssl);

#ifdef OPENSSL_IS_BORINGSSL
        issuers = principalBytes(e, SSL_get0_server_requested_CAs(ssl));
#else
        issuers = principalBytes(e, SSL_get_client_CA_list(ssl));
#endif // OPENSSL_IS_BORINGSSL
    }

    int ret = 0;
    // Let's check if we should provide the certificate callback as task that can be run on another Thread.
    if (state->ctx->use_tasks != 0) {
        // Lets create the CertificateCallbackTask and store it on the SSL object. We then later retrieve it via
        // SSL.getTask(ssl) and run it.
        NETTY_JNI_UTIL_NEW_LOCAL_FROM_WEAK(e, certificateCallbackTask_class, certificateCallbackTask_class_weak, complete);
        jobject task = (*e)->NewObject(e, certificateCallbackTask_class, certificateCallbackTask_init, P2J(ssl), types, issuers, state->ctx->certificate_callback);

        if ((state->ssl_task = tcn_ssl_task_new(e, task)) != NULL) {
            // Signal back that we want to suspend the handshake.
            ret = -1;
        }
    } else {
        // Execute the java callback
        (*e)->CallVoidMethod(e, state->ctx->certificate_callback, state->ctx->certificate_callback_method,
                 P2J(ssl), types, issuers);

        // Check if java threw an exception and if so signal back that we should not continue with the handshake.
        if ((*e)->ExceptionCheck(e) != JNI_TRUE) {
            // Everything good...
            ret = 1;
        }
    }

complete:
    NETTY_JNI_UTIL_DELETE_LOCAL(e, certificateCallbackTask_class);
    return ret;
}
#endif // LIBRESSL_VERSION_NUMBER

TCN_IMPLEMENT_CALL(void, SSLContext, setCertificateCallback)(TCN_STDARGS, jlong ctx, jobject callback)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, /* void */);

#if defined(LIBRESSL_VERSION_NUMBER)
    tcn_Throw(e, "Not supported with LibreSSL");
#else

// Use weak linking with GCC as this will alow us to run the same packaged version with multiple
// version of openssl.
#if !defined(OPENSSL_IS_BORINGSSL) && (defined(__GNUC__) || defined(__GNUG__))
    if (!SSL_CTX_set_cert_cb) {
        tcn_ThrowException(e, "Requires OpenSSL 1.0.2+");
        return;
    }
#endif

// We can only support it when either use openssl version >= 1.0.2 or GCC as this way we can use weak linking
#if OPENSSL_VERSION_NUMBER >= 0x10002000L || defined(__GNUC__) || defined(__GNUG__)
    jobject oldCallback = c->certificate_callback;
    if (callback == NULL) {
        c->certificate_callback = NULL;
        c->certificate_callback_method = NULL;

        SSL_CTX_set_cert_cb(c->ctx, NULL, NULL);
    } else {
        jclass callback_class = (*e)->GetObjectClass(e, callback);
        if (callback_class == NULL) {
            tcn_Throw(e, "Unable to retrieve callback class");
            return;
        }

        jmethodID method = (*e)->GetMethodID(e, callback_class, "handle", "(J[B[[B)V");

        if (method == NULL) {
            tcn_ThrowIllegalArgumentException(e, "Unable to retrieve handle method");
            return;
        }
        jobject cb = (*e)->NewGlobalRef(e, callback);
        if (cb == NULL) {
            tcn_throwOutOfMemoryError(e, "Unable to allocate memory for global reference");
            return;
        }

        c->certificate_callback = cb;
        c->certificate_callback_method = method;

        SSL_CTX_set_cert_cb(c->ctx, certificate_cb, NULL);
    }
        
    if (oldCallback != NULL) {
        (*e)->DeleteGlobalRef(e, oldCallback);
     }

#endif // OPENSSL_VERSION_NUMBER >= 0x10002000L || defined(__GNUC__) || defined(__GNUG__)

#endif // defined(LIBRESSL_VERSION_NUMBER)
}

// Support for SSL_PRIVATE_KEY_METHOD.
#ifdef OPENSSL_IS_BORINGSSL

static enum ssl_private_key_result_t tcn_private_key_sign_java(SSL *ssl, uint8_t *out, size_t *out_len, size_t max_out, uint16_t signature_algorithm, const uint8_t *in, size_t in_len) {
    enum ssl_private_key_result_t ret = ssl_private_key_failure;

    tcn_ssl_state_t* state = tcn_SSL_get_app_state(ssl);
    jbyteArray resultBytes = NULL;
    jbyteArray inputArray = NULL;
    jbyte* b = NULL;
    jclass sslPrivateKeyMethodSignTask_class = NULL;
    int arrayLen = 0;
    JNIEnv *e = NULL;

    if (state == NULL || state->ctx->ssl_private_key_method == NULL) {
        goto complete;
    }

    if (tcn_get_java_env(&e) != JNI_OK) {
        goto complete;
    }

    if ((inputArray = (*e)->NewByteArray(e, in_len)) == NULL) {
        goto complete;
    }
    (*e)->SetByteArrayRegion(e, inputArray, 0, in_len, (jbyte*) in);

    if (state->ctx->use_tasks) {
        // Lets create the SSLPrivateKeyMethodSignTask and store it on the SSL object. We then later retrieve it via
        // SSL.getTask(ssl) and run it.
        NETTY_JNI_UTIL_NEW_LOCAL_FROM_WEAK(e, sslPrivateKeyMethodSignTask_class, sslPrivateKeyMethodSignTask_class_weak, complete);
        jobject task = (*e)->NewObject(e, sslPrivateKeyMethodSignTask_class, sslPrivateKeyMethodSignTask_init, P2J(ssl),
                signature_algorithm, inputArray, state->ctx->ssl_private_key_method);
        if ((state->ssl_task = tcn_ssl_task_new(e, task)) == NULL) {
            goto complete;
        }

        ret = ssl_private_key_retry;
    } else {
        resultBytes = (*e)->CallObjectMethod(e, state->ctx->ssl_private_key_method, state->ctx->ssl_private_key_sign_method,
                P2J(ssl), signature_algorithm, inputArray);
        if ((*e)->ExceptionCheck(e) == JNI_FALSE) {
            if (resultBytes == NULL) {
                ret = ssl_private_key_failure;
            } else {
                arrayLen = (*e)->GetArrayLength(e, resultBytes);
                if (max_out >= arrayLen) {
                    if ((b = (*e)->GetByteArrayElements(e, resultBytes, NULL)) == NULL) {
                        ret = ssl_private_key_failure;
                        goto complete;
                    }

                    memcpy(out, b, arrayLen);
                    (*e)->ReleaseByteArrayElements(e, resultBytes, b, JNI_ABORT);
                    *out_len = arrayLen;

                    ret = ssl_private_key_success;
                }
            }
        } else {
            (*e)->ExceptionClear(e);
            ret = ssl_private_key_failure;
        }
    }
complete:
    // Free up any allocated memory and return.
    NETTY_JNI_UTIL_DELETE_LOCAL(e, inputArray);
    NETTY_JNI_UTIL_DELETE_LOCAL(e, sslPrivateKeyMethodSignTask_class);

    return ret;
}

static enum ssl_private_key_result_t tcn_private_key_decrypt_java(SSL *ssl, uint8_t *out, size_t *out_len, size_t max_out, const uint8_t *in, size_t in_len) {
    enum ssl_private_key_result_t ret = ssl_private_key_failure;
    tcn_ssl_state_t* state = tcn_SSL_get_app_state(ssl);
    jbyteArray resultBytes = NULL;
    jbyteArray inArray = NULL;
    jbyte* b = NULL;
    int arrayLen = 0;
    jclass sslPrivateKeyMethodDecryptTask_class = NULL;
    JNIEnv *e = NULL;

    if (state == NULL || state->ctx->ssl_private_key_method == NULL) {
        goto complete;
    }

    if (tcn_get_java_env(&e) != JNI_OK) {
        goto complete;
    }

    if ((inArray = (*e)->NewByteArray(e, in_len)) == NULL) {
        goto complete;
    }
    (*e)->SetByteArrayRegion(e, inArray, 0, in_len, (jbyte*) in);

    if (state->ctx->use_tasks) {
        // Lets create the SSLPrivateKeyMethodDecryptTask and store it on the SSL object. We then later retrieve it via
        // SSL.getTask(ssl) and run it.
        NETTY_JNI_UTIL_NEW_LOCAL_FROM_WEAK(e, sslPrivateKeyMethodDecryptTask_class, sslPrivateKeyMethodDecryptTask_class_weak, complete);
        jobject task = (*e)->NewObject(e, sslPrivateKeyMethodDecryptTask_class, sslPrivateKeyMethodDecryptTask_init,
                P2J(ssl), inArray, state->ctx->ssl_private_key_method);

        if ((state->ssl_task = tcn_ssl_task_new(e, task)) == NULL) {
            goto complete;
        }

        ret = ssl_private_key_retry;
    } else {
        resultBytes = (*e)->CallObjectMethod(e, state->ctx->ssl_private_key_method, state->ctx->ssl_private_key_decrypt_method,
            P2J(ssl), inArray);
        if ((*e)->ExceptionCheck(e) == JNI_FALSE) {
            if (resultBytes == NULL) {
                ret = ssl_private_key_failure;
            } else {
                arrayLen = (*e)->GetArrayLength(e, resultBytes);
                if (max_out >= arrayLen) {
                    if ((b = (*e)->GetByteArrayElements(e, resultBytes, NULL)) == NULL) {
                        ret = ssl_private_key_failure;
                        goto complete;
                    }

                    memcpy(out, b, arrayLen);
                    (*e)->ReleaseByteArrayElements(e, resultBytes, b, JNI_ABORT);
                    *out_len = arrayLen;
                    ret = ssl_private_key_success;
                }
            }
        } else {
            (*e)->ExceptionClear(e);
            ret = ssl_private_key_failure;
        }
    }

complete:
    // Delete the local reference as this is executed by a callback.
    NETTY_JNI_UTIL_DELETE_LOCAL(e, inArray);
    NETTY_JNI_UTIL_DELETE_LOCAL(e, sslPrivateKeyMethodDecryptTask_class);
    return ret;
}

static enum ssl_private_key_result_t tcn_private_key_complete_java(SSL *ssl, uint8_t *out, size_t *out_len, size_t max_out) {
    tcn_ssl_state_t* state = tcn_SSL_get_app_state(ssl);
    jbyte* b = NULL;
    int arrayLen = 0;
    JNIEnv *e = NULL;

    if (state == NULL || state->ctx == NULL) {
        return ssl_private_key_failure;
    }
    if (state->ctx->use_tasks == 0) {
        // We do not use any asynchronous implementation so just report success.
        return ssl_private_key_success;
    }

    // Let's check if we retried the operation and so have stored a sslTask that runs the sign / decrypt callback.
    if (state->ssl_task != NULL) {
        if (tcn_get_java_env(&e) != JNI_OK) {
            return ssl_private_key_failure;
        }

        // Check if the task complete yet. If not the complete field will be still false.
        if ((*e)->GetBooleanField(e, state->ssl_task->task, sslTask_complete) == JNI_FALSE) {
            // Not done yet, try again later.
            return ssl_private_key_retry;
        }

        // The task is complete, retrieve the return value that should be signaled back.
        jbyteArray resultBytes = (*e)->GetObjectField(e, state->ssl_task->task, sslPrivateKeyMethodTask_resultBytes);

        tcn_ssl_task_free(e, state->ssl_task);
        state->ssl_task = NULL;

        if (resultBytes == NULL) {
            return ssl_private_key_failure;
        }

        arrayLen = (*e)->GetArrayLength(e, resultBytes);
        if (max_out < arrayLen) {
             // We need to fail as otherwise we would end up writing into memory which does not
             // belong to us.
            return ssl_private_key_failure;
        }
        if ((b = (*e)->GetByteArrayElements(e, resultBytes, NULL)) == NULL) {
            return ssl_private_key_failure;
        }
        memcpy(out, b, arrayLen);
        (*e)->ReleaseByteArrayElements(e, resultBytes, b, JNI_ABORT);
        *out_len = arrayLen;
        return ssl_private_key_success;
    }
    return ssl_private_key_failure;
}

const SSL_PRIVATE_KEY_METHOD private_key_method = {
    &tcn_private_key_sign_java,
    &tcn_private_key_decrypt_java,
    &tcn_private_key_complete_java
};
#endif // OPENSSL_IS_BORINGSSL


TCN_IMPLEMENT_CALL(void, SSLContext, setPrivateKeyMethod0)(TCN_STDARGS, jlong ctx, jobject method) {
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, /* void */);
#ifdef OPENSSL_IS_BORINGSSL
    char* name = NULL;
    char* combinedName = NULL;

    jobject oldMethod = c->ssl_private_key_method;
    if (method == NULL) {
        c->ssl_private_key_method = NULL;
        c->ssl_private_key_sign_method = NULL;
        c->ssl_private_key_decrypt_method = NULL;

        SSL_CTX_set_private_key_method(c->ctx, NULL);
    } else {
        jclass method_class = (*e)->GetObjectClass(e, method);
        if (method_class == NULL) {
            tcn_Throw(e, "Unable to retrieve method class");
            return;
        }

        NETTY_JNI_UTIL_PREPEND(staticPackagePrefix, "io/netty/internal/tcnative/ResultCallback;)V", name, error);
        NETTY_JNI_UTIL_PREPEND("(JI[BL", name, combinedName, error);
        TCN_REASSIGN(name, combinedName);

        jmethodID signMethod = (*e)->GetMethodID(e, method_class, "sign", name);
        if (signMethod == NULL) {
            tcn_ThrowIllegalArgumentException(e, "Unable to retrieve sign method");
            return;
        }

        NETTY_JNI_UTIL_PREPEND(staticPackagePrefix, "io/netty/internal/tcnative/ResultCallback;)V", name, error);
        NETTY_JNI_UTIL_PREPEND("(J[BL", name, combinedName, error);
        TCN_REASSIGN(name, combinedName);

        jmethodID decryptMethod = (*e)->GetMethodID(e, method_class, "decrypt", name);
        if (decryptMethod == NULL) {
            tcn_ThrowIllegalArgumentException(e, "Unable to retrieve decrypt method");
            return;
        }

        jobject m = (*e)->NewGlobalRef(e, method);
        if (m == NULL) {
            tcn_throwOutOfMemoryError(e, "Unable to allocate memory for global reference");
            return;
        }
        c->ssl_private_key_method = m;
        c->ssl_private_key_sign_method = signMethod;
        c->ssl_private_key_decrypt_method = decryptMethod;

        SSL_CTX_set_private_key_method(c->ctx, &private_key_method);
    }
    if (oldMethod != NULL) {
        (*e)->DeleteGlobalRef(e, oldMethod);
    }

error:
    free(name);
    free(combinedName);
#else
    tcn_ThrowException(e, "Requires BoringSSL");
#endif // OPENSSL_IS_BORINGSSL
}

static int tcn_new_session_cb(SSL *ssl, SSL_SESSION *session) {
    JNIEnv *e = NULL;
    jboolean result = JNI_FALSE;
    tcn_ssl_ctxt_t *c = NULL;

    TCN_GET_SSL_CTX(ssl, c);
    TCN_ASSERT(c != NULL);

    if (tcn_get_java_env(&e) != JNI_OK) {
        return 0;
    }
    if (c->ssl_session_cache == NULL) {
        return 0;
    }

    result = (*e)->CallBooleanMethod(e, c->ssl_session_cache, c->ssl_session_cache_creation_method, P2J(ssl), P2J(session));

    if ((*e)->ExceptionCheck(e)) {
        return 0;
    }

    if (result == JNI_TRUE) {
        return 1;
    }
    return 0;
}

#if OPENSSL_VERSION_NUMBER >= 0x10100000L
static SSL_SESSION* tcn_get_session_cb(SSL *ssl, const unsigned char *session_id, int len, int *copy) {
#else
// Older versions of OpenSSL expect another signature then newer versions
// See https://github.com/openssl/openssl/blob/OpenSSL_1_0_2/ssl/ssl.h
static SSL_SESSION* tcn_get_session_cb(SSL *ssl, unsigned char *session_id, int len, int *copy) {
#endif // OPENSSL_VERSION_NUMBER >= 0x10100000L
    JNIEnv *e = NULL;
    jlong result = JNI_FALSE;
    tcn_ssl_ctxt_t *c = NULL;
    jbyteArray bArray = NULL;

    TCN_GET_SSL_CTX(ssl, c);
    TCN_ASSERT(c != NULL);

    if (tcn_get_java_env(&e) != JNI_OK) {
        return NULL;
    }
    if (c->ssl_session_cache == NULL) {
        return NULL;
    }

    if ((bArray = (*e)->NewByteArray(e, len)) == NULL) {
        return NULL;
    }

    (*e)->SetByteArrayRegion(e, bArray, 0, len, (jbyte*) session_id);

    result = (*e)->CallLongMethod(e, c->ssl_session_cache, c->ssl_session_cache_get_method, P2J(ssl), bArray);

    if ((*e)->ExceptionCheck(e)) {
        return NULL;
    }
    if (result == -1) {
        return NULL;
    }
    // Set copy to 0 and require the callback to explict call SSL_SESSION_up_ref to avoid issues in multi-threaded enviroments.
    // See https://commondatastorage.googleapis.com/chromium-boringssl-docs/ssl.h.html#SSL_CTX_sess_set_get_cb
    *copy = 0;
    return (SSL_SESSION*) result;
}

TCN_IMPLEMENT_CALL(void, SSLContext, setSSLSessionCache)(TCN_STDARGS, jlong ctx, jobject cache) {
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, /* void */);
    
    jobject oldCache = c->ssl_session_cache;
    if (cache == NULL) {
        c->ssl_session_cache = NULL;
        c->ssl_session_cache_creation_method = NULL;
        c->ssl_session_cache_get_method = NULL;

        SSL_CTX_sess_set_new_cb(c->ctx, NULL);
        SSL_CTX_sess_set_remove_cb(c->ctx, NULL);
        SSL_CTX_sess_set_get_cb(c->ctx, NULL);
    } else {
        jclass cache_class = (*e)->GetObjectClass(e, cache);
        if (cache_class == NULL) {
            tcn_Throw(e, "Unable to retrieve cache class");
            return;
        }

        jmethodID creationMethod = (*e)->GetMethodID(e, cache_class, "sessionCreated", "(JJ)Z");
        if (creationMethod == NULL) {
            tcn_ThrowIllegalArgumentException(e, "Unable to retrieve sessionCreated method");
            return;
        }

        jmethodID getMethod = (*e)->GetMethodID(e, cache_class, "getSession", "(J[B)J");
        if (getMethod == NULL) {
            tcn_ThrowIllegalArgumentException(e, "Unable to retrieve getSession method");
            return;
        }

        jobject ref = (*e)->NewGlobalRef(e, cache);
        if (ref == NULL) {
            tcn_throwOutOfMemoryError(e, "Unable to allocate memory for global reference");
            return;
        }
        c->ssl_session_cache = ref;
        c->ssl_session_cache_creation_method = creationMethod;
        c->ssl_session_cache_get_method = getMethod;

        SSL_CTX_sess_set_new_cb(c->ctx, &tcn_new_session_cb);
        SSL_CTX_sess_set_get_cb(c->ctx, &tcn_get_session_cb);
    }
    if (oldCache != NULL) {
        (*e)->DeleteGlobalRef(e, oldCache);
    } 
}

static int ssl_servername_cb(SSL *ssl, int *ad, void *arg)
{
    JNIEnv *e = NULL;
    tcn_ssl_ctxt_t *c = arg;
    jstring servername_str = NULL;
    jboolean result;

    const char *servername = SSL_get_servername(ssl, TLSEXT_NAMETYPE_host_name);
    if (servername != NULL) {
        if (tcn_get_java_env(&e) != JNI_OK) {
            return SSL_TLSEXT_ERR_ALERT_FATAL;
        }
        if ((servername_str = (*e)->NewStringUTF(e, servername)) == NULL) {
            return SSL_TLSEXT_ERR_ALERT_FATAL;
        }
        result = (*e)->CallBooleanMethod(e, c->sni_hostname_matcher, c->sni_hostname_matcher_method, P2J(ssl), servername_str);

        // We need to delete the local references so we not leak memory as this method is called via callback.
        NETTY_JNI_UTIL_DELETE_LOCAL(e, servername_str);

        // Check if java threw an exception.
        if ((*e)->ExceptionCheck(e)) {
            return SSL_TLSEXT_ERR_ALERT_FATAL;
        }

        return result == JNI_FALSE ? SSL_TLSEXT_ERR_ALERT_FATAL : SSL_TLSEXT_ERR_OK;
    }
    return SSL_TLSEXT_ERR_OK;
}

TCN_IMPLEMENT_CALL(void, SSLContext, setSniHostnameMatcher)(TCN_STDARGS, jlong ctx, jobject matcher)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, /* void */);

    jobject oldMatcher = c->sni_hostname_matcher;
    if (matcher == NULL) {
        c->sni_hostname_matcher = NULL;
        c->sni_hostname_matcher_method = NULL;

        SSL_CTX_set_tlsext_servername_callback(c->ctx, NULL);
        SSL_CTX_set_tlsext_servername_arg(c->ctx, NULL);
    } else {
        jclass matcher_class = (*e)->GetObjectClass(e, matcher);
        jmethodID method = (*e)->GetMethodID(e, matcher_class, "match", "(JLjava/lang/String;)Z");
        if (method == NULL) {
            tcn_ThrowIllegalArgumentException(e, "Unable to retrieve match method");
            return;
        }

        jobject m = (*e)->NewGlobalRef(e, matcher);
        if (m == NULL) {
            tcn_throwOutOfMemoryError(e, "Unable to allocate memory for global reference");
            return;
        }

        c->sni_hostname_matcher = m;
        c->sni_hostname_matcher_method = method;

        SSL_CTX_set_tlsext_servername_callback(c->ctx, ssl_servername_cb);
        SSL_CTX_set_tlsext_servername_arg(c->ctx, c);
    }

     // Delete the reference to the previous specified matcher if needed.
     if (oldMatcher != NULL) {
        (*e)->DeleteGlobalRef(e, oldMatcher);
    }
}

#ifdef OPENSSL_IS_BORINGSSL
static void keylog_cb(const SSL* ssl, const char *line) {
    if (line == NULL) {
        return;
    }

    tcn_ssl_state_t *state = tcn_SSL_get_app_state(ssl);
    if (state == NULL || state->ctx == NULL) {
        // There's nothing we can do without tcn_ssl_state_t.
        return;
    }

    JNIEnv *e = NULL;
    if (tcn_get_java_env(&e) != JNI_OK) {
        // There's nothing we can do with the JNIEnv*.
        return;
    }

    jbyteArray outputLine = NULL;
    int maxLen = 1048576; // 1 MiB.
    int len = strnlen(line, maxLen);
    if (len == maxLen) {
        // This line is suspiciously large. Bail on it.
        return;
    }
    if ((outputLine = (*e)->NewByteArray(e, len)) == NULL) {
        // We failed to allocate a byte array.
        return;
    }
    (*e)->SetByteArrayRegion(e, outputLine, 0, len, (const jbyte*) line);
    
    // Execute the java callback
    (*e)->CallVoidMethod(e, state->ctx->keylog_callback, state->ctx->keylog_callback_method,
                P2J(ssl), outputLine);
}
#endif // OPENSSL_IS_BORINGSSL

TCN_IMPLEMENT_CALL(jboolean, SSLContext, setKeyLogCallback)(TCN_STDARGS, jlong ctx, jobject callback)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, JNI_FALSE);

#ifdef OPENSSL_IS_BORINGSSL
    jobject oldCallback = c->keylog_callback;
    if (callback == NULL) {
        c->keylog_callback = NULL;
        c->keylog_callback_method = NULL;

        SSL_CTX_set_keylog_callback(c->ctx, NULL);
    } else {
        jclass callback_class = (*e)->GetObjectClass(e, callback);
        jmethodID method = (*e)->GetMethodID(e, callback_class, "handle", "(J[B)V");
        if (method == NULL) {
            tcn_ThrowIllegalArgumentException(e, "Unable to retrieve handle method");
            return JNI_FALSE;
        }

        jobject m = (*e)->NewGlobalRef(e, callback);
        if (m == NULL) {
            tcn_throwOutOfMemoryError(e, "Unable to allocate memory for global reference");
            return JNI_FALSE;
        }

        c->keylog_callback = m;
        c->keylog_callback_method = method;

        SSL_CTX_set_keylog_callback(c->ctx, keylog_cb);
    }

     // Delete the reference to the previous specified callback if needed.
     if (oldCallback != NULL) {
        (*e)->DeleteGlobalRef(e, oldCallback);
    }
    return JNI_TRUE;
#else
    return JNI_FALSE;
#endif // OPENSSL_IS_BORINGSSL
}

TCN_IMPLEMENT_CALL(jboolean, SSLContext, setSessionIdContext)(TCN_STDARGS, jlong ctx, jbyteArray sidCtx)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, JNI_FALSE);

    int len = (*e)->GetArrayLength(e, sidCtx);
    unsigned char *buf = NULL;
    int res;

    if ((buf = OPENSSL_malloc(len)) == NULL) {
        return JNI_FALSE;
    }

    (*e)->GetByteArrayRegion(e, sidCtx, 0, len, (jbyte*) buf);

    res = SSL_CTX_set_session_id_context(c->ctx, buf, len);
    OPENSSL_free(buf);

    if (res == 1) {
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

TCN_IMPLEMENT_CALL(jint, SSLContext, setMode)(TCN_STDARGS, jlong ctx, jint mode)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, 0);

    return (jint) SSL_CTX_set_mode(c->ctx, mode);
}


TCN_IMPLEMENT_CALL(jint, SSLContext, getMode)(TCN_STDARGS, jlong ctx)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, 0);

    return (jint) SSL_CTX_get_mode(c->ctx);
}

TCN_IMPLEMENT_CALL(jlong, SSLContext, getSslCtx)(TCN_STDARGS, jlong ctx)
{
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, 0);
    return P2J(c->ctx);
}


#if !defined(OPENSSL_NO_OCSP) && !defined(TCN_OCSP_NOT_SUPPORTED) && !defined(OPENSSL_IS_BORINGSSL)

static const int OCSP_CLIENT_ACK = 1;
static const int OCSP_SERVER_ACK = SSL_TLSEXT_ERR_OK;

/**
 * This function is being called from OpenSSL. We do everything in
 * Java-land and this callback is just a stub that returns the
 * right values to keep OpenSSL happy.
 *
 * The arg that is passed into this function is the pointer for one
 * of these values:
 *   OCSP_CLIENT_ACK
 *   OCSP_SERVER_ACK
 */
static int openssl_ocsp_callback(SSL *ssl, void *arg) {
    return *(const int*)arg;
}

#endif /* !defined(OPENSSL_NO_OCSP) && !defined(TCN_OCSP_NOT_SUPPORTED) && !defined(OPENSSL_IS_BORINGSSL) */

/**
 * Enables OCSP stapling for the given SSLContext
 */
TCN_IMPLEMENT_CALL(void, SSLContext, enableOcsp)(TCN_STDARGS, jlong ctx, jboolean client) {

    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, /* void */);

#if defined(OPENSSL_NO_OCSP) && !defined(OPENSSL_IS_BORINGSSL)
    tcn_ThrowException(e, "netty-tcnative was built without OCSP support");

#elif defined(TCN_OCSP_NOT_SUPPORTED)
    tcn_ThrowException(e, "OCSP stapling is not supported");

#elif !defined(OPENSSL_IS_BORINGSSL)
    //
    // The client and server use slightly different return values to signal
    // error and success to OpenSSL. We're going to do something naughty to
    // align OpenSSL's and BoringSSL's APIs and simply tell OpenSSL to use
    // a stubbed callback function that is always saying that things are OK.
    // The argument for the callback function is simply the pointer of the
    // return value.
    //
    const int *arg = (client ? &OCSP_CLIENT_ACK : &OCSP_SERVER_ACK);
    if (SSL_CTX_set_tlsext_status_arg(c->ctx, (void*) arg) <= 0L) {
        tcn_ThrowException(e, "SSL_CTX_set_tlsext_status_arg() failed");
        return;
    }

    if (SSL_CTX_set_tlsext_status_cb(c->ctx, openssl_ocsp_callback) <= 0L) {
        tcn_ThrowException(e, "SSL_CTX_set_tlsext_status_cb() failed");
        return;
    }
#endif
}

/**
 * Disables OCSP stapling for the given SSLContext
 */
TCN_IMPLEMENT_CALL(void, SSLContext, disableOcsp)(TCN_STDARGS, jlong ctx) {
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, /* void */);

// This does nothing in BoringSSL
#if !defined(OPENSSL_NO_OCSP) && !defined(TCN_OCSP_NOT_SUPPORTED) && !defined(OPENSSL_IS_BORINGSSL)
    SSL_CTX_set_tlsext_status_cb(c->ctx, NULL);
    SSL_CTX_set_tlsext_status_arg(c->ctx, NULL);
#endif
}

TCN_IMPLEMENT_CALL(void, SSLContext, setUseTasks)(TCN_STDARGS, jlong ctx, jboolean useTasks) {
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, /* void */);

    c->use_tasks = useTasks == JNI_TRUE ? 1 : 0;
}


TCN_IMPLEMENT_CALL(jboolean, SSLContext, setCurvesList0)(TCN_STDARGS, jlong ctx, jstring curves) {
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, JNI_FALSE);

    if (curves == NULL) {
        return JNI_FALSE;
    }
    const char *nativeString = (*e)->GetStringUTFChars(e, curves, 0);
    int ret = tcn_SSL_CTX_set1_curves_list(c->ctx, nativeString);
    (*e)->ReleaseStringUTFChars(e, curves, nativeString);

    return ret == 1 ? JNI_TRUE : JNI_FALSE;
}

TCN_IMPLEMENT_CALL(void, SSLContext, setMaxCertList)(TCN_STDARGS, jlong ctx, jint size) {
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);

    TCN_CHECK_NULL(c, ctx, /* void */);

    SSL_CTX_set_max_cert_list(c->ctx, size);
}

TCN_IMPLEMENT_CALL(jint, SSLContext, addCertificateCompressionAlgorithm0)(TCN_STDARGS, jlong ctx, jint direction, jint algorithmId, jobject algorithm) {
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);
    TCN_CHECK_NULL(c, ctx, 0);
    if (algorithm == NULL) {
        tcn_ThrowIllegalArgumentException(e, "Compression algorithm may not be null");
        return 0;
    }
    if (!(direction & SSL_CERT_COMPRESSION_DIRECTION_COMPRESS) && !(direction & SSL_CERT_COMPRESSION_DIRECTION_DECOMPRESS)) {
        tcn_ThrowIllegalArgumentException(e, "Invalid direction specified");
        return 0;
    }

#ifdef OPENSSL_IS_BORINGSSL

    jclass algorithmClass = (*e)->GetObjectClass(e, algorithm);
    if (algorithmClass == NULL) {
        tcn_Throw(e, "Unable to retrieve cert compression algorithm class");
        return 0;
    }

    jmethodID compressMethod = (*e)->GetMethodID(e, algorithmClass, "compress", "(J[B)[B");
    if (compressMethod == NULL) {
        tcn_ThrowIllegalArgumentException(e, "Unable to retrieve compress method");
        return 0;
    }

    jmethodID decompressMethod = (*e)->GetMethodID(e, algorithmClass, "decompress", "(JI[B)[B");
    if (decompressMethod == NULL) {
        tcn_ThrowIllegalArgumentException(e, "Unable to retrieve decompress method");
        return 0;
    }

    jobject algoRef = (*e)->NewGlobalRef(e, algorithm);
    if (algoRef == NULL) {
        tcn_throwOutOfMemoryError(e, "Unable to allocate memory for global cert compression algorithm reference");
        return 0;
    }

    int result = 0;
    switch (algorithmId) {
        case TLSEXT_cert_compression_zlib:
            result = SSL_CTX_add_cert_compression_alg(c->ctx, algorithmId,
                direction & SSL_CERT_COMPRESSION_DIRECTION_COMPRESS ? zlib_compress_java : NULL,
                direction & SSL_CERT_COMPRESSION_DIRECTION_DECOMPRESS ? zlib_decompress_java : NULL);
            if (result) {
                if (c->ssl_cert_compression_zlib_algorithm != NULL) {
                    (*e)->DeleteGlobalRef(e, c->ssl_cert_compression_zlib_algorithm);
                }
                c->ssl_cert_compression_zlib_algorithm = algoRef;
                c->ssl_cert_compression_zlib_compress_method = compressMethod;
                c->ssl_cert_compression_zlib_decompress_method = decompressMethod;
            }
            break;
        case TLSEXT_cert_compression_brotli:
            result = SSL_CTX_add_cert_compression_alg(c->ctx, algorithmId,
                direction & SSL_CERT_COMPRESSION_DIRECTION_COMPRESS ? brotli_compress_java : NULL,
                direction & SSL_CERT_COMPRESSION_DIRECTION_DECOMPRESS ? brotli_decompress_java : NULL);
            if (result) {
                if (c->ssl_cert_compression_brotli_algorithm != NULL) {
                    (*e)->DeleteGlobalRef(e, c->ssl_cert_compression_brotli_algorithm);
                }
                c->ssl_cert_compression_brotli_algorithm = algoRef;
                c->ssl_cert_compression_brotli_compress_method = compressMethod;
                c->ssl_cert_compression_brotli_decompress_method = decompressMethod;
            }
            break;
        case TLSEXT_cert_compression_zstd:
            result = SSL_CTX_add_cert_compression_alg(c->ctx, algorithmId,
                direction & SSL_CERT_COMPRESSION_DIRECTION_COMPRESS ? zstd_compress_java : NULL,
                direction & SSL_CERT_COMPRESSION_DIRECTION_DECOMPRESS ? zstd_decompress_java : NULL);
            if (result) {
                if (c->ssl_cert_compression_zstd_algorithm != NULL) {
                    (*e)->DeleteGlobalRef(e, c->ssl_cert_compression_zstd_algorithm);
                }
                c->ssl_cert_compression_zstd_algorithm = algoRef;
                c->ssl_cert_compression_zstd_compress_method = compressMethod;
                c->ssl_cert_compression_zstd_decompress_method = decompressMethod;
            }
            break;
        default:
             (*e)->DeleteGlobalRef(e, algoRef);
            tcn_ThrowException(e, "Unrecognized certificate compression algorithm");
            return 0;
    }
    if (!result) {
        (*e)->DeleteGlobalRef(e, algoRef);
        tcn_ThrowException(e, "Failed trying to add certificate compression algorithm");
    }
    return result;
#else
    tcn_Throw(e, "TLS Cert Compression only supported by BoringSSL");
    return 0;
#endif // OPENSSL_IS_BORINGSSL
}

// JNI Method Registration Table Begin
static const JNINativeMethod fixed_method_table[] = {
  { TCN_METHOD_TABLE_ENTRY(make, (II)J, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(free, (J)I, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(setContextId, (JLjava/lang/String;)V, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(setOptions, (JI)V, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(getOptions, (J)I, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(clearOptions, (JI)V, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(setCipherSuite, (JLjava/lang/String;Z)Z, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(setCertificateChainFile, (JLjava/lang/String;Z)Z, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(setCertificateChainBio, (JJZ)Z, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(setCACertificateBio, (JJ)Z, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(setTmpDHLength, (JI)V, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(setVerify, (JII)V, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(setCertificate, (JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(setCertificateBio, (JJJLjava/lang/String;)Z, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(setNpnProtos0, (J[BI)V, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(setAlpnProtos0, (J[BI)V, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(setSessionCacheMode, (JJ)J, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(getSessionCacheMode, (J)J, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(setSessionCacheTimeout, (JJ)J, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(getSessionCacheTimeout, (J)J, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(setSessionCacheSize, (JJ)J, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(getSessionCacheSize, (J)J, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(sessionNumber, (J)J, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(sessionConnect, (J)J, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(sessionConnectGood, (J)J, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(sessionConnectRenegotiate, (J)J, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(sessionAccept, (J)J, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(sessionAcceptGood, (J)J, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(sessionAcceptRenegotiate, (J)J, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(sessionHits, (J)J, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(sessionCbHits, (J)J, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(sessionMisses, (J)J, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(sessionTimeouts, (J)J, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(sessionCacheFull, (J)J, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(sessionTicketKeyNew, (J)J, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(sessionTicketKeyResume, (J)J, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(sessionTicketKeyRenew, (J)J, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(sessionTicketKeyFail, (J)J, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(setSessionTicketKeys0, (J[B)V, SSLContext) },

  // setCertVerifyCallback -> needs dynamic method table
  // setCertRequestedCallback -> needs dynamic method table
  // setCertificateCallback -> needs dynamic method table
  // setSniHostnameMatcher -> needs dynamic method table
  // setKeyLogCallback -> needs dynamic method table
  // setPrivateKeyMethod0 --> needs dynamic method table
  // setSSLSessionCache --> needs dynamic method table

  { TCN_METHOD_TABLE_ENTRY(setSessionIdContext, (J[B)Z, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(setMode, (JI)I, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(getMode, (J)I, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(enableOcsp, (JZ)V, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(disableOcsp, (J)V, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(getSslCtx, (J)J, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(setUseTasks, (JZ)V, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(setNumTickets, (JI)Z, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(setCurvesList0, (JLjava/lang/String;)Z, SSLContext) },
  { TCN_METHOD_TABLE_ENTRY(setMaxCertList, (JI)V, SSLContext) }
  // addCertificateCompressionAlgorithm0 --> needs dynamic method table
};

static const jint fixed_method_table_size = sizeof(fixed_method_table) / sizeof(fixed_method_table[0]);

static jint dynamicMethodsTableSize() {
    return fixed_method_table_size + 8;
}

static JNINativeMethod* createDynamicMethodsTable(const char* packagePrefix) {
    char* dynamicTypeName = NULL;
    int len = sizeof(JNINativeMethod) * dynamicMethodsTableSize();
    JNINativeMethod* dynamicMethods = malloc(len);
    if (dynamicMethods == NULL) {
        goto error;
    }
    memset(dynamicMethods, 0, len);
    memcpy(dynamicMethods, fixed_method_table, sizeof(fixed_method_table));

    JNINativeMethod* dynamicMethod = &dynamicMethods[fixed_method_table_size];
    NETTY_JNI_UTIL_PREPEND(packagePrefix, "io/netty/internal/tcnative/CertificateVerifier;)V", dynamicTypeName, error);
    NETTY_JNI_UTIL_PREPEND("(JL", dynamicTypeName,  dynamicMethod->signature, error);
    netty_jni_util_free_dynamic_name(&dynamicTypeName);
    dynamicMethod->name = "setCertVerifyCallback";
    dynamicMethod->fnPtr = (void *) TCN_FUNCTION_NAME(SSLContext, setCertVerifyCallback);

    dynamicMethod = &dynamicMethods[fixed_method_table_size + 1];
    NETTY_JNI_UTIL_PREPEND(packagePrefix, "io/netty/internal/tcnative/CertificateRequestedCallback;)V", dynamicTypeName, error);
    NETTY_JNI_UTIL_PREPEND("(JL", dynamicTypeName,  dynamicMethod->signature, error);
    netty_jni_util_free_dynamic_name(&dynamicTypeName);
    dynamicMethod->name = "setCertRequestedCallback";
    dynamicMethod->fnPtr = (void *) TCN_FUNCTION_NAME(SSLContext, setCertRequestedCallback);

    dynamicMethod = &dynamicMethods[fixed_method_table_size + 2];
    NETTY_JNI_UTIL_PREPEND(packagePrefix, "io/netty/internal/tcnative/CertificateCallback;)V", dynamicTypeName, error);
    NETTY_JNI_UTIL_PREPEND("(JL", dynamicTypeName,  dynamicMethod->signature, error);
    netty_jni_util_free_dynamic_name(&dynamicTypeName);
    dynamicMethod->name = "setCertificateCallback";
    dynamicMethod->fnPtr = (void *) TCN_FUNCTION_NAME(SSLContext, setCertificateCallback);

    dynamicMethod = &dynamicMethods[fixed_method_table_size + 3];
    NETTY_JNI_UTIL_PREPEND(packagePrefix, "io/netty/internal/tcnative/SniHostNameMatcher;)V", dynamicTypeName, error);
    NETTY_JNI_UTIL_PREPEND("(JL", dynamicTypeName,  dynamicMethod->signature, error);
    netty_jni_util_free_dynamic_name(&dynamicTypeName);
    dynamicMethod->name = "setSniHostnameMatcher";
    dynamicMethod->fnPtr = (void *) TCN_FUNCTION_NAME(SSLContext, setSniHostnameMatcher);

    dynamicMethod = &dynamicMethods[fixed_method_table_size + 4];
    NETTY_JNI_UTIL_PREPEND(packagePrefix, "io/netty/internal/tcnative/KeyLogCallback;)Z", dynamicTypeName, error);
    NETTY_JNI_UTIL_PREPEND("(JL", dynamicTypeName,  dynamicMethod->signature, error);
    netty_jni_util_free_dynamic_name(&dynamicTypeName);
    dynamicMethod->name = "setKeyLogCallback";
    dynamicMethod->fnPtr = (void *) TCN_FUNCTION_NAME(SSLContext, setKeyLogCallback);
  
    dynamicMethod = &dynamicMethods[fixed_method_table_size + 5];
    NETTY_JNI_UTIL_PREPEND(packagePrefix, "io/netty/internal/tcnative/AsyncSSLPrivateKeyMethod;)V", dynamicTypeName, error);
    NETTY_JNI_UTIL_PREPEND("(JL", dynamicTypeName,  dynamicMethod->signature, error);
    netty_jni_util_free_dynamic_name(&dynamicTypeName);
    dynamicMethod->name = "setPrivateKeyMethod0";
    dynamicMethod->fnPtr = (void *) TCN_FUNCTION_NAME(SSLContext, setPrivateKeyMethod0);

    dynamicMethod = &dynamicMethods[fixed_method_table_size + 6];
    NETTY_JNI_UTIL_PREPEND(packagePrefix, "io/netty/internal/tcnative/SSLSessionCache;)V", dynamicTypeName, error); 
    NETTY_JNI_UTIL_PREPEND("(JL", dynamicTypeName,  dynamicMethod->signature, error);
    netty_jni_util_free_dynamic_name(&dynamicTypeName);
    dynamicMethod->name = "setSSLSessionCache";
    dynamicMethod->fnPtr = (void *) TCN_FUNCTION_NAME(SSLContext, setSSLSessionCache);

    dynamicMethod = &dynamicMethods[fixed_method_table_size + 7];
    NETTY_JNI_UTIL_PREPEND(packagePrefix, "io/netty/internal/tcnative/CertificateCompressionAlgo;)I", dynamicTypeName, error);
    NETTY_JNI_UTIL_PREPEND("(JIIL", dynamicTypeName,  dynamicMethod->signature, error);
    netty_jni_util_free_dynamic_name(&dynamicTypeName);
    dynamicMethod->name = "addCertificateCompressionAlgorithm0";
    dynamicMethod->fnPtr = (void *) TCN_FUNCTION_NAME(SSLContext, addCertificateCompressionAlgorithm0);

    return dynamicMethods;
error:
    netty_jni_util_free_dynamic_methods_table(dynamicMethods, fixed_method_table_size, dynamicMethodsTableSize());
    free(dynamicTypeName);
    return NULL;
}

// JNI Method Registration Table End

// IMPORTANT: If you add any NETTY_JNI_UTIL_LOAD_CLASS or NETTY_JNI_UTIL_FIND_CLASS calls you also need to update
//            Library to reflect that.
jint netty_internal_tcnative_SSLContext_JNI_OnLoad(JNIEnv* env, const char* packagePrefix) {
    char* name = NULL;
    char* combinedName = NULL;
    jclass sslTask_class = NULL;
    jclass certificateCallbackTask_class = NULL;
    jclass certificateVerifierTask_class = NULL;
    jclass sslPrivateKeyMethodTask_class = NULL;
    jclass sslPrivateKeyMethodSignTask_class = NULL;
    jclass sslPrivateKeyMethodDecryptTask_class = NULL;
    JNINativeMethod* dynamicMethods = createDynamicMethodsTable(packagePrefix);
    if (dynamicMethods == NULL) {
        goto error;
    }
    if (netty_jni_util_register_natives(env,
            packagePrefix,
            SSLCONTEXT_CLASSNAME,
            dynamicMethods,
            dynamicMethodsTableSize()) != 0) {
        goto error;
    }

    NETTY_JNI_UTIL_PREPEND(packagePrefix, "io/netty/internal/tcnative/SSLTask", name, error);

    NETTY_JNI_UTIL_LOAD_CLASS_WEAK(env, sslTask_class_weak, name, error);
    NETTY_JNI_UTIL_NEW_LOCAL_FROM_WEAK(env, sslTask_class, sslTask_class_weak, error);
    NETTY_JNI_UTIL_GET_FIELD(env, sslTask_class, sslTask_returnValue, "returnValue", "I", error);
    NETTY_JNI_UTIL_GET_FIELD(env, sslTask_class, sslTask_complete, "complete", "Z", error);

    NETTY_JNI_UTIL_PREPEND(packagePrefix, "io/netty/internal/tcnative/CertificateCallbackTask", name, error);
    NETTY_JNI_UTIL_LOAD_CLASS_WEAK(env, certificateCallbackTask_class_weak, name, error);
    NETTY_JNI_UTIL_NEW_LOCAL_FROM_WEAK(env, certificateCallbackTask_class, certificateCallbackTask_class_weak, error);

    NETTY_JNI_UTIL_PREPEND(packagePrefix, "io/netty/internal/tcnative/CertificateCallback;)V", name, error);
    NETTY_JNI_UTIL_PREPEND("(J[B[[BL", name, combinedName, error);
    TCN_REASSIGN(name, combinedName);
    NETTY_JNI_UTIL_GET_METHOD(env, certificateCallbackTask_class, certificateCallbackTask_init, "<init>", name, error);

    NETTY_JNI_UTIL_PREPEND(packagePrefix, "io/netty/internal/tcnative/CertificateVerifierTask", name, error);
    NETTY_JNI_UTIL_LOAD_CLASS_WEAK(env, certificateVerifierTask_class_weak, name, error);
    NETTY_JNI_UTIL_NEW_LOCAL_FROM_WEAK(env, certificateVerifierTask_class, certificateVerifierTask_class_weak, error);

    NETTY_JNI_UTIL_PREPEND(packagePrefix, "io/netty/internal/tcnative/CertificateVerifier;)V", name, error);
    NETTY_JNI_UTIL_PREPEND("(J[[BLjava/lang/String;L", name, combinedName, error);
    TCN_REASSIGN(name, combinedName);
    NETTY_JNI_UTIL_GET_METHOD(env, certificateVerifierTask_class, certificateVerifierTask_init, "<init>", name, error);

    NETTY_JNI_UTIL_PREPEND(packagePrefix, "io/netty/internal/tcnative/SSLPrivateKeyMethodTask", name, error);
    NETTY_JNI_UTIL_LOAD_CLASS_WEAK(env, sslPrivateKeyMethodTask_class_weak, name, error);
    NETTY_JNI_UTIL_NEW_LOCAL_FROM_WEAK(env, sslPrivateKeyMethodTask_class, sslPrivateKeyMethodTask_class_weak, error);
    NETTY_JNI_UTIL_GET_FIELD(env, sslPrivateKeyMethodTask_class, sslPrivateKeyMethodTask_resultBytes, "resultBytes", "[B", error);

    NETTY_JNI_UTIL_PREPEND(packagePrefix, "io/netty/internal/tcnative/SSLPrivateKeyMethodSignTask", name, error);
    NETTY_JNI_UTIL_LOAD_CLASS_WEAK(env, sslPrivateKeyMethodSignTask_class_weak, name, error);
    NETTY_JNI_UTIL_NEW_LOCAL_FROM_WEAK(env, sslPrivateKeyMethodSignTask_class, sslPrivateKeyMethodSignTask_class_weak, error);

    NETTY_JNI_UTIL_PREPEND(packagePrefix, "io/netty/internal/tcnative/AsyncSSLPrivateKeyMethod;)V", name, error);
    NETTY_JNI_UTIL_PREPEND("(JI[BL", name, combinedName, error);
    TCN_REASSIGN(name, combinedName);
    NETTY_JNI_UTIL_GET_METHOD(env, sslPrivateKeyMethodSignTask_class, sslPrivateKeyMethodSignTask_init, "<init>", name, error);

    NETTY_JNI_UTIL_PREPEND(packagePrefix, "io/netty/internal/tcnative/SSLPrivateKeyMethodDecryptTask", name, error);
    NETTY_JNI_UTIL_LOAD_CLASS_WEAK(env, sslPrivateKeyMethodDecryptTask_class_weak, name, error);
    NETTY_JNI_UTIL_NEW_LOCAL_FROM_WEAK(env, sslPrivateKeyMethodDecryptTask_class, sslPrivateKeyMethodDecryptTask_class_weak, error);

    NETTY_JNI_UTIL_PREPEND(packagePrefix, "io/netty/internal/tcnative/AsyncSSLPrivateKeyMethod;)V", name, error);
    NETTY_JNI_UTIL_PREPEND("(J[BL", name, combinedName, error);
    TCN_REASSIGN(name, combinedName);
    NETTY_JNI_UTIL_GET_METHOD(env, sslPrivateKeyMethodDecryptTask_class, sslPrivateKeyMethodDecryptTask_init, "<init>", name, error);

    if (packagePrefix != NULL) {
        staticPackagePrefix = strdup(packagePrefix);
    }
    return NETTY_JNI_UTIL_JNI_VERSION;
error:
    free(name);
    free(combinedName);
    netty_jni_util_free_dynamic_methods_table(dynamicMethods, fixed_method_table_size, dynamicMethodsTableSize());

    NETTY_JNI_UTIL_DELETE_LOCAL(env, sslTask_class);
    NETTY_JNI_UTIL_DELETE_LOCAL(env, certificateCallbackTask_class);
    NETTY_JNI_UTIL_DELETE_LOCAL(env, certificateVerifierTask_class);
    NETTY_JNI_UTIL_DELETE_LOCAL(env, sslPrivateKeyMethodTask_class);
    NETTY_JNI_UTIL_DELETE_LOCAL(env, sslPrivateKeyMethodSignTask_class);
    NETTY_JNI_UTIL_DELETE_LOCAL(env, sslPrivateKeyMethodDecryptTask_class);
    return JNI_ERR;
}

void netty_internal_tcnative_SSLContext_JNI_OnUnLoad(JNIEnv* env, const char* packagePrefix) {
    NETTY_JNI_UTIL_UNLOAD_CLASS_WEAK(env, sslTask_class_weak);
    NETTY_JNI_UTIL_UNLOAD_CLASS_WEAK(env, certificateCallbackTask_class_weak);
    NETTY_JNI_UTIL_UNLOAD_CLASS_WEAK(env, certificateVerifierTask_class_weak);
    NETTY_JNI_UTIL_UNLOAD_CLASS_WEAK(env, sslPrivateKeyMethodTask_class_weak);
    NETTY_JNI_UTIL_UNLOAD_CLASS_WEAK(env, sslPrivateKeyMethodSignTask_class_weak);
    NETTY_JNI_UTIL_UNLOAD_CLASS_WEAK(env, sslPrivateKeyMethodDecryptTask_class_weak);

    free((void*) staticPackagePrefix);
    staticPackagePrefix = NULL;

    netty_jni_util_unregister_natives(env, packagePrefix, SSLCONTEXT_CLASSNAME);
}

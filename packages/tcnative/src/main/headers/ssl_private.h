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

#ifndef SSL_PRIVATE_H
#define SSL_PRIVATE_H

/* Exclude unused OpenSSL features
 * even if the OpenSSL supports them
 */
#ifndef OPENSSL_NO_IDEA
#define OPENSSL_NO_IDEA
#endif
#ifndef OPENSSL_NO_KRB5
#define OPENSSL_NO_KRB5
#endif
#ifndef OPENSSL_NO_MDC2
#define OPENSSL_NO_MDC2
#endif
#ifndef OPENSSL_NO_RC5
#define OPENSSL_NO_RC5
#endif

#include "apr_thread_rwlock.h"
#include "apr_atomic.h"
#include <stdbool.h>

/* OpenSSL headers */
#include <openssl/opensslv.h>
#include <openssl/ssl.h>
#include <openssl/err.h>
#include <openssl/x509.h>
#include <openssl/pem.h>
#include <openssl/pkcs12.h>
#include <openssl/crypto.h>
#include <openssl/evp.h>
#include <openssl/rand.h>
#include <openssl/x509v3.h>
#include <openssl/hmac.h>
#include <openssl/dh.h>
#if OPENSSL_VERSION_NUMBER >= 0x30000000
#include <openssl/core_names.h>
#endif

#define ERR_LEN 256

/* Avoid tripping over an engine build installed globally and detected
 * when the user points at an explicit non-engine flavor of OpenSSL
 */
#ifndef OPENSSL_NO_ENGINE
#include <openssl/engine.h>
#endif

#ifndef RAND_MAX
#include <limits.h>
#define RAND_MAX INT_MAX
#endif

/*
 * Define IDs for the temporary RSA keys and DH params
 */

#define SSL_TMP_KEY_DH_512      (1)
#define SSL_TMP_KEY_DH_1024     (2)
#define SSL_TMP_KEY_DH_2048     (3)
#define SSL_TMP_KEY_DH_4096     (4)
#define SSL_TMP_KEY_MAX         (5)

/*
 * Define the SSL Protocol options
 */
#define SSL_PROTOCOL_NONE       (0)
#define SSL_PROTOCOL_SSLV2      (1<<0)
#define SSL_PROTOCOL_SSLV3      (1<<1)
#define SSL_PROTOCOL_TLSV1      (1<<2)
#define SSL_PROTOCOL_TLSV1_1    (1<<3)
#define SSL_PROTOCOL_TLSV1_2    (1<<4)
/* TLS_*method according to https://www.openssl.org/docs/manmaster/ssl/SSL_CTX_new.html */
#define SSL_PROTOCOL_TLS        (SSL_PROTOCOL_SSLV3|SSL_PROTOCOL_TLSV1|SSL_PROTOCOL_TLSV1_1|SSL_PROTOCOL_TLSV1_2)
#define SSL_PROTOCOL_ALL        (SSL_PROTOCOL_SSLV2|SSL_PROTOCOL_TLS)

#define SSL_MODE_CLIENT         (0)
#define SSL_MODE_SERVER         (1)
#define SSL_MODE_COMBINED       (2)

#define SSL_DEFAULT_CACHE_SIZE  (256)
#define SSL_DEFAULT_VHOST_NAME  ("_default_:443")

#define SSL_CVERIFY_IGNORED             (-1)
#define SSL_CVERIFY_NONE                (0)
#define SSL_CVERIFY_OPTIONAL            (1)
#define SSL_CVERIFY_REQUIRED            (2)

#define SSL_TO_APR_ERROR(X)         (APR_OS_START_USERERR + 1000 + X)

extern const char* TCN_UNKNOWN_AUTH_METHOD;

/* ECC: make sure we have at least 1.0.0 */
#if !defined(OPENSSL_NO_EC) && defined(TLSEXT_ECPOINTFORMAT_uncompressed)
#define HAVE_ECC              1
#endif

// TODO(scott): remove this as OpenSSL supports it in older version, or we drop support for older versions.
#ifndef TLS1_3_VERSION
#define TLS1_3_VERSION 0x0304
#endif

#ifndef SSL_OP_NO_TLSv1_3
// TLSV1_3 is not really supported by the underlying OPENSSL version
#ifndef OPENSSL_NO_TLS1_3
#define OPENSSL_NO_TLS1_3
#endif // OPENSSL_NO_TLS1_3

#define SSL_OP_NO_TLSv1_3                               0x00000000U
#endif // SSL_OP_NO_TLSv1_3

/* OpenSSL 1.0.2 compatibility */
#if OPENSSL_VERSION_NUMBER < 0x10100000L || (defined(LIBRESSL_VERSION_NUMBER) && LIBRESSL_VERSION_NUMBER < 0x20700000)
#define TLS_method SSLv23_method
#define TLS_client_method SSLv23_client_method
#define TLS_server_method SSLv23_server_method

// This is only needed if we are not using LibreSSL 2.7.x or higher as otherwise it
// is defined already.
#if !defined(LIBRESSL_VERSION_NUMBER) || LIBRESSL_VERSION_NUMBER < 0x20700000L
#define OPENSSL_VERSION SSLEAY_VERSION
#endif

#define OpenSSL_version SSLeay_version
#define OPENSSL_malloc_init CRYPTO_malloc_init
#define X509_REVOKED_get0_serialNumber(x) x->serialNumber
#define OpenSSL_version_num SSLeay
#define BIO_get_init(x)       ((x)->init)
#define BIO_set_init(x, v)     ((x)->init = (v))
#define BIO_get_data(x)       ((x)->ptr)
#define BIO_set_data(x, v)     ((x)->ptr = (v))
#define BIO_set_shutdown(x, v) ((x)->shutdown = (v))
#define BIO_get_shutdown(x)   ((x)->shutdown)
#endif /* OPENSSL_VERSION_NUMBER < 0x10100000L */

#define SSL_SELECTOR_FAILURE_NO_ADVERTISE                       0
#define SSL_SELECTOR_FAILURE_CHOOSE_MY_LAST_PROTOCOL            1

#define SSL_SESSION_TICKET_KEY_NAME_LEN 16
#define SSL_SESSION_TICKET_AES_KEY_LEN  16
#define SSL_SESSION_TICKET_HMAC_KEY_LEN 16
#define SSL_SESSION_TICKET_KEY_SIZE     48

#define SSL_CERT_COMPRESSION_DIRECTION_COMPRESS     0x01
#define SSL_CERT_COMPRESSION_DIRECTION_DECOMPRESS   0x02
#define SSL_CERT_COMPRESSION_DIRECTION_BOTH         0x03

extern void *SSL_temp_keys[SSL_TMP_KEY_MAX];

// HACK!
// LibreSSL 2.4.x doesn't support the X509_V_ERR_UNSPECIFIED so we introduce a work around to make sure a supported alert is used.
// This should be reverted when we support LibreSSL 2.5.x (which does support X509_V_ERR_UNSPECIFIED).
#ifndef X509_V_ERR_UNSPECIFIED
#define TCN_X509_V_ERR_UNSPECIFIED 99999
#else
#define TCN_X509_V_ERR_UNSPECIFIED (X509_V_ERR_UNSPECIFIED)
#endif /*X509_V_ERR_UNSPECIFIED*/

// BoringSSL compat
#ifndef OPENSSL_IS_BORINGSSL
#ifndef SSL_ERROR_WANT_PRIVATE_KEY_OPERATION
#define SSL_ERROR_WANT_PRIVATE_KEY_OPERATION -1
#endif // SSL_ERROR_WANT_PRIVATE_KEY_OPERATION

#ifndef SSL_MODE_ENABLE_FALSE_START
#define SSL_MODE_ENABLE_FALSE_START 0
#endif // SSL_MODE_ENABLE_FALSE_START

// SSL_SIGN_* are signature algorithm values as defined in TLS 1.3.
#ifndef SSL_SIGN_RSA_PKCS1_SHA1
#define SSL_SIGN_RSA_PKCS1_SHA1 0x0201
#endif // SSL_SIGN_RSA_PKCS1_SHA1

#ifndef SSL_SIGN_RSA_PKCS1_SHA256
#define SSL_SIGN_RSA_PKCS1_SHA256 0x0401
#endif // SSL_SIGN_RSA_PKCS1_SHA256

#ifndef SSL_SIGN_RSA_PKCS1_SHA384
#define SSL_SIGN_RSA_PKCS1_SHA384 0x0501
#endif // SSL_SIGN_RSA_PKCS1_SHA384

#ifndef SSL_SIGN_RSA_PKCS1_SHA512
#define SSL_SIGN_RSA_PKCS1_SHA512 0x0601
#endif // SSL_SIGN_RSA_PKCS1_SHA512

#ifndef SSL_SIGN_ECDSA_SHA1
#define SSL_SIGN_ECDSA_SHA1 0x0203
#endif // SSL_SIGN_ECDSA_SHA1

#ifndef SSL_SIGN_ECDSA_SECP256R1_SHA256
#define SSL_SIGN_ECDSA_SECP256R1_SHA256 0x0403
#endif // SSL_SIGN_ECDSA_SECP256R1_SHA256

#ifndef SSL_SIGN_ECDSA_SECP384R1_SHA384
#define SSL_SIGN_ECDSA_SECP384R1_SHA384 0x0503
#endif // SSL_SIGN_ECDSA_SECP384R1_SHA384

#ifndef SSL_SIGN_ECDSA_SECP521R1_SHA512
#define SSL_SIGN_ECDSA_SECP521R1_SHA512 0x0603
#endif

#ifndef SSL_SIGN_RSA_PSS_RSAE_SHA256
#define SSL_SIGN_RSA_PSS_RSAE_SHA256 0x0804
#endif // SSL_SIGN_RSA_PSS_RSAE_SHA256

#ifndef SSL_SIGN_RSA_PSS_RSAE_SHA384
#define SSL_SIGN_RSA_PSS_RSAE_SHA384 0x0805
#endif // SSL_SIGN_RSA_PSS_RSAE_SHA384

#ifndef SSL_SIGN_RSA_PSS_RSAE_SHA512
#define SSL_SIGN_RSA_PSS_RSAE_SHA512 0x0806
#endif // SSL_SIGN_RSA_PSS_RSAE_SHA512

#ifndef SSL_SIGN_ED25519
#define SSL_SIGN_ED25519 0x0807
#endif // SSL_SIGN_ED25519

#ifndef SSL_SIGN_RSA_PKCS1_MD5_SHA1
#define SSL_SIGN_RSA_PKCS1_MD5_SHA1 0xff01
#endif // SSL_SIGN_RSA_PKCS1_MD5_SHA1

#endif // OPENSSL_IS_BORINGSSL

// OCSP stapling should be present in OpenSSL as of version 1.0.0 but
// we've only tested 1.0.2 and we need to support 1.0.1 because the
// dynamically linked version of netty-tcnative is built with 1.0.1.
#if OPENSSL_VERSION_NUMBER < 0x10001000L
#define TCN_OCSP_NOT_SUPPORTED
#endif

/* Define if not already exists as this will be used to be able to compile against older versions of openssl
   while use newer when running the app */
#ifndef SSL_CTRL_CHAIN_CERT
#define SSL_CTRL_CHAIN_CERT                     89
#endif
#ifndef SSL_CTRL_GET_CLIENT_CERT_TYPES
#define SSL_CTRL_GET_CLIENT_CERT_TYPES          103
#endif

#ifndef SSL_ERROR_WANT_CERTIFICATE_VERIFY
// See https://github.com/google/boringssl/blob/chromium-stable/include/openssl/ssl.h#L538
#define SSL_ERROR_WANT_CERTIFICATE_VERIFY       -1
#endif

#ifndef TLSEXT_cert_compression_zlib
// See https://datatracker.ietf.org/doc/html/rfc8879#section-3
#define TLSEXT_cert_compression_zlib            1
#endif

#ifndef TLSEXT_cert_compression_brotli
// See https://datatracker.ietf.org/doc/html/rfc8879#section-3
#define TLSEXT_cert_compression_brotli          2
#endif

#ifndef TLSEXT_cert_compression_zstd
// See https://datatracker.ietf.org/doc/html/rfc8879#section-3
#define TLSEXT_cert_compression_zstd            3
#endif

typedef struct tcn_ssl_ctxt_t tcn_ssl_ctxt_t;

typedef struct {
    unsigned char   key_name[SSL_SESSION_TICKET_KEY_NAME_LEN];
#if OPENSSL_VERSION_NUMBER < 0x30000000L
    unsigned char hmac_key[SSL_SESSION_TICKET_HMAC_KEY_LEN];
#else
    OSSL_PARAM mac_params[3];
#endif
    unsigned char   aes_key[SSL_SESSION_TICKET_AES_KEY_LEN];
} tcn_ssl_ticket_key_t;

typedef struct {
    int verify_depth;
    int verify_mode;
} tcn_ssl_verify_config_t;

#ifdef OPENSSL_IS_BORINGSSL
extern const SSL_PRIVATE_KEY_METHOD private_key_method;
#endif // OPENSSL_IS_BORINGSSL

struct tcn_ssl_ctxt_t {
    apr_pool_t*              pool;
    SSL_CTX*                 ctx;

    /* Holds the alpn protocols, each of them prefixed with the len of the protocol */
    unsigned char*           alpn_proto_data;
    unsigned char*           next_proto_data;

    /* for client or downstream server authentication */
    char*                    password;

    apr_thread_rwlock_t*     mutex; // Session ticket mutext
    tcn_ssl_ticket_key_t*    ticket_keys;

    /* certificate verifier callback */
    jobject                  verifier;
    jmethodID                verifier_method;

    jobject                  cert_requested_callback;
    jmethodID                cert_requested_callback_method;

    jobject                  certificate_callback;
    jmethodID                certificate_callback_method;

    jobject                  sni_hostname_matcher;
    jmethodID                sni_hostname_matcher_method;

    jobject                  ssl_session_cache;
    jmethodID                ssl_session_cache_creation_method;
    jmethodID                ssl_session_cache_get_method;

#ifdef OPENSSL_IS_BORINGSSL
    jobject                  ssl_private_key_method;
    jmethodID                ssl_private_key_sign_method;
    jmethodID                ssl_private_key_decrypt_method;

    jobject                  ssl_cert_compression_zlib_algorithm;
    jmethodID                ssl_cert_compression_zlib_compress_method;
    jmethodID                ssl_cert_compression_zlib_decompress_method;

    jobject                  ssl_cert_compression_brotli_algorithm;
    jmethodID                ssl_cert_compression_brotli_compress_method;
    jmethodID                ssl_cert_compression_brotli_decompress_method;

    jobject                  ssl_cert_compression_zstd_algorithm;
    jmethodID                ssl_cert_compression_zstd_compress_method;
    jmethodID                ssl_cert_compression_zstd_decompress_method;

    jobject                  keylog_callback;
    jmethodID                keylog_callback_method;
#endif // OPENSSL_IS_BORINGSSL

    tcn_ssl_verify_config_t  verify_config;

    int                      protocol;
    /* we are one or the other */
    int                      mode;

    unsigned int             next_proto_len;
    int                      next_selector_failure_behavior;

    unsigned int             alpn_proto_len;
    int                      alpn_selector_failure_behavior;

    unsigned int             ticket_keys_len;
    unsigned int             pad;

    /* TLS ticket key session resumption statistics */

    // The client did not present a ticket and we issued a new one.
    apr_uint32_t             ticket_keys_new;
    // The client presented a ticket derived from the primary key
    apr_uint32_t             ticket_keys_resume;
    // The client presented a ticket derived from an older key, and we upgraded to the primary key.
    apr_uint32_t             ticket_keys_renew;
    // The client presented a ticket that did not match any key in the list.
    apr_uint32_t             ticket_keys_fail;

    unsigned char            context_id[SHA_DIGEST_LENGTH];

    int                      use_tasks;
};

// Store the callback to run and also if it was consumed via SSL.getTask(...).
typedef struct tcn_ssl_task_t tcn_ssl_task_t;
struct tcn_ssl_task_t {
    jboolean consumed;
    jobject task;
};

tcn_ssl_task_t* tcn_ssl_task_new(JNIEnv*, jobject);
void tcn_ssl_task_free(JNIEnv*, tcn_ssl_task_t*);

typedef struct tcn_ssl_state_t tcn_ssl_state_t;
struct tcn_ssl_state_t {
    int handshakeCount;
    tcn_ssl_ctxt_t *ctx;
    tcn_ssl_task_t* ssl_task;
    tcn_ssl_verify_config_t verify_config;
};

#define TCN_GET_SSL_CTX(ssl, C)                             \
    NETTY_JNI_UTIL_BEGIN_MACRO                              \
        tcn_ssl_state_t* _S = tcn_SSL_get_app_state(ssl);   \
        if (_S == NULL) {                                   \
            C = NULL;                                       \
        } else {                                            \
            C = _S->ctx;                                    \
        }                                                   \
   NETTY_JNI_UTIL_END_MACRO

/*
 *  Additional Functions
 */
void        tcn_init_app_state_idx(void);
// The app_data is used to store the tcn_ssl_ctxt_t pointer for the SSL instance.
void       *tcn_SSL_get_app_state(const SSL *);
void        tcn_SSL_set_app_state(SSL *, void *);
void       *tcn_SSL_CTX_get_app_state(const SSL_CTX *);
void        tcn_SSL_CTX_set_app_state(SSL_CTX *, void *);

int         tcn_SSL_password_callback(char *, int, int, void *);
#if OPENSSL_VERSION_NUMBER < 0x30000000
DH         *tcn_SSL_dh_get_tmp_param(int);
DH         *tcn_SSL_callback_tmp_DH(SSL *, int, int);
// The following provided callbacks will always return DH of a given length.
// See https://www.openssl.org/docs/manmaster/ssl/SSL_CTX_set_tmp_dh_callback.html
DH         *tcn_SSL_callback_tmp_DH_512(SSL *, int, int);
DH         *tcn_SSL_callback_tmp_DH_1024(SSL *, int, int);
DH         *tcn_SSL_callback_tmp_DH_2048(SSL *, int, int);
DH         *tcn_SSL_callback_tmp_DH_4096(SSL *, int, int);
#endif
int         tcn_SSL_CTX_use_certificate_chain(SSL_CTX *, const char *, bool);
int         tcn_SSL_CTX_use_certificate_chain_bio(SSL_CTX *, BIO *, bool);
int         tcn_SSL_CTX_use_client_CA_bio(SSL_CTX *, BIO *);

#ifndef OPENSSL_IS_BORINGSSL
X509        *tcn_load_pem_cert_bio(const char *, const BIO *);
int         tcn_SSL_use_certificate_chain_bio(SSL *, BIO *, bool);
#endif // OPENSSL_IS_BORINGSSL

EVP_PKEY    *tcn_load_pem_key_bio(const char *, const BIO *);
int         tcn_set_verify_config(tcn_ssl_verify_config_t* c, jint tcn_mode, jint depth);
int         tcn_EVP_PKEY_up_ref(EVP_PKEY* pkey);
int         tcn_X509_up_ref(X509* cert);
int         tcn_SSL_callback_next_protos(SSL *, const unsigned char **, unsigned int *, void *);
int         tcn_SSL_callback_select_next_proto(SSL *, unsigned char **, unsigned char *, const unsigned char *, unsigned int, void *);
int         tcn_SSL_callback_alpn_select_proto(SSL *, const unsigned char **, unsigned char *, const unsigned char *, unsigned int, void *);
const char *tcn_SSL_cipher_authentication_method(const SSL_CIPHER *);

#ifdef OPENSSL_IS_BORINGSSL
enum ssl_verify_result_t tcn_SSL_cert_custom_verify(SSL* ssl, uint8_t *out_alert);
#endif // OPENSSL_IS_BORINGSSL

#if (OPENSSL_VERSION_NUMBER >= 0x10002000L && !defined(LIBRESSL_VERSION_NUMBER)) || LIBRESSL_VERSION_NUMBER >= 0x2090200fL

#ifndef OPENSSL_IS_BORINGSSL
#define tcn_SSL_add1_chain_cert(ssl, x509) SSL_add1_chain_cert(ssl, x509)
#define tcn_SSL_add0_chain_cert(ssl, x509) SSL_add0_chain_cert(ssl, x509)
#endif // OPENSSL_IS_BORINGSSL

#define tcn_SSL_get0_certificate_types(ssl, clist) SSL_get0_certificate_types(ssl, clist)
#else
// This is what is defined in the SSL_add1_chain_cert / SSL_add0_chain_cert / SSL_get0_certificate_types macros.
#define tcn_SSL_add1_chain_cert(ssl, x509) SSL_ctrl(ssl, SSL_CTRL_CHAIN_CERT, 1, (char *) x509)
#define tcn_SSL_add0_chain_cert(ssl, x509) SSL_ctrl(ssl, SSL_CTRL_CHAIN_CERT, 0, (char *) x509)
#define tcn_SSL_get0_certificate_types(ssl, clist) SSL_ctrl(ssl, SSL_CTRL_GET_CLIENT_CERT_TYPES, 0, (char *)(clist))
#endif // defined(OPENSSL_IS_BORINGSSL) || (OPENSSL_VERSION_NUMBER >= 0x10002000L && !defined(LIBRESSL_VERSION_NUMBER))

#if defined(__GNUC__) || defined(__GNUG__)
    // only supported with GCC, this will be used to support different openssl versions at the same time.
#ifndef OPENSSL_IS_BORINGSSL
    extern int SSL_CTX_set_alpn_protos(SSL_CTX *ctx, const unsigned char *protos,
           unsigned protos_len) __attribute__((weak));
    extern void SSL_CTX_set_alpn_select_cb(SSL_CTX *ctx, int (*cb) (SSL *ssl, const unsigned char **out,
           unsigned char *outlen, const unsigned char *in, unsigned int inlen,
           void *arg), void *arg) __attribute__((weak));
    extern void SSL_get0_alpn_selected(const SSL *ssl, const unsigned char **data,
           unsigned *len) __attribute__((weak));
    extern void SSL_CTX_set_cert_cb(SSL_CTX *c, int (*cert_cb)(SSL *ssl, void *arg), void *arg) __attribute__((weak));

    extern X509_VERIFY_PARAM *SSL_get0_param(SSL *ssl) __attribute__((weak));
    extern void X509_VERIFY_PARAM_set_hostflags(X509_VERIFY_PARAM *param, unsigned int flags) __attribute__((weak));
    extern int X509_VERIFY_PARAM_set1_host(X509_VERIFY_PARAM *param, const char *name, size_t namelen) __attribute__((weak));
    extern int SSL_CTX_set_num_tickets(SSL_CTX *ctx, size_t num_tickets) __attribute__((weak));
    extern int SSL_SESSION_up_ref(SSL_SESSION *session) __attribute__((weak));
#endif // OPENSSL_IS_BORINGSSL

    extern int SSL_get_sigalgs(SSL *s, int idx, int *psign, int *phash, int *psignhash, unsigned char *rsig, unsigned char *rhash) __attribute__((weak));
#endif

#ifdef OPENSSL_IS_BORINGSSL
#define tcn_SSL_CTX_set1_curves_list(ctx, s) SSL_CTX_set1_curves_list(ctx, s)
#else
#ifndef SSL_CTRL_SET_GROUPS_LIST
#define SSL_CTRL_SET_GROUPS_LIST                92
#endif // SSL_CTRL_SET_GROUPS_LIST
#define tcn_SSL_CTX_set1_curves_list(ctx, s) SSL_CTX_ctrl(ctx, SSL_CTRL_SET_GROUPS_LIST, 0, (char *)(s))
#endif // OPENSSL_IS_BORINGSSL

#endif /* SSL_PRIVATE_H */

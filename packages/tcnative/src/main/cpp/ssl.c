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

#include <stdbool.h>
#include <openssl/bio.h>

#ifndef OPENSSL_NO_ENGINE
#include <openssl/ui.h>
#endif // OPENSSL_NO_ENGINE

#include "tcn.h"
#include "apr_file_io.h"
#include "apr_thread_mutex.h"
#include "apr_atomic.h"
#include "apr_strings.h"
#include "apr_portable.h"
#include "ssl_private.h"
#include "ssl.h"

#define SSL_CLASSNAME  "io/netty/internal/tcnative/SSL"

static int ssl_initialized = 0;
extern apr_pool_t *tcn_global_pool;

void *SSL_temp_keys[SSL_TMP_KEY_MAX];

#ifndef OPENSSL_NO_ENGINE
static ENGINE *tcn_ssl_engine = NULL;
static UI_METHOD *ui_method = NULL;
#endif // OPENSSL_NO_ENGINE


#if defined(OPENSSL_FIPS) && (OPENSSL_VERSION_NUMBER < 0x30000000L)
#define tcn_enable_fips(to)   FIPS_mode_set((to))
#else
#define tcn_enable_fips(to)   EVP_default_properties_enable_fips(NULL, (to))
#endif

#if OPENSSL_VERSION_NUMBER < 0x10100000L || (defined(LIBRESSL_VERSION_NUMBER) && LIBRESSL_VERSION_NUMBER < 0x2090000fL)

/* Global reference to the pool used by the dynamic mutexes */
static apr_pool_t *dynlockpool = NULL;

/* Dynamic lock structure */
struct CRYPTO_dynlock_value {
    apr_pool_t *pool;
    const char* file;
    int line;
    apr_thread_mutex_t *mutex;
};
#endif // OPENSSL_VERSION_NUMBER < 0x10100000L || (defined(LIBRESSL_VERSION_NUMBER) && LIBRESSL_VERSION_NUMBER < 0x2090000fL)

struct TCN_bio_bytebuffer {
    // Pointer arithmetic is done on this variable. The type must correspond to a "byte" size.
    char* buffer;
    char* nonApplicationBuffer;
    jint  nonApplicationBufferSize;
    jint  nonApplicationBufferOffset;
    jint  nonApplicationBufferLength;
    jint  bufferLength;
    bool  bufferIsSSLWriteSink;
};

/*
 * Handle the Temporary RSA Keys and DH Params
 */

#define SSL_TMP_KEY_FREE(type, idx)                     \
    if (SSL_temp_keys[idx]) {                           \
        type##_free((type *)SSL_temp_keys[idx]);        \
        SSL_temp_keys[idx] = NULL;                      \
    } else (void)(0)

#define SSL_TMP_KEYS_FREE(type) \
    SSL_TMP_KEY_FREE(type, SSL_TMP_KEY_##type##_512);   \
    SSL_TMP_KEY_FREE(type, SSL_TMP_KEY_##type##_1024);  \
    SSL_TMP_KEY_FREE(type, SSL_TMP_KEY_##type##_2048);  \
    SSL_TMP_KEY_FREE(type, SSL_TMP_KEY_##type##_4096)

#define SSL_TMP_KEY_INIT_DH(bits)  \
    ssl_tmp_key_init_dh(bits, SSL_TMP_KEY_DH_##bits)

#define SSL_TMP_KEYS_INIT(R)                    \
    R |= SSL_TMP_KEY_INIT_DH(512);              \
    R |= SSL_TMP_KEY_INIT_DH(1024);             \
    R |= SSL_TMP_KEY_INIT_DH(2048);             \
    R |= SSL_TMP_KEY_INIT_DH(4096)

#if !defined(OPENSSL_IS_BORINGSSL)
// This is the maximum overhead when encrypting plaintext as defined by
// <a href="https://www.ietf.org/rfc/rfc5246.txt">rfc5264</a>,
// <a href="https://www.ietf.org/rfc/rfc5289.txt">rfc5289</a> and openssl implementation itself.
//
// Please note that we use a padding of 16 here as openssl uses PKC#5 which uses 16 bytes while the spec itself
// allow up to 255 bytes. 16 bytes is the max for PKC#5 (which handles it the same way as PKC#7) as we use a block
// size of 16. See <a href="https://tools.ietf.org/html/rfc5652#section-6.3">rfc5652#section-6.3</a>.
//
// 16 (IV) + 48 (MAC) + 1 (Padding_length field) + 15 (Padding) + 1 (ContentType) + 2 (ProtocolVersion) + 2 (Length)
//
// TODO(scott): We may need to review this calculation once TLS 1.3 becomes available.
//              Which may add a KeyUpdate in front of the current record.
#define TCN_MAX_ENCRYPTED_PACKET_LENGTH (16 + 48 + 1 + 15 + 1 + 2 + 2)

// This also includes the header overhead for TLS 1.2 and below.
// See SSL#getMaxWrapOverhead for the overhead based upon the SSL*
// TODO(scott): this may be an over estimate because we don't account for short headers.
#define TCN_MAX_SEAL_OVERHEAD_LENGTH (TCN_MAX_ENCRYPTED_PACKET_LENGTH + SSL3_RT_HEADER_LENGTH)
#endif /*!defined(OPENSSL_IS_BORINGSSL)*/

static jint tcn_flush_sslbuffer_to_bytebuffer(struct TCN_bio_bytebuffer* bioUserData) {
    jint writeAmount = TCN_MIN(bioUserData->bufferLength, bioUserData->nonApplicationBufferLength) * sizeof(char);
    jint writeChunk = bioUserData->nonApplicationBufferSize - bioUserData->nonApplicationBufferOffset;

#ifdef NETTY_TCNATIVE_BIO_DEBUG
    fprintf(stderr, "tcn_flush_sslbuffer_to_bytebuffer1 bioUserData->nonApplicationBufferLength %d bioUserData->nonApplicationBufferOffset %d writeChunk %d writeAmount %d\n", bioUserData->nonApplicationBufferLength, bioUserData->nonApplicationBufferOffset, writeChunk, writeAmount);
#endif

    // check if we need to account for wrap around when draining the internal SSL buffer.
    if (writeAmount > writeChunk) {
        jint newnonApplicationBufferOffset = writeAmount - writeChunk;
        memcpy(bioUserData->buffer, &bioUserData->nonApplicationBuffer[bioUserData->nonApplicationBufferOffset], (size_t) writeChunk);
        memcpy(&bioUserData->buffer[writeChunk], bioUserData->nonApplicationBuffer, (size_t) newnonApplicationBufferOffset);
        bioUserData->nonApplicationBufferOffset = newnonApplicationBufferOffset;
    } else {
        memcpy(bioUserData->buffer, &bioUserData->nonApplicationBuffer[bioUserData->nonApplicationBufferOffset], (size_t) writeAmount);
        bioUserData->nonApplicationBufferOffset += writeAmount;
    }
    bioUserData->nonApplicationBufferLength -= writeAmount;
    bioUserData->bufferLength -= writeAmount;
    bioUserData->buffer += writeAmount; // Pointer arithmetic based on char* type

    if (bioUserData->nonApplicationBufferLength == 0) {
        bioUserData->nonApplicationBufferOffset = 0;
    }

#ifdef NETTY_TCNATIVE_BIO_DEBUG
    fprintf(stderr, "tcn_flush_sslbuffer_to_bytebuffer2 bioUserData->nonApplicationBufferLength %d bioUserData->nonApplicationBufferOffset %d\n", bioUserData->nonApplicationBufferLength, bioUserData->nonApplicationBufferOffset);
#endif

    return writeAmount;
}

static jint tcn_write_to_bytebuffer(BIO* bio, const char* in, int inl) {
    jint writeAmount = 0;
    jint writeChunk;
    struct TCN_bio_bytebuffer* bioUserData = (struct TCN_bio_bytebuffer*) BIO_get_data(bio);
    TCN_ASSERT(bioUserData != NULL);

#ifdef NETTY_TCNATIVE_BIO_DEBUG
    fprintf(stderr, "tcn_write_to_bytebuffer bioUserData->bufferIsSSLWriteSink %d inl %d [%.*s]\n", bioUserData->bufferIsSSLWriteSink, inl, inl, in);
#endif

    if (in == NULL || inl <= 0) {
        return 0;
    }

    // If the buffer is currently being used for reading then we have to use the internal SSL buffer to queue the data.
    if (!bioUserData->bufferIsSSLWriteSink) {
        jint nonApplicationBufferFreeSpace = bioUserData->nonApplicationBufferSize - bioUserData->nonApplicationBufferLength;
        jint startIndex;

#ifdef NETTY_TCNATIVE_BIO_DEBUG
       fprintf(stderr, "tcn_write_to_bytebuffer nonApplicationBufferFreeSpace %d\n", nonApplicationBufferFreeSpace);
#endif
        if (nonApplicationBufferFreeSpace == 0) {
            BIO_set_retry_write(bio); /* buffer is full */
            return -1;
        }

        writeAmount = TCN_MIN(nonApplicationBufferFreeSpace, (jint) inl) * sizeof(char);
        startIndex = bioUserData->nonApplicationBufferOffset + bioUserData->nonApplicationBufferLength;
        writeChunk = bioUserData->nonApplicationBufferSize - startIndex;

#ifdef NETTY_TCNATIVE_BIO_DEBUG
        fprintf(stderr, "tcn_write_to_bytebuffer bioUserData->nonApplicationBufferLength %d bioUserData->nonApplicationBufferOffset %d startIndex %d writeChunk %d writeAmount %d\n", bioUserData->nonApplicationBufferLength, bioUserData->nonApplicationBufferOffset, startIndex, writeChunk, writeAmount);
#endif

        // check if the write will wrap around the buffer.
        if (writeAmount > writeChunk) {
            memcpy(&bioUserData->nonApplicationBuffer[startIndex], in, (size_t) writeChunk);
            memcpy(bioUserData->nonApplicationBuffer, &in[writeChunk], (size_t) (writeAmount - writeChunk));
        } else {
            memcpy(&bioUserData->nonApplicationBuffer[startIndex], in, (size_t) writeAmount);
        }
        bioUserData->nonApplicationBufferLength += writeAmount;
        // This write amount will not be used by Java, and doesn't correlate to the ByteBuffer source.
        // The internal SSL buffer exists because a SSL_read operation may actually write data (e.g. handshake).
        return writeAmount;
    }

    if (bioUserData->buffer == NULL || bioUserData->bufferLength == 0) {
        BIO_set_retry_write(bio); /* no buffer to write into */
        return -1;
    }

    // First check if we need to drain data queued in the internal SSL buffer.
    if (bioUserData->nonApplicationBufferLength != 0) {
        writeAmount = tcn_flush_sslbuffer_to_bytebuffer(bioUserData);
    }

    // Next write "in" into what ever space the ByteBuffer has available.
    writeChunk = TCN_MIN(bioUserData->bufferLength, (jint) inl) * sizeof(char);

#ifdef NETTY_TCNATIVE_BIO_DEBUG
    fprintf(stderr, "tcn_write_to_bytebuffer2 writeChunk %d\n", writeChunk);
#endif

    memcpy(bioUserData->buffer, in, (size_t) writeChunk);
    bioUserData->bufferLength -= writeChunk;
    bioUserData->buffer += writeChunk; // Pointer arithmetic based on char* type

    return writeAmount + writeChunk;
}

static jint tcn_read_from_bytebuffer(BIO* bio, char *out, int outl) {
    jint readAmount;
    struct TCN_bio_bytebuffer* bioUserData = (struct TCN_bio_bytebuffer*) BIO_get_data(bio);
    TCN_ASSERT(bioUserData != NULL);

#ifdef NETTY_TCNATIVE_BIO_DEBUG
    fprintf(stderr, "tcn_read_from_bytebuffer bioUserData->bufferIsSSLWriteSink %d outl %d [%.*s]\n", bioUserData->bufferIsSSLWriteSink, outl, outl, out);
#endif

    if (out == NULL || outl <= 0) {
        return 0;
    }

    if (bioUserData->bufferIsSSLWriteSink || bioUserData->buffer == NULL || bioUserData->bufferLength == 0) {
        // During handshake this may happen, and it means we are not setup to read yet.
        BIO_set_retry_read(bio);
        return -1;
    }

    readAmount = TCN_MIN(bioUserData->bufferLength, (jint) outl) * sizeof(char);

#ifdef NETTY_TCNATIVE_BIO_DEBUG
    fprintf(stderr, "tcn_read_from_bytebuffer readAmount %d\n", readAmount);
#endif

    memcpy(out, bioUserData->buffer, (size_t) readAmount);
    bioUserData->bufferLength -= readAmount;
    bioUserData->buffer += readAmount; // Pointer arithmetic based on char* type

    return readAmount;
}

static int bio_java_bytebuffer_create(BIO* bio) {
    struct TCN_bio_bytebuffer* bioUserData = (struct TCN_bio_bytebuffer*) OPENSSL_malloc(sizeof(struct TCN_bio_bytebuffer));
    if (bioUserData == NULL) {
        return 0;
    }
    // The actual ByteBuffer is set from java and may be swapped out for each operation.
    bioUserData->buffer = NULL;
    bioUserData->bufferLength = 0;
    bioUserData->bufferIsSSLWriteSink = false;
    bioUserData->nonApplicationBuffer = NULL;
    bioUserData->nonApplicationBufferSize = 0;
    bioUserData->nonApplicationBufferOffset = 0;
    bioUserData->nonApplicationBufferLength = 0;

    BIO_set_data(bio, bioUserData);

    // In order to for OpenSSL to properly manage the lifetime of a BIO it relies on some shutdown and init state.
    // The behavior expected by OpenSSL can be found here: https://www.openssl.org/docs/man1.1.0/crypto/BIO_set_data.html
    BIO_set_shutdown(bio, 1);
    BIO_set_init(bio, 1);

    return 1;
}

static int bio_java_bytebuffer_destroy(BIO* bio) {
    struct TCN_bio_bytebuffer* bioUserData = NULL;

    if (bio == NULL) {
        return 0;
    }

    bioUserData = (struct TCN_bio_bytebuffer*) BIO_get_data(bio);
    if (bioUserData == NULL) {
        return 1;
    }

    if (bioUserData->nonApplicationBuffer != NULL) {
        OPENSSL_free(bioUserData->nonApplicationBuffer);
        bioUserData->nonApplicationBuffer = NULL;
    }

    // The buffer is not owned by tcn, so just free the native memory.
    OPENSSL_free(bioUserData);
    BIO_set_data(bio, NULL);

    return 1;
}

static int bio_java_bytebuffer_write(BIO* bio, const char* in, int inl) {
    BIO_clear_retry_flags(bio);
    return (int) tcn_write_to_bytebuffer(bio, in, inl);
}

static int bio_java_bytebuffer_read(BIO* bio, char* out, int outl) {
    BIO_clear_retry_flags(bio);
    return (int) tcn_read_from_bytebuffer(bio, out, outl);
}

static int bio_java_bytebuffer_puts(BIO* bio, const char *in) {
    BIO_clear_retry_flags(bio);
    return (int) tcn_write_to_bytebuffer(bio, in, strlen(in));
}

static int bio_java_bytebuffer_gets(BIO* b, char* out, int outl) {
    // Not supported https://www.openssl.org/docs/man1.0.2/crypto/BIO_write.html
    return -2;
}

static long bio_java_bytebuffer_ctrl(BIO* bio, int cmd, long num, void* ptr) {
    // see https://www.openssl.org/docs/man1.0.1/crypto/BIO_ctrl.html
    switch (cmd) {
        case BIO_CTRL_GET_CLOSE:
            return (long) BIO_get_shutdown(bio);
        case BIO_CTRL_SET_CLOSE:
            BIO_set_shutdown(bio, (int) num);
            return 1;
        case BIO_CTRL_FLUSH:
            return 1;
        case BIO_C_SET_FD:
            // Make this a no op.
            return 1;
        default:
            return 0;
    }
}

// This code is based on libcurl:
// https://github.com/curl/curl/blob/curl-7_61_0/lib/vtls/openssl.c#L521
#ifndef OPENSSL_NO_ENGINE
/*
 * Supply default password to the engine user interface conversation.
 * The password is passed by OpenSSL engine from ENGINE_load_private_key()
 * last argument to the ui and can be obtained by UI_get0_user_data(ui) here.
 */
static int ssl_ui_reader(UI *ui, UI_STRING *uis)
{
    const char *password = NULL;
    switch (UI_get_string_type(uis)) {
    case UIT_PROMPT:
    case UIT_VERIFY:
        password = (const char *) UI_get0_user_data(ui);
        if (password != NULL && (UI_get_input_flags(uis) & UI_INPUT_FLAG_DEFAULT_PWD) != 0) {
            UI_set_result(ui, uis, password);
            return 1;
        }
        // fall-through
    default:
        return (UI_method_get_reader(UI_OpenSSL()))(ui, uis);
  }
}

/*
 * Suppress interactive request for a default password if available.
 */
static int ssl_ui_writer(UI *ui, UI_STRING *uis)
{
    switch(UI_get_string_type(uis)) {
    case UIT_PROMPT:
    case UIT_VERIFY:
        if (UI_get0_user_data(ui) != NULL && (UI_get_input_flags(uis) & UI_INPUT_FLAG_DEFAULT_PWD) != 0) {
            return 1;
        }
        // fall-through
    default:
        return (UI_method_get_writer(UI_OpenSSL()))(ui, uis);
  }
}
#endif // OPENSSL_NO_ENGINE

TCN_IMPLEMENT_CALL(void, SSL, bioSetFd)(TCN_STDARGS, jlong ssl, jint fd) {
    SSL *ssl_ = J2P(ssl, SSL *);
    TCN_CHECK_NULL(ssl_, ssl, /* void */);
    // no op.
}

TCN_IMPLEMENT_CALL(jint, SSL, bioLengthByteBuffer)(TCN_STDARGS, jlong bioAddress) {
    BIO* bio = J2P(bioAddress, BIO*);
    struct TCN_bio_bytebuffer* bioUserData = NULL;

    TCN_CHECK_NULL(bio, bioAddress, 0);

    bioUserData = (struct TCN_bio_bytebuffer*) BIO_get_data(bio);
    return bioUserData == NULL ? 0 : bioUserData->bufferLength;
}

TCN_IMPLEMENT_CALL(jint, SSL, bioLengthNonApplication)(TCN_STDARGS, jlong bioAddress) {
    BIO* bio = J2P(bioAddress, BIO*);
    struct TCN_bio_bytebuffer* bioUserData = NULL;

    TCN_CHECK_NULL(bio, bioAddress, 0);

    bioUserData = (struct TCN_bio_bytebuffer*) BIO_get_data(bio);
    return bioUserData == NULL ? 0 : bioUserData->nonApplicationBufferLength;
}

#if OPENSSL_VERSION_NUMBER < 0x10100000L || defined(LIBRESSL_VERSION_NUMBER)
static BIO_METHOD bio_java_bytebuffer_methods = {
    BIO_TYPE_MEM,
    "Java ByteBuffer",
    bio_java_bytebuffer_write,
    bio_java_bytebuffer_read,
    bio_java_bytebuffer_puts,
    bio_java_bytebuffer_gets,
    bio_java_bytebuffer_ctrl,
    bio_java_bytebuffer_create,
    bio_java_bytebuffer_destroy,
    NULL
};
#else
static BIO_METHOD* bio_java_bytebuffer_methods = NULL;

static void init_bio_methods(void) {
    bio_java_bytebuffer_methods = BIO_meth_new(BIO_TYPE_MEM, "Java ByteBuffer");
    BIO_meth_set_write(bio_java_bytebuffer_methods, &bio_java_bytebuffer_write);
    BIO_meth_set_read(bio_java_bytebuffer_methods, &bio_java_bytebuffer_read);
    BIO_meth_set_puts(bio_java_bytebuffer_methods, &bio_java_bytebuffer_puts);
    BIO_meth_set_gets(bio_java_bytebuffer_methods, &bio_java_bytebuffer_gets);
    BIO_meth_set_ctrl(bio_java_bytebuffer_methods, &bio_java_bytebuffer_ctrl);
    BIO_meth_set_create(bio_java_bytebuffer_methods, &bio_java_bytebuffer_create);
    BIO_meth_set_destroy(bio_java_bytebuffer_methods, &bio_java_bytebuffer_destroy);
}

static void free_bio_methods(void) {
    BIO_meth_free(bio_java_bytebuffer_methods);
}
#endif

static BIO_METHOD* BIO_java_bytebuffer() {
#if OPENSSL_VERSION_NUMBER < 0x10100000L || defined(LIBRESSL_VERSION_NUMBER)
    return &bio_java_bytebuffer_methods;
#else
    return bio_java_bytebuffer_methods;
#endif
}

#if OPENSSL_VERSION_NUMBER < 0x30000000L
static int ssl_tmp_key_init_dh(int bits, int idx)
{
    return (SSL_temp_keys[idx] = tcn_SSL_dh_get_tmp_param(bits)) ? 0 : 1;
}
#endif

TCN_IMPLEMENT_CALL(jint, SSL, version)(TCN_STDARGS)
{
    return OpenSSL_version_num();
}

TCN_IMPLEMENT_CALL(jstring, SSL, versionString)(TCN_STDARGS)
{
    return AJP_TO_JSTRING(OpenSSL_version(OPENSSL_VERSION));
}

/*
 *  the various processing hooks
 */
static apr_status_t ssl_init_cleanup(void *data)
{
    if (!ssl_initialized) {
        return APR_SUCCESS;
    }

    ssl_initialized = 0;

    SSL_TMP_KEYS_FREE(DH);
    /*
     * Try to kill the internals of the SSL library.
     */
#if OPENSSL_VERSION_NUMBER >= 0x00907001 && !defined(OPENSSL_IS_BORINGSSL)
    /* Corresponds to OPENSSL_load_builtin_modules():
     * XXX: borrowed from apps.h, but why not CONF_modules_free()
     * which also invokes CONF_modules_finish()?
     */
    CONF_modules_unload(1);
#endif
    /* Corresponds to SSL_library_init: */
    EVP_cleanup();

#if OPENSSL_VERSION_NUMBER >= 0x00907001
    CRYPTO_cleanup_all_ex_data();
#endif
#if OPENSSL_VERSION_NUMBER < 0x10100000L
    ERR_remove_thread_state(NULL);
#endif
#if OPENSSL_VERSION_NUMBER >= 0x10100000L && !defined(LIBRESSL_VERSION_NUMBER)
    free_bio_methods();
#endif

#if defined(OPENSSL_FIPS) && (OPENSSL_VERSION_NUMBER < 0x30000000L)
    // Reset fips mode to the default.
    tcn_enable_fips(0);
#endif

#ifndef OPENSSL_NO_ENGINE
     /* Free our "structural" reference. */
     if (tcn_ssl_engine != NULL) {
         ENGINE_free(tcn_ssl_engine);
         tcn_ssl_engine = NULL;
     }

     if (ui_method != NULL) {
         UI_destroy_method(ui_method);
         ui_method = NULL;
     }

// In case we loaded any engine we should also call cleanup. This is especialy important in openssl < 1.1.
#ifndef OPENSSL_IS_BORINGSSL
    // This is deprecated since openssl 1.1 but does not exist at all in BoringSSL.
    ENGINE_cleanup();
#endif // OPENSSL_IS_BORINGSSL
#endif // OPENSSL_NO_ENGINE

    /* Don't call ERR_free_strings here; ERR_load_*_strings only
     * actually load the error strings once per process due to static
     * variable abuse in OpenSSL. */

    /*
     * TODO: determine somewhere we can safely shove out diagnostics
     *       (when enabled) at this late stage in the game:
     * CRYPTO_mem_leaks_fp(stderr);
     */
    return APR_SUCCESS;
}

#ifndef OPENSSL_NO_ENGINE
/* Try to load an engine in a shareable library */
static ENGINE *ssl_try_load_engine(const char *engine)
{
    ENGINE *e = ENGINE_by_id("dynamic");
    if (e) {
        if (!ENGINE_ctrl_cmd_string(e, "SO_PATH", engine, 0)
            || !ENGINE_ctrl_cmd_string(e, "LOAD", NULL, 0)) {
            ENGINE_free(e);
            e = NULL;
        }
    }
    return e;
}
#endif

/*
 * To ensure thread-safetyness in OpenSSL
 */

#if OPENSSL_VERSION_NUMBER < 0x10100000L || (defined(LIBRESSL_VERSION_NUMBER) && LIBRESSL_VERSION_NUMBER < 0x2090000fL)

static apr_thread_mutex_t **ssl_lock_cs = NULL;
static int                  ssl_lock_num_locks;

static void ssl_thread_lock(int mode, int type,
                            const char *file, int line)
{
    if (type < ssl_lock_num_locks) {
        if (mode & CRYPTO_LOCK) {
            apr_thread_mutex_lock(ssl_lock_cs[type]);
        } else {
            apr_thread_mutex_unlock(ssl_lock_cs[type]);
        }
    }
}

static unsigned long ssl_thread_id(void)
{
    /* OpenSSL needs this to return an unsigned long.  On OS/390, the pthread
     * id is a structure twice that big.  Use the TCB pointer instead as a
     * unique unsigned long.
     */
#ifdef __MVS__
    struct PSA {
        char unmapped[540];
        unsigned long PSATOLD;
    } *psaptr = 0;

    return psaptr->PSATOLD;
#elif defined(_WIN32)
    return (unsigned long)GetCurrentThreadId();
#else
    return (unsigned long)(apr_os_thread_current());
#endif
}

static void ssl_set_thread_id(CRYPTO_THREADID *id)
{
    CRYPTO_THREADID_set_numeric(id, ssl_thread_id());
}

static apr_status_t ssl_thread_cleanup(void *data)
{
    CRYPTO_set_locking_callback(NULL);
    CRYPTO_THREADID_set_callback(NULL);
    CRYPTO_set_dynlock_create_callback(NULL);
    CRYPTO_set_dynlock_lock_callback(NULL);
    CRYPTO_set_dynlock_destroy_callback(NULL);

    dynlockpool = NULL;

    /* Let the registered mutex cleanups do their own thing
     */
    return APR_SUCCESS;
}

/*
 * Dynamic lock creation callback
 */
static struct CRYPTO_dynlock_value *ssl_dyn_create_function(const char *file,
                                                     int line)
{
    struct CRYPTO_dynlock_value *value = NULL;
    apr_pool_t *p = NULL;
    apr_status_t rv;

    /*
     * We need a pool to allocate our mutex.  Since we can't clear
     * allocated memory from a pool, create a subpool that we can blow
     * away in the destruction callback.
     */
    rv = apr_pool_create(&p, dynlockpool);
    if (rv != APR_SUCCESS) {
        /* TODO log that fprintf(stderr, "Failed to create subpool for dynamic lock"); */
        return NULL;
    }

    value = (struct CRYPTO_dynlock_value *)apr_palloc(p,
                                                      sizeof(struct CRYPTO_dynlock_value));
    if (!value) {
        /* TODO log that fprintf(stderr, "Failed to allocate dynamic lock structure"); */
        return NULL;
    }

    value->pool = p;
    /* Keep our own copy of the place from which we were created,
       using our own pool. */
    value->file = apr_pstrdup(p, file);
    value->line = line;
    rv = apr_thread_mutex_create(&(value->mutex), APR_THREAD_MUTEX_DEFAULT,
                                p);
    if (rv != APR_SUCCESS) {
        /* TODO log that fprintf(stderr, "Failed to create thread mutex for dynamic lock"); */
        apr_pool_destroy(p);
        return NULL;
    }
    return value;
}

/*
 * Dynamic locking and unlocking function
 */
static void ssl_dyn_lock_function(int mode, struct CRYPTO_dynlock_value *l,
                           const char *file, int line)
{
    if (mode & CRYPTO_LOCK) {
        apr_thread_mutex_lock(l->mutex);
    }
    else {
        apr_thread_mutex_unlock(l->mutex);
    }
}

/*
 * Dynamic lock destruction callback
 */
static void ssl_dyn_destroy_function(struct CRYPTO_dynlock_value *l,
                          const char *file, int line)
{
    apr_status_t rv;
    rv = apr_thread_mutex_destroy(l->mutex);
    if (rv != APR_SUCCESS) {
        /* TODO log that fprintf(stderr, "Failed to destroy mutex for dynamic lock %s:%d", l->file, l->line); */
    }

    /* Trust that whomever owned the CRYPTO_dynlock_value we were
     * passed has no future use for it...
     */
    apr_pool_destroy(l->pool);
}

static void ssl_thread_setup(apr_pool_t *p)
{
    int i;

    ssl_lock_num_locks = CRYPTO_num_locks();
    ssl_lock_cs = apr_palloc(p, ssl_lock_num_locks * sizeof(*ssl_lock_cs));

    for (i = 0; i < ssl_lock_num_locks; i++) {
        apr_thread_mutex_create(&(ssl_lock_cs[i]),
                                APR_THREAD_MUTEX_DEFAULT, p);
    }

    CRYPTO_THREADID_set_callback(ssl_set_thread_id);
    CRYPTO_set_locking_callback(ssl_thread_lock);

    /* Set up dynamic locking scaffolding for OpenSSL to use at its
     * convenience.
     */
    dynlockpool = p;

    CRYPTO_set_dynlock_create_callback(ssl_dyn_create_function);
    CRYPTO_set_dynlock_lock_callback(ssl_dyn_lock_function);
    CRYPTO_set_dynlock_destroy_callback(ssl_dyn_destroy_function);

    apr_pool_cleanup_register(p, NULL, ssl_thread_cleanup,
                              apr_pool_cleanup_null);
}
#endif // OPENSSL_VERSION_NUMBER < 0x10100000L || (defined(LIBRESSL_VERSION_NUMBER) && LIBRESSL_VERSION_NUMBER < 0x2090000fL)


TCN_IMPLEMENT_CALL(jint, SSL, initialize)(TCN_STDARGS, jstring engine)
{
    int r = 0;

    TCN_ALLOC_CSTRING(engine);

    if (!tcn_global_pool) {
        TCN_FREE_CSTRING(engine);
        tcn_ThrowAPRException(e, APR_EINVAL);
        return (jint)APR_EINVAL;
    }
    /* Check if already initialized */
    if (ssl_initialized++) {
        TCN_FREE_CSTRING(engine);
        return (jint)APR_SUCCESS;
    }

#if OPENSSL_VERSION_NUMBER < 0x10100000L
    if (OpenSSL_version_num() < 0x0090700L) {
        TCN_FREE_CSTRING(engine);
        tcn_ThrowAPRException(e, APR_EINVAL);
        ssl_initialized = 0;
        return (jint)APR_EINVAL;
    }
#endif

#if OPENSSL_VERSION_NUMBER < 0x10100000L || (defined(LIBRESSL_VERSION_NUMBER) && LIBRESSL_VERSION_NUMBER < 0x2090200fL)
    /* We must register the library in full, to ensure our configuration
     * code can successfully test the SSL environment.
     */
    OPENSSL_malloc_init();
#endif

    ERR_load_crypto_strings();
    SSL_load_error_strings();
    SSL_library_init();
    OpenSSL_add_all_algorithms();
#if OPENSSL_VERSION_NUMBER >= 0x00907001
    OPENSSL_load_builtin_modules();
#endif

#if OPENSSL_VERSION_NUMBER < 0x10100000L || (defined(LIBRESSL_VERSION_NUMBER) && LIBRESSL_VERSION_NUMBER < 0x2090000fL)
    /* Initialize thread support */
    ssl_thread_setup(tcn_global_pool);
#endif // OPENSSL_VERSION_NUMBER < 0x10100000L || (defined(LIBRESSL_VERSION_NUMBER) && LIBRESSL_VERSION_NUMBER < 0x2090000fL)

    apr_status_t err = APR_SUCCESS;

#ifndef OPENSSL_NO_ENGINE
    if (J2S(engine)) {
        // Let us load the builtin engines as we want to use a specific one. This will also allow us
        // to use OPENSSL_ENGINES to define where a custom engine is located.
        ENGINE_load_builtin_engines();
        if(strcmp(J2S(engine), "auto") == 0) {
            ENGINE_register_all_complete();
        } else {

            // ssl_init_cleanup will take care of free the engine (tcn_ssl_engine) if needed.

            if ((tcn_ssl_engine = ENGINE_by_id(J2S(engine))) == NULL
                && (tcn_ssl_engine = ssl_try_load_engine(J2S(engine))) == NULL) {
                err = APR_ENOTIMPL;
            } else {
#ifdef ENGINE_CTRL_CHIL_SET_FORKCHECK
                if (strcmp(J2S(engine), "chil") == 0) {
                    ENGINE_ctrl(tcn_ssl_engine, ENGINE_CTRL_CHIL_SET_FORKCHECK, 1, 0, 0);
                }
#endif
                if (!ENGINE_set_default(tcn_ssl_engine, ENGINE_METHOD_ALL)) {
                    err = APR_ENOTIMPL;
                }
            }

            if (err == APR_SUCCESS) {
                // This code is based on libcurl:
                // https://github.com/curl/curl/blob/curl-7_61_0/lib/vtls/openssl.c#L521
                ui_method = UI_create_method((char *)"netty-tcnative user interface");
                if (ui_method != NULL) {
                    if (UI_method_set_opener(ui_method, UI_method_get_opener(UI_OpenSSL())) != 0) {
                        err = APR_EINVAL;
                        goto error;
                    }
                    if (UI_method_set_closer(ui_method, UI_method_get_closer(UI_OpenSSL())) != 0) {
                        err = APR_EINVAL;
                        goto error;
                    }
                    if (UI_method_set_reader(ui_method, ssl_ui_reader) != 0) {
                        err = APR_EINVAL;
                        goto error;
                    }
                    if (UI_method_set_writer(ui_method, ssl_ui_writer) != 0) {
                        err = APR_EINVAL;
                        goto error;
                    }
                } else {
                    err = APR_EINVAL;
                    goto error;
                }
            } else {
                goto error;
            }
        }
    }
#endif

    // For tcn_SSL_get_app_state() / tcn_SSL_CTX_get_app_state at request time
    tcn_init_app_state_idx();

#if OPENSSL_VERSION_NUMBER >= 0x10100000L && !defined(LIBRESSL_VERSION_NUMBER)
    init_bio_methods();
#endif

#if OPENSSL_VERSION_NUMBER < 0x30000000L
    SSL_TMP_KEYS_INIT(r);
#endif
    if (r) {
        // TODO: Should we really do this as the user may want to inspect the error stack ?
        ERR_clear_error();
        err = APR_ENOTIMPL;
        goto error;
    }
    /*
     * Let us cleanup the ssl library when the library is unloaded
     */
    apr_pool_cleanup_register(tcn_global_pool, NULL,
                              ssl_init_cleanup,
                              apr_pool_cleanup_null);
    TCN_FREE_CSTRING(engine);

    return (jint)APR_SUCCESS;

error:
    TCN_FREE_CSTRING(engine);
    ssl_init_cleanup(NULL);
    tcn_ThrowAPRException(e, err);
    return (jint)err;
}

TCN_IMPLEMENT_CALL(jlong, SSL, newMemBIO)(TCN_STDARGS)
{
    BIO *bio = NULL;

    // TODO: Use BIO_s_secmem() once included in stable release
    if ((bio = BIO_new(BIO_s_mem())) == NULL) {
        tcn_ThrowException(e, "Create BIO failed");
        return 0;
    }
    return P2J(bio);
}

TCN_IMPLEMENT_CALL(jstring, SSL, getLastError)(TCN_STDARGS)
{
    char buf[ERR_LEN];
    ERR_error_string_n(ERR_get_error(), buf, ERR_LEN);
    return tcn_new_string(e, buf);
}

/*** Begin Twitter 1:1 API addition ***/
TCN_IMPLEMENT_CALL(jint, SSL, getLastErrorNumber)(TCN_STDARGS) {
    return ERR_get_error();
}

static void ssl_info_callback(const SSL *ssl, int where, int ret) {
    tcn_ssl_state_t* state = NULL;
    if (0 != (where & SSL_CB_HANDSHAKE_START)) {
        if ((state = tcn_SSL_get_app_state(ssl)) != NULL) {
            state->handshakeCount++;
        }
    }
}

static tcn_ssl_state_t* new_ssl_state(tcn_ssl_ctxt_t* ctx) {
    if (ctx == NULL) {
        return NULL;
    }

    tcn_ssl_state_t* state = OPENSSL_malloc(sizeof(tcn_ssl_state_t));
    if (state == NULL) {
        return NULL;
    }
    memset(state, 0, sizeof(tcn_ssl_state_t));
    state->ctx = ctx;

     // Initially we will copy the configuration from the SSLContext.
    memcpy(&state->verify_config, &ctx->verify_config, sizeof(tcn_ssl_verify_config_t));
    return state;
}

static void free_ssl_state(JNIEnv* e, tcn_ssl_state_t* state) {
    if (state == NULL) {
        return;
    }

    tcn_ssl_task_free(e, state->ssl_task);
    state->ssl_task = NULL;

    // Free the tcn_ssl_state_t itself as it was allocated via OPENSSL_malloc(...) before
    //
    // https://github.com/netty/netty-tcnative/issues/532
    OPENSSL_free(state);
}

TCN_IMPLEMENT_CALL(jlong /* SSL * */, SSL, newSSL)(TCN_STDARGS,
                                                   jlong ctx /* tcn_ssl_ctxt_t * */,
                                                   jboolean server) {
    SSL *ssl = NULL;
    tcn_ssl_ctxt_t *c = J2P(ctx, tcn_ssl_ctxt_t *);
    tcn_ssl_state_t *state = NULL;

    TCN_CHECK_NULL(c, ctx, 0);

    if ((ssl = SSL_new(c->ctx)) == NULL) {
        tcn_ThrowException(e, "cannot create new ssl");
        return 0;
    }

    if ((state = new_ssl_state(c)) == NULL) {
        SSL_free(ssl);
        tcn_ThrowException(e, "cannot create new ssl state struct");
        return 0;
    }

    // Set the app_data2 before all the others because it may be used in SSL_free.
    tcn_SSL_set_app_state(ssl, state);

    // Add callback to keep track of handshakes.
    SSL_CTX_set_info_callback(c->ctx, ssl_info_callback);

    if (server) {
        SSL_set_accept_state(ssl);
    } else {
        SSL_set_connect_state(ssl);
    }

    return P2J(ssl);
}

TCN_IMPLEMENT_CALL(jint, SSL, getError)(TCN_STDARGS,
                                       jlong ssl /* SSL * */,
                                       jint ret) {
    SSL *ssl_ = J2P(ssl, SSL *);

    TCN_CHECK_NULL(ssl_, ssl, 0);

    return SSL_get_error(ssl_, ret);
}

// Write wlen bytes from wbuf into bio
TCN_IMPLEMENT_CALL(jint /* status */, SSL, bioWrite)(TCN_STDARGS,
                                                     jlong bioAddress /* BIO* */,
                                                     jlong wbufAddress /* char* */,
                                                     jint wlen /* sizeof(wbuf) */) {
    BIO* bio = J2P(bioAddress, BIO*);
    void* wbuf = J2P(wbufAddress, void*);

    TCN_CHECK_NULL(bio, bioAddress, 0);
    TCN_CHECK_NULL(wbuf, wbufAddress, 0);

    return BIO_write(bio, wbuf, wlen);
}

TCN_IMPLEMENT_CALL(void, SSL, bioSetByteBuffer)(TCN_STDARGS,
                                                jlong bioAddress /* BIO* */,
                                                jlong bufferAddress /* Address for direct memory */,
                                                jint maxUsableBytes /* max number of bytes to use */,
                                                jboolean isSSLWriteSink) {
    BIO* bio = J2P(bioAddress, BIO*);
    char* buffer = J2P(bufferAddress, char*);
    struct TCN_bio_bytebuffer* bioUserData = NULL;
    TCN_CHECK_NULL(bio, bioAddress, /* void */);
    TCN_CHECK_NULL(buffer, bufferAddress, /* void */);

    bioUserData = (struct TCN_bio_bytebuffer*) BIO_get_data(bio);
    TCN_ASSERT(bioUserData != NULL);

    bioUserData->buffer = buffer;
    bioUserData->bufferLength = maxUsableBytes;
    bioUserData->bufferIsSSLWriteSink = (bool) isSSLWriteSink;
}

TCN_IMPLEMENT_CALL(void, SSL, bioClearByteBuffer)(TCN_STDARGS, jlong bioAddress) {
    BIO* bio = J2P(bioAddress, BIO*);
    struct TCN_bio_bytebuffer* bioUserData = NULL;

    if (bio == NULL || (bioUserData = (struct TCN_bio_bytebuffer*) BIO_get_data(bio)) == NULL) {
        return;
    }

    bioUserData->buffer = NULL;
    bioUserData->bufferLength = 0;
    bioUserData->bufferIsSSLWriteSink = false;
}

TCN_IMPLEMENT_CALL(jint, SSL, bioFlushByteBuffer)(TCN_STDARGS, jlong bioAddress) {
    BIO* bio = J2P(bioAddress, BIO*);
    struct TCN_bio_bytebuffer* bioUserData = NULL;

    return (bio == NULL ||
           (bioUserData = (struct TCN_bio_bytebuffer*) BIO_get_data(bio)) == NULL ||
            bioUserData->nonApplicationBufferLength == 0 ||
            bioUserData->buffer == NULL ||
           !bioUserData->bufferIsSSLWriteSink) ? 0 : tcn_flush_sslbuffer_to_bytebuffer(bioUserData);
}

TCN_IMPLEMENT_CALL(jint, SSL, sslPending)(TCN_STDARGS, jlong ssl) {
    SSL *ssl_ = J2P(ssl, SSL *);
    return ssl_ != NULL ? SSL_pending(ssl_) : 0;
}

// Write up to wlen bytes of application data to the ssl BIO (encrypt)
TCN_IMPLEMENT_CALL(jint /* status */, SSL, writeToSSL)(TCN_STDARGS,
                                                       jlong ssl /* SSL * */,
                                                       jlong wbuf /* char * */,
                                                       jint wlen /* sizeof(wbuf) */) {
    SSL *ssl_ = J2P(ssl, SSL *);
    void *w = J2P(wbuf, void *);

    TCN_CHECK_NULL(ssl_, ssl, 0);
    TCN_CHECK_NULL(w, wbuf, 0);

    return SSL_write(ssl_, w, wlen);
}

// Read up to rlen bytes of application data from the given SSL BIO (decrypt)
TCN_IMPLEMENT_CALL(jint /* status */, SSL, readFromSSL)(TCN_STDARGS,
                                                        jlong ssl /* SSL * */,
                                                        jlong rbuf /* char * */,
                                                        jint rlen /* sizeof(rbuf) - 1 */) {
    SSL *ssl_ = J2P(ssl, SSL *);
    void *r = J2P(rbuf, void *);

    TCN_CHECK_NULL(ssl_, ssl, 0);
    TCN_CHECK_NULL(r, rbuf, 0);

    return SSL_read(ssl_, r, rlen);
}

// Get the shutdown status of the engine
TCN_IMPLEMENT_CALL(jint /* status */, SSL, getShutdown)(TCN_STDARGS,
                                                        jlong ssl /* SSL * */) {
    SSL *ssl_ = J2P(ssl, SSL *);

    TCN_CHECK_NULL(ssl_, ssl, 0);

    return SSL_get_shutdown(ssl_);
}

// Called when the peer closes the connection
TCN_IMPLEMENT_CALL(void, SSL, setShutdown)(TCN_STDARGS,
                                           jlong ssl /* SSL * */,
                                           jint mode) {
    SSL *ssl_ = J2P(ssl, SSL *);

    TCN_CHECK_NULL(ssl_, ssl, /* void */);

    SSL_set_shutdown(ssl_, mode);
}

TCN_IMPLEMENT_CALL(jobject /* task */, SSL, getTask)(TCN_STDARGS,
                                                        jlong ssl /* SSL * */) {

    tcn_ssl_state_t* state = NULL;
    SSL *ssl_ = J2P(ssl, SSL *);

    TCN_CHECK_NULL(ssl_, ssl, 0);

    if ((state = tcn_SSL_get_app_state(ssl_)) == NULL) {
        return NULL;
    }
    if (state->ssl_task == NULL || state->ssl_task->consumed == JNI_TRUE) {
        // Either no task was produced or it was already consumed by SSL.getTask(...).
        return NULL;
    }
    state->ssl_task->consumed = JNI_TRUE;
    return state->ssl_task->task;
}


// Free the SSL * and its associated internal BIO
TCN_IMPLEMENT_CALL(void, SSL, freeSSL)(TCN_STDARGS,
                                       jlong ssl /* SSL * */) {
    SSL *ssl_ = J2P(ssl, SSL *);

    TCN_CHECK_NULL(ssl_, ssl, /* void */);

    free_ssl_state(e, tcn_SSL_get_app_state(ssl_));

    SSL_free(ssl_);
}

TCN_IMPLEMENT_CALL(jlong, SSL, bioNewByteBuffer)(TCN_STDARGS,
                                                 jlong ssl /* SSL* */,
                                                 jint nonApplicationBufferSize) {
    SSL* ssl_ = J2P(ssl, SSL*);
    BIO* bio = NULL;
    struct TCN_bio_bytebuffer* bioUserData;

    TCN_CHECK_NULL(ssl_, ssl, 0);

    if (nonApplicationBufferSize <= 0) {
        tcn_ThrowException(e, "nonApplicationBufferSize <= 0");
        return 0;
    }

    bio = BIO_new(BIO_java_bytebuffer());
    if (bio == NULL) {
        tcn_ThrowException(e, "BIO_new failed");
        return 0;
    }

    bioUserData = BIO_get_data(bio);
    if (bioUserData == NULL) {
        BIO_free(bio);
        tcn_ThrowException(e, "BIO_get_data failed");
        return 0;
    }

    bioUserData->nonApplicationBuffer = (char*) OPENSSL_malloc(nonApplicationBufferSize * sizeof(char));
    if (bioUserData->nonApplicationBuffer == NULL) {
        BIO_free(bio);
        tcn_Throw(e, "Failed to allocate internal buffer of size %d", nonApplicationBufferSize);
        return 0;
    }
    bioUserData->nonApplicationBufferSize = nonApplicationBufferSize;

    SSL_set_bio(ssl_, bio, bio);

    return P2J(bio);
}

// Free a BIO * (typically, the network BIO)
TCN_IMPLEMENT_CALL(void, SSL, freeBIO)(TCN_STDARGS,
                                       jlong bio /* BIO * */) {
    BIO *bio_ = J2P(bio, BIO *);

    if (bio_ != NULL) {
        BIO_free(bio_);
    }
}

// Send CLOSE_NOTIFY to peer
TCN_IMPLEMENT_CALL(jint /* status */, SSL, shutdownSSL)(TCN_STDARGS,
                                                        jlong ssl /* SSL * */) {
    SSL *ssl_ = J2P(ssl, SSL *);

    TCN_CHECK_NULL(ssl_, ssl, 0);

    return SSL_shutdown(ssl_);
}

// Read which cipher was negotiated for the given SSL *.
TCN_IMPLEMENT_CALL(jstring, SSL, getCipherForSSL)(TCN_STDARGS,
                                                  jlong ssl /* SSL * */)
{
    SSL *ssl_ = J2P(ssl, SSL *);

    TCN_CHECK_NULL(ssl_, ssl, NULL);

    return AJP_TO_JSTRING(SSL_get_cipher(ssl_));
}

// Read which protocol was negotiated for the given SSL *.
TCN_IMPLEMENT_CALL(jstring, SSL, getVersion)(TCN_STDARGS,
                                                  jlong ssl /* SSL * */)
{
    SSL *ssl_ = J2P(ssl, SSL *);

    TCN_CHECK_NULL(ssl_, ssl, NULL);

    return AJP_TO_JSTRING(SSL_get_version(ssl_));
}

// Is the handshake over yet?
TCN_IMPLEMENT_CALL(jint, SSL, isInInit)(TCN_STDARGS,
                                        jlong ssl /* SSL * */) {
    SSL *ssl_ = J2P(ssl, SSL *);

    TCN_CHECK_NULL(ssl_, ssl, 0);

    return SSL_in_init(ssl_);
}

TCN_IMPLEMENT_CALL(jint, SSL, doHandshake)(TCN_STDARGS,
                                           jlong ssl /* SSL * */) {
    SSL *ssl_ = J2P(ssl, SSL *);

    TCN_CHECK_NULL(ssl_, ssl, 0);

    return SSL_do_handshake(ssl_);
}

// Read which protocol was negotiated for the given SSL *.
TCN_IMPLEMENT_CALL(jstring, SSL, getNextProtoNegotiated)(TCN_STDARGS,
                                                         jlong ssl /* SSL * */) {
    SSL *ssl_ = J2P(ssl, SSL *);
    const unsigned char *proto;
    unsigned int proto_len;

    TCN_CHECK_NULL(ssl_, ssl, NULL);

    SSL_get0_next_proto_negotiated(ssl_, &proto, &proto_len);
    return tcn_new_stringn(e, (char*) proto, proto_len);
}

/*** End Twitter API Additions ***/

/*** Apple API Additions ***/

TCN_IMPLEMENT_CALL(jstring, SSL, getAlpnSelected)(TCN_STDARGS,
                                                         jlong ssl /* SSL * */) {
    // Use weak linking with GCC as this will alow us to run the same packaged version with multiple
    // version of openssl.
    #if !defined(OPENSSL_IS_BORINGSSL) && (defined(__GNUC__) || defined(__GNUG__))
        if (!SSL_get0_alpn_selected) {
            return NULL;
        }
    #endif

    // We can only support it when either use openssl version >= 1.0.2 or GCC as this way we can use weak linking
    #if OPENSSL_VERSION_NUMBER >= 0x10002000L || defined(__GNUC__) || defined(__GNUG__)
        SSL *ssl_ = J2P(ssl, SSL *);
        const unsigned char *proto;
        unsigned int proto_len;

        TCN_CHECK_NULL(ssl_, ssl, NULL);

        SSL_get0_alpn_selected(ssl_, &proto, &proto_len);
        return tcn_new_stringn(e, (char*) proto, proto_len);
    #else
        return NULL;
    #endif
}

TCN_IMPLEMENT_CALL(jobjectArray, SSL, getPeerCertChain)(TCN_STDARGS,
                                                  jlong ssl /* SSL * */)
{
#ifdef OPENSSL_IS_BORINGSSL
    const STACK_OF(CRYPTO_BUFFER) *chain = NULL;
    const CRYPTO_BUFFER * cert = NULL;
    const tcn_ssl_ctxt_t* c = NULL;
#else
    STACK_OF(X509) *chain = NULL;
    X509 *cert = NULL;
    unsigned char *buf = NULL;
#endif // OPENSSL_IS_BORINGSSL
    int len;
    int i;
    int length;
    int offset;
    jobjectArray array = NULL;
    jbyteArray bArray = NULL;
    jclass byteArrayClass = tcn_get_byte_array_class();

    SSL *ssl_ = J2P(ssl, SSL *);

    TCN_CHECK_NULL(ssl_, ssl, NULL);

    // Get a stack of all certs in the chain.
#ifdef OPENSSL_IS_BORINGSSL
    TCN_GET_SSL_CTX(ssl_, c);

    TCN_ASSERT(c != NULL);

    chain = SSL_get0_peer_certificates(ssl_);
    len = sk_CRYPTO_BUFFER_num(chain);

    if (c->mode == SSL_MODE_SERVER) {
        // We don't want to include the leaf certificate to mimic the behaviour of SSL_get_peer_cert_chain(...).
        offset = 1;
    } else {
        offset = 0;
    }
#else
    chain = SSL_get_peer_cert_chain(ssl_);
    len = sk_X509_num(chain);
    offset = 0;
#endif // OPENSSL_IS_BORINGSSL

    len -= offset;
    if (len <= 0) {
        // No peer certificate chain as no auth took place yet, or the auth was not successful.
        return NULL;
    }

    // Create the byte[][] array that holds all the certs
    if ((array = (*e)->NewObjectArray(e, len, byteArrayClass, NULL)) == NULL) {
        // Out of memory
        return NULL;
    }

    for(i = 0; i < len; i++) {

#ifdef OPENSSL_IS_BORINGSSL
        cert = sk_CRYPTO_BUFFER_value(chain, i + offset);
        length = CRYPTO_BUFFER_len(cert);
#else
        cert = sk_X509_value(chain, i + offset);

        length = i2d_X509(cert, &buf);
        if (length < 0) {
            OPENSSL_free(buf);
            // In case of error just return an empty byte[][]
            return (*e)->NewObjectArray(e, 0, byteArrayClass, NULL);
        }
#endif // OPENSSL_IS_BORINGSSL

        bArray = (*e)->NewByteArray(e, length);

#ifdef OPENSSL_IS_BORINGSSL
        if (bArray == NULL) {
            return NULL;
        }
        (*e)->SetByteArrayRegion(e, bArray, 0, length, (jbyte*) CRYPTO_BUFFER_data(cert));
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

TCN_IMPLEMENT_CALL(jbyteArray, SSL, getPeerCertificate)(TCN_STDARGS,
                                                  jlong ssl /* SSL * */)
{
#ifdef OPENSSL_IS_BORINGSSL
    const STACK_OF(CRYPTO_BUFFER) *certs = NULL;
    const CRYPTO_BUFFER *leafCert = NULL;
#else
    X509 *cert = NULL;
    unsigned char *buf = NULL;
#endif // OPENSSL_IS_BORINGSSL

    jbyteArray bArray = NULL;
    int length;

    SSL *ssl_ = J2P(ssl, SSL *);

    TCN_CHECK_NULL(ssl_, ssl, NULL);

#ifdef OPENSSL_IS_BORINGSSL
    // Get a stack of all certs in the chain, the first is the leaf.
    certs = SSL_get0_peer_certificates(ssl_);
    if (certs == NULL || sk_CRYPTO_BUFFER_num(certs) <= 0) {
        return NULL;
    }
    leafCert = sk_CRYPTO_BUFFER_value(certs, 0);
    length = CRYPTO_BUFFER_len(leafCert);
#else
    cert = SSL_get_peer_certificate(ssl_);
    if (cert == NULL) {
        return NULL;
    }

    length = i2d_X509(cert, &buf);
#endif // OPENSSL_IS_BORINGSSL

    if ((bArray = (*e)->NewByteArray(e, length)) != NULL) {
#ifdef OPENSSL_IS_BORINGSSL
        (*e)->SetByteArrayRegion(e, bArray, 0, length, (jbyte*) CRYPTO_BUFFER_data(leafCert));
    }
#else
        (*e)->SetByteArrayRegion(e, bArray, 0, length, (jbyte*) buf);
    }

    // We need to free the cert as the reference count is incremented by one and it is not destroyed when the
    // session is freed.
    // See https://www.openssl.org/docs/ssl/SSL_get_peer_certificate.html
    X509_free(cert);

    OPENSSL_free(buf);
#endif // OPENSSL_IS_BORINGSSL
    return bArray;
}

TCN_IMPLEMENT_CALL(jstring, SSL, getErrorString)(TCN_STDARGS, jlong number)
{
    char buf[ERR_LEN];
    ERR_error_string_n(number, buf, ERR_LEN);
    return tcn_new_string(e, buf);
}

TCN_IMPLEMENT_CALL(jlong, SSL, getSession)(TCN_STDARGS, jlong ssl)
{
    SSL *ssl_ = J2P(ssl, SSL *);
    SSL_SESSION *session = NULL;

    TCN_CHECK_NULL(ssl_, ssl, 0);

    session = SSL_get_session(ssl_);
    if (session == NULL) {
        // BoringSSL does not protect against a NULL session. OpenSSL
        // returns 0 if the session is NULL, so do that here.
        return -1;
    }

    return P2J(session);
}

TCN_IMPLEMENT_CALL(jlong, SSL, getTime)(TCN_STDARGS, jlong ssl)
{
    SSL *ssl_ = J2P(ssl, SSL *);
    SSL_SESSION *session = NULL;

    TCN_CHECK_NULL(ssl_, ssl, 0);

    session = SSL_get_session(ssl_);
    if (session == NULL) {
        // BoringSSL does not protect against a NULL session. OpenSSL
        // returns 0 if the session is NULL, so do that here.
        return 0;
    }

    return SSL_get_time(session);
}


TCN_IMPLEMENT_CALL(jlong, SSL, getTimeout)(TCN_STDARGS, jlong ssl)
{
    SSL *ssl_ = J2P(ssl, SSL *);
    SSL_SESSION *session = NULL;

    TCN_CHECK_NULL(ssl_, ssl, 0);

    session = SSL_get_session(ssl_);
    if (session == NULL) {
        // BoringSSL does not protect against a NULL session. OpenSSL
        // returns 0 if the session is NULL, so do that here.
        return 0;
    }

    return SSL_get_timeout(session);
}


TCN_IMPLEMENT_CALL(jlong, SSL, setTimeout)(TCN_STDARGS, jlong ssl, jlong seconds)
{
    SSL *ssl_ = J2P(ssl, SSL *);
    SSL_SESSION *session = NULL;

    TCN_CHECK_NULL(ssl_, ssl, 0);

    session = SSL_get_session(ssl_);
    if (session == NULL) {
        // BoringSSL does not protect against a NULL session. OpenSSL
        // returns 0 if the session is NULL, so do that here.
        return 0;
    }

    return SSL_set_timeout(session, seconds);
}

TCN_IMPLEMENT_CALL(jboolean, SSL, setSession)(TCN_STDARGS, jlong ssl, jlong session)
{
    SSL *ssl_ = J2P(ssl, SSL *);
    SSL_SESSION *session_ = J2P(session, SSL_SESSION *);

    TCN_CHECK_NULL(ssl_, ssl, JNI_FALSE);
    TCN_CHECK_NULL(session_, session, JNI_FALSE);

    return SSL_set_session(ssl_, session_) == 0 ? JNI_FALSE : JNI_TRUE;
}

TCN_IMPLEMENT_CALL(void, SSL, setVerify)(TCN_STDARGS, jlong ssl, jint level, jint depth)
{
    tcn_ssl_state_t* state = NULL;
    SSL *ssl_ = J2P(ssl, SSL *);

    TCN_CHECK_NULL(ssl_, ssl, /* void */);

    state = tcn_SSL_get_app_state(ssl_);
    TCN_ASSERT(state != NULL);
    TCN_ASSERT(state->ctx != NULL);

#ifdef OPENSSL_IS_BORINGSSL
    SSL_set_custom_verify(ssl_, tcn_set_verify_config(&state->verify_config, level, depth), tcn_SSL_cert_custom_verify);
#else
    // No need to specify a callback for SSL_set_verify because we override the default certificate verification via SSL_CTX_set_cert_verify_callback.
    SSL_set_verify(ssl_, tcn_set_verify_config(&state->verify_config, level, depth), NULL);
    SSL_set_verify_depth(ssl_, state->verify_config.verify_depth);
#endif // OPENSSL_IS_BORINGSSL
}

TCN_IMPLEMENT_CALL(void, SSL, setOptions)(TCN_STDARGS, jlong ssl,
                                                 jint opt)
{
    SSL *ssl_ = J2P(ssl, SSL *);

    TCN_CHECK_NULL(ssl_, ssl, /* void */);

    SSL_set_options(ssl_, opt);
}

TCN_IMPLEMENT_CALL(void, SSL, clearOptions)(TCN_STDARGS, jlong ssl,
                                                 jint opt)
{
    SSL *ssl_ = J2P(ssl, SSL *);

    TCN_CHECK_NULL(ssl_, ssl, /* void */);

    SSL_clear_options(ssl_, opt);
}

TCN_IMPLEMENT_CALL(jint, SSL, getOptions)(TCN_STDARGS, jlong ssl)
{
    SSL *ssl_ = J2P(ssl, SSL *);

    TCN_CHECK_NULL(ssl_, ssl, 0);

    return SSL_get_options(ssl_);
}

TCN_IMPLEMENT_CALL(jint, SSL, setMode)(TCN_STDARGS, jlong ssl, jint mode)
{
    SSL* ssl_ = J2P(ssl, SSL*);

    TCN_CHECK_NULL(ssl_, ssl, 0);

    return (jint) SSL_set_mode(ssl_, mode);
}

TCN_IMPLEMENT_CALL(jint, SSL, getMode)(TCN_STDARGS, jlong ssl)
{
    SSL* ssl_ = J2P(ssl, SSL*);

    TCN_CHECK_NULL(ssl_, ssl, 0);

    return (jint) SSL_get_mode(ssl_);
}

TCN_IMPLEMENT_CALL(jint, SSL, getMaxWrapOverhead)(TCN_STDARGS, jlong ssl)
{
    SSL* ssl_ = J2P(ssl, SSL*);

    TCN_CHECK_NULL(ssl_, ssl, 0);


#ifdef OPENSSL_IS_BORINGSSL
    return (jint) SSL_max_seal_overhead(ssl_);
#else
    // TODO(scott): When OpenSSL supports something like SSL_max_seal_overhead ... use it!
    // TODO(scott): If we support SSL_MODE_CBC_RECORD_SPLITTING this must be calculated dynamically!
    // TLS 1.3 requires an extra bit for the header.
    return (jint) (SSL_version(ssl_) >= TLS1_3_VERSION ? TCN_MAX_SEAL_OVERHEAD_LENGTH + 1
                                                      : TCN_MAX_SEAL_OVERHEAD_LENGTH);
#endif
}

TCN_IMPLEMENT_CALL(jobjectArray, SSL, getCiphers)(TCN_STDARGS, jlong ssl)
{
    STACK_OF(SSL_CIPHER) *sk = NULL;
    int len;
    jobjectArray array = NULL;
    const SSL_CIPHER *cipher = NULL;
    const char *name = NULL;
    int i;
    jstring c_name = NULL;
    SSL *ssl_ = J2P(ssl, SSL *);

    TCN_CHECK_NULL(ssl_, ssl, NULL);

    sk = SSL_get_ciphers(ssl_);
    len = sk_SSL_CIPHER_num(sk);

    if (len <= 0) {
        // No peer certificate chain as no auth took place yet, or the auth was not successful.
        return NULL;
    }

    // Create the byte[][] array that holds all the certs
    if ((array = (*e)->NewObjectArray(e, len, tcn_get_string_class(), NULL)) == NULL) {
        // Out of memory
        return NULL;
    }

    for (i = 0; i < len; i++) {
        cipher = sk_SSL_CIPHER_value(sk, i);
        name = SSL_CIPHER_get_name(cipher);

        if ((c_name = (*e)->NewStringUTF(e, name)) == NULL) {
            // Out of memory
            return NULL;
        }
        (*e)->SetObjectArrayElement(e, array, i, c_name);
    }
    return array;
}

TCN_IMPLEMENT_CALL(jboolean, SSL, setCipherSuites)(TCN_STDARGS, jlong ssl,
                                                         jstring ciphers, jboolean tlsv13)
{
    jboolean rv = JNI_TRUE;
    SSL *ssl_ = J2P(ssl, SSL *);

    TCN_CHECK_NULL(ssl_, ssl, JNI_FALSE);

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
    rv = SSL_set_cipher_list(ssl_, J2S(ciphers)) == 0 ? JNI_FALSE : JNI_TRUE;
#else
    if (tlsv13 == JNI_TRUE) {
#ifdef OPENSSL_IS_BORINGSSL
        // BoringSSL does not support setting TLSv1.3 cipher suites explicit for now.
        rv = JNI_TRUE;
#else
        rv = SSL_set_ciphersuites(ssl_, J2S(ciphers)) == 0 ? JNI_FALSE : JNI_TRUE;
#endif // OPENSSL_IS_BORINGSSL
    } else {
        rv = SSL_set_cipher_list(ssl_, J2S(ciphers)) == 0 ? JNI_FALSE : JNI_TRUE;
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

#if OPENSSL_VERSION_NUMBER < 0x10100000L
/*
 * Backport of SSL_SESSION_get_master_key from 1.1
 */
size_t SSL_SESSION_get_master_key(const SSL_SESSION *session,
                                  unsigned char *out, size_t outlen)
{
    if (outlen == 0) {
        return session->master_key_length;
    }
    if (outlen > session->master_key_length) {
        outlen = session->master_key_length;
    }
    memcpy(out, session->master_key, outlen);
    return outlen;
}

/*
 * Backport of SSL_get_server_random from 1.1
 */
size_t SSL_get_server_random(const SSL *ssl, unsigned char *out, size_t outlen)
{
    if (outlen == 0) {
        return sizeof(ssl->s3->server_random);
    }
    if (outlen > sizeof(ssl->s3->server_random)) {
        outlen = sizeof(ssl->s3->server_random);
    }
    memcpy(out, ssl->s3->server_random, outlen);
    return outlen;
}

/*
 * Backport of SSL_get_client_random from 1.1
 */
size_t SSL_get_client_random(const SSL *ssl, unsigned char *out, size_t outlen)
{
    if (outlen == 0) {
        return sizeof(ssl->s3->client_random);
    }
    if (outlen > sizeof(ssl->s3->client_random)) {
        outlen = sizeof(ssl->s3->client_random);
    }
    memcpy(out, ssl->s3->client_random, outlen);
    return outlen;
}
#endif

TCN_IMPLEMENT_CALL(jbyteArray, SSL, getClientRandom)(TCN_STDARGS, jlong ssl)
{

    SSL *ssl_ = J2P(ssl, SSL *);
    TCN_CHECK_NULL(ssl_, ssl, NULL);

    size_t keyLength = SSL_get_client_random(ssl_, NULL, 0);
    TCN_ASSERT(keyLength <= 0x7FFFFFFF); /* must fit into 32 bit unsigned jsize */

    unsigned char *key = OPENSSL_malloc(sizeof(unsigned char) * keyLength);
    if (key == NULL) {
        tcn_ThrowException(e, "OPENSSL_malloc() returned null");
        return NULL;
    }

    size_t bytesMoved = SSL_get_client_random(ssl_, key, keyLength);
    TCN_ASSERT(bytesMoved == keyLength);

    jbyteArray jKey = (*e)->NewByteArray(e, (jsize) bytesMoved);
    if (jKey == NULL) {
        // Out of memory
        OPENSSL_free(key);
        return NULL;
    }
    (*e)->SetByteArrayRegion(e, jKey, 0, bytesMoved, (jbyte*) key);

    OPENSSL_free(key);

    return jKey;
}

TCN_IMPLEMENT_CALL(jbyteArray, SSL, getServerRandom)(TCN_STDARGS, jlong ssl)
{

    SSL *ssl_ = J2P(ssl, SSL *);
    TCN_CHECK_NULL(ssl_, ssl, NULL);

    size_t keyLength = SSL_get_server_random(ssl_, NULL, 0);
    TCN_ASSERT(keyLength <= 0x7FFFFFFF); /* must fit into 32 bit unsigned jsize */

    unsigned char *key = OPENSSL_malloc(sizeof(unsigned char) * keyLength);
    if (key == NULL) {
        tcn_ThrowException(e, "OPENSSL_malloc() returned null");
        return NULL;
    }

    size_t bytesMoved = SSL_get_server_random(ssl_, key, keyLength);
    TCN_ASSERT(bytesMoved == keyLength);

    jbyteArray jKey = (*e)->NewByteArray(e, (jsize) bytesMoved);
    if (jKey == NULL) {
        OPENSSL_free(key);
        return NULL;
    }
    (*e)->SetByteArrayRegion(e, jKey, 0, bytesMoved, (jbyte*) key);

    OPENSSL_free(key);

    return jKey;
}

TCN_IMPLEMENT_CALL(jbyteArray, SSL, getMasterKey)(TCN_STDARGS, jlong ssl)
{

    SSL *ssl_ = J2P(ssl, SSL *);
    TCN_CHECK_NULL(ssl_, ssl, NULL);

    SSL_SESSION *session = SSL_get0_session(ssl_);
    if (session == NULL) {
        return NULL;
    }

    size_t keyLength = SSL_SESSION_get_master_key(session, NULL, 0);
    TCN_ASSERT(keyLength <= 0x7FFFFFFF); /* must fit into 32 bit unsigned jsize */

    unsigned char *key = OPENSSL_malloc(sizeof(unsigned char) * keyLength);
    if (key == NULL) {
        tcn_ThrowException(e, "OPENSSL_malloc() returned null");
        return NULL;
    }

    size_t bytesMoved = SSL_SESSION_get_master_key(session, key, keyLength);
    TCN_ASSERT(bytesMoved == keyLength);

    jbyteArray jKey = (*e)->NewByteArray(e, (jsize) bytesMoved);
    if (jKey == NULL) {
        OPENSSL_free(key);
        return NULL;
    }
    (*e)->SetByteArrayRegion(e, jKey, 0, bytesMoved, (jbyte*) key);

    OPENSSL_free(key);

    return jKey;
}

TCN_IMPLEMENT_CALL(jbyteArray, SSL, getSessionId)(TCN_STDARGS, jlong ssl)
{

    unsigned int len;
    const unsigned char *session_id = NULL;
    SSL_SESSION *session = NULL;
    jbyteArray bArray = NULL;
    SSL *ssl_ = J2P(ssl, SSL *);

    TCN_CHECK_NULL(ssl_, ssl, NULL);

    session = SSL_get_session(ssl_);
    if (session == NULL) {
        return NULL;
    }

    session_id = SSL_SESSION_get_id(session, &len);
    if (len == 0 || session_id == NULL) {
        return NULL;
    }

    if ((bArray = (*e)->NewByteArray(e, len)) == NULL) {
        return NULL;
    }
    (*e)->SetByteArrayRegion(e, bArray, 0, len, (jbyte*) session_id);
    return bArray;
}

TCN_IMPLEMENT_CALL(jint, SSL, getHandshakeCount)(TCN_STDARGS, jlong ssl)
{
    tcn_ssl_state_t *state = NULL;
    SSL *ssl_ = J2P(ssl, SSL *);

    TCN_CHECK_NULL(ssl_, ssl, 0);

    if ((state = tcn_SSL_get_app_state(ssl_)) != NULL) {
        return state->handshakeCount;
    }
    return 0;
}


TCN_IMPLEMENT_CALL(void, SSL, clearError)(TCN_STDARGS)
{
    ERR_clear_error();
}

TCN_IMPLEMENT_CALL(void, SSL, setTlsExtHostName0)(TCN_STDARGS, jlong ssl, jstring hostname) {
    SSL *ssl_ = J2P(ssl, SSL *);

    TCN_CHECK_NULL(ssl_, ssl, /* void */);

    TCN_ALLOC_CSTRING(hostname);

    if (SSL_set_tlsext_host_name(ssl_, J2S(hostname)) != 1) {
        char err[ERR_LEN];
        ERR_error_string_n(ERR_get_error(), err, ERR_LEN);
        tcn_Throw(e, "Unable to set TLS servername extension (%s)", err);
    }

    TCN_FREE_CSTRING(hostname);
}

TCN_IMPLEMENT_CALL(void, SSL, setHostNameValidation)(TCN_STDARGS, jlong ssl, jint flags, jstring hostnameString) {
    SSL* ssl_ = J2P(ssl, SSL*);

    TCN_CHECK_NULL(ssl_, ssl, /* void */);

#ifdef OPENSSL_IS_BORINGSSL
    if (flags != 0) {
        tcn_ThrowException(e, "flags must be 0");
    }
    // Let's just ignore this as it is done in the Java level anyway.
#else
// Use weak linking with GCC as this will allow us to run the same packaged version with multiple
// version of openssl.
#if defined(__GNUC__) || defined(__GNUG__)
    if (!SSL_get0_param || !X509_VERIFY_PARAM_set_hostflags || !X509_VERIFY_PARAM_set1_host) {
        tcn_ThrowException(e, "hostname verification requires OpenSSL 1.0.2+");
        return;
    }
#endif // defined(__GNUC__) || defined(__GNUG__)


#if (OPENSSL_VERSION_NUMBER >= 0x10002000L && !defined(LIBRESSL_VERSION_NUMBER)) || LIBRESSL_VERSION_NUMBER >= 0x2060000fL || defined(__GNUC__) || defined(__GNUG__)
    if (hostnameString == NULL) {
        return;
    }
    X509_VERIFY_PARAM* param = SSL_get0_param(ssl_);
    X509_VERIFY_PARAM_set_hostflags(param, flags);

    jsize hostnameLen = (*e)->GetStringUTFLength(e, hostnameString);
    if (hostnameLen == 0) {
        return;
    }

    const char *hostname = (*e)->GetStringUTFChars(e, hostnameString, JNI_FALSE);

    if (X509_VERIFY_PARAM_set1_host(param, hostname, hostnameLen) != 1) {
        char err[ERR_LEN];
        ERR_error_string_n(ERR_get_error(), err, ERR_LEN);
        tcn_Throw(e, "X509_VERIFY_PARAM_set1_host error (%s)", err);
    }
    (*e)->ReleaseStringUTFChars(e, hostnameString, hostname);
#else
    tcn_ThrowException(e, "hostname verification requires OpenSSL 1.0.2+");
#endif // (OPENSSL_VERSION_NUMBER >= 0x10002000L && !defined(LIBRESSL_VERSION_NUMBER)) || LIBRESSL_VERSION_NUMBER >= 0x2060000fL || defined(__GNUC__) || defined(__GNUG__)

#endif // OPENSSL_IS_BORINGSSL
}

TCN_IMPLEMENT_CALL(jobjectArray, SSL, authenticationMethods)(TCN_STDARGS, jlong ssl) {
    SSL *ssl_ = J2P(ssl, SSL *);
    const STACK_OF(SSL_CIPHER) *ciphers = NULL;
    int len;
    int i;
    jobjectArray array = NULL;
    jstring methodString = NULL;

    TCN_CHECK_NULL(ssl_, ssl, NULL);

    ciphers = SSL_get_ciphers(ssl_);
    len = sk_SSL_CIPHER_num(ciphers);

    if ((array = (*e)->NewObjectArray(e, len, tcn_get_string_class(), NULL)) == NULL) {
        return NULL;
    }

    for (i = 0; i < len; i++) {
        if ((methodString = (*e)->NewStringUTF(e, tcn_SSL_cipher_authentication_method(sk_SSL_CIPHER_value(ciphers, i)))) == NULL) {
            // Out of memory
            return NULL;
        }
        (*e)->SetObjectArrayElement(e, array, i, methodString);
    }
    return array;
}

TCN_IMPLEMENT_CALL(void, SSL, setCertificateBio)(TCN_STDARGS, jlong ssl,
                                                         jlong cert, jlong key,
                                                         jstring password)
{
#ifdef OPENSSL_IS_BORINGSSL
    tcn_Throw(e, "Not supported using BoringSSL");
#else
    SSL *ssl_ = J2P(ssl, SSL *);

    TCN_CHECK_NULL(ssl_, ssl, /* void */);

    BIO *cert_bio = J2P(cert, BIO *);
    BIO *key_bio = J2P(key, BIO *);
    EVP_PKEY* pkey = NULL;
    X509* xcert = NULL;
    TCN_ALLOC_CSTRING(password);
    char err[ERR_LEN];

    TCN_ASSERT(ssl != NULL);

    if (key <= 0) {
        key = cert;
    }

    if (cert <= 0 || key <= 0) {
        tcn_Throw(e, "No Certificate file specified or invalid file format");
        goto cleanup;
    }

    if ((pkey = tcn_load_pem_key_bio(cpassword, key_bio)) == NULL) {
        ERR_error_string_n(ERR_get_error(), err, ERR_LEN);
        ERR_clear_error();
        tcn_Throw(e, "Unable to load certificate key (%s)",err);
        goto cleanup;
    }
    if ((xcert = tcn_load_pem_cert_bio(cpassword, cert_bio)) == NULL) {
        ERR_error_string_n(ERR_get_error(), err, ERR_LEN);
        ERR_clear_error();
        tcn_Throw(e, "Unable to load certificate (%s) ", err);
        goto cleanup;
    }

    if (SSL_use_certificate(ssl_, xcert) <= 0) {
        ERR_error_string_n(ERR_get_error(), err, ERR_LEN);
        ERR_clear_error();
        tcn_Throw(e, "Error setting certificate (%s)", err);
        goto cleanup;
    }
    if (SSL_use_PrivateKey(ssl_, pkey) <= 0) {
        ERR_error_string_n(ERR_get_error(), err, ERR_LEN);
        ERR_clear_error();
        tcn_Throw(e, "Error setting private key (%s)", err);
        goto cleanup;
    }
    if (SSL_check_private_key(ssl_) <= 0) {
        ERR_error_string_n(ERR_get_error(), err, ERR_LEN);
        ERR_clear_error();

        tcn_Throw(e, "Private key does not match the certificate public key (%s)",
                  err);
        goto cleanup;
    }
cleanup:
    TCN_FREE_CSTRING(password);
    EVP_PKEY_free(pkey); // this function is safe to call with NULL
    X509_free(xcert); // this function is safe to call with NULL
#endif // OPENSSL_IS_BORINGSSL
}

TCN_IMPLEMENT_CALL(void, SSL, setCertificateChainBio)(TCN_STDARGS, jlong ssl,
                                                                  jlong chain,
                                                                  jboolean skipfirst)
{
    SSL *ssl_ = J2P(ssl, SSL *);
    BIO *b = J2P(chain, BIO *);

    TCN_CHECK_NULL(ssl_, ssl, /* void */);
    TCN_CHECK_NULL(b, chain, /* void */);

// This call is only used to detect if we support KeyManager or not in netty. As we know that we support it in
// BoringSSL we can just ignore this call. In the future we should remove the method all together.
#ifndef OPENSSL_IS_BORINGSSL
    char err[ERR_LEN];

    if (tcn_SSL_use_certificate_chain_bio(ssl_, b, skipfirst) < 0)  {
        ERR_error_string_n(ERR_get_error(), err, ERR_LEN);
        ERR_clear_error();
        tcn_Throw(e, "Error setting certificate chain (%s)", err);
    }
#endif // OPENSSL_IS_BORINGSSL
}

TCN_IMPLEMENT_CALL(jlong, SSL, loadPrivateKeyFromEngine)(TCN_STDARGS, jstring keyId, jstring password)
{
#ifndef OPENSSL_NO_ENGINE
    char err[ERR_LEN];
    EVP_PKEY* pkey = NULL;

    TCN_ALLOC_CSTRING(keyId);
    TCN_ALLOC_CSTRING(password);

    pkey = ENGINE_load_private_key(tcn_ssl_engine, ckeyId, ui_method, (void*) cpassword);

    TCN_FREE_CSTRING(password);
    TCN_FREE_CSTRING(keyId);

    if (pkey == NULL) {
         ERR_error_string_n(ERR_get_error(), err, ERR_LEN);
         ERR_clear_error();
         tcn_Throw(e, "Unable to load private key (%s)", err);
         return -1;
    } else {
        return P2J(pkey);
    }
#else
    // Not supported
    tcn_Throw(e, "Not supported");
    return -1;
#endif
}

TCN_IMPLEMENT_CALL(jlong, SSL, parsePrivateKey)(TCN_STDARGS, jlong privateKeyBio, jstring password)
{
    EVP_PKEY* pkey = NULL;
    BIO *bio = J2P(privateKeyBio, BIO *);

    TCN_CHECK_NULL(bio, privateKeyBio, 0);

    TCN_ALLOC_CSTRING(password);
    char err[ERR_LEN];

    if ((pkey = tcn_load_pem_key_bio(cpassword, bio)) == NULL) {
        ERR_error_string_n(ERR_get_error(), err, ERR_LEN);
        ERR_clear_error();
        tcn_Throw(e, "Unable to load certificate key (%s)",err);
        goto cleanup;
    }

cleanup:
    TCN_FREE_CSTRING(password);
    return P2J(pkey);
}

TCN_IMPLEMENT_CALL(void, SSL, freePrivateKey)(TCN_STDARGS, jlong privateKey)
{
    EVP_PKEY *key = J2P(privateKey, EVP_PKEY *);
    EVP_PKEY_free(key); // Safe to call with NULL as well.
}

TCN_IMPLEMENT_CALL(jlong, SSL, parseX509Chain)(TCN_STDARGS, jlong x509ChainBio)
{
    BIO *cert_bio = J2P(x509ChainBio, BIO *);

#ifdef OPENSSL_IS_BORINGSSL
    STACK_OF(CRYPTO_BUFFER) *chain = sk_CRYPTO_BUFFER_new_null();
    CRYPTO_BUFFER *buffer = NULL;
    char *name = NULL;
    char *header = NULL;
    uint8_t *data = NULL;
    long data_len;
#else
    X509* cert = NULL;
    STACK_OF(X509) *chain = NULL;
#endif // OPENSSL_IS_BORINGSSL

    char err[ERR_LEN];
    unsigned long error;

    TCN_CHECK_NULL(cert_bio, x509ChainBio, 0);

#ifdef OPENSSL_IS_BORINGSSL
    while (PEM_read_bio(cert_bio, &name, &header, &data, &data_len)) {

        OPENSSL_free(name);
        name = NULL;

        OPENSSL_free(header);
        header = NULL;

        buffer = CRYPTO_BUFFER_new(data, data_len, NULL);
        OPENSSL_free(data);
        data = NULL;

        if (buffer == NULL || sk_CRYPTO_BUFFER_push(chain, buffer) <= 0) {
#else
    chain = sk_X509_new_null();
    while ((cert = PEM_read_bio_X509(cert_bio, NULL, NULL, NULL)) != NULL) {
        if (sk_X509_push(chain, cert) <= 0) {
#endif // OPENSSL_IS_BORINGSSL

            tcn_Throw(e, "No Certificate specified or invalid format");
            goto cleanup;
        }

#ifndef OPENSSL_IS_BORINGSSL
        cert = NULL;
#endif // OPENSSL_IS_BORINGSSL
    }

    // ensure that if we have an error its just for EOL.
    if ((error = ERR_peek_error()) > 0) {
        if (!(ERR_GET_LIB(error) == ERR_LIB_PEM
              && ERR_GET_REASON(error) == PEM_R_NO_START_LINE)) {

            ERR_error_string_n(ERR_get_error(), err, ERR_LEN);
            tcn_Throw(e, "Invalid format (%s)", err);
            goto cleanup;
        }
        ERR_clear_error();
    }

    return P2J(chain);

cleanup:
    ERR_clear_error();

#ifdef OPENSSL_IS_BORINGSSL
    sk_CRYPTO_BUFFER_pop_free(chain, CRYPTO_BUFFER_free);
#else
    sk_X509_pop_free(chain, X509_free);
    X509_free(cert);
#endif // OPENSSL_IS_BORINGSSL

    return 0;
}

TCN_IMPLEMENT_CALL(void, SSL, freeX509Chain)(TCN_STDARGS, jlong x509Chain)
{
#ifdef OPENSSL_IS_BORINGSSL
    STACK_OF(CRYPTO_BUFFER) *chain = J2P(x509Chain, STACK_OF(CRYPTO_BUFFER) *);
    sk_CRYPTO_BUFFER_pop_free(chain, CRYPTO_BUFFER_free);
#else
    STACK_OF(X509) *chain = J2P(x509Chain, STACK_OF(X509) *);
    sk_X509_pop_free(chain, X509_free);
#endif // OPENSSL_IS_BORINGSSL
}

TCN_IMPLEMENT_CALL(void, SSL, setKeyMaterial)(TCN_STDARGS, jlong ssl, jlong chain, jlong key)
{
#if (defined(LIBRESSL_VERSION_NUMBER) && LIBRESSL_VERSION_NUMBER < 0x2090200fL)
    tcn_Throw(e, "Not supported with LibreSSL < 2.9.2");
#else
    SSL *ssl_ = J2P(ssl, SSL *);

    TCN_CHECK_NULL(ssl_, ssl, /* void */);

    EVP_PKEY* pkey = J2P(key, EVP_PKEY *);

#ifdef OPENSSL_IS_BORINGSSL
    STACK_OF(CRYPTO_BUFFER) *cchain = J2P(chain, STACK_OF(CRYPTO_BUFFER) *);
    int numCerts = sk_CRYPTO_BUFFER_num(cchain);
    CRYPTO_BUFFER** certs = NULL;
#else
    STACK_OF(X509) *cchain = J2P(chain, STACK_OF(X509) *);
    int numCerts = sk_X509_num(cchain);
#endif // OPENSSL_IS_BORINGSSL

    char err[ERR_LEN];
    int i;

    TCN_ASSERT(ssl != NULL);

    TCN_CHECK_NULL(cchain, chain, /* void */);

#ifdef OPENSSL_IS_BORINGSSL
    if ((certs = OPENSSL_malloc(sizeof(CRYPTO_BUFFER*) * numCerts)) == NULL) {
        tcn_Throw(e, "OPENSSL_malloc returned NULL");
        return;
    }

    for (i = 0; i < numCerts; i++) {
        certs[i] = sk_CRYPTO_BUFFER_value(cchain, i);
    }

    if (numCerts <= 0 || SSL_set_chain_and_key(ssl_, certs, numCerts, pkey, pkey == NULL ? &private_key_method : NULL) <= 0) {
#else
    // SSL_use_certificate will increment the reference count of the cert.
    if (numCerts <= 0 || SSL_use_certificate(ssl_, sk_X509_value(cchain, 0)) <= 0) {
#endif // OPENSSL_IS_BORINGSSL

        ERR_error_string_n(ERR_get_error(), err, ERR_LEN);
        ERR_clear_error();
        tcn_Throw(e, "Error setting certificate (%s)", err);
    }

#ifdef OPENSSL_IS_BORINGSSL
    OPENSSL_free(certs);
#else
    if (pkey != NULL) {
        // SSL_use_PrivateKey will increment the reference count of the key.
        if (SSL_use_PrivateKey(ssl_, pkey) <= 0) {
            ERR_error_string_n(ERR_get_error(), err, ERR_LEN);
            ERR_clear_error();
            tcn_Throw(e, "Error setting private key (%s)", err);
            return;
        }
        if (SSL_check_private_key(ssl_) <= 0) {
            ERR_error_string_n(ERR_get_error(), err, ERR_LEN);
            ERR_clear_error();

            tcn_Throw(e, "Private key does not match the certificate public key (%s)", err);
            return;
        }
    }

    // The first cert was loaded via SSL_use_certificate so skip it.
    for (i = 1; i < numCerts; ++i) {

        // tcn_SSL_add1_chain_cert will increment the reference count of the cert.
        if (tcn_SSL_add1_chain_cert(ssl_, sk_X509_value(cchain, i)) != 1) {
            ERR_error_string_n(ERR_get_error(), err, ERR_LEN);
            ERR_clear_error();

            tcn_Throw(e, "Could not add certificate to chain (%s)", err);
            return;
        }
    }
#endif // OPENSSL_IS_BORINGSSL

#endif
}

TCN_IMPLEMENT_CALL(void, SSL, setKeyMaterialClientSide)(TCN_STDARGS, jlong ssl, jlong certOut, jlong keyOut, jlong chain, jlong key)
{
#if defined(LIBRESSL_VERSION_NUMBER)
    tcn_Throw(e, "Not supported with LibreSSL");
#elif defined(OPENSSL_IS_BORINGSSL)
    tcn_Throw(e, "Not supported with BoringSSL");
#else
    SSL *ssl_ = J2P(ssl, SSL *);

    TCN_CHECK_NULL(ssl_, ssl, /* void */);

    EVP_PKEY* pkey = J2P(key, EVP_PKEY *);
    STACK_OF(X509) *cchain = J2P(chain, STACK_OF(X509) *);
    X509** x509Out = J2P(certOut, X509 **);
    EVP_PKEY** pkeyOut = J2P(keyOut, EVP_PKEY **);

    int numCerts;
    X509* x509 = NULL;
    char err[ERR_LEN];
    int i;

    TCN_ASSERT(ssl != NULL);

    if (cchain == NULL || pkey == NULL) {
       return;
    }

    numCerts = sk_X509_num(cchain);

    if (numCerts <= 0) {
       return;
    }

    // Skip the first cert in the chain as we will write this to x509Out.
    // See https://github.com/netty/netty-tcnative/issues/184
    for (i = 1; i < numCerts; ++i) {
        // We need to explicit add extra certs to the chain as stated in:
        // https://www.openssl.org/docs/manmaster/ssl/SSL_CTX_set_client_cert_cb.html
        //
        // Using SSL_add1_chain_cert(...) here as we want to increment the reference count.
        if (tcn_SSL_add1_chain_cert(ssl_, sk_X509_value(cchain, i)) <= 0) {
            ERR_error_string_n(ERR_get_error(), err, ERR_LEN);
            ERR_clear_error();
            tcn_Throw(e, "Could not add certificate to chain (%s)", err);
            return;
        }
    }

    x509 = sk_X509_value(cchain, 0);
    if (tcn_X509_up_ref(x509) < 1) {
        // We could not increment the reference count
        ERR_error_string_n(ERR_get_error(), err, ERR_LEN);
        ERR_clear_error();
        tcn_Throw(e, "Could not add certificate (%s)", err);
        return;
    }

    if (tcn_EVP_PKEY_up_ref(pkey) < 1) {
        // We could not increment the reference count,  we need to explicit call X509_free here as we
        // incremented the reference count of the certificate before.
        X509_free(x509);

        ERR_error_string_n(ERR_get_error(), err, ERR_LEN);
        ERR_clear_error();
        tcn_Throw(e, "Could not add private key (%s)", err);
        return;
    }
    *x509Out = x509;
    *pkeyOut = pkey;
#endif
}

/**
 * Enables OCSP stapling on the SSLEngine.
 */
TCN_IMPLEMENT_CALL(void, SSL, enableOcsp)(TCN_STDARGS, jlong ssl) {
    SSL *ssl_ = J2P(ssl, SSL *);

    TCN_CHECK_NULL(ssl_, ssl, /* void */);

#if defined(OPENSSL_NO_OCSP) && !defined(OPENSSL_IS_BORINGSSL)
    tcn_ThrowException(e, "netty-tcnative was built without OCSP support");

#elif defined(TCN_OCSP_NOT_SUPPORTED)
    tcn_ThrowException(e, "OCSP stapling is not supported");

#elif defined(OPENSSL_IS_BORINGSSL)
    SSL_enable_ocsp_stapling(ssl_);

#else
    if (SSL_set_tlsext_status_type(ssl_, TLSEXT_STATUSTYPE_ocsp) != 1L) {
        tcn_ThrowException(e, "SSL_set_tlsext_status_type() failed");
        return;
    }
#endif
}

/**
 * Server: Sets OCSP response bytes.
 */
TCN_IMPLEMENT_CALL(void, SSL, setOcspResponse)(TCN_STDARGS, jlong ssl, jbyteArray response) {
    SSL *ssl_ = J2P(ssl, SSL *);

    TCN_CHECK_NULL(ssl_, ssl, /* void */);

    jsize length = (*e)->GetArrayLength(e, response);
    if (length <= 0) {
        return;
    }

#if defined(OPENSSL_NO_OCSP) && !defined(OPENSSL_IS_BORINGSSL)
    tcn_ThrowException(e, "netty-tcnative was built without OCSP support");

#elif defined(TCN_OCSP_NOT_SUPPORTED)
    tcn_ThrowException(e, "OCSP stapling is not supported");

#elif defined(OPENSSL_IS_BORINGSSL)
    uint8_t *value = OPENSSL_malloc(sizeof(uint8_t) * length);
    if (value == NULL) {
        tcn_ThrowException(e, "OPENSSL_malloc() returned null");
        return;
    }

    (*e)->GetByteArrayRegion(e, response, 0, length, (jbyte*)value);
    int code = SSL_set_ocsp_response(ssl_, value, (size_t)length);

    OPENSSL_free(value);

    if (code != 1) {
        tcn_ThrowException(e, "SSL_set_ocsp_response() failed");
        return;
    }
#else
    //
    // ATTENTION: This took a while to figure out but OpenSSL wants to (and will)
    // free() this pointer on its own. Give it something it can free or it will crash.
    //
    unsigned char *value = OPENSSL_malloc(sizeof(unsigned char) * length);
    if (value == NULL) {
        tcn_ThrowException(e, "OPENSSL_malloc() returned null");
        return;
    }

    (*e)->GetByteArrayRegion(e, response, 0, length, (jbyte*)value);
    if (SSL_set_tlsext_status_ocsp_resp(ssl_, value, length) != 1L) {
        OPENSSL_free(value);
        tcn_ThrowException(e, "SSL_set_tlsext_status_ocsp_resp() failed");
        return;
    }
#endif
}

/**
 * Client: Returns the OCSP response as sent by the server or null
 * if the server didn't provide a stapled OCSP response.
 */
TCN_IMPLEMENT_CALL(jbyteArray, SSL, getOcspResponse)(TCN_STDARGS, jlong ssl) {
    SSL *ssl_ = J2P(ssl, SSL *);

    TCN_CHECK_NULL(ssl_, ssl, NULL);

#if defined(OPENSSL_NO_OCSP) && !defined(OPENSSL_IS_BORINGSSL)
    tcn_ThrowException(e, "netty-tcnative was built without OCSP support");

#elif defined(TCN_OCSP_NOT_SUPPORTED)
    tcn_ThrowException(e, "OCSP stapling is not supported");

#elif defined(OPENSSL_IS_BORINGSSL)
    const uint8_t *response = NULL;
    size_t length = 0;

    SSL_get0_ocsp_response(ssl_, &response, &length);
    if (response == NULL || length == 0) {
        return NULL;
    }

    jbyteArray value = (*e)->NewByteArray(e, length);
    if (value == NULL) {
        return NULL;
    }
    (*e)->SetByteArrayRegion(e, value, 0, length, (jbyte*)response);
    return value;

#else
    unsigned char *response = NULL;
    jint length = (jint)SSL_get_tlsext_status_ocsp_resp(ssl_, &response);
    if (response == NULL || length < 0) {
        return NULL;
    }

    jbyteArray value = (*e)->NewByteArray(e, length);
    if (value == NULL) {
        // Out of memory
        return NULL;
    }
    (*e)->SetByteArrayRegion(e, value, 0, length, (jbyte*)response);
    return value;
#endif
}

TCN_IMPLEMENT_CALL(void, SSL, fipsModeSet)(TCN_STDARGS, jint mode)
{
#if defined(OPENSSL_FIPS) || (OPENSSL_VERSION_NUMBER >= 0x30000000L)
    if (tcn_enable_fips((int) mode) == 0) {
        char err[ERR_LEN];
        ERR_error_string_n(ERR_get_error(), err, ERR_LEN);
        ERR_clear_error();
        tcn_Throw(e, "Unable set fips mode (%s)", err);
    }
#else
    /* FIPS is unavailable */
    tcn_ThrowException(e, "netty-tcnative was built without FIPS support");
#endif
}

TCN_IMPLEMENT_CALL(jstring, SSL, getSniHostname)(TCN_STDARGS, jlong ssl)
{
    SSL *ssl_ = J2P(ssl, SSL *);
    TCN_CHECK_NULL(ssl_, ssl, 0);

    const char *servername = SSL_get_servername(ssl_, TLSEXT_NAMETYPE_host_name);
    if (servername == NULL) {
        return NULL;
    }
    return tcn_new_string(e, servername);
}

TCN_IMPLEMENT_CALL(jboolean, SSL, isSessionReused)(TCN_STDARGS, jlong ssl)
{
    SSL *ssl_ = J2P(ssl, SSL *);
    TCN_CHECK_NULL(ssl_, ssl, 0);
    if (SSL_session_reused(ssl_) == 1) {
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

TCN_IMPLEMENT_CALL(jobjectArray, SSL, getSigAlgs)(TCN_STDARGS, jlong ssl) {
    SSL *ssl_ = J2P(ssl, SSL *);
    TCN_CHECK_NULL(ssl_, ssl, NULL);

// Not supported in LibreSSL
#if defined(LIBRESSL_VERSION_NUMBER)
    return NULL;
#elif defined(OPENSSL_IS_BORINGSSL)
    // Using a different API in BoringSSL
    // https://boringssl.googlesource.com/boringssl/+/ba16a1e405c617f4179bd780ad15522fb25b0a65%5E%21/
    int i;
    int num_known_sigalgs = 0;
    jobjectArray array = NULL;
    jstring algString = NULL;
    const char *alg = NULL;
    const char** algs = NULL;
    const uint16_t *peer_sigalgs = NULL;
    size_t num_peer_sigalgs = SSL_get0_peer_verify_algorithms(ssl_, &peer_sigalgs);

    if (num_peer_sigalgs <= 0) {
        return NULL;
    }

    if ((algs = OPENSSL_malloc(sizeof(char*) * num_peer_sigalgs)) == NULL) {
        return NULL;
    }

    for (i = 0; i < num_peer_sigalgs; i++) {
        if ((alg = SSL_get_signature_algorithm_name(peer_sigalgs[i], SSL_version(ssl_) != TLS1_2_VERSION)) == NULL) {
            // The signature algorithm is not known to BoringSSL, skip it.
            continue;
        }

        algs[num_known_sigalgs++] = alg;
    }

    if (num_known_sigalgs == 0) {
        goto complete;
    }

    if ((array = (*e)->NewObjectArray(e, num_known_sigalgs, tcn_get_string_class(), NULL)) == NULL) {
        goto complete;
    }

    for (i = 0; i < num_known_sigalgs; i++) {
        if ((algString = (*e)->NewStringUTF(e, algs[i])) == NULL) {
            // something is wrong we should better bail out.
            array = NULL;
            goto complete;
        }

        (*e)->SetObjectArrayElement(e, array, i, algString);
    }

complete:
    OPENSSL_free(algs);
    return array;
#else

// Use weak linking with GCC as this will alow us to run the same packaged version with multiple
// version of openssl.
#if defined(__GNUC__) || defined(__GNUG__)
    if (!SSL_get_sigalgs) {
        return NULL;
    }
#endif

// We can only support it when either use openssl version >= 1.0.2 or GCC as this way we can use weak linking
#if OPENSSL_VERSION_NUMBER >= 0x10002000L || defined(__GNUC__) || defined(__GNUG__)
    int i;
    int nsig;
    int psignhash;
    jobjectArray array = NULL;
    jstring algString = NULL;

    nsig = SSL_get_sigalgs(ssl_, 0, NULL, NULL, NULL, NULL, NULL);
    if (nsig <= 0) {
        return NULL;
    }

    if ((array = (*e)->NewObjectArray(e, nsig, tcn_get_string_class(), NULL)) == NULL) {
        return NULL;
    }

    for (i = 0; i < nsig; i++) {
        SSL_get_sigalgs(ssl_, i, NULL, NULL, &psignhash, NULL, NULL);
        if ((algString = (*e)->NewStringUTF(e, OBJ_nid2ln(psignhash))) == NULL) {
            // something is wrong we should better just return here
            return NULL;
        }
        (*e)->SetObjectArrayElement(e, array, i, algString);
    }
    return array;
#endif // OPENSSL_VERSION_NUMBER >= 0x10002000L || defined(__GNUC__) || defined(__GNUG__)
#endif // defined(OPENSSL_IS_BORINGSSL) || defined(LIBRESSL_VERSION_NUMBER)
}

TCN_IMPLEMENT_CALL(void, SSL, setRenegotiateMode)(TCN_STDARGS, jlong ssl, jint mode) {
    SSL *ssl_ = J2P(ssl, SSL *);

    TCN_CHECK_NULL(ssl_, ssl, /* void */);
#ifndef OPENSSL_IS_BORINGSSL
    tcn_Throw(e, "Not supported");
#else
    SSL_set_renegotiate_mode(ssl_, (enum ssl_renegotiate_mode_t) mode);
#endif
}

// JNI Method Registration Table Begin
static const JNINativeMethod method_table[] = {
  { TCN_METHOD_TABLE_ENTRY(bioLengthByteBuffer, (J)I, SSL) },
  { TCN_METHOD_TABLE_ENTRY(bioLengthNonApplication, (J)I, SSL) },
  { TCN_METHOD_TABLE_ENTRY(version, ()I, SSL) },
  { TCN_METHOD_TABLE_ENTRY(versionString, ()Ljava/lang/String;, SSL) },
  { TCN_METHOD_TABLE_ENTRY(initialize, (Ljava/lang/String;)I, SSL) },
  { TCN_METHOD_TABLE_ENTRY(newMemBIO, ()J, SSL) },
  { TCN_METHOD_TABLE_ENTRY(getLastError, ()Ljava/lang/String;, SSL) },
  { TCN_METHOD_TABLE_ENTRY(getLastErrorNumber, ()I, SSL) },
  { TCN_METHOD_TABLE_ENTRY(newSSL, (JZ)J, SSL) },
  { TCN_METHOD_TABLE_ENTRY(getError, (JI)I, SSL) },
  { TCN_METHOD_TABLE_ENTRY(bioWrite, (JJI)I, SSL) },
  { TCN_METHOD_TABLE_ENTRY(bioSetByteBuffer, (JJIZ)V, SSL) },
  { TCN_METHOD_TABLE_ENTRY(bioClearByteBuffer, (J)V, SSL) },
  { TCN_METHOD_TABLE_ENTRY(bioFlushByteBuffer, (J)I, SSL) },
  { TCN_METHOD_TABLE_ENTRY(sslPending, (J)I, SSL) },
  { TCN_METHOD_TABLE_ENTRY(writeToSSL, (JJI)I, SSL) },
  { TCN_METHOD_TABLE_ENTRY(readFromSSL, (JJI)I, SSL) },
  { TCN_METHOD_TABLE_ENTRY(getShutdown, (J)I, SSL) },
  { TCN_METHOD_TABLE_ENTRY(setShutdown, (JI)V, SSL) },
  { TCN_METHOD_TABLE_ENTRY(freeSSL, (J)V, SSL) },
  { TCN_METHOD_TABLE_ENTRY(bioSetFd, (JI)V, SSL) },
  { TCN_METHOD_TABLE_ENTRY(bioNewByteBuffer, (JI)J, SSL) },
  { TCN_METHOD_TABLE_ENTRY(freeBIO, (J)V, SSL) },
  { TCN_METHOD_TABLE_ENTRY(shutdownSSL, (J)I, SSL) },
  { TCN_METHOD_TABLE_ENTRY(getCipherForSSL, (J)Ljava/lang/String;, SSL) },
  { TCN_METHOD_TABLE_ENTRY(getVersion, (J)Ljava/lang/String;, SSL) },
  { TCN_METHOD_TABLE_ENTRY(isInInit, (J)I, SSL) },
  { TCN_METHOD_TABLE_ENTRY(doHandshake, (J)I, SSL) },
  { TCN_METHOD_TABLE_ENTRY(getNextProtoNegotiated, (J)Ljava/lang/String;, SSL) },
  { TCN_METHOD_TABLE_ENTRY(getAlpnSelected, (J)Ljava/lang/String;, SSL) },
  { TCN_METHOD_TABLE_ENTRY(getPeerCertChain, (J)[[B, SSL) },
  { TCN_METHOD_TABLE_ENTRY(getPeerCertificate, (J)[B, SSL) },
  { TCN_METHOD_TABLE_ENTRY(getErrorString, (J)Ljava/lang/String;, SSL) },
  { TCN_METHOD_TABLE_ENTRY(getTime, (J)J, SSL) },
  { TCN_METHOD_TABLE_ENTRY(getTimeout, (J)J, SSL) },
  { TCN_METHOD_TABLE_ENTRY(setTimeout, (JJ)J, SSL) },
  { TCN_METHOD_TABLE_ENTRY(setSession, (JJ)Z, SSL) },
  { TCN_METHOD_TABLE_ENTRY(setVerify, (JII)V, SSL) },
  { TCN_METHOD_TABLE_ENTRY(setOptions, (JI)V, SSL) },
  { TCN_METHOD_TABLE_ENTRY(clearOptions, (JI)V, SSL) },
  { TCN_METHOD_TABLE_ENTRY(getOptions, (J)I, SSL) },
  { TCN_METHOD_TABLE_ENTRY(setMode, (JI)I, SSL) },
  { TCN_METHOD_TABLE_ENTRY(getMode, (J)I, SSL) },
  { TCN_METHOD_TABLE_ENTRY(getMaxWrapOverhead, (J)I, SSL) },
  { TCN_METHOD_TABLE_ENTRY(getCiphers, (J)[Ljava/lang/String;, SSL) },
  { TCN_METHOD_TABLE_ENTRY(setCipherSuites, (JLjava/lang/String;Z)Z, SSL) },
  { TCN_METHOD_TABLE_ENTRY(getSessionId, (J)[B, SSL) },
  { TCN_METHOD_TABLE_ENTRY(getHandshakeCount, (J)I, SSL) },
  { TCN_METHOD_TABLE_ENTRY(clearError, ()V, SSL) },
  { TCN_METHOD_TABLE_ENTRY(setTlsExtHostName0, (JLjava/lang/String;)V, SSL) },
  { TCN_METHOD_TABLE_ENTRY(setHostNameValidation, (JILjava/lang/String;)V, SSL) },
  { TCN_METHOD_TABLE_ENTRY(authenticationMethods, (J)[Ljava/lang/String;, SSL) },
  { TCN_METHOD_TABLE_ENTRY(setCertificateBio, (JJJLjava/lang/String;)V, SSL) },
  { TCN_METHOD_TABLE_ENTRY(setCertificateChainBio, (JJZ)V, SSL) },
  { TCN_METHOD_TABLE_ENTRY(loadPrivateKeyFromEngine, (Ljava/lang/String;Ljava/lang/String;)J, SSL) },
  { TCN_METHOD_TABLE_ENTRY(parsePrivateKey, (JLjava/lang/String;)J, SSL) },
  { TCN_METHOD_TABLE_ENTRY(freePrivateKey, (J)V, SSL) },
  { TCN_METHOD_TABLE_ENTRY(parseX509Chain, (J)J, SSL) },
  { TCN_METHOD_TABLE_ENTRY(freeX509Chain, (J)V, SSL) },
  { TCN_METHOD_TABLE_ENTRY(setKeyMaterial, (JJJ)V, SSL) },
  { TCN_METHOD_TABLE_ENTRY(setKeyMaterialClientSide, (JJJJJ)V, SSL) },
  { TCN_METHOD_TABLE_ENTRY(enableOcsp, (J)V, SSL) },
  { TCN_METHOD_TABLE_ENTRY(setOcspResponse, (J[B)V, SSL) },
  { TCN_METHOD_TABLE_ENTRY(getOcspResponse, (J)[B, SSL) },
  { TCN_METHOD_TABLE_ENTRY(fipsModeSet, (I)V, SSL) },
  { TCN_METHOD_TABLE_ENTRY(getSniHostname, (J)Ljava/lang/String;, SSL) },
  { TCN_METHOD_TABLE_ENTRY(getSigAlgs, (J)[Ljava/lang/String;, SSL) },
  { TCN_METHOD_TABLE_ENTRY(getMasterKey, (J)[B, SSL) },
  { TCN_METHOD_TABLE_ENTRY(getClientRandom, (J)[B, SSL) },
  { TCN_METHOD_TABLE_ENTRY(getServerRandom, (J)[B, SSL) },
  { TCN_METHOD_TABLE_ENTRY(getTask, (J)Ljava/lang/Runnable;, SSL) },
  { TCN_METHOD_TABLE_ENTRY(getSession, (J)J, SSL) },
  { TCN_METHOD_TABLE_ENTRY(isSessionReused, (J)Z, SSL) },
  { TCN_METHOD_TABLE_ENTRY(setRenegotiateMode, (JI)V, SSL) }
};

static const jint method_table_size = sizeof(method_table) / sizeof(method_table[0]);

// JNI Method Registration Table End

// IMPORTANT: If you add any NETTY_JNI_UTIL_LOAD_CLASS or NETTY_JNI_UTIL_FIND_CLASS calls you also need to update
//            Library to reflect that.
jint netty_internal_tcnative_SSL_JNI_OnLoad(JNIEnv* env, const char* packagePrefix) {
    if (netty_jni_util_register_natives(env,
             packagePrefix,
             SSL_CLASSNAME,
             method_table, method_table_size) != 0) {
        return JNI_ERR;
    }
    return NETTY_JNI_UTIL_JNI_VERSION;
}

void netty_internal_tcnative_SSL_JNI_OnUnLoad(JNIEnv* env, const char* packagePrefix) {
    netty_jni_util_unregister_natives(env, packagePrefix, SSL_CLASSNAME);
}

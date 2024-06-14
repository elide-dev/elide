/*
 * Copyright 2022 The Netty Project
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
#ifdef OPENSSL_IS_BORINGSSL
#include "cert_compress.h"

static int compress(jobject compression_algorithm, jmethodID compress_method, SSL* ssl, CBB* out,
    const uint8_t* in, size_t in_len) {

    JNIEnv *e = NULL;
    jbyteArray inputArray = NULL;

    if (compression_algorithm == NULL || compress_method == NULL) {
        return 0;
    }
    if (tcn_get_java_env(&e) != JNI_OK) {
        return 0;
    }
    if ((inputArray = (*e)->NewByteArray(e, in_len)) == NULL) {
        return 0;
    }

    (*e)->SetByteArrayRegion(e, inputArray, 0, in_len, (jbyte*) in);

    jbyteArray resultArray = (*e)->CallObjectMethod(e, compression_algorithm, compress_method,
                    P2J(ssl), inputArray);

    if ((*e)->ExceptionCheck(e) != JNI_FALSE) {
        (*e)->ExceptionClear(e);
        return 0; // Exception while calling into Java
    }
    if (resultArray == NULL) {
        return 0; // Received NULL array from call to Java
    }

    int resultLen = (*e)->GetArrayLength(e, resultArray);
    uint8_t* outData = NULL;
    if (!CBB_reserve(out, &outData, resultLen)) {
        return 0; // Unable to reserve space for compressed data
    }
    jbyte* resultData = (*e)->GetByteArrayElements(e, resultArray, NULL);
    if (resultData == NULL) {
        return 0;
    }
    memcpy(outData, resultData, resultLen);
    (*e)->ReleaseByteArrayElements(e, resultArray, resultData, JNI_ABORT);
    if (!CBB_did_write(out, resultLen)) {
        return 0; // Unable to advance bytes written to CBB
    }
    return 1; // Success
}

static int decompress(jobject compression_algorithm, jmethodID decompress_method, SSL* ssl, CRYPTO_BUFFER** out,
    size_t uncompressed_len, const uint8_t* in, size_t in_len) {

    JNIEnv* e = NULL;
    jbyteArray inputArray = NULL;

    if (compression_algorithm == NULL || decompress_method == NULL) {
        return 0;
    }
    if (tcn_get_java_env(&e) != JNI_OK) {
        return 0;
    }
    if ((inputArray = (*e)->NewByteArray(e, in_len)) == NULL) {
        return 0;
    }

    (*e)->SetByteArrayRegion(e, inputArray, 0, in_len, (jbyte*) in);

    // BoringSSL checks that `uncompressed_len <= ssl->max_cert_list` before calling `ssl_cert_decompression_func_t`
    // `max_cert_list` contains the max cert size, avoiding excessive allocations.
    jbyteArray resultArray = (*e)->CallObjectMethod(e, compression_algorithm, decompress_method,
                    P2J(ssl), uncompressed_len, inputArray);

    if ((*e)->ExceptionCheck(e) != JNI_FALSE) {
        (*e)->ExceptionClear(e);
        return 0; // Exception while calling into Java
    }
    if (resultArray == NULL) {
        return 0; // Received NULL array from call to Java
    }

    int resultLen = (*e)->GetArrayLength(e, resultArray);
    if (uncompressed_len != resultLen) {
        return 0; // Unexpected uncompressed length
    }
    uint8_t* outData;
    if (!((*out) = CRYPTO_BUFFER_alloc(&outData, uncompressed_len))) {
        return 0; // Unable to allocate certificate decompression buffer
    }
    jbyte* resultData = (*e)->GetByteArrayElements(e, resultArray, NULL);
    if (resultData == NULL) {
        return 0;
    }
    memcpy(outData, resultData, uncompressed_len);
    (*e)->ReleaseByteArrayElements(e, resultArray, resultData, JNI_ABORT);
    return 1; // Success

}

int zlib_compress_java(SSL* ssl, CBB* out, const uint8_t* in, size_t in_len)
{
    tcn_ssl_ctxt_t* c = NULL;
    TCN_GET_SSL_CTX(ssl, c);
    TCN_ASSERT(c != NULL);
    return compress(c->ssl_cert_compression_zlib_algorithm, c->ssl_cert_compression_zlib_compress_method,
        ssl, out, in, in_len);
}

int zlib_decompress_java(SSL* ssl, CRYPTO_BUFFER** out, size_t uncompressed_len, const uint8_t* in, size_t in_len)
{
    tcn_ssl_ctxt_t* c = NULL;
    TCN_GET_SSL_CTX(ssl, c);
    TCN_ASSERT(c != NULL);
    return decompress(c->ssl_cert_compression_zlib_algorithm, c->ssl_cert_compression_zlib_decompress_method,
        ssl, out, uncompressed_len, in, in_len);
}

int brotli_compress_java(SSL* ssl, CBB* out, const uint8_t* in, size_t in_len)
{
    tcn_ssl_ctxt_t* c = NULL;
    TCN_GET_SSL_CTX(ssl, c);
    TCN_ASSERT(c != NULL);
    return compress(c->ssl_cert_compression_brotli_algorithm, c->ssl_cert_compression_brotli_compress_method,
        ssl, out, in, in_len);
}

int brotli_decompress_java(SSL* ssl, CRYPTO_BUFFER** out, size_t uncompressed_len, const uint8_t* in, size_t in_len)
{
    tcn_ssl_ctxt_t* c = NULL;
    TCN_GET_SSL_CTX(ssl, c);
    TCN_ASSERT(c != NULL);
    return decompress(c->ssl_cert_compression_brotli_algorithm, c->ssl_cert_compression_brotli_decompress_method,
        ssl, out, uncompressed_len, in, in_len);
}

int zstd_compress_java(SSL* ssl, CBB* out, const uint8_t* in, size_t in_len)
{
    tcn_ssl_ctxt_t* c = NULL;
    TCN_GET_SSL_CTX(ssl, c);
    TCN_ASSERT(c != NULL);
    return compress(c->ssl_cert_compression_zstd_algorithm, c->ssl_cert_compression_zstd_compress_method,
        ssl, out, in, in_len);
}

int zstd_decompress_java(SSL* ssl, CRYPTO_BUFFER** out, size_t uncompressed_len, const uint8_t* in, size_t in_len)
{
    tcn_ssl_ctxt_t* c = NULL;
    TCN_GET_SSL_CTX(ssl, c);
    TCN_ASSERT(c != NULL);
    return decompress(c->ssl_cert_compression_zstd_algorithm, c->ssl_cert_compression_zstd_decompress_method,
        ssl, out, uncompressed_len, in, in_len);
}

#endif // OPENSSL_IS_BORINGSSL
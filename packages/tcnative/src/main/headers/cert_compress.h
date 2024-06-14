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

#ifndef NETTY_TCNATIVE_CERT_COMPRESS_H_
#define NETTY_TCNATIVE_CERT_COMPRESS_H_

#ifdef OPENSSL_IS_BORINGSSL

int zlib_decompress_java(SSL* ssl, CRYPTO_BUFFER** out, size_t uncompressed_len, const uint8_t* in, size_t in_len);
int zlib_compress_java(SSL* ssl, CBB* out, const uint8_t* in, size_t in_len);

int brotli_decompress_java(SSL* ssl, CRYPTO_BUFFER** out, size_t uncompressed_len, const uint8_t* in, size_t in_len);
int brotli_compress_java(SSL* ssl, CBB* out, const uint8_t* in, size_t in_len);

int zstd_decompress_java(SSL* ssl, CRYPTO_BUFFER** out, size_t uncompressed_len, const uint8_t* in, size_t in_len);
int zstd_compress_java(SSL* ssl, CBB* out, const uint8_t* in, size_t in_len);

#endif // OPENSSL_IS_BORINGSSL

#endif /* NETTY_TCNATIVE_CERT_COMPRESS_H_ */
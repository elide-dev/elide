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
package io.netty.internal.tcnative;

/**
 * Provides compression/decompression implementations for TLS Certificate Compression
 * (<a href="https://tools.ietf.org/html/rfc8879">RFC 8879</a>).
 */
public interface CertificateCompressionAlgo {
    int TLS_EXT_CERT_COMPRESSION_ZLIB   = NativeStaticallyReferencedJniMethods.tlsExtCertCompressionZlib();
    int TLS_EXT_CERT_COMPRESSION_BROTLI = NativeStaticallyReferencedJniMethods.tlsExtCertCompressionBrotli();
    int TLS_EXT_CERT_COMPRESSION_ZSTD   = NativeStaticallyReferencedJniMethods.tlsExtCertCompressionZstd();

    /**
     * Compress the given input with the specified algorithm and return the compressed bytes.
     *
     * @param ssl           the SSL instance
     * @param input         the uncompressed form of the certificate
     * @return              the compressed form of the certificate
     * @throws Exception    thrown if an error occurs while compressing
     */
    byte[] compress(long ssl, byte[] input) throws Exception;

    /**
     * Decompress the given input with the specified algorithm and return the decompressed bytes.
     *
     * <h3>Implementation
     * <a href="https://tools.ietf.org/html/rfc8879#section-5">Security Considerations</a></h3>
     * <p>Implementations SHOULD bound the memory usage when decompressing the CompressedCertificate message.</p>
     * <p>
     * Implementations MUST limit the size of the resulting decompressed chain to the specified {@code uncompressedLen},
     * and they MUST abort the connection (throw an exception) if the size of the output of the decompression
     * function exceeds that limit.
     * </p>
     *
     * @param ssl               the SSL instance
     * @param uncompressedLen   the expected length of the uncompressed certificate
     * @param input             the compressed form of the certificate
     * @return                  the decompressed form of the certificate
     * @throws Exception        thrown if an error occurs while decompressing or output
     * size exceeds {@code uncompressedLen}
     */
    byte[] decompress(long ssl, int uncompressedLen, byte[] input) throws Exception;

    /**
     * Return the ID for the compression algorithm provided for by a given implementation.
     *
     * @return compression algorithm ID as specified by RFC8879
     * <PRE>
     * {@link CertificateCompressionAlgo#TLS_EXT_CERT_COMPRESSION_ZLIB}
     * {@link CertificateCompressionAlgo#TLS_EXT_CERT_COMPRESSION_BROTLI}
     * {@link CertificateCompressionAlgo#TLS_EXT_CERT_COMPRESSION_ZSTD}
     * </PRE>
     */
    int algorithmId();

}

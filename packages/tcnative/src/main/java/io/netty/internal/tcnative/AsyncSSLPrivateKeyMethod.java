/*
 * Copyright 2021 The Netty Project
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
 * Allows to customize private key signing / decrypt (when using RSA).
 */
public interface AsyncSSLPrivateKeyMethod {
    int SSL_SIGN_RSA_PKCS1_SHA1 = NativeStaticallyReferencedJniMethods.sslSignRsaPkcsSha1();
    int SSL_SIGN_RSA_PKCS1_SHA256 = NativeStaticallyReferencedJniMethods.sslSignRsaPkcsSha256();
    int SSL_SIGN_RSA_PKCS1_SHA384 = NativeStaticallyReferencedJniMethods.sslSignRsaPkcsSha384();
    int SSL_SIGN_RSA_PKCS1_SHA512 = NativeStaticallyReferencedJniMethods.sslSignRsaPkcsSha512();
    int SSL_SIGN_ECDSA_SHA1 = NativeStaticallyReferencedJniMethods.sslSignEcdsaPkcsSha1();
    int SSL_SIGN_ECDSA_SECP256R1_SHA256 = NativeStaticallyReferencedJniMethods.sslSignEcdsaSecp256r1Sha256();
    int SSL_SIGN_ECDSA_SECP384R1_SHA384 = NativeStaticallyReferencedJniMethods.sslSignEcdsaSecp384r1Sha384();
    int SSL_SIGN_ECDSA_SECP521R1_SHA512 = NativeStaticallyReferencedJniMethods.sslSignEcdsaSecp521r1Sha512();
    int SSL_SIGN_RSA_PSS_RSAE_SHA256 = NativeStaticallyReferencedJniMethods.sslSignRsaPssRsaeSha256();
    int SSL_SIGN_RSA_PSS_RSAE_SHA384 = NativeStaticallyReferencedJniMethods.sslSignRsaPssRsaeSha384();
    int SSL_SIGN_RSA_PSS_RSAE_SHA512 = NativeStaticallyReferencedJniMethods.sslSignRsaPssRsaeSha512();
    int SSL_SIGN_ED25519 = NativeStaticallyReferencedJniMethods.sslSignEd25519();
    int SSL_SIGN_RSA_PKCS1_MD5_SHA1 = NativeStaticallyReferencedJniMethods.sslSignRsaPkcs1Md5Sha1();

    /**
     * Sign the input with given EC key and notify {@link ResultCallback} with the signed bytes.
     *
     * @param ssl                   the SSL instance
     * @param signatureAlgorithm    the algorithm to use for signing
     * @param input                 the input itself
     * @param resultCallback        the callback that will be notified once the operation completes
     */
    void sign(long ssl, int signatureAlgorithm, byte[] input, ResultCallback<byte[]> resultCallback);

    /**
     * Decrypts the input with the given RSA key and notify {@link ResultCallback} with the decrypted bytes.
     *
     * @param ssl                   the SSL instance
     * @param input                 the input which should be decrypted
     * @param resultCallback        the callback that will be notified once the operation completes
     */
    void decrypt(long ssl, byte[] input, ResultCallback<byte[]> resultCallback);
}

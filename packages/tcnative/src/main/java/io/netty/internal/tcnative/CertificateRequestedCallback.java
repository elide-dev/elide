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
package io.netty.internal.tcnative;

/**
 * Is called during handshake and hooked into openssl via {@code SSL_CTX_set_client_cert_cb}.
 *
 * IMPORTANT: Implementations of this interface should be static as it is stored as a global reference via JNI. This
 *            means if you use an inner / anonymous class to implement this and also depend on the finalizer of the
 *            class to free up the SSLContext the finalizer will never run as the object is never GC, due the hard
 *            reference to the enclosing class. This will most likely result in a memory leak.+
 *
 * @deprecated use {@link CertificateCallback}
 */
@Deprecated
public interface CertificateRequestedCallback {

    /**
     * The types contained in the {@code keyTypeBytes} array.
     */
    // Extracted from https://github.com/openssl/openssl/blob/master/include/openssl/tls1.h
    byte TLS_CT_RSA_SIGN = CertificateCallback.TLS_CT_RSA_SIGN;
    byte TLS_CT_DSS_SIGN = CertificateCallback.TLS_CT_DSS_SIGN;
    byte TLS_CT_RSA_FIXED_DH = CertificateCallback.TLS_CT_RSA_FIXED_DH;
    byte TLS_CT_DSS_FIXED_DH = CertificateCallback.TLS_CT_DSS_FIXED_DH;
    byte TLS_CT_ECDSA_SIGN = CertificateCallback.TLS_CT_ECDSA_SIGN;
    byte TLS_CT_RSA_FIXED_ECDH = CertificateCallback.TLS_CT_RSA_FIXED_ECDH;
    byte TLS_CT_ECDSA_FIXED_ECDH = CertificateCallback.TLS_CT_ECDSA_FIXED_ECDH;

    /**
     * Called during cert selection. If a certificate chain / key should be used
     * {@link SSL#setKeyMaterialClientSide(long, long, long, long, long)} must be called from this callback after
     * all preparations / validations were completed.
     *
     * @param ssl                       the SSL instance
     * @param certOut                   the pointer to the pointer of the certificate to use.
     * @param keyOut                    the pointer to the pointer of the private key to use.
     * @param keyTypeBytes              an array of the key types.
     * @param asn1DerEncodedPrincipals  the principals
     *
     */
    void requested(long ssl, long certOut, long keyOut, byte[] keyTypeBytes, byte[][] asn1DerEncodedPrincipals)
            throws Exception;
}

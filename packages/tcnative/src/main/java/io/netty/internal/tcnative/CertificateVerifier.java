/*
 * Copyright 2014 The Netty Project
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static io.netty.internal.tcnative.NativeStaticallyReferencedJniMethods.*;

/**
 * Is called during handshake and hooked into openssl via {@code SSL_CTX_set_cert_verify_callback}.
 *
 * IMPORTANT: Implementations of this interface should be static as it is stored as a global reference via JNI. This
 *            means if you use an inner / anonymous class to implement this and also depend on the finalizer of the
 *            class to free up the SSLContext the finalizer will never run as the object is never GC, due the hard
 *            reference to the enclosing class. This will most likely result in a memory leak.
 */
public abstract class CertificateVerifier {

    // WARNING: If you add any new field here you also need to add it to the ERRORS set!
    public static final int X509_V_OK = x509vOK();
    public static final int X509_V_ERR_UNSPECIFIED = x509vErrUnspecified();
    public static final int X509_V_ERR_UNABLE_TO_GET_ISSUER_CERT = x509vErrUnableToGetIssuerCert();
    public static final int X509_V_ERR_UNABLE_TO_GET_CRL = x509vErrUnableToGetCrl();
    public static final int X509_V_ERR_UNABLE_TO_DECRYPT_CERT_SIGNATURE = x509vErrUnableToDecryptCertSignature();
    public static final int X509_V_ERR_UNABLE_TO_DECRYPT_CRL_SIGNATURE = x509vErrUnableToDecryptCrlSignature();
    public static final int X509_V_ERR_UNABLE_TO_DECODE_ISSUER_PUBLIC_KEY = x509vErrUnableToDecodeIssuerPublicKey();
    public static final int X509_V_ERR_CERT_SIGNATURE_FAILURE = x509vErrCertSignatureFailure();
    public static final int X509_V_ERR_CRL_SIGNATURE_FAILURE = x509vErrCrlSignatureFailure();
    public static final int X509_V_ERR_CERT_NOT_YET_VALID = x509vErrCertNotYetValid();
    public static final int X509_V_ERR_CERT_HAS_EXPIRED = x509vErrCertHasExpired();
    public static final int X509_V_ERR_CRL_NOT_YET_VALID = x509vErrCrlNotYetValid();
    public static final int X509_V_ERR_CRL_HAS_EXPIRED = x509vErrCrlHasExpired();
    public static final int X509_V_ERR_ERROR_IN_CERT_NOT_BEFORE_FIELD = x509vErrErrorInCertNotBeforeField();
    public static final int X509_V_ERR_ERROR_IN_CERT_NOT_AFTER_FIELD = x509vErrErrorInCertNotAfterField();
    public static final int X509_V_ERR_ERROR_IN_CRL_LAST_UPDATE_FIELD = x509vErrErrorInCrlLastUpdateField();
    public static final int X509_V_ERR_ERROR_IN_CRL_NEXT_UPDATE_FIELD = x509vErrErrorInCrlNextUpdateField();
    public static final int X509_V_ERR_OUT_OF_MEM = x509vErrOutOfMem();
    public static final int X509_V_ERR_DEPTH_ZERO_SELF_SIGNED_CERT = x509vErrDepthZeroSelfSignedCert();
    public static final int X509_V_ERR_SELF_SIGNED_CERT_IN_CHAIN = x509vErrSelfSignedCertInChain();
    public static final int X509_V_ERR_UNABLE_TO_GET_ISSUER_CERT_LOCALLY = x509vErrUnableToGetIssuerCertLocally();
    public static final int X509_V_ERR_UNABLE_TO_VERIFY_LEAF_SIGNATURE  = x509vErrUnableToVerifyLeafSignature();
    public static final int X509_V_ERR_CERT_CHAIN_TOO_LONG = x509vErrCertChainTooLong();
    public static final int X509_V_ERR_CERT_REVOKED = x509vErrCertRevoked();
    public static final int X509_V_ERR_INVALID_CA = x509vErrInvalidCa();
    public static final int X509_V_ERR_PATH_LENGTH_EXCEEDED = x509vErrPathLengthExceeded();
    public static final int X509_V_ERR_INVALID_PURPOSE = x509vErrInvalidPurpose();
    public static final int X509_V_ERR_CERT_UNTRUSTED = x509vErrCertUntrusted();
    public static final int X509_V_ERR_CERT_REJECTED = x509vErrCertRejected();
    public static final int X509_V_ERR_SUBJECT_ISSUER_MISMATCH = x509vErrSubjectIssuerMismatch();
    public static final int X509_V_ERR_AKID_SKID_MISMATCH = x509vErrAkidSkidMismatch();
    public static final int X509_V_ERR_AKID_ISSUER_SERIAL_MISMATCH = x509vErrAkidIssuerSerialMismatch();
    public static final int X509_V_ERR_KEYUSAGE_NO_CERTSIGN = x509vErrKeyUsageNoCertSign();
    public static final int X509_V_ERR_UNABLE_TO_GET_CRL_ISSUER = x509vErrUnableToGetCrlIssuer();
    public static final int X509_V_ERR_UNHANDLED_CRITICAL_EXTENSION = x509vErrUnhandledCriticalExtension();
    public static final int X509_V_ERR_KEYUSAGE_NO_CRL_SIGN = x509vErrKeyUsageNoCrlSign();
    public static final int X509_V_ERR_UNHANDLED_CRITICAL_CRL_EXTENSION = x509vErrUnhandledCriticalCrlExtension();
    public static final int X509_V_ERR_INVALID_NON_CA = x509vErrInvalidNonCa();
    public static final int X509_V_ERR_PROXY_PATH_LENGTH_EXCEEDED = x509vErrProxyPathLengthExceeded();
    public static final int X509_V_ERR_KEYUSAGE_NO_DIGITAL_SIGNATURE = x509vErrKeyUsageNoDigitalSignature();
    public static final int X509_V_ERR_PROXY_CERTIFICATES_NOT_ALLOWED = x509vErrProxyCertificatesNotAllowed();
    public static final int X509_V_ERR_INVALID_EXTENSION = x509vErrInvalidExtension();
    public static final int X509_V_ERR_INVALID_POLICY_EXTENSION = x509vErrInvalidPolicyExtension();
    public static final int X509_V_ERR_NO_EXPLICIT_POLICY = x509vErrNoExplicitPolicy();
    public static final int X509_V_ERR_DIFFERENT_CRL_SCOPE = x509vErrDifferntCrlScope();
    public static final int X509_V_ERR_UNSUPPORTED_EXTENSION_FEATURE = x509vErrUnsupportedExtensionFeature();
    public static final int X509_V_ERR_UNNESTED_RESOURCE = x509vErrUnnestedResource();
    public static final int X509_V_ERR_PERMITTED_VIOLATION = x509vErrPermittedViolation();
    public static final int X509_V_ERR_EXCLUDED_VIOLATION  = x509vErrExcludedViolation();
    public static final int X509_V_ERR_SUBTREE_MINMAX = x509vErrSubtreeMinMax();
    public static final int X509_V_ERR_APPLICATION_VERIFICATION = x509vErrApplicationVerification();
    public static final int X509_V_ERR_UNSUPPORTED_CONSTRAINT_TYPE = x509vErrUnsupportedConstraintType();
    public static final int X509_V_ERR_UNSUPPORTED_CONSTRAINT_SYNTAX = x509vErrUnsupportedConstraintSyntax();
    public static final int X509_V_ERR_UNSUPPORTED_NAME_SYNTAX = x509vErrUnsupportedNameSyntax();
    public static final int X509_V_ERR_CRL_PATH_VALIDATION_ERROR = x509vErrCrlPathValidationError();
    public static final int X509_V_ERR_PATH_LOOP = x509vErrPathLoop();
    public static final int X509_V_ERR_SUITE_B_INVALID_VERSION = x509vErrSuiteBInvalidVersion();
    public static final int X509_V_ERR_SUITE_B_INVALID_ALGORITHM = x509vErrSuiteBInvalidAlgorithm();
    public static final int X509_V_ERR_SUITE_B_INVALID_CURVE = x509vErrSuiteBInvalidCurve();
    public static final int X509_V_ERR_SUITE_B_INVALID_SIGNATURE_ALGORITHM = x509vErrSuiteBInvalidSignatureAlgorithm();
    public static final int X509_V_ERR_SUITE_B_LOS_NOT_ALLOWED = x509vErrSuiteBLosNotAllowed();
    public static final int X509_V_ERR_SUITE_B_CANNOT_SIGN_P_384_WITH_P_256 = x509vErrSuiteBCannotSignP384WithP256();
    public static final int X509_V_ERR_HOSTNAME_MISMATCH = x509vErrHostnameMismatch();
    public static final int X509_V_ERR_EMAIL_MISMATCH = x509vErrEmailMismatch();
    public static final int X509_V_ERR_IP_ADDRESS_MISMATCH = x509vErrIpAddressMismatch();
    public static final int X509_V_ERR_DANE_NO_MATCH = x509vErrDaneNoMatch();

    private static final Set<Integer> ERRORS;

    static {
        Set<Integer> errors = new HashSet<Integer>();
        errors.add(X509_V_OK);
        errors.add(X509_V_ERR_UNSPECIFIED);
        errors.add(X509_V_ERR_UNABLE_TO_GET_ISSUER_CERT);
        errors.add(X509_V_ERR_UNABLE_TO_GET_CRL);
        errors.add(X509_V_ERR_UNABLE_TO_DECRYPT_CERT_SIGNATURE);
        errors.add(X509_V_ERR_UNABLE_TO_DECRYPT_CRL_SIGNATURE);
        errors.add(X509_V_ERR_UNABLE_TO_DECODE_ISSUER_PUBLIC_KEY);
        errors.add(X509_V_ERR_CERT_SIGNATURE_FAILURE);
        errors.add(X509_V_ERR_CRL_SIGNATURE_FAILURE);
        errors.add(X509_V_ERR_CERT_NOT_YET_VALID);
        errors.add(X509_V_ERR_CERT_HAS_EXPIRED);
        errors.add(X509_V_ERR_CRL_NOT_YET_VALID);
        errors.add(X509_V_ERR_CRL_HAS_EXPIRED);
        errors.add(X509_V_ERR_ERROR_IN_CERT_NOT_BEFORE_FIELD);
        errors.add(X509_V_ERR_ERROR_IN_CERT_NOT_AFTER_FIELD);
        errors.add(X509_V_ERR_ERROR_IN_CRL_LAST_UPDATE_FIELD);
        errors.add(X509_V_ERR_ERROR_IN_CRL_NEXT_UPDATE_FIELD);
        errors.add(X509_V_ERR_OUT_OF_MEM);
        errors.add(X509_V_ERR_DEPTH_ZERO_SELF_SIGNED_CERT);
        errors.add(X509_V_ERR_SELF_SIGNED_CERT_IN_CHAIN);
        errors.add(X509_V_ERR_UNABLE_TO_GET_ISSUER_CERT_LOCALLY);
        errors.add(X509_V_ERR_UNABLE_TO_VERIFY_LEAF_SIGNATURE);
        errors.add(X509_V_ERR_CERT_CHAIN_TOO_LONG);
        errors.add(X509_V_ERR_CERT_REVOKED);
        errors.add(X509_V_ERR_INVALID_CA);
        errors.add(X509_V_ERR_PATH_LENGTH_EXCEEDED);
        errors.add(X509_V_ERR_INVALID_PURPOSE);
        errors.add(X509_V_ERR_CERT_UNTRUSTED);
        errors.add(X509_V_ERR_CERT_REJECTED);
        errors.add(X509_V_ERR_SUBJECT_ISSUER_MISMATCH);
        errors.add(X509_V_ERR_AKID_SKID_MISMATCH);
        errors.add(X509_V_ERR_AKID_ISSUER_SERIAL_MISMATCH);
        errors.add(X509_V_ERR_KEYUSAGE_NO_CERTSIGN);
        errors.add(X509_V_ERR_UNABLE_TO_GET_CRL_ISSUER);
        errors.add(X509_V_ERR_UNHANDLED_CRITICAL_EXTENSION);
        errors.add(X509_V_ERR_KEYUSAGE_NO_CRL_SIGN);
        errors.add(X509_V_ERR_UNHANDLED_CRITICAL_CRL_EXTENSION);
        errors.add(X509_V_ERR_INVALID_NON_CA);
        errors.add(X509_V_ERR_PROXY_PATH_LENGTH_EXCEEDED);
        errors.add(X509_V_ERR_KEYUSAGE_NO_DIGITAL_SIGNATURE);
        errors.add(X509_V_ERR_PROXY_CERTIFICATES_NOT_ALLOWED);
        errors.add(X509_V_ERR_INVALID_EXTENSION);
        errors.add(X509_V_ERR_INVALID_POLICY_EXTENSION);
        errors.add(X509_V_ERR_NO_EXPLICIT_POLICY);
        errors.add(X509_V_ERR_DIFFERENT_CRL_SCOPE);
        errors.add(X509_V_ERR_UNSUPPORTED_EXTENSION_FEATURE);
        errors.add(X509_V_ERR_UNNESTED_RESOURCE);
        errors.add(X509_V_ERR_PERMITTED_VIOLATION);
        errors.add(X509_V_ERR_EXCLUDED_VIOLATION);
        errors.add(X509_V_ERR_SUBTREE_MINMAX);
        errors.add(X509_V_ERR_APPLICATION_VERIFICATION);
        errors.add(X509_V_ERR_UNSUPPORTED_CONSTRAINT_TYPE);
        errors.add(X509_V_ERR_UNSUPPORTED_CONSTRAINT_SYNTAX);
        errors.add(X509_V_ERR_UNSUPPORTED_NAME_SYNTAX);
        errors.add(X509_V_ERR_CRL_PATH_VALIDATION_ERROR);
        errors.add(X509_V_ERR_PATH_LOOP);
        errors.add(X509_V_ERR_SUITE_B_INVALID_VERSION);
        errors.add(X509_V_ERR_SUITE_B_INVALID_ALGORITHM);
        errors.add(X509_V_ERR_SUITE_B_INVALID_CURVE);
        errors.add(X509_V_ERR_SUITE_B_INVALID_SIGNATURE_ALGORITHM);
        errors.add(X509_V_ERR_SUITE_B_LOS_NOT_ALLOWED);
        errors.add(X509_V_ERR_SUITE_B_CANNOT_SIGN_P_384_WITH_P_256);
        errors.add(X509_V_ERR_HOSTNAME_MISMATCH);
        errors.add(X509_V_ERR_EMAIL_MISMATCH);
        errors.add(X509_V_ERR_IP_ADDRESS_MISMATCH);
        errors.add(X509_V_ERR_DANE_NO_MATCH);
        ERRORS = Collections.unmodifiableSet(errors);
    }

    /**
     * Returns {@code} true if the given {@code errorCode} is valid, {@code false} otherwise.
     */
    public static boolean isValid(int errorCode) {
        return ERRORS.contains(errorCode);
    }

    /**
     * Returns {@code true} if the passed in certificate chain could be verified and so the handshake
     * should be successful, {@code false} otherwise.
     *
     * @param ssl               the SSL instance
     * @param x509              the {@code X509} certificate chain
     * @param authAlgorithm     the auth algorithm
     * @return verified         {@code true} if verified successful, {@code false} otherwise
     */
    public abstract int verify(long ssl, byte[][] x509, String authAlgorithm);
}

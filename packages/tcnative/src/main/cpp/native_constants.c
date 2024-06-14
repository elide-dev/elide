/*
 * Copyright 2017 The Netty Project
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
#include "native_constants.h"

#define NATIVE_CONSTANTS_CLASSNAME "io/netty/internal/tcnative/NativeStaticallyReferencedJniMethods"

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslOpCipherServerPreference)(TCN_STDARGS) {
    return SSL_OP_CIPHER_SERVER_PREFERENCE;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslOpNoSSLv2)(TCN_STDARGS) {
    return SSL_OP_NO_SSLv2;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslOpNoSSLv3)(TCN_STDARGS) {
    return SSL_OP_NO_SSLv3;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslOpNoTLSv1)(TCN_STDARGS) {
    return SSL_OP_NO_TLSv1;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslOpNoTLSv11)(TCN_STDARGS) {
    return SSL_OP_NO_TLSv1_1;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslOpNoTLSv12)(TCN_STDARGS) {
    return SSL_OP_NO_TLSv1_2;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslOpNoTLSv13)(TCN_STDARGS) {
    return SSL_OP_NO_TLSv1_3;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslOpNoTicket)(TCN_STDARGS) {
    return SSL_OP_NO_TICKET;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslOpNoCompression)(TCN_STDARGS) {
    return SSL_OP_NO_COMPRESSION;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslOpAllowUnsafeLegacyRenegotiation)(TCN_STDARGS) {
    return SSL_OP_ALLOW_UNSAFE_LEGACY_RENEGOTIATION;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslOpLegacyServerConnect)(TCN_STDARGS) {
    return SSL_OP_LEGACY_SERVER_CONNECT;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSessCacheOff)(TCN_STDARGS) {
    return SSL_SESS_CACHE_OFF;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSessCacheServer)(TCN_STDARGS) {
    return SSL_SESS_CACHE_SERVER;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSessCacheClient)(TCN_STDARGS) {
    return SSL_SESS_CACHE_CLIENT;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSessCacheNoInternalLookup)(TCN_STDARGS) {
    return SSL_SESS_CACHE_NO_INTERNAL_LOOKUP;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSessCacheNoInternalStore)(TCN_STDARGS) {
    return SSL_SESS_CACHE_NO_INTERNAL_STORE;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslStConnect)(TCN_STDARGS) {
    return SSL_ST_CONNECT;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslStAccept)(TCN_STDARGS) {
    return SSL_ST_ACCEPT;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslModeEnablePartialWrite)(TCN_STDARGS) {
    return SSL_MODE_ENABLE_PARTIAL_WRITE;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslModeAcceptMovingWriteBuffer)(TCN_STDARGS) {
    return SSL_MODE_ACCEPT_MOVING_WRITE_BUFFER;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslModeReleaseBuffers)(TCN_STDARGS) {
    return SSL_MODE_RELEASE_BUFFERS;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslModeEnableFalseStart)(TCN_STDARGS) {
    return SSL_MODE_ENABLE_FALSE_START;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSendShutdown)(TCN_STDARGS) {
    return SSL_SENT_SHUTDOWN;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslReceivedShutdown)(TCN_STDARGS) {
    return SSL_RECEIVED_SHUTDOWN;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslErrorNone)(TCN_STDARGS) {
    return SSL_ERROR_NONE;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslErrorSSL)(TCN_STDARGS) {
    return SSL_ERROR_SSL;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslErrorWantRead)(TCN_STDARGS) {
    return SSL_ERROR_WANT_READ;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslErrorWantWrite)(TCN_STDARGS) {
    return SSL_ERROR_WANT_WRITE;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslErrorWantX509Lookup)(TCN_STDARGS) {
    return SSL_ERROR_WANT_X509_LOOKUP;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslErrorSyscall)(TCN_STDARGS) {
    return SSL_ERROR_SYSCALL;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslErrorZeroReturn)(TCN_STDARGS) {
    return SSL_ERROR_ZERO_RETURN;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslErrorWantConnect)(TCN_STDARGS) {
    return SSL_ERROR_WANT_CONNECT;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslErrorWantAccept)(TCN_STDARGS) {
    return SSL_ERROR_WANT_ACCEPT;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslMaxPlaintextLength)(TCN_STDARGS) {
    return SSL3_RT_MAX_PLAIN_LENGTH;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslMaxEncryptedLength)(TCN_STDARGS) {
    return SSL3_RT_MAX_ENCRYPTED_LENGTH;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslMaxRecordLength)(TCN_STDARGS) {
    // SSL3_RT_MAX_ENCRYPTED_OVERHEAD = Padding + Message Digest Hash
    // IV + Padding + Message Digest + Length allowed by RFC + Extra data amount
    return 256 + SSL3_RT_MAX_ENCRYPTED_OVERHEAD + SSL3_RT_MAX_PLAIN_LENGTH + SSL3_RT_MAX_PLAIN_LENGTH;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509CheckFlagAlwaysCheckSubject)(TCN_STDARGS) {
#ifdef X509_CHECK_FLAG_ALWAYS_CHECK_SUBJECT
    return X509_CHECK_FLAG_ALWAYS_CHECK_SUBJECT;
#else
    return 0;
#endif
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509CheckFlagDisableWildCards)(TCN_STDARGS) {
#ifdef X509_CHECK_FLAG_NO_WILD_CARDS
    return X509_CHECK_FLAG_NO_WILD_CARDS;
#else
    return 0;
#endif
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509CheckFlagNoPartialWildCards)(TCN_STDARGS) {
#ifdef X509_CHECK_FLAG_NO_PARTIAL_WILD_CARDS
    return X509_CHECK_FLAG_NO_PARTIAL_WILD_CARDS;
#else
    return 0;
#endif
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509CheckFlagMultiLabelWildCards)(TCN_STDARGS) {
#ifdef X509_CHECK_FLAG_MULTI_LABEL_WILDCARDS
    return X509_CHECK_FLAG_MULTI_LABEL_WILDCARDS;
#else
    return 0;
#endif
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vOK)(TCN_STDARGS) {
    return X509_V_OK;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrUnspecified)(TCN_STDARGS) {
#ifdef X509_V_ERR_UNSPECIFIED
    return X509_V_ERR_UNSPECIFIED;
#else
    return TCN_X509_V_ERR_UNSPECIFIED;
#endif
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrUnableToGetIssuerCert)(TCN_STDARGS) {
    return X509_V_ERR_UNABLE_TO_GET_ISSUER_CERT;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrUnableToGetCrl)(TCN_STDARGS) {
    return X509_V_ERR_UNABLE_TO_GET_CRL;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrUnableToDecryptCertSignature)(TCN_STDARGS) {
    return X509_V_ERR_UNABLE_TO_DECRYPT_CERT_SIGNATURE;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrUnableToDecryptCrlSignature)(TCN_STDARGS) {
    return X509_V_ERR_UNABLE_TO_DECRYPT_CRL_SIGNATURE;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrUnableToDecodeIssuerPublicKey)(TCN_STDARGS) {
    return X509_V_ERR_UNABLE_TO_DECODE_ISSUER_PUBLIC_KEY;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrCertSignatureFailure)(TCN_STDARGS) {
    return X509_V_ERR_CERT_SIGNATURE_FAILURE;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrCrlSignatureFailure)(TCN_STDARGS) {
    return X509_V_ERR_CRL_SIGNATURE_FAILURE;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrCertNotYetValid)(TCN_STDARGS) {
    return X509_V_ERR_CERT_NOT_YET_VALID;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrCertHasExpired)(TCN_STDARGS) {
    return X509_V_ERR_CERT_HAS_EXPIRED;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrCrlNotYetValid)(TCN_STDARGS) {
    return X509_V_ERR_CRL_NOT_YET_VALID;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrCrlHasExpired)(TCN_STDARGS) {
    return X509_V_ERR_CRL_HAS_EXPIRED;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrErrorInCertNotBeforeField)(TCN_STDARGS) {
    return X509_V_ERR_ERROR_IN_CERT_NOT_BEFORE_FIELD;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrErrorInCertNotAfterField)(TCN_STDARGS) {
    return X509_V_ERR_ERROR_IN_CERT_NOT_AFTER_FIELD;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrErrorInCrlLastUpdateField)(TCN_STDARGS) {
    return X509_V_ERR_ERROR_IN_CRL_LAST_UPDATE_FIELD;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrErrorInCrlNextUpdateField)(TCN_STDARGS) {
    return X509_V_ERR_ERROR_IN_CRL_NEXT_UPDATE_FIELD;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrOutOfMem)(TCN_STDARGS) {
    return X509_V_ERR_OUT_OF_MEM;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrDepthZeroSelfSignedCert)(TCN_STDARGS) {
    return X509_V_ERR_DEPTH_ZERO_SELF_SIGNED_CERT;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrSelfSignedCertInChain)(TCN_STDARGS) {
    return X509_V_ERR_SELF_SIGNED_CERT_IN_CHAIN;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrUnableToGetIssuerCertLocally)(TCN_STDARGS) {
    return X509_V_ERR_UNABLE_TO_GET_ISSUER_CERT_LOCALLY;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrUnableToVerifyLeafSignature)(TCN_STDARGS) {
    return X509_V_ERR_UNABLE_TO_VERIFY_LEAF_SIGNATURE;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrCertChainTooLong)(TCN_STDARGS) {
    return X509_V_ERR_CERT_CHAIN_TOO_LONG;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrCertRevoked)(TCN_STDARGS) {
    return X509_V_ERR_CERT_REVOKED;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrInvalidCa)(TCN_STDARGS) {
    return X509_V_ERR_INVALID_CA;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrPathLengthExceeded)(TCN_STDARGS) {
    return X509_V_ERR_PATH_LENGTH_EXCEEDED;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrInvalidPurpose)(TCN_STDARGS) {
    return X509_V_ERR_INVALID_PURPOSE;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrCertUntrusted)(TCN_STDARGS) {
    return X509_V_ERR_CERT_UNTRUSTED;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrCertRejected)(TCN_STDARGS) {
    return X509_V_ERR_CERT_REJECTED;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrSubjectIssuerMismatch)(TCN_STDARGS) {
    return X509_V_ERR_SUBJECT_ISSUER_MISMATCH;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrAkidSkidMismatch)(TCN_STDARGS) {
    return X509_V_ERR_AKID_SKID_MISMATCH;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrAkidIssuerSerialMismatch)(TCN_STDARGS) {
    return X509_V_ERR_AKID_ISSUER_SERIAL_MISMATCH;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrKeyUsageNoCertSign)(TCN_STDARGS) {
    return X509_V_ERR_KEYUSAGE_NO_CERTSIGN;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrUnableToGetCrlIssuer)(TCN_STDARGS) {
    return X509_V_ERR_UNABLE_TO_GET_CRL_ISSUER;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrUnhandledCriticalExtension)(TCN_STDARGS) {
    return X509_V_ERR_UNHANDLED_CRITICAL_EXTENSION;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrKeyUsageNoCrlSign)(TCN_STDARGS) {
    return X509_V_ERR_KEYUSAGE_NO_CRL_SIGN;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrUnhandledCriticalCrlExtension)(TCN_STDARGS) {
    return X509_V_ERR_UNHANDLED_CRITICAL_CRL_EXTENSION;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrInvalidNonCa)(TCN_STDARGS) {
    return X509_V_ERR_INVALID_NON_CA;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrProxyPathLengthExceeded)(TCN_STDARGS) {
    return X509_V_ERR_PROXY_PATH_LENGTH_EXCEEDED;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrKeyUsageNoDigitalSignature)(TCN_STDARGS) {
    return X509_V_ERR_KEYUSAGE_NO_DIGITAL_SIGNATURE;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrProxyCertificatesNotAllowed)(TCN_STDARGS) {
    return X509_V_ERR_PROXY_CERTIFICATES_NOT_ALLOWED;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrInvalidExtension)(TCN_STDARGS) {
    return X509_V_ERR_INVALID_EXTENSION;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrInvalidPolicyExtension)(TCN_STDARGS) {
    return X509_V_ERR_INVALID_POLICY_EXTENSION;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrNoExplicitPolicy)(TCN_STDARGS) {
    return X509_V_ERR_NO_EXPLICIT_POLICY;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrDifferntCrlScope)(TCN_STDARGS) {
    return X509_V_ERR_DIFFERENT_CRL_SCOPE;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrUnsupportedExtensionFeature)(TCN_STDARGS) {
    return X509_V_ERR_UNSUPPORTED_EXTENSION_FEATURE;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrUnnestedResource)(TCN_STDARGS) {
    return X509_V_ERR_UNNESTED_RESOURCE;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrPermittedViolation)(TCN_STDARGS) {
    return X509_V_ERR_PERMITTED_VIOLATION;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrExcludedViolation)(TCN_STDARGS) {
    return X509_V_ERR_EXCLUDED_VIOLATION;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrSubtreeMinMax)(TCN_STDARGS) {
    return X509_V_ERR_SUBTREE_MINMAX;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrApplicationVerification)(TCN_STDARGS) {
    return X509_V_ERR_APPLICATION_VERIFICATION;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrUnsupportedConstraintType)(TCN_STDARGS) {
    return X509_V_ERR_UNSUPPORTED_CONSTRAINT_TYPE;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrUnsupportedConstraintSyntax)(TCN_STDARGS) {
    return X509_V_ERR_UNSUPPORTED_CONSTRAINT_SYNTAX;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrUnsupportedNameSyntax)(TCN_STDARGS) {
    return X509_V_ERR_UNSUPPORTED_NAME_SYNTAX;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrCrlPathValidationError)(TCN_STDARGS) {
    return X509_V_ERR_CRL_PATH_VALIDATION_ERROR;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrPathLoop)(TCN_STDARGS) {
#ifdef X509_V_ERR_PATH_LOOP
    return X509_V_ERR_PATH_LOOP;
#else
    return TCN_X509_V_ERR_UNSPECIFIED;
#endif
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrSuiteBInvalidVersion)(TCN_STDARGS) {
#ifdef X509_V_ERR_SUITE_B_INVALID_VERSION
    return X509_V_ERR_SUITE_B_INVALID_VERSION;
#else
    return TCN_X509_V_ERR_UNSPECIFIED;
#endif
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrSuiteBInvalidAlgorithm)(TCN_STDARGS) {
#ifdef X509_V_ERR_SUITE_B_INVALID_ALGORITHM
    return X509_V_ERR_SUITE_B_INVALID_ALGORITHM;
#else
    return TCN_X509_V_ERR_UNSPECIFIED;
#endif
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrSuiteBInvalidCurve)(TCN_STDARGS) {
#ifdef X509_V_ERR_SUITE_B_INVALID_CURVE
    return X509_V_ERR_SUITE_B_INVALID_CURVE;
#else
    return TCN_X509_V_ERR_UNSPECIFIED;
#endif
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrSuiteBInvalidSignatureAlgorithm)(TCN_STDARGS) {
#ifdef X509_V_ERR_SUITE_B_INVALID_SIGNATURE_ALGORITHM
    return X509_V_ERR_SUITE_B_INVALID_SIGNATURE_ALGORITHM;
#else
    return TCN_X509_V_ERR_UNSPECIFIED;
#endif
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrSuiteBLosNotAllowed)(TCN_STDARGS) {
#ifdef X509_V_ERR_SUITE_B_LOS_NOT_ALLOWED
    return X509_V_ERR_SUITE_B_LOS_NOT_ALLOWED;
#else
    return TCN_X509_V_ERR_UNSPECIFIED;
#endif
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrSuiteBCannotSignP384WithP256)(TCN_STDARGS) {
#ifdef X509_V_ERR_SUITE_B_CANNOT_SIGN_P_384_WITH_P_256
    return X509_V_ERR_SUITE_B_CANNOT_SIGN_P_384_WITH_P_256;
#else
    return TCN_X509_V_ERR_UNSPECIFIED;
#endif
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrHostnameMismatch)(TCN_STDARGS) {
#ifdef X509_V_ERR_HOSTNAME_MISMATCH
    return X509_V_ERR_HOSTNAME_MISMATCH;
#else
    return TCN_X509_V_ERR_UNSPECIFIED;
#endif
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrEmailMismatch)(TCN_STDARGS) {
#ifdef X509_V_ERR_EMAIL_MISMATCH
    return X509_V_ERR_EMAIL_MISMATCH;
#else
    return TCN_X509_V_ERR_UNSPECIFIED;
#endif
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrIpAddressMismatch)(TCN_STDARGS) {
#ifdef X509_V_ERR_IP_ADDRESS_MISMATCH
    return X509_V_ERR_IP_ADDRESS_MISMATCH;
#else
    return TCN_X509_V_ERR_UNSPECIFIED;
#endif
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrDaneNoMatch)(TCN_STDARGS) {
#ifdef X509_V_ERR_DANE_NO_MATCH
    return X509_V_ERR_DANE_NO_MATCH;
#else
    return TCN_X509_V_ERR_UNSPECIFIED;
#endif
}

// BoringSSL specific
TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslErrorWantCertificateVerify)(TCN_STDARGS) {
    return SSL_ERROR_WANT_CERTIFICATE_VERIFY;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslErrorWantPrivateKeyOperation)(TCN_STDARGS) {
    return SSL_ERROR_WANT_PRIVATE_KEY_OPERATION;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSignRsaPkcsSha1)(TCN_STDARGS) {
    return SSL_SIGN_RSA_PKCS1_SHA1;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSignRsaPkcsSha256)(TCN_STDARGS) {
    return SSL_SIGN_RSA_PKCS1_SHA256;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSignRsaPkcsSha384)(TCN_STDARGS) {
    return SSL_SIGN_RSA_PKCS1_SHA384;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSignRsaPkcsSha512)(TCN_STDARGS) {
    return SSL_SIGN_RSA_PKCS1_SHA512;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSignEcdsaPkcsSha1)(TCN_STDARGS) {
    return SSL_SIGN_ECDSA_SHA1;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSignEcdsaSecp256r1Sha256)(TCN_STDARGS) {
    return SSL_SIGN_ECDSA_SECP256R1_SHA256;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSignEcdsaSecp384r1Sha384)(TCN_STDARGS) {
    return SSL_SIGN_ECDSA_SECP384R1_SHA384;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSignEcdsaSecp521r1Sha512)(TCN_STDARGS) {
    return SSL_SIGN_ECDSA_SECP521R1_SHA512;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSignRsaPssRsaeSha256)(TCN_STDARGS) {
    return SSL_SIGN_RSA_PSS_RSAE_SHA256;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSignRsaPssRsaeSha384)(TCN_STDARGS) {
    return SSL_SIGN_RSA_PSS_RSAE_SHA384;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSignRsaPssRsaeSha512)(TCN_STDARGS) {
    return SSL_SIGN_RSA_PSS_RSAE_SHA512;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSignEd25519)(TCN_STDARGS) {
    return SSL_SIGN_ED25519;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSignRsaPkcs1Md5Sha1)(TCN_STDARGS) {
    return SSL_SIGN_RSA_PKCS1_MD5_SHA1;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslRenegotiateNever)(TCN_STDARGS) {
#ifdef OPENSSL_IS_BORINGSSL
    return (jint) ssl_renegotiate_never;
#else
    return 0;
#endif
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslRenegotiateOnce)(TCN_STDARGS) {
#ifdef OPENSSL_IS_BORINGSSL
    return (jint) ssl_renegotiate_once;
#else
    return 0;
#endif
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslRenegotiateFreely)(TCN_STDARGS) {
#ifdef OPENSSL_IS_BORINGSSL
    return (jint) ssl_renegotiate_freely;
#else
    return 0;
#endif
}


TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslRenegotiateIgnore)(TCN_STDARGS) {
#ifdef OPENSSL_IS_BORINGSSL
    return (jint) ssl_renegotiate_ignore;
#else
    return 0;
#endif
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslRenegotiateExplicit)(TCN_STDARGS) {
#ifdef OPENSSL_IS_BORINGSSL
    return (jint) ssl_renegotiate_explicit;
#else
    return 0;
#endif
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslCertCompressionDirectionCompress)(TCN_STDARGS) {
    return SSL_CERT_COMPRESSION_DIRECTION_COMPRESS;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslCertCompressionDirectionDecompress)(TCN_STDARGS) {
    return SSL_CERT_COMPRESSION_DIRECTION_DECOMPRESS;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslCertCompressionDirectionBoth)(TCN_STDARGS) {
    return SSL_CERT_COMPRESSION_DIRECTION_BOTH;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, tlsExtCertCompressionZlib)(TCN_STDARGS) {
    return TLSEXT_cert_compression_zlib;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, tlsExtCertCompressionBrotli)(TCN_STDARGS) {
    return TLSEXT_cert_compression_brotli;
}

TCN_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, tlsExtCertCompressionZstd)(TCN_STDARGS) {
    return TLSEXT_cert_compression_zstd;
}

// JNI Method Registration Table Begin
static const JNINativeMethod method_table[] = {
  { TCN_METHOD_TABLE_ENTRY(sslOpCipherServerPreference, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslOpNoSSLv2, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslOpNoSSLv3, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslOpNoTLSv1, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslOpNoTLSv11, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslOpNoTLSv12, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslOpNoTLSv13, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslOpNoTicket, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslOpNoCompression, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslOpAllowUnsafeLegacyRenegotiation, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslOpLegacyServerConnect, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslSessCacheOff, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslSessCacheServer, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslSessCacheClient, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslSessCacheNoInternalLookup, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslSessCacheNoInternalStore, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslStConnect, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslStAccept, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslModeEnablePartialWrite, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslModeAcceptMovingWriteBuffer, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslModeEnableFalseStart, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslModeReleaseBuffers, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslSendShutdown, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslReceivedShutdown, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslErrorNone, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslErrorSSL, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslErrorWantRead, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslErrorWantWrite, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslErrorWantX509Lookup, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslErrorSyscall, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslErrorZeroReturn, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslErrorWantConnect, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslErrorWantAccept, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslMaxPlaintextLength, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslMaxEncryptedLength, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslMaxRecordLength, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509CheckFlagAlwaysCheckSubject, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509CheckFlagDisableWildCards, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509CheckFlagNoPartialWildCards, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509CheckFlagMultiLabelWildCards, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vOK, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrUnspecified, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrUnableToGetIssuerCert, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrUnableToGetCrl, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrUnableToDecryptCertSignature, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrUnableToDecryptCrlSignature, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrUnableToDecodeIssuerPublicKey, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrCertSignatureFailure, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrCrlSignatureFailure, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrCertNotYetValid, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrCertHasExpired, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrCrlNotYetValid, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrCrlHasExpired, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrErrorInCertNotBeforeField, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrErrorInCertNotAfterField, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrErrorInCrlLastUpdateField, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrErrorInCrlNextUpdateField, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrOutOfMem, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrDepthZeroSelfSignedCert, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrSelfSignedCertInChain, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrUnableToGetIssuerCertLocally, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrUnableToVerifyLeafSignature, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrCertChainTooLong, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrCertRevoked, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrInvalidCa, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrPathLengthExceeded, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrInvalidPurpose, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrCertUntrusted, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrCertRejected, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrSubjectIssuerMismatch, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrAkidSkidMismatch, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrAkidIssuerSerialMismatch, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrKeyUsageNoCertSign, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrUnableToGetCrlIssuer, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrUnhandledCriticalExtension, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrKeyUsageNoCrlSign, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrUnhandledCriticalCrlExtension, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrInvalidNonCa, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrProxyPathLengthExceeded, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrKeyUsageNoDigitalSignature, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrProxyCertificatesNotAllowed, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrInvalidExtension, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrInvalidPolicyExtension, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrNoExplicitPolicy, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrDifferntCrlScope, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrUnsupportedExtensionFeature, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrUnnestedResource, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrPermittedViolation, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrExcludedViolation, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrSubtreeMinMax, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrApplicationVerification, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrUnsupportedConstraintType, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrUnsupportedConstraintSyntax, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrUnsupportedNameSyntax, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrCrlPathValidationError, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrPathLoop, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrSuiteBInvalidVersion, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrSuiteBInvalidAlgorithm, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrSuiteBInvalidCurve, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrSuiteBInvalidSignatureAlgorithm, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrSuiteBLosNotAllowed, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrSuiteBCannotSignP384WithP256, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrHostnameMismatch, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrEmailMismatch, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrIpAddressMismatch, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(x509vErrDaneNoMatch, ()I, NativeStaticallyReferencedJniMethods) },
  // BoringSSL specific
  { TCN_METHOD_TABLE_ENTRY(sslErrorWantCertificateVerify, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslErrorWantPrivateKeyOperation, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslSignRsaPkcsSha1, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslSignRsaPkcsSha256, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslSignRsaPkcsSha384, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslSignRsaPkcsSha512, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslSignEcdsaPkcsSha1, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslSignEcdsaSecp256r1Sha256, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslSignEcdsaSecp384r1Sha384, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslSignEcdsaSecp521r1Sha512, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslSignRsaPssRsaeSha256, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslSignRsaPssRsaeSha384, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslSignRsaPssRsaeSha512, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslSignEd25519, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslSignRsaPkcs1Md5Sha1, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslRenegotiateNever, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslRenegotiateOnce, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslRenegotiateFreely, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslRenegotiateIgnore, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslRenegotiateExplicit, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslCertCompressionDirectionCompress, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslCertCompressionDirectionDecompress, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(sslCertCompressionDirectionBoth, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(tlsExtCertCompressionZlib, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(tlsExtCertCompressionBrotli, ()I, NativeStaticallyReferencedJniMethods) },
  { TCN_METHOD_TABLE_ENTRY(tlsExtCertCompressionZstd, ()I, NativeStaticallyReferencedJniMethods) }
};

static const jint method_table_size = sizeof(method_table) / sizeof(method_table[0]);
// JNI Method Registration Table End

// IMPORTANT: If you add any NETTY_JNI_UTIL_LOAD_CLASS or NETTY_JNI_UTIL_FIND_CLASS calls you also need to update
//            Library to reflect that.
jint netty_internal_tcnative_NativeStaticallyReferencedJniMethods_JNI_OnLoad(JNIEnv* env, const char* packagePrefix) {
    if (netty_jni_util_register_natives(env,
             packagePrefix,
             NATIVE_CONSTANTS_CLASSNAME,
             method_table, method_table_size) != 0) {
        return JNI_ERR;
    }
    return NETTY_JNI_UTIL_JNI_VERSION;
}

void netty_internal_tcnative_NativeStaticallyReferencedJniMethods_JNI_OnUnLoad(JNIEnv* env, const char* packagePrefix) {
    netty_jni_util_unregister_natives(env, packagePrefix, NATIVE_CONSTANTS_CLASSNAME);
 }

//
// -----------------------------------
//

#ifndef __GVM_ENTRY_H
#define __GVM_ENTRY_H
#ifdef ELIDE_GVM_STATIC

#define GVM_FUNCTION_NAME(CL, FN)  \
    Java_io_netty_internal_tcnative_##CL##_##FN

#define GVM_IMPLEMENT_CALL(RT, CL, FN)  \
    JNIEXPORT RT JNICALL GVM_FUNCTION_NAME(CL, FN)(TCN_STDARGS) {\
      return TCN_FUNCTION_NAME(CL, FN)(e, o);\
    }

GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslOpCipherServerPreference);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslOpNoSSLv2);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslOpNoSSLv3);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslOpNoTLSv1);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslOpNoTLSv11);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslOpNoTLSv12);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslOpNoTLSv13);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslOpNoTicket);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslOpAllowUnsafeLegacyRenegotiation);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslOpLegacyServerConnect);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslOpNoCompression);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSessCacheOff);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSessCacheServer);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSessCacheClient);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSessCacheNoInternalLookup);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSessCacheNoInternalStore);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslStConnect);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslStAccept);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslModeEnablePartialWrite);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslModeAcceptMovingWriteBuffer);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslModeReleaseBuffers);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslModeEnableFalseStart);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSendShutdown);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslReceivedShutdown);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslErrorNone);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslErrorSSL);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslErrorWantRead);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslErrorWantWrite);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslErrorWantX509Lookup);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslErrorSyscall);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslErrorZeroReturn);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslErrorWantConnect);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslErrorWantAccept);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslMaxPlaintextLength);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslMaxEncryptedLength);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslMaxRecordLength);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509CheckFlagAlwaysCheckSubject);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509CheckFlagDisableWildCards);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509CheckFlagNoPartialWildCards);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509CheckFlagMultiLabelWildCards);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vOK);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrUnspecified);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrUnableToGetIssuerCert);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrUnableToGetCrl);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrUnableToDecryptCertSignature);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrUnableToDecryptCrlSignature);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrUnableToDecodeIssuerPublicKey);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrCertSignatureFailure);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrCrlSignatureFailure);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrCertNotYetValid);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrCertHasExpired);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrCrlNotYetValid);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrCrlHasExpired);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrErrorInCertNotBeforeField);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrErrorInCertNotAfterField);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrErrorInCrlLastUpdateField);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrErrorInCrlNextUpdateField);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrOutOfMem);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrDepthZeroSelfSignedCert);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrSelfSignedCertInChain);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrUnableToGetIssuerCertLocally);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrUnableToVerifyLeafSignature);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrCertChainTooLong);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrCertRevoked);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrInvalidCa);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrPathLengthExceeded);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrInvalidPurpose);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrCertUntrusted);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrCertRejected);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrSubjectIssuerMismatch);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrAkidSkidMismatch);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrAkidIssuerSerialMismatch);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrKeyUsageNoCertSign);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrUnableToGetCrlIssuer);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrUnhandledCriticalExtension);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrKeyUsageNoCrlSign);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrUnhandledCriticalCrlExtension);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrInvalidNonCa);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrProxyPathLengthExceeded);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrKeyUsageNoDigitalSignature);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrProxyCertificatesNotAllowed);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrInvalidExtension);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrInvalidPolicyExtension);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrNoExplicitPolicy);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrDifferntCrlScope);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrUnsupportedExtensionFeature);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrUnnestedResource);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrPermittedViolation);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrExcludedViolation);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrSubtreeMinMax);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrApplicationVerification);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrUnsupportedConstraintType);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrUnsupportedConstraintSyntax);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrUnsupportedNameSyntax);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrCrlPathValidationError);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrPathLoop);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrSuiteBInvalidVersion);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrSuiteBInvalidAlgorithm);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrSuiteBInvalidCurve);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrSuiteBInvalidSignatureAlgorithm);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrSuiteBLosNotAllowed);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrSuiteBCannotSignP384WithP256);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrHostnameMismatch);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrEmailMismatch);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrIpAddressMismatch);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, x509vErrDaneNoMatch);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslErrorWantCertificateVerify);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslErrorWantPrivateKeyOperation);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSignRsaPkcsSha1);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSignRsaPkcsSha256);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSignRsaPkcsSha384);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSignRsaPkcsSha512);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSignEcdsaPkcsSha1);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSignEcdsaSecp256r1Sha256);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSignEcdsaSecp384r1Sha384);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSignEcdsaSecp521r1Sha512);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSignRsaPssRsaeSha256);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSignRsaPssRsaeSha384);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSignRsaPssRsaeSha512);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSignEd25519);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslSignRsaPkcs1Md5Sha1);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslRenegotiateNever);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslRenegotiateOnce);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslRenegotiateFreely);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslRenegotiateIgnore);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslRenegotiateExplicit);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslCertCompressionDirectionCompress);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslCertCompressionDirectionDecompress);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, sslCertCompressionDirectionBoth);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, tlsExtCertCompressionZlib);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, tlsExtCertCompressionBrotli);
GVM_IMPLEMENT_CALL(jint, NativeStaticallyReferencedJniMethods, tlsExtCertCompressionZstd);

#endif /* ELIDE_GVM_STATIC */
#endif /* __GVM_ENTRY_H */

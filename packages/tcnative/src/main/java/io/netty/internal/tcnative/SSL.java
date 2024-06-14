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
/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.netty.internal.tcnative;

import java.nio.ByteBuffer;

import javax.net.ssl.SSLEngine;

import static io.netty.internal.tcnative.NativeStaticallyReferencedJniMethods.*;

public final class SSL {

    private SSL() { }

    /*
     * Define the SSL Protocol options
     */
    public static final int SSL_PROTOCOL_NONE  = 0;
    public static final int SSL_PROTOCOL_SSLV2 = (1<<0);
    public static final int SSL_PROTOCOL_SSLV3 = (1<<1);
    public static final int SSL_PROTOCOL_TLSV1 = (1<<2);
    public static final int SSL_PROTOCOL_TLSV1_1 = (1<<3);
    public static final int SSL_PROTOCOL_TLSV1_2 = (1<<4);
    public static final int SSL_PROTOCOL_TLSV1_3 = (1<<5);

    /** TLS_*method according to <a href="https://www.openssl.org/docs/man1.0.2/ssl/SSL_CTX_new.html">SSL_CTX_new</a> */
    public static final int SSL_PROTOCOL_TLS   = (SSL_PROTOCOL_SSLV3 | SSL_PROTOCOL_TLSV1 | SSL_PROTOCOL_TLSV1_1 | SSL_PROTOCOL_TLSV1_2 | SSL_PROTOCOL_TLSV1_3);
    public static final int SSL_PROTOCOL_ALL   = (SSL_PROTOCOL_SSLV2 | SSL_PROTOCOL_TLS);

    /*
     * Define the SSL verify levels
     */
    public static final int SSL_CVERIFY_IGNORED            = -1;
    public static final int SSL_CVERIFY_NONE               = 0;
    public static final int SSL_CVERIFY_OPTIONAL           = 1;
    public static final int SSL_CVERIFY_REQUIRED           = 2;

    public static final int SSL_OP_CIPHER_SERVER_PREFERENCE = sslOpCipherServerPreference();
    public static final int SSL_OP_NO_SSLv2 = sslOpNoSSLv2();
    public static final int SSL_OP_NO_SSLv3 = sslOpNoSSLv3();
    public static final int SSL_OP_NO_TLSv1 = sslOpNoTLSv1();
    public static final int SSL_OP_NO_TLSv1_1 = sslOpNoTLSv11();
    public static final int SSL_OP_NO_TLSv1_2 = sslOpNoTLSv12();
    public static final int SSL_OP_NO_TLSv1_3 = sslOpNoTLSv13();
    public static final int SSL_OP_NO_TICKET = sslOpNoTicket();

    public static final int SSL_OP_NO_COMPRESSION = sslOpNoCompression();
    public static final int SSL_OP_ALLOW_UNSAFE_LEGACY_RENEGOTIATION = sslOpAllowUnsafeLegacyRenegotiation();
    public static final int SSL_OP_LEGACY_SERVER_CONNECT = sslOpLegacyServerConnect();

    public static final int SSL_MODE_CLIENT         = 0;
    public static final int SSL_MODE_SERVER         = 1;
    public static final int SSL_MODE_COMBINED       = 2;

    public static final long SSL_SESS_CACHE_OFF = sslSessCacheOff();
    public static final long SSL_SESS_CACHE_SERVER = sslSessCacheServer();
    public static final long SSL_SESS_CACHE_CLIENT = sslSessCacheClient();
    public static final long SSL_SESS_CACHE_NO_INTERNAL_LOOKUP = sslSessCacheNoInternalLookup();
    public static final long SSL_SESS_CACHE_NO_INTERNAL_STORE = sslSessCacheNoInternalStore();

    public static final int SSL_SELECTOR_FAILURE_NO_ADVERTISE = 0;
    public static final int SSL_SELECTOR_FAILURE_CHOOSE_MY_LAST_PROTOCOL = 1;

    public static final int SSL_ST_CONNECT = sslStConnect();
    public static final int SSL_ST_ACCEPT =  sslStAccept();

    public static final int SSL_MODE_ENABLE_PARTIAL_WRITE           = sslModeEnablePartialWrite();
    public static final int SSL_MODE_ACCEPT_MOVING_WRITE_BUFFER     = sslModeAcceptMovingWriteBuffer();
    public static final int SSL_MODE_RELEASE_BUFFERS                = sslModeReleaseBuffers();
    public static final int SSL_MODE_ENABLE_FALSE_START             = sslModeEnableFalseStart();
    public static final int SSL_MAX_PLAINTEXT_LENGTH = sslMaxPlaintextLength();
    public static final int SSL_MAX_ENCRYPTED_LENGTH = sslMaxEncryptedLength();

    /**
     * The <a href="https://tools.ietf.org/html/rfc5246#section-6.2.1">TLS 1.2 RFC</a> defines the maximum length to be
     * {@link #SSL_MAX_PLAINTEXT_LENGTH}, but there are some implementations such as
     * <a href="http://hg.openjdk.java.net/jdk8u/jdk8u/jdk/file/d5a00b1e8f78/src/share/classes/sun/security/ssl/SSLSessionImpl.java#l793">OpenJDK's SSLEngineImpl</a>
     * that also allow sending larger packets. This can be used as a upper bound for data to support legacy systems.
     */
    public static final int SSL_MAX_RECORD_LENGTH = sslMaxRecordLength();

    // https://www.openssl.org/docs/man1.0.2/crypto/X509_check_host.html
    public static final int X509_CHECK_FLAG_ALWAYS_CHECK_SUBJECT = x509CheckFlagAlwaysCheckSubject();
    public static final int X509_CHECK_FLAG_NO_WILD_CARDS = x509CheckFlagDisableWildCards();
    public static final int X509_CHECK_FLAG_NO_PARTIAL_WILD_CARDS = x509CheckFlagNoPartialWildCards();
    public static final int X509_CHECK_FLAG_MULTI_LABEL_WILDCARDS = x509CheckFlagMultiLabelWildCards();

    public static final int SSL_RENEGOTIATE_NEVER = sslRenegotiateNever();
    public static final int SSL_RENEGOTIATE_ONCE = sslRenegotiateOnce();
    public static final int SSL_RENEGOTIATE_FREELY = sslRenegotiateFreely();
    public static final int SSL_RENEGOTIATE_IGNORE = sslRenegotiateIgnore();
    public static final int SSL_RENEGOTIATE_EXPLICIT = sslRenegotiateExplicit();

    public static final int SSL_CERT_COMPRESSION_DIRECTION_COMPRESS = sslCertCompressionDirectionCompress();
    public static final int SSL_CERT_COMPRESSION_DIRECTION_DECOMPRESS = sslCertCompressionDirectionDecompress();
    public static final int SSL_CERT_COMPRESSION_DIRECTION_BOTH = sslCertCompressionDirectionBoth();

    /* Return OpenSSL version number */
    public static native int version();

    /* Return OpenSSL version string */
    public static native String versionString();

    /**
     * Initialize OpenSSL support.
     *
     * This function needs to be called once for the
     * lifetime of JVM. See {@link Library#initialize(String, String)}
     *
     * @param engine Support for external a Crypto Device ("engine"),
     *                usually a hardware accelerator card for crypto operations.
     * @return APR status code
     */
    static native int initialize(String engine);

    /**
     * Initialize new in-memory BIO that is located in the secure heap.
     *
     * @return New BIO handle
     * @throws Exception if an error happened.
     */
    public static native long newMemBIO() throws Exception;

    /**
     * Return last SSL error string
     *
     * @return the last SSL error string.
     */
    public static native String getLastError();

    /*
     * Begin Twitter API additions
     */

    public static final int SSL_SENT_SHUTDOWN = sslSendShutdown();
    public static final int SSL_RECEIVED_SHUTDOWN = sslReceivedShutdown();

    public static final int SSL_ERROR_NONE             = sslErrorNone();
    public static final int SSL_ERROR_SSL              = sslErrorSSL();
    public static final int SSL_ERROR_WANT_READ        = sslErrorWantRead();
    public static final int SSL_ERROR_WANT_WRITE       = sslErrorWantWrite();
    public static final int SSL_ERROR_WANT_X509_LOOKUP = sslErrorWantX509Lookup();
    public static final int SSL_ERROR_SYSCALL          = sslErrorSyscall(); /* look at error stack/return value/errno */
    public static final int SSL_ERROR_ZERO_RETURN      = sslErrorZeroReturn();
    public static final int SSL_ERROR_WANT_CONNECT     = sslErrorWantConnect();
    public static final int SSL_ERROR_WANT_ACCEPT      = sslErrorWantAccept();
    // https://boringssl.googlesource.com/boringssl/+/chromium-stable/include/openssl/ssl.h#519
    public static final int SSL_ERROR_WANT_PRIVATE_KEY_OPERATION = sslErrorWantPrivateKeyOperation();

    // BoringSSL specific
    public static final int SSL_ERROR_WANT_CERTIFICATE_VERIFY = sslErrorWantCertificateVerify();

    /**
     * SSL_new
     * @param ctx Server or Client context to use.
     * @param server if true configure SSL instance to use accept handshake routines
     *               if false configure SSL instance to use connect handshake routines
     * @return pointer to SSL instance (SSL *)
     */
    public static native long newSSL(long ctx, boolean server);

    /**
     * SSL_get_error
     * @param ssl SSL pointer (SSL *)
     * @param ret TLS/SSL I/O return value
     * @return the error code
     */
    public static native int getError(long ssl, int ret);

    /**
     * BIO_write
     * @param bioAddress The address of a {@code BIO*}.
     * @param wbufAddress The address of a native {@code char*}.
     * @param wlen The length to write starting at {@code wbufAddress}.
     * @return The number of bytes that were written.
     * See <a href="https://www.openssl.org/docs/man1.0.1/crypto/BIO_write.html">BIO_write</a> for exceptional return values.
     */
    public static native int bioWrite(long bioAddress, long wbufAddress, int wlen);

    /**
     * Initialize the BIO for the SSL instance. This is a custom BIO which is designed to play nicely with a direct
     * {@link ByteBuffer}. Because it is a special BIO it requires special usage such that
     * {@link #bioSetByteBuffer(long, long, int, boolean)} and {@link #bioClearByteBuffer(long)} are called in order to provide
     * to supply data to SSL, and also to ensure the internal SSL buffering mechanism is expecting write at the appropriate times.
     *
     * @param ssl the SSL instance (SSL *)
     * @param nonApplicationBufferSize The size of the internal buffer for write operations that are not
     *                                 initiated directly by the application attempting to encrypt data.
     *                                 Must be &gt;{@code 0}.
     * @return pointer to the Network BIO (BIO *).
     *         The memory is owned by {@code ssl} and will be cleaned up by {@link #freeSSL(long)}.
     */
    public static native long bioNewByteBuffer(long ssl, int nonApplicationBufferSize);

    /**
     * Sets the socket file descriptor
     *
     * @param ssl the SSL instance (SSL *)
     * @param fd the file descriptor of the socket used for the given SSL connection
     *
     * @deprecated This is not supported official by OpenSSL or BoringSSL so its just a no op.
     */
    @Deprecated
    public static native void bioSetFd(long ssl, int fd);

    /**
     * Set the memory location which that OpenSSL's internal BIO will use to write encrypted data to, or read encrypted
     * data from.
     * <p>
     * After you are done buffering data you should call {@link #bioClearByteBuffer(long)}.
     * @param bio {@code BIO*}.
     * @param bufferAddress The memory address (typically from a direct {@link ByteBuffer}) which will be used
     *                    to either write encrypted data to, or read encrypted data from by OpenSSL's internal BIO pair.
     * @param maxUsableBytes The maximum usable length in bytes starting at {@code bufferAddress}.
     * @param isSSLWriteSink {@code true} if this buffer is expected to buffer data as a result of calls to {@code SSL_write}.
     *                       {@code false} if this buffer is expected to buffer data as a result of calls to {@code SSL_read}.
     */
    public static native void bioSetByteBuffer(long bio, long bufferAddress, int maxUsableBytes, boolean isSSLWriteSink);

    /**
     * After you are done buffering data from {@link #bioSetByteBuffer(long, long, int, boolean)}, this will ensure the
     * internal SSL write buffers are ready to capture data which may unexpectedly happen (e.g. handshake, renegotiation, etc..).
     * @param bio {@code BIO*}.
     */
    public static native void bioClearByteBuffer(long bio);

    /**
     * Flush any pending bytes in the internal SSL write buffer.
     * <p>
     * This does the same thing as {@code BIO_flush} for a {@code BIO*} of type {@link #bioNewByteBuffer(long, int)} but
     * returns the number of bytes that were flushed.
     * @param bio {@code BIO*}.
     * @return The number of bytes that were flushed.
     */
    public static native int bioFlushByteBuffer(long bio);

    /**
     * Get the remaining length of the {@link ByteBuffer} set by {@link #bioSetByteBuffer(long, long, int, boolean)}.
     * @param bio {@code BIO*}.
     * @return The remaining length of the {@link ByteBuffer} set by {@link #bioSetByteBuffer(long, long, int, boolean)}.
     */
    public static native int bioLengthByteBuffer(long bio);

    /**
     * Get the amount of data pending in buffer used for non-application writes.
     * This value will not exceed the value configured in {@link #bioNewByteBuffer(long, int)}.
     * @param bio {@code BIO*}.
     * @return the amount of data pending in buffer used for non-application writes.
     */
    public static native int bioLengthNonApplication(long bio);

    /**
     * The number of bytes pending in SSL which can be read immediately.
     * See <a href="https://www.openssl.org/docs/man1.0.1/ssl/SSL_pending.html">SSL_pending</a>.
     * @param ssl the SSL instance (SSL *)
     * @return The number of bytes pending in SSL which can be read immediately.
     */
    public static native int sslPending(long ssl);

    /**
     * SSL_write
     * @param ssl the SSL instance (SSL *)
     * @param wbuf the memory address of the buffer
     * @param wlen the length
     * @return the number of written bytes
     */
    public static native int writeToSSL(long ssl, long wbuf, int wlen);

    /**
     * SSL_read
     * @param ssl the SSL instance (SSL *)
     * @param rbuf the memory address of the buffer
     * @param rlen the length
     * @return the number of read bytes
     */
    public static native int readFromSSL(long ssl, long rbuf, int rlen);

    /**
     * SSL_get_shutdown
     * @param ssl the SSL instance (SSL *)
     * @return the return code of {@code SSL_get_shutdown}
     */
    public static native int getShutdown(long ssl);

    /**
     * SSL_set_shutdown
     * @param ssl the SSL instance (SSL *)
     * @param mode the mode to use
     */
    public static native void setShutdown(long ssl, int mode);

    /**
     * SSL_free
     * @param ssl the SSL instance (SSL *)
     */
    public static native void freeSSL(long ssl);

    /**
     * BIO_free
     * @param bio the BIO
     */
    public static native void freeBIO(long bio);

    /**
     * SSL_shutdown
     * @param ssl the SSL instance (SSL *)
     * @return the return code of {@code SSL_shutdown}
     */
    public static native int shutdownSSL(long ssl);

    /**
     * Get the error number representing the last error OpenSSL encountered on this thread.
     * @return the last error code for the calling thread.
     */
    public static native int getLastErrorNumber();

    /**
     * SSL_get_cipher
     * @param ssl the SSL instance (SSL *)
     * @return the name of the current cipher.
     */
    public static native String getCipherForSSL(long ssl);

    /**
     * SSL_get_version
     * @param ssl the SSL instance (SSL *)
     * @return the version.
     */
    public static native String getVersion(long ssl);

    /**
     * SSL_do_handshake
     * @param ssl the SSL instance (SSL *)
     * @return the return code of {@code SSL_do_handshake}.
     */
    public static native int doHandshake(long ssl);

    /**
     * SSL_in_init
     * @param ssl the SSL instance (SSL *)
     * @return the return code of {@code SSL_in_init}.
     */
    public static native int isInInit(long ssl);

    /**
     * SSL_get0_next_proto_negotiated
     * @param ssl the SSL instance (SSL *)
     * @return the name of the negotiated proto
     */
    public static native String getNextProtoNegotiated(long ssl);

    /*
     * End Twitter API Additions
     */

    /**
     * SSL_get0_alpn_selected
     * @param ssl the SSL instance (SSL *)
     * @return the name of the selected ALPN protocol
     */
    public static native String getAlpnSelected(long ssl);

    /**
     * Get the peer certificate chain or {@code null} if none was send.
     * @param ssl the SSL instance (SSL *)
     * @return the chain or {@code null} if none was send
     */
    public static native byte[][] getPeerCertChain(long ssl);

    /**
     * Get the peer certificate or {@code null} if non was send.
     * @param ssl the SSL instance (SSL *)
     * @return the peer certificate or {@code null} if none was send
     */
    public static native byte[] getPeerCertificate(long ssl);

    /**
     * Get the error string representing for the given {@code errorNumber}.
     *
     * @param errorNumber the error number / code
     * @return the error string
     */
    public static native String getErrorString(long errorNumber);

    /**
     * SSL_get_time
     * @param ssl the SSL instance (SSL *)
     * @return returns the time at which the session ssl was established. The time is given in seconds since the Epoch
     */
    public static native long getTime(long ssl);

    /**
     * SSL_get_timeout
     * @param ssl the SSL instance (SSL *)
     * @return returns the timeout for the session ssl The time is given in seconds since the Epoch
     */
    public static native long getTimeout(long ssl);

    /**
     * SSL_set_timeout
     * @param ssl the SSL instance (SSL *)
     * @param seconds timeout in seconds
     * @return returns the timeout for the session ssl before this call. The time is given in seconds since the Epoch
     */
    public static native long setTimeout(long ssl, long seconds);

    /**
     * Set Type of Client Certificate verification and Maximum depth of CA Certificates
     * in Client Certificate verification.
     * <p>
     * This directive sets the Certificate verification level for the Client
     * Authentication. Notice that this directive can be used both in per-server
     * and per-directory context. In per-server context it applies to the client
     * authentication process used in the standard SSL handshake when a connection
     * is established. In per-directory context it forces a SSL renegotiation with
     * the reconfigured client verification level after the HTTP request was read
     * but before the HTTP response is sent.
     * <p>
     * The following levels are available for level:
     * <ul>
     * <li>{@link #SSL_CVERIFY_IGNORED} - The level is ignored. Only depth will change.</li>
     * <li>{@link #SSL_CVERIFY_NONE} - No client Certificate is required at all</li>
     * <li>{@link #SSL_CVERIFY_OPTIONAL} - The client may present a valid Certificate</li>
     * <li>{@link #SSL_CVERIFY_REQUIRED} - The client has to present a valid Certificate</li>
     * </ul>
     * The depth actually is the maximum number of intermediate certificate issuers,
     * i.e. the number of CA certificates which are max allowed to be followed while
     * verifying the client certificate. A depth of 0 means that self-signed client
     * certificates are accepted only, the default depth of 1 means the client
     * certificate can be self-signed or has to be signed by a CA which is directly
     * known to the server (i.e. the CA's certificate is under
     * {@code setCACertificatePath}, etc.
     *
     * @param ssl the SSL instance (SSL *)
     * @param level Type of Client Certificate verification.
     * @param depth Maximum depth of CA Certificates in Client Certificate
     *              verification. Ignored if value is {@code <0}.
     */
    public static native void setVerify(long ssl, int level, int depth);

    /**
     * Set OpenSSL Option.
     * @param ssl the SSL instance (SSL *)
     * @param options  See SSL.SSL_OP_* for option flags.
     */
    public static native void setOptions(long ssl, int options);

    /**
     * Clear OpenSSL Option.
     * @param ssl the SSL instance (SSL *)
     * @param options  See SSL.SSL_OP_* for option flags.
     */
    public static native void clearOptions(long ssl, int options);

    /**
     * Get OpenSSL Option.
     * @param ssl the SSL instance (SSL *)
     * @return options  See SSL.SSL_OP_* for option flags.
     */
    public static native int getOptions(long ssl);

    /**
     * Call SSL_set_mode
     *
     * @param ssl the SSL instance (SSL *).
     * @param mode the mode
     * @return the set mode.
     */
    public static native int setMode(long ssl, int mode);

    /**
     * Call SSL_get_mode
     *
     * @param ssl the SSL instance (SSL *).
     * @return the mode.
     */
    public static native int getMode(long ssl);

    /**
     * Get the maximum overhead, in bytes, of wrapping (a.k.a sealing) a record with ssl.
     * See <a href="https://commondatastorage.googleapis.com/chromium-boringssl-docs/ssl.h.html#SSL_max_seal_overhead">SSL_max_seal_overhead</a>.
     * @param ssl the SSL instance (SSL *).
     * @return Maximum overhead, in bytes, of wrapping (a.k.a sealing) a record with ssl.
     */
    public static native int getMaxWrapOverhead(long ssl);

    /**
     * Returns all Returns the cipher suites that are available for negotiation in an SSL handshake.
     * @param ssl the SSL instance (SSL *)
     * @return ciphers
     */
    public static native String[] getCiphers(long ssl);

    /**
     * Returns the cipher suites available for negotiation in SSL handshake.
     * <p>
     * This complex directive uses a colon-separated cipher-spec string consisting
     * of OpenSSL cipher specifications to configure the Cipher Suite the client
     * is permitted to negotiate in the SSL handshake phase. Notice that this
     * directive can be used both in per-server and per-directory context.
     * In per-server context it applies to the standard SSL handshake when a
     * connection is established. In per-directory context it forces a SSL
     * renegotiation with the reconfigured Cipher Suite after the HTTP request
     * was read but before the HTTP response is sent.
     * @param ssl the SSL instance (SSL *)
     * @param ciphers an SSL cipher specification
     * @return {@code true} if successful
     * @throws Exception if an error happened
     * @deprecated Use {@link #setCipherSuites(long, String, boolean)}
     */
    @Deprecated
    public static boolean setCipherSuites(long ssl, String ciphers)
            throws Exception {
        return setCipherSuites(ssl, ciphers, false);
    }

    /**
     * Returns the cipher suites available for negotiation in SSL handshake.
     * <p>
     * This complex directive uses a colon-separated cipher-spec string consisting
     * of OpenSSL cipher specifications to configure the Cipher Suite the client
     * is permitted to negotiate in the SSL handshake phase. Notice that this
     * directive can be used both in per-server and per-directory context.
     * In per-server context it applies to the standard SSL handshake when a
     * connection is established. In per-directory context it forces a SSL
     * renegotiation with the reconfigured Cipher Suite after the HTTP request
     * was read but before the HTTP response is sent.
     * @param ssl the SSL instance (SSL *)
     * @param ciphers an SSL cipher specification
     * @param tlsv13 {@code true} if the ciphers are for TLSv1.3
     * @return {@code true} if successful
     * @throws Exception if an error happened
     */
    public static native boolean setCipherSuites(long ssl, String ciphers, boolean tlsv13)
            throws Exception;
    /**
     * Returns the ID of the session as byte array representation.
     *
     * @param ssl the SSL instance (SSL *)
     * @return the session as byte array representation obtained via SSL_SESSION_get_id.
     */
    public static native byte[] getSessionId(long ssl);

    /**
     * Returns the number of handshakes done for this SSL instance. This also includes renegations.
     *
     * @param ssl the SSL instance (SSL *)
     * @return the number of handshakes done for this SSL instance.
     */
    public static native int getHandshakeCount(long ssl);

    /**
     * Clear all the errors from the error queue that OpenSSL encountered on this thread.
     */
    public static native void clearError();

    /**
     * Call SSL_set_tlsext_host_name
     *
     * @param ssl the SSL instance (SSL *)
     * @param hostname the hostname
     */
    public static void setTlsExtHostName(long ssl, String hostname) {
        if (hostname != null && hostname.endsWith(".")) {
            // Strip trailing dot if included.
            // See https://github.com/netty/netty-tcnative/issues/400
            hostname = hostname.substring(0, hostname.length() - 1);
        }
        setTlsExtHostName0(ssl, hostname);
    }

    private static native void setTlsExtHostName0(long ssl, String hostname);

    /**
     * Explicitly control <a href="https://wiki.openssl.org/index.php/Hostname_validation">hostname validation</a>
     * <a href="https://www.openssl.org/docs/man1.0.2/crypto/X509_check_host.html">see X509_check_host for X509_CHECK_FLAG* definitions</a>.
     * Values are defined as a bitmask of {@code X509_CHECK_FLAG*} values.
     * @param ssl the SSL instance (SSL*).
     * @param flags a bitmask of {@code X509_CHECK_FLAG*} values.
     * @param hostname the hostname which is expected for validation.
     */
    public static native void setHostNameValidation(long ssl, int flags, String hostname);

    /**
     * Return the methods used for authentication.
     *
     * @param ssl the SSL instance (SSL*)
     * @return the methods
     */
    public static native String[] authenticationMethods(long ssl);

    /**
     * Set BIO of PEM-encoded Server CA Certificates
     * <p>
     * This directive sets the optional all-in-one file where you can assemble the
     * certificates of Certification Authorities (CA) which form the certificate
     * chain of the server certificate. This starts with the issuing CA certificate
     * of the server certificate and can range up to the root CA certificate.
     * Such a file is simply the concatenation of the various PEM-encoded CA
     * Certificate files, usually in certificate chain order.
     * <p>
     * But be careful: Providing the certificate chain works only if you are using
     * a single (either RSA or DSA) based server certificate. If you are using a
     * coupled RSA+DSA certificate pair, this will work only if actually both
     * certificates use the same certificate chain. Otherwsie the browsers will be
     * confused in this situation.
     * @param ssl Server or Client to use.
     * @param bio BIO of PEM-encoded Server CA Certificates.
     * @param skipfirst Skip first certificate if chain file is inside
     *                  certificate file.
     *
     * @deprecated use {@link #setKeyMaterial(long, long, long)}
     */
    @Deprecated
    public static native void setCertificateChainBio(long ssl, long bio, boolean skipfirst);

    /**
     * Set Certificate
     * <br>
     * Point setCertificate at a PEM encoded certificate stored in a BIO. If
     * the certificate is encrypted, then you will be prompted for a
     * pass phrase.  Note that a kill -HUP will prompt again. A test
     * certificate can be generated with `make certificate' under
     * built time. Keep in mind that if you've both a RSA and a DSA
     * certificate you can configure both in parallel (to also allow
     * the use of DSA ciphers, etc.)
     * <br>
     * If the key is not combined with the certificate, use key param
     * to point at the key file.  Keep in mind that if
     * you've both a RSA and a DSA private key you can configure
     * both in parallel (to also allow the use of DSA ciphers, etc.)
     * @param ssl Server or Client to use.
     * @param certBio Certificate BIO.
     * @param keyBio Private Key BIO to use if not in cert.
     * @param password Certificate password. If null and certificate
     *                 is encrypted.
     * @throws Exception if an error happened
     *
     * @deprecated use {@link #setKeyMaterial(long, long, long)}
     */
    @Deprecated
    public static native void setCertificateBio(
            long ssl, long certBio, long keyBio, String password) throws Exception;

    /**
     * Load a private key from the used OpenSSL ENGINE via the
     * <a href="https://www.openssl.org/docs/man1.1.0/crypto/ENGINE_load_private_key.html">ENGINE_load_private_key</a>
     * function.
     *
     * <p>Be sure you understand how OpenSsl will behave with respect to reference counting!
     *
     * If the ownership is not transferred you need to call {@link #freePrivateKey(long)} once the key is not used
     * anymore to prevent memory leaks.
     *
     * @param keyId the id of the key.
     * @param password the password to use or {@code null} if none.
     * @return {@code EVP_PKEY} pointer
     * @throws Exception if an error happened
     */
    public static native long loadPrivateKeyFromEngine(String keyId, String password) throws Exception;

    /**
     * Parse private key from BIO and return {@code EVP_PKEY} pointer.
     *
     * <p>Be sure you understand how OpenSsl will behave with respect to reference counting!
     *
     * If the {@code EVP_PKEY} pointer is used with the client certificate callback
     * {@link CertificateRequestedCallback} the ownership goes over to OpenSsl / Tcnative and so calling
     * {@link #freePrivateKey(long)} should <strong>NOT</strong> be done in this case. Otherwise you may
     * need to call {@link #freePrivateKey(long)} to decrement the reference count and free memory.
     *
     * @param privateKeyBio the pointer to the {@code BIO} that contains the private key
     * @param password the password or {@code null} if no password is needed
     * @return {@code EVP_PKEY} pointer
     * @throws Exception if an error happened
     */
    public static native long parsePrivateKey(long privateKeyBio, String password) throws Exception;

    /**
     * Free private key ({@code EVP_PKEY} pointer).
     *
     * @param privateKey {@code EVP_PKEY} pointer
     */
    public static native void freePrivateKey(long privateKey);

    /**
     * Parse X509 chain from BIO and return ({@code STACK_OF(X509)} pointer).
     *
     * <p>Be sure you understand how OpenSsl will behave with respect to reference counting!
     *
     * If the {@code STACK_OF(X509)} pointer is used with the client certificate callback
     * {@link CertificateRequestedCallback} the ownership goes over to OpenSsl / Tcnative and so calling
     * {@link #freeX509Chain(long)} should <strong>NOT</strong> be done in this case. Otherwise you may
     * need to call {@link #freeX509Chain(long)} to decrement the reference count and free memory.
     *
     * @param x509ChainBio the pointer to the {@code BIO} that contains the X509 chain
     * @return {@code STACK_OF(X509)} pointer
     * @throws Exception if an error happened
     */
    public static native long parseX509Chain(long x509ChainBio) throws Exception;

    /**
     * Free x509 chain ({@code STACK_OF(X509)} pointer).
     *
     * @param x509Chain {@code STACK_OF(X509)} pointer
     */
    public static native void freeX509Chain(long x509Chain);
    
    /**
     * Enables OCSP stapling for the given {@link SSLEngine} or throws an
     * exception if OCSP stapling is not supported.
     * 
     * <p>NOTE: This needs to happen before the SSL handshake.
     * 
     * <p><a href="https://www.openssl.org/docs/man1.0.2/ssl/SSL_set_tlsext_status_type.html">SSL_set_tlsext_status_type</a>
     * <p><a href="https://commondatastorage.googleapis.com/chromium-boringssl-docs/ssl.h.html">Search for OCSP</a>
     */
    public static native void enableOcsp(long ssl);

    /**
     * Sets the keymaterial to be used for the server side. The passed in chain and key needs to be generated via
     * {@link #parseX509Chain(long)} and {@link #parsePrivateKey(long, String)}. It's important to note that the caller
     * of the method is responsible to free the passed in chain and key in any case as this method will increment the
     * reference count of the chain and key.
     *
     * @deprecated use {@link #setKeyMaterial(long, long, long)}
     */
    @Deprecated
    public static void setKeyMaterialServerSide(long ssl, long chain, long key) throws Exception {
        setKeyMaterial(ssl, chain, key);
    }

    /**
     * Sets the keymaterial to be used. The passed in chain and key needs to be generated via
     * {@link #parseX509Chain(long)} and {@link #parsePrivateKey(long, String)}. It's important to note that the caller
     * of the method is responsible to free the passed in chain and key in any case as this method will increment the
     * reference count of the chain and key.
     */
    public static native void setKeyMaterial(long ssl, long chain, long key) throws Exception;

    /**
     * Sets the keymaterial to be used for the client side. The passed in chain and key needs to be generated via
     * {@link #parseX509Chain(long)} and {@link #parsePrivateKey(long, String)}. It's important to note that the caller
     * of the method is responsible to free the passed in chain and key in any case as this method will increment the
     * reference count of the chain and key.
     *
     * @deprecated use {@link #setKeyMaterial(long, long, long)}
     */
    @Deprecated
    public static native void setKeyMaterialClientSide(long ssl, long x509Out, long pkeyOut, long chain, long key) throws Exception;

    /**
     * Sets the OCSP response for the given {@link SSLEngine} or throws an
     * exception in case of an error.
     *
     * <p>NOTE: This is only meant to be called for server {@link SSLEngine}s.
     *
     * <p><a href="https://www.openssl.org/docs/man1.0.2/ssl/SSL_set_tlsext_status_type.html">SSL_set_tlsext_status_type</a>
     * <p><a href="https://commondatastorage.googleapis.com/chromium-boringssl-docs/ssl.h.html">Search for OCSP</a>
     *
     * @param ssl the SSL instance (SSL *)
     */
    public static native void setOcspResponse(long ssl, byte[] response);

    /**
     * Returns the OCSP response for the given {@link SSLEngine} or {@code null}
     * if the server didn't provide a stapled OCSP response.
     *
     * <p>NOTE: This is only meant to be called for client {@link SSLEngine}s.
     *
     * <p><a href="https://www.openssl.org/docs/man1.0.2/ssl/SSL_set_tlsext_status_type.html">SSL_set_tlsext_status_type</a>
     * <p><a href="https://commondatastorage.googleapis.com/chromium-boringssl-docs/ssl.h.html">Search for OCSP</a>
     *
     * @param ssl the SSL instance (SSL *)
     */
    public static native byte[] getOcspResponse(long ssl);

    /**
     * Set the FIPS mode to use. See <a href="https://wiki.openssl.org/index.php/FIPS_mode_set()"> man FIPS_mode_set</a>.
     *
     * @param mode the mode to use.
     * @throws Exception throws if setting the fips mode failed.
     */
    public static native void fipsModeSet(int mode) throws Exception;

    /**
     * Return the SNI hostname that was sent as part of the SSL Hello.
     * @param ssl the SSL instance (SSL *)
     * @return the SNI hostname or {@code null} if none was used.
     */
    public static native String getSniHostname(long ssl);

    /**
     * Return the signature algorithms that the remote peer supports or {@code null} if none are supported.
     * See <a href="https://www.openssl.org/docs/man1.1.0/ssl/SSL_get_sigalgs.html"> man SSL_get_sigalgs</a> for more details.
     * The returned names are generated using {@code OBJ_nid2ln} with the {@code psignhash} as parameter.
     *
     * @param ssl the SSL instance (SSL *)
     * @return the signature algorithms or {@code null}.
     */
    public static native String[] getSigAlgs(long ssl);

    /**
     * Returns the master key used for the current ssl session.
     * This should be used extremely sparingly as leaking this key defeats the whole purpose of encryption
     * especially forward secrecy. This exists here strictly for debugging purposes.
     *
     * @param ssl the SSL instance (SSL *)
     * @return the master key used for the ssl session
     */
    public static native byte[] getMasterKey(long ssl);

    /**
     * Extracts the random value sent from the server to the client during the initial SSL/TLS handshake.
     * This is needed to extract the HMAC & keys from the master key according to the TLS PRF.
     * <b>This is not a random number generator.</b>
     *
     * @param ssl the SSL instance (SSL *)
     * @return the random server value used for the ssl session
     */
    public static native byte[] getServerRandom(long ssl);

    /**
     * Extracts the random value sent from the client to the server during the initial SSL/TLS handshake.
     * This is needed to extract the HMAC & keys from the master key according to the TLS PRF.
     * <b>This is not a random number generator.</b>
     *
     * @param ssl the SSL instance (SSL *)
     * @return the random client value used for the ssl session
     */
    public static native byte[] getClientRandom(long ssl);

    /**
     * Return the {@link Runnable} that needs to be run as an operation did signal that a task needs to be completed
     * before we can retry the previous action.
     *
     * After the task was run we should retry the operation that did signal back that a task needed to be run.
     *
     *
     * The {@link Runnable} may also implement {@link AsyncTask} which allows for fully asynchronous execution if
     * {@link AsyncTask#runAsync(Runnable)} is used.
     *
     * @param ssl the SSL instance (SSL *)
     * @return the task to run.
     */
    public static native Runnable getTask(long ssl);

    /**
     * Return the {@link AsyncTask} that needs to be run as an operation did signal that a task needs to be completed
     * before we can retry it.
     *
     * After the task was run we should retry the operation that did signal back that a task needed to be run.
     *
     * @param ssl the SSL instance (SSL *)
     * @return the task to run.
     */
    public static AsyncTask getAsyncTask(long ssl) {
        return (AsyncTask) getTask(ssl);
    }

    /**
     * Return {@code true} if the SSL_SESSION was reused. 
     * See <a href="https://www.openssl.org/docs/man1.1.0/man3/SSL_session_reused.html">SSL_session_reused</a>.
     * 
     * @param ssl the SSL instance (SSL *)
     * @return {@code true} if the SSL_SESSION was reused, {@code false} otherwise.
     */
    public static native boolean isSessionReused(long ssl);

    /**
     * Sets the {@code SSL_SESSION} that should be used for {@code SSL}.
     * @param ssl the SSL instance (SSL *)
     * @param session the SSL_SESSION instance (SSL_SESSION *)
     * @return {@code true} if successful, {@code false} otherwise. 
     */
    public static native boolean setSession(long ssl, long session);

    /**
     * Returns the {@code SSL_SESSION} that is used for {@code SSL}.
     * See <a href="https://www.openssl.org/docs/man1.1.0/man3/SSL_get_session.html">SSL_get_session</a>.
     * 
     * @param ssl the SSL instance (SSL *)
     * @return the SSL_SESSION instance (SSL_SESSION *) used
     */
    public static native long getSession(long ssl);

    /**
     * Allow to set the renegotiation mode that is used. This is only support by {@code BoringSSL}.
     *
     * See <a href="https://boringssl.googlesource.com/boringssl/+/refs/heads/master/include/openssl/ssl.h#4081">
     *     SSL_set_renegotiate_mode</a>..
     * @param ssl the SSL instance (SSL *)
     * @param mode  the mode.
     * @throws Exception thrown if some error happens.
     */
    public static native void setRenegotiateMode(long ssl, int mode) throws Exception;
}

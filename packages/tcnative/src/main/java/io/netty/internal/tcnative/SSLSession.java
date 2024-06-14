/*
 * Copyright 2020 The Netty Project
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
 * Methods to operate on a {@code SSL_SESSION}.
 */
public final class SSLSession {

    private SSLSession() { }
    
    /**
     * See <a href="https://www.openssl.org/docs/man1.1.1/man3/SSL_SESSION_get_time.html">SSL_SESSION_get_time</a>.
     *
     * @param session the SSL_SESSION instance (SSL_SESSION *)
     * @return returns the time at which the session was established. The time is given in seconds since the Epoch
     */
    public static native long getTime(long session);

    /**
     * See <a href="https://www.openssl.org/docs/man1.1.1/man3/SSL_SESSION_get_timeout.html">SSL_SESSION_get_timeout</a>.
     *
     * @param session the SSL_SESSION instance (SSL_SESSION *)
     * @return returns the timeout for the session. The time is given in seconds since the Epoch
     */
    public static native long getTimeout(long session);

    /**
     * See <a href="https://www.openssl.org/docs/man1.1.1/man3/SSL_SESSION_set_timeout.html">SSL_SESSION_set_timeout</a>.
     * 
     * @param session the SSL_SESSION instance (SSL_SESSION *)
     * @param seconds timeout in seconds
     * @return returns the timeout for the session before this call. The time is given in seconds since the Epoch
     */
    public static native long setTimeout(long session, long seconds);

    /**
     * See <a href="https://www.openssl.org/docs/man1.1.1/man3/SSL_SESSION_get_id.html">SSL_SESSION_get_id</a>.
     *
     * @param session the SSL_SESSION instance (SSL_SESSION *)
     * @return the session id as byte array representation obtained via SSL_SESSION_get_id.
     */
    public static native byte[] getSessionId(long session);

    /**
     * See <a href="https://www.openssl.org/docs/man1.1.1/man3/SSL_SESSION_up_ref.html">SSL_SESSION_up_ref</a>.
     *
     * @param session the SSL_SESSION instance (SSL_SESSION *)
     * @return {@code true} if successful, {@code false} otherwise. 
     */
    public static native boolean upRef(long session);

     /**
     * See <a href="https://www.openssl.org/docs/man1.1.1/man3/SSL_SESSION_free.html">SSL_SESSION_free</a>.
     *
     * @param session the SSL_SESSION instance (SSL_SESSION *)
     */
    public static native void free(long session);

    /**
     * Will return {@code true} if the session should only re-used once.
     * See <a href="https://commondatastorage.googleapis.com/chromium-boringssl-docs/ssl.h.html#SSL_SESSION_should_be_single_use">SSL_SESSION_should_be_single_use</a>. 
     * @param session
     * @return {@code true} if the session should be re-used once only, {@code false} otherwise.
     */
    public static native boolean shouldBeSingleUse(long session);
}

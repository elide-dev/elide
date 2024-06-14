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
 * Allows to implement a custom external {@code SSL_SESSION} cache.
 * 
 * See <a href="https://www.openssl.org/docs/man1.1.0/man3/SSL_CTX_sess_set_get_cb.html">SSL_CTX_sess_set_get_cb.html</a>
 * and {a href="https://www.openssl.org/docs/man1.1.0/man3/SSL_CTX_set_session_cache_mode.html">SSL_CTX_set_session_cache_mode</a>.
 */
public interface SSLSessionCache {

    /**
     * Returns {@code true} if the cache takes ownership of the {@code SSL_SESSION} and will call {@code SSL_SESSION_free} once it should be destroyed,
     * {@code false} otherwise.
     *
     * See <a href="https://www.openssl.org/docs/man1.1.0/man3/SSL_CTX_sess_set_new_cb.html">SSL_CTX_sess_set_new_cb</a>.
     * 
     * @param ssl {@code SSL*} 
     * @param sslSession {@code SSL_SESSION*}
     * @return {@code true} if session ownership was transfered, {@code false} if not. 
     */
    boolean sessionCreated(long ssl, long sslSession);

    /**
     * Called once a {@code SSL_SESSION} should be retrieved for the given {@code SSL} and with the given session ID.
     * See <a href="https://www.openssl.org/docs/man1.1.0/man3/SSL_CTX_sess_set_get_cb.html">SSL_CTX_sess_set_get_cb</a>.
     * If the session is shared you need to call {@link SSLSession#upRef(long)} explicit in this callback and explicit free all {@code SSL_SESSION}s
     * once the cache is destroyed via {@link SSLSession#free(long)}.
     * 
     * @param sslCtx {code SSL_CTX*}
     * @param sessionId the session id
     * @return the {@link SSL_SESSION} or {@code -1} if none was found in the cache.
     */
    long getSession(long sslCtx, byte[] sessionId);
}

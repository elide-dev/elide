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
 * Callback that is called once an operation completed.
 *
 * @param <T>   The result type.
 */
public interface ResultCallback<T> {
    /**
     * Called when the operation completes with the given result.
     *
     * @param ssl       the SSL instance (SSL *)
     * @param result    the result.
     */
    void onSuccess(long ssl, T result);

    /**
     * Called when the operation completes with an error.
     *
     * @param ssl       the SSL instance (SSL *)
     * @param cause     the error.
     */
    void onError(long ssl, Throwable cause);
}

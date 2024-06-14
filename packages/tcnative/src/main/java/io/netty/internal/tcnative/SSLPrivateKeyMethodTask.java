/*
 * Copyright 2019 The Netty Project
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

abstract class SSLPrivateKeyMethodTask extends SSLTask implements AsyncTask {
    private static final byte[] EMPTY = new byte[0];
    private final AsyncSSLPrivateKeyMethod method;

    // Will be accessed via JNI.
    private byte[] resultBytes;

    SSLPrivateKeyMethodTask(long ssl, AsyncSSLPrivateKeyMethod method) {
        super(ssl);
        this.method = method;
    }


    @Override
    public final void runAsync(final Runnable completeCallback) {
        run(completeCallback);
    }

    @Override
    protected final void runTask(final long ssl, final TaskCallback callback) {
        runTask(ssl, method, new ResultCallback<byte[]>() {
            @Override
            public void onSuccess(long ssl, byte[] result) {
                resultBytes = result;
                callback.onResult(ssl, 1);
            }

            @Override
            public void onError(long ssl, Throwable cause) {
                // Return 0 as this signals back that the operation failed.
                resultBytes = EMPTY;
                callback.onResult(ssl, 0);
            }
        });
    }

    protected abstract void runTask(long ssl, AsyncSSLPrivateKeyMethod method,
                                      ResultCallback<byte[]> resultCallback);
}

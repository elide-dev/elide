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

final class AsyncSSLPrivateKeyMethodAdapter implements AsyncSSLPrivateKeyMethod {
    private final SSLPrivateKeyMethod method;

    AsyncSSLPrivateKeyMethodAdapter(SSLPrivateKeyMethod method) {
        if (method == null) {
            throw new NullPointerException("method");
        }
        this.method = method;
    }

    @Override
    public void sign(long ssl, int signatureAlgorithm, byte[] input, ResultCallback<byte[]> resultCallback) {
        final byte[] result;
        try {
            result = method.sign(ssl, signatureAlgorithm, input);
        } catch (Throwable cause) {
            resultCallback.onError(ssl, cause);
            return;
        }
        resultCallback.onSuccess(ssl, result);
    }

    @Override
    public void decrypt(long ssl, byte[] input, ResultCallback<byte[]> resultCallback) {
        final byte[] result;
        try {
            result = method.decrypt(ssl, input);
        } catch (Throwable cause) {
            resultCallback.onError(ssl, cause);
            return;
        }
        resultCallback.onSuccess(ssl, result);
    }
}

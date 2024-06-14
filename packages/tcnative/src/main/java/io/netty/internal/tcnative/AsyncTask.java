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

public interface AsyncTask extends Runnable {

    /**
     * Run this {@link AsyncTask} in an async fashion. Which means it will be run and completed at some point.
     * Once it is done the {@link Runnable} is called
     *
     * @param completeCallback  The {@link Runnable} that is run once the task was run and completed.
     */
    void runAsync(Runnable completeCallback);
}

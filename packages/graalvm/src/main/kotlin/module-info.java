/*
 * Copyright (c) 2023 Elide Ventures, LLC.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

module elide.graalvm {
    requires java.base;
    requires jakarta.inject;
    requires jakarta.annotation;
    requires kotlin.stdlib;
    requires kotlin.reflect;
    requires kotlinx.coroutines.core;
    requires kotlinx.coroutines.jdk9;
    requires kotlinx.datetime;
    requires kotlinx.serialization.core;
    requires kotlinx.serialization.json;
    requires io.micronaut.http;
    requires io.micronaut.inject;

    requires reactor.netty.http;
    requires io.netty.codec.http;

    requires org.graalvm.sdk;
    requires org.graalvm.truffle;

    requires elide.core;
    requires elide.base;
    requires elide.ssr;

    exports elide.runtime.intrinsics.js;
    exports elide.runtime.intrinsics.js.express;
    exports elide.runtime.gvm;
    exports elide.runtime.gvm.internals;
    exports elide.runtime.gvm.internals.intrinsics.js.base64;
    exports elide.runtime.gvm.internals.intrinsics.js.console;
    exports elide.runtime.gvm.internals.intrinsics.js.crypto;
    exports elide.runtime.gvm.internals.intrinsics.js.express;
    exports elide.runtime.gvm.internals.intrinsics.js.url;
    exports elide.runtime.gvm.vfs;
}

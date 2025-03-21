/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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
  requires jdk.zipfs;
  requires jakarta.inject;
  requires jakarta.annotation;
  requires kotlin.stdlib;
  requires kotlin.reflect;
  requires kotlinx.coroutines.core;
  requires kotlinx.coroutines.jdk9;
  requires kotlinx.datetime;
  requires kotlinx.serialization.core;
  requires kotlinx.serialization.json;
  requires io.micronaut.core;
  requires io.micronaut.http;
  requires io.micronaut.inject;
  requires io.netty.codec.http;
  requires reactor.netty.core;
  requires reactor.netty.http;
  requires org.reactivestreams;
  requires com.lmax.disruptor;

  requires elide.core;
  requires elide.base;
  requires elide.ssr;

  requires org.graalvm.polyglot;

  requires com.google.common.jimfs;
  requires org.apache.commons.compress;
  requires io.micronaut.context;
  requires java.logging;
  requires org.graalvm.nativeimage.builder;

  exports elide.runtime.intrinsics.js;
  exports elide.runtime.intrinsics.js.express;
  exports elide.runtime.gvm;
  exports elide.runtime.gvm.cfg;
  exports elide.runtime.gvm.internals;
  exports elide.runtime.gvm.internals.context;
  exports elide.runtime.gvm.internals.intrinsics.js;
  exports elide.runtime.gvm.internals.intrinsics.js.base64;
  exports elide.runtime.gvm.internals.intrinsics.js.console;
  exports elide.runtime.gvm.internals.intrinsics.js.crypto;
  exports elide.runtime.gvm.internals.intrinsics.js.express;
  exports elide.runtime.gvm.internals.intrinsics.js.url;
  exports elide.runtime.gvm.vfs;

  exports elide.runtime.core;
  exports elide.runtime.core.extensions;
  exports elide.runtime.plugins;
  exports elide.runtime.plugins.debug;
  exports elide.runtime.plugins.vfs;
  exports elide.runtime.plugins.js;
}

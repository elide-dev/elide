/*
 * Copyright (c) 2023-2024 Elide Ventures, LLC.
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

/**
 * Elide for Embedded
 *
 * <p>Provides native API access for running apps on Elide.</p>
 */
module elide.embedded {
  requires static jakarta.annotation;
  requires static jakarta.inject;

  requires java.base;

  requires kotlin.stdlib;
  requires kotlinx.atomicfu;

  requires org.graalvm.nativeimage;
  requires org.graalvm.polyglot;
  requires org.graalvm.js;
  requires org.graalvm.ruby;
  requires org.graalvm.py;
  requires org.graalvm.espresso;

  requires elide.core;
  requires elide.base;
  requires io.micronaut.inject;

  exports elide.embedded.api;
  exports elide.embedded.err;
}

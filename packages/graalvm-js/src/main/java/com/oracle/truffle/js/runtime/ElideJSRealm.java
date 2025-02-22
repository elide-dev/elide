/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
package com.oracle.truffle.js.runtime;

/**
 * Patched version of the {@link JSRealm} class to allow for injection of Elide's JavaScript resources and tools.
 */
public class ElideJSRealm extends JSRealm {
  private final JSRealm base;

  private ElideJSRealm(JSRealm base) {
    super(base.getContext(), base.getEnv(), base.getParent());
    this.base = base;
  }

  public static ElideJSRealm wrapping(JSRealm realm) {
    return new ElideJSRealm(realm);
  }

  @Override
  public JSRealm createChildRealm() {
    return base.createChildRealm();
  }
}

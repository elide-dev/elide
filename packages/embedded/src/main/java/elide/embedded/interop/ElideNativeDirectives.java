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

package elide.embedded.interop;

import org.graalvm.nativeimage.c.CContext.Directives;

import java.util.List;


/**
 * Build-time directives for the shared library housing the embedded runtime. This class includes a header defining C
 * types used at runtime so that they can be used via the GraalVM C interoperability API.
 */
public class ElideNativeDirectives implements Directives {
  @Override
  public List<String> getHeaderFiles() {
    return List.of("<" + BuildConstants.TYPEDEF_HEADER_PATH + ">");
  }
}

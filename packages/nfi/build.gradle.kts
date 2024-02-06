/*
 * Copyright (c) 2024 Elide Ventures, LLC.
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


import elide.internal.cpp.cpp

plugins {
  id("elide.internal.conventions")
  id("elide.internal.cpp")
  `cpp-library`
}

group = "dev.elide.embedded"

elide {
  cpp {
    headersOnly = true
  }
}

library {
  source.from(file("src"))
  privateHeaders.from(file("src"))
  publicHeaders.from(file("include"))
}

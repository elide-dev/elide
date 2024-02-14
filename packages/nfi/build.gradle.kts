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

plugins {
  id("elide.internal.conventions")
}

elide {
  // Nothing to set.
}

group = "dev.elide.embedded"

val publicHeaders: Configuration by configurations.creating {
  isCanBeConsumed = true
  isCanBeResolved = false

  attributes {
    attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
    attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.C_PLUS_PLUS_API))
    attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EMBEDDED))
  }
}

val publicHeadersZip: TaskProvider<Zip> by tasks.registering(Zip::class) {
  archiveBaseName = "elide-headers"
  from(layout.projectDirectory.dir("include"))
}

artifacts {
  add("publicHeaders", publicHeadersZip)
}

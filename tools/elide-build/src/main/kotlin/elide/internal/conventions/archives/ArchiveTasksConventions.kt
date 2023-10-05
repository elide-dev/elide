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

package elide.internal.conventions.archives

import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy.EXCLUDE
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.bundling.Jar

/** Adjusts [Zip] and [Jar] tasks to improve build caching and hermeticity. */
internal fun Project.reproducibleArchiveTasks() {
  fun configureArchiveTask(task: Zip) = task.apply {
    isReproducibleFileOrder = true
    isPreserveFileTimestamps = false
    isZip64 = true
  }

  tasks.withType(Jar::class.java).configureEach(::configureArchiveTask)
  tasks.withType(Zip::class.java).configureEach(::configureArchiveTask)
}

/** Adjusts [Zip], [Jar], and [Tar] tasks to set the duplicate strategy to [EXCLUDE]. */
internal fun Project.excludeDuplicateArchives() {
  fun configureArchiveTask(task: AbstractArchiveTask) = task.apply {
    duplicatesStrategy = EXCLUDE
  }

  tasks.withType(Jar::class.java).configureEach(::configureArchiveTask)
  tasks.withType(Zip::class.java).configureEach(::configureArchiveTask)
  tasks.withType(Tar::class.java).configureEach(::configureArchiveTask)
}

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

package elide.annotations

/**
 * # Annotation: Context
 *
 * Marks a class as "context"-phase for purposes of dependency injection. Classes marked with this annotation are
 * initialized eagerly on application startup.
 *
 * When used on JVM platforms, this annotation is translated into a Micronaut `Context` annotation:
 * https://docs.micronaut.io/snapshot/api/io/micronaut/context/annotation/Context.html
 *
 * @see Eager which behaves similarly.
 */
public expect annotation class Context

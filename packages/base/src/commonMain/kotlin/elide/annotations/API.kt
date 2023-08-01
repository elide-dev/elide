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
 * # Annotation: API
 *
 * Marks an application-level class as an API interface, which defines the abstract surface of a single unit of business
 * logic; combined with [Logic], classes annotated with `API` constitute a set of interface and implementation pairs.
 *
 * API should only be affixed to interfaces or abstract classes. API interface parameters are preserved and other AOT-
 * style configurations are possible based on this annotation.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@MustBeDocumented
public annotation class API

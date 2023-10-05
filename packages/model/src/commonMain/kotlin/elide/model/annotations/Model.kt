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

package elide.model.annotations

/**
 * Marks an application-level class as a data model, which makes it eligible for reflective use (even in native
 * circumstances such as on GraalVM).
 *
 * Classes marked as models become available for reflection for all constructors, fields, and methods. Models should
 * typically only depend on other models (ideally via encapsulation), and should be immutable. Kotlin data classes are
 * an example of good model semantics.
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.TYPE)
public annotation class Model

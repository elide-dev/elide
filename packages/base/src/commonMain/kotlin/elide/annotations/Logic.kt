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
 * # Annotation: Logic
 *
 * Marks an application class as "business logic," which automatically makes it eligible for dependency injection,
 * autowired logging, and other framework features.
 *
 * This annotation should be used on the *implementation* of a given interface. API interfaces should be marked with
 * [API] to participate in auto-documentation and other AOT-based features.
 *
 * @see API corresponding annotation for API interfaces.
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
public annotation class Logic

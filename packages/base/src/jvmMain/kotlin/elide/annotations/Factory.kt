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
 * # Annotation: Factory (JVM)
 *
 * Marks a class as a factory for an injected type. Factories are responsible for creating instances of injected types.
 * On JVM platforms, this annotation is aliased to a Micronaut `Factory` annotation.
 *
 * See also: https://docs.micronaut.io/snapshot/api/io/micronaut/context/annotation/Factory.html
 */
public actual typealias Factory = io.micronaut.context.annotation.Factory

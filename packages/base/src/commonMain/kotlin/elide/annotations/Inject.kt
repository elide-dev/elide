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
 * # Annotation: Inject
 *
 * This annotation marks a constructor argument, field/property, or parameter as an injected value. Injected values are
 * generally resolved at build-time but may be resolved at run-time. Dependency Injection (DI) is an opt-in pattern
 * which inverts control of object creation. Instead of creating objects directly, they are created by a DI container
 * and provided to each object that requires them.
 *
 * ## Using DI
 *
 * Using dependency injection depends on your target platform. On JVM platforms, elide uses Micronaut as an injection
 * engine and DI container, but the annotations used are standard and may be used with whatever engine the developer
 * chooses.
 *
 * This annotation, on JVM platforms, is aliased to the standard Jakarta inject annotation:
 * https://jakarta.ee/specifications/dependency-injection/2.0/apidocs/jakarta/inject/inject
 *
 * See other annotations from Jakarta supported by Elide:
 * - [Named]: Naming for injectable value qualifiers
 * - [Qualifier]: Qualification for injectable values
 * - [Singleton]: Singleton lifecycle restriction
 */
public expect annotation class Inject

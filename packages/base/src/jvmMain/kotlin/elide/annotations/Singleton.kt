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

package elide.annotations

/**
 * # Annotation: Singleton (JVM)
 *
 * Marks a class which participates in injection (DI) as a singleton; singletons are classes with the constraint that
 * only one instance may exist at runtime. In injected contexts, the singleton lifecycle is managed by the DI container.
 *
 * On JVM platforms, this annotation behaves like a standard Jakarta `Singleton` annotation:
 * https://jakarta.ee/specifications/dependency-injection/2.0/apidocs/jakarta/inject/singleton
 */
public actual typealias Singleton = jakarta.inject.Singleton

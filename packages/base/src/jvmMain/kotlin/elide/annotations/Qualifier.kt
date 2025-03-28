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
 * # Annotation: Qualifier (JVM)
 *
 * This annotation works in cooperation with [Inject] to qualify an injectable value by some criteria. Qualifiers can
 * be affixed to any injectable value; the annotation values at the call-site must match the values on an eligible
 * instance for injection.
 *
 * Other qualifier annotations exist, such as [Named]. On JVM platforms, this annotation is aliased to a standard
 * Jakarta `Qualifier` annotation:
 *
 * https://jakarta.ee/specifications/dependency-injection/2.0/apidocs/jakarta/inject/qualifier
 */
public actual typealias Qualifier = jakarta.inject.Qualifier

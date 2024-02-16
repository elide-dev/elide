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
 * # Annotation: Named (JVM)
 *
 * This annotation works in cooperation with [Inject] to qualify injected values via simple string names. "Qualified"
 * values are filtered by their qualification criteria at injection time. In this case, we are filtering by a simple
 * name which should correspond with a name of equal value on an injected instance.
 *
 * Qualifiers can be further customized or filtered via other annotations, such as [Qualifier]. On JVM platforms, this
 * annotation is aliased to a standard Jakarta `Named` annotation:
 *
 * https://jakarta.ee/specifications/dependency-injection/2.0/apidocs/jakarta/inject/named
 *
 * @see Inject to mark a value as injected.
 */
public actual typealias Named = jakarta.inject.Named

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

package elide.annotations.core

/**
 * # Annotation: Internal
 *
 * Marks a type or member as internal. Internal types and members may generate compile warnings if used outside the
 * compile unit where they are defined.
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
  AnnotationTarget.CLASS,
  AnnotationTarget.PROPERTY,
  AnnotationTarget.FIELD,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.CONSTRUCTOR,
  AnnotationTarget.PROPERTY_GETTER,
  AnnotationTarget.PROPERTY_SETTER,
  AnnotationTarget.TYPEALIAS,
  AnnotationTarget.FILE,
)
public annotation class Internal

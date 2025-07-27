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
 * # Annotation: Secret
 *
 * Marks a value which holds some sort of secret or protected material; such material should be redacted from output as
 * much as possible, and otherwise held with care.
 *
 * `Secret` can be applied to properties or classes; in each case, the data associated with the annotated value is
 * withheld from generated output code such as `toString` (where supported).
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
@MustBeDocumented
public annotation class Secret

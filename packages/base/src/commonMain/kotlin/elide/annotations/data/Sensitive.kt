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

package elide.annotations.data

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.PROPERTY

/**
 * # Annotation: Sensitive
 *
 * Marks a type or member as a container of sensitive data. Sensitive data containers behave differently than normal
 * with respect to logging and other output facilities. For example, if a field marked `Sensitive` is converted to a
 * string for logging purposes, it may be redacted from view.
 */
@MustBeDocumented
@Target(CLASS, PROPERTY)
@Retention(BINARY)
public annotation class Sensitive

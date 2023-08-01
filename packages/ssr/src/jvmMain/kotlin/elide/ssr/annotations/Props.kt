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

@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package elide.ssr.annotations

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import kotlinx.serialization.MetaSerializable

/**
 * TBD
 */
@Introspected
@ReflectiveAccess
@Target(AnnotationTarget.CLASS)
@MetaSerializable
public actual annotation class Props

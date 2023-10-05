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

@file:Suppress("RedundantVisibilityModifier")

package kotlinx.serialization.protobuf.schema

import kotlinx.serialization.descriptors.SerialDescriptor

/**
 * # Protobuf: Schema Generator
 *
 * Describes the interface which protocol buffer schema generators adhere to. There is only one method at the time of
 * this writing; [generateSchemaText], which generates proto-schema syntax for a given set of [SerialDescriptor]
 * instances, each for a Kotlin model.
 */
public interface ProtoBufSchemaBuilder {
  /**
   * Generate protocol buffer schema from the provided set of model [descriptors].
   *
   * @param descriptors Descriptors to generate schema for.
   * @param options Options to apply.
   * @return Generated syntax.
   */
  public fun generateSchemaText(
    descriptors: List<SerialDescriptor>,
    options: ProtoBufGeneratorOptions = ProtoBufGeneratorOptions.DEFAULTS,
  ): String
}

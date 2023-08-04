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

package elide.util.proto.adapters

import com.google.protobuf.Descriptors
import com.google.protobuf.Message
import kotlin.reflect.full.companionObjectInstance

/**
 *
 */
interface ProtoConvertible<M: Message> : ProtoSchemaConvertible {
  /**
   *
   */
  fun toMessage(): M

  /** @inheritDoc */
  @Suppress("UNCHECKED_CAST")
  override fun toDescriptor(): Descriptors.Descriptor = (this::class.companionObjectInstance as ProtoModel<M>)
    .toDescriptor()
}

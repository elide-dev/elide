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

package elide.runtime.feature

import org.graalvm.nativeimage.hosted.Feature
import org.graalvm.nativeimage.hosted.RuntimeReflection
import java.lang.reflect.Executable
import elide.annotations.internal.VMFeature

/**
 * # Feature: Protocol Buffers
 *
 * Installs types and reflective access for efficient and safe use of Protocol Buffer messages in a native image.
 */
@VMFeature
internal class ProtocolBuffers : FrameworkFeature {
  companion object {
    private const val PROTO_MESSAGE_CLASS = "com.google.protobuf.GeneratedMessageV3"
    private const val PROTO_ENUM_CLASS = "com.google.protobuf.ProtocolMessageEnum"
    private const val ENUM_VAL_DESCRIPTOR_CLASS = "com.google.protobuf.Descriptors\$EnumValueDescriptor"
    private val METHOD_ACCESSOR_PREFIXES = listOf("get", "set", "has", "add", "clear", "newBuilder")

    /**
     * Given a proto class, registers the public accessor methods for the provided proto class.
     */
    private fun registerFieldAccessors(protoClass: Class<*>) {
      for (method in protoClass.methods) {
        val hasAccessorPrefix = METHOD_ACCESSOR_PREFIXES.stream().anyMatch { prefix: String? ->
          method.name.startsWith(
            prefix!!
          )
        }
        if (hasAccessorPrefix) {
          RuntimeReflection.register(method)
        }
      }
    }

    /**
     * Given a proto class, returns the Builder nested class.
     */
    private fun getBuilderClass(protoClass: Class<*>): Class<*> {
      for (clazz in protoClass.classes) {
        if (clazz.name.endsWith("Builder")) {
          return clazz
        }
      }
      error("Failed to builder class for proto message '${protoClass.name}'")
    }
  }

  /** @inheritDoc */
  override fun isInConfiguration(access: Feature.IsInConfigurationAccess): Boolean =
    access.findClassByName("com.google.protobuf.GeneratedMessageV3") != null

  /** @inheritDoc */
  override fun beforeAnalysis(access: Feature.BeforeAnalysisAccess) {
    val protoMessageClass = access.findClassByName(PROTO_MESSAGE_CLASS)
    if (protoMessageClass != null) {
      val internalAccessorMethod = getMethodOrFail(
        protoMessageClass,
        "internalGetFieldAccessorTable",
      )

      // Finds every class whose `internalGetFieldAccessorTable()` is reached and registers it.
      // `internalGetFieldAccessorTable()` is used downstream to access the class reflectively.
      access.registerMethodOverrideReachabilityHandler({ _: Feature.DuringAnalysisAccess, method: Executable ->
        registerFieldAccessors(method.declaringClass)
        registerFieldAccessors(getBuilderClass(method.declaringClass))
      }, internalAccessorMethod)
    }

    val protoEnumClass = access.findClassByName(PROTO_ENUM_CLASS)
    if (protoEnumClass != null) {
      // Finds every reachable proto enum class and registers specific methods for reflection.
      access.registerSubtypeReachabilityHandler({ duringAccess: Feature.DuringAnalysisAccess, subtypeClass: Class<*> ->
        if (PROTO_ENUM_CLASS != subtypeClass.name) {
          var method = getMethodOrFail(
            subtypeClass,
            "valueOf",
            duringAccess.findClassByName(ENUM_VAL_DESCRIPTOR_CLASS)
          )
          RuntimeReflection.register(method)
          method =
            getMethodOrFail(subtypeClass, "getValueDescriptor")
          RuntimeReflection.register(method)
        }
      }, protoEnumClass)
    }
  }

  /** @inheritDoc */
  override fun getDescription(): String = "Configures native Protocol Buffers support"
}

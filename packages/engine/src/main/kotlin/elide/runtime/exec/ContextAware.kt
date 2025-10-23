/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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
package elide.runtime.exec

/**
 * Marks a function or property as being context-aware, that is, it requires an entered
 * [Context][org.graalvm.polyglot.Context] in the calling thread.
 *
 * **Declarations with this annotation must only be invoked by other declarations marked as [ContextAware]**, to
 * prevent them from being unintentionally called on invalid execution contexts. An exception is the
 * [ContextAwareEntrypoint] annotation, which allows the use of [ContextAware] symbols by breaking the semantic chain.
 *
 * @see ContextAwareEntrypoint
 */
@Target(
  AnnotationTarget.FUNCTION,
  AnnotationTarget.PROPERTY_GETTER,
  AnnotationTarget.PROPERTY_SETTER,
  AnnotationTarget.CONSTRUCTOR,
  AnnotationTarget.PROPERTY,
  AnnotationTarget.ANNOTATION_CLASS,
)
public annotation class ContextAware

/**
 * Allows a function or property to use [ContextAware] symbols without propagating the semantic chain. This annotation
 * is intended for use only by internal execution components, such as the [ContextAwareExecutor], it **must not** be
 * used by general code to circumvent the [ContextAware] semantics chain.
 */
@Target(
  AnnotationTarget.FUNCTION,
  AnnotationTarget.PROPERTY_GETTER,
  AnnotationTarget.PROPERTY_SETTER,
  AnnotationTarget.CONSTRUCTOR,
  AnnotationTarget.PROPERTY,
  AnnotationTarget.ANNOTATION_CLASS,
)
@InternalExecutorApi
public annotation class ContextAwareEntrypoint

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

package elide.runtime.core

import org.graalvm.polyglot.Context

/**
 * A builder allowing configuration of [PolyglotContext] instances. Plugins can intercept builders to apply custom
 * options using this class.
 *
 * Under the current implementation, this is an alias over GraalVM's [Context.Builder].
 */
@DelicateElideApi public typealias PolyglotContextBuilder = Context.Builder

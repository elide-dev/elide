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

package elide.runtime.gvm.kotlin

import org.graalvm.polyglot.Value
import java.util.concurrent.Callable
import elide.runtime.precompiler.Precompiler

/**
 * ## Kotlin Runnable
 *
 * Describes each type of supported Kotlin runnable; includes Kotlin scripts and precompiled Kotlin JARs.
 */
public sealed interface KotlinRunnable : Callable<Value?>, Precompiler.BundleInfo

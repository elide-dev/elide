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

package elide.runtime.core.extensions

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContextBuilder

/** Enable an option given its [key]. This sets the option's value to `"true"`. */
@DelicateElideApi public fun PolyglotContextBuilder.enableOption(key: String) {
  option(key, "true")
}

/** Enable a group of options given their [keys]. This sets each option's value to `"true"`. */
@DelicateElideApi public fun PolyglotContextBuilder.enableOptions(vararg keys: String) {
  options(keys.associateWith { "true" })
}

/** Disable an option given its [key]. This sets the option's value to `"false"`. */
@DelicateElideApi public fun PolyglotContextBuilder.disableOption(key: String) {
  option(key, "false")
}

/** Disable a group of options given their [keys]. This sets each option's value to `"false"`. */
@DelicateElideApi public fun PolyglotContextBuilder.disableOptions(vararg keys: String) {
  options(keys.associateWith { "false" })
}

/** Configure a group of options given a series of key-value pairs. */
@DelicateElideApi public fun PolyglotContextBuilder.setOptions(vararg options: Pair<String, String>) {
  options(mapOf(*options))
}

/**
 * Configure an option given its [key] and a boolean [value]. The value will be converted to `"true"` or `"false"`.
 */
@DelicateElideApi public fun PolyglotContextBuilder.setOption(key: String, value: Boolean) {
  option(key, if(value) "true" else "false")
}

/**
 * Configure a group of boolean options given a series of key-value pairs. Each value will be converted to `"true"`
 * or `"false"` accordingly.
 */
@DelicateElideApi
@JvmName("setBooleanOptions")
public fun PolyglotContextBuilder.setOptions(vararg options: Pair<String, Boolean>) {
  options(options.associate { (key, value) -> key to if(value) "true" else "false" })
}

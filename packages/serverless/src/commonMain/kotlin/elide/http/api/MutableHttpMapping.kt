/*
 * Copyright (c) 2024 Elide Ventures, LLC.
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

package elide.http.api

/**
 * # HTTP Mapping: Mutable
 *
 * Describes the abstract concept of an "HTTP mapping," in mutable form, which is generally a map of key-value string
 * pairs; keys are ordered by default and comparable on a case-insensitive basis.
 *
 * Examples of HTTP mappings include URL query parameters, HTTP headers, and so on.
 *
 * @see HttpMapping for the immutable base form of this interface.
 * @see MutableHttpHeaders for an example of a mutable HTTP mapping.
 */
public interface MutableHttpMapping<Key, Value> : MutableMap<Key, Value>, HttpMapping<Key, Value>
        where Key: HttpText

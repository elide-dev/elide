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
package elide.tooling.containers

/**
 * String name portion of a container's image coordinate.
 */
public typealias ContainerName = String

/**
 * String hash value of a container's fingerprint.
 */
public typealias ContainerHashValue = String

/**
 * String tag value portion of the container's image coordinate.
 */
public typealias ContainerTagValue = String

/**
 * String value of a container registry coordinate, which is typically a URL or URI.
 */
public typealias ContainerRegistryValue = String

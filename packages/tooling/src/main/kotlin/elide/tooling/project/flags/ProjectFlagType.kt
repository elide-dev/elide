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
package elide.tooling.project.flags

/**
 * ## Project Flag Type
 *
 * Enumerates the types of project flags which may be defined; the "type" of a project flag determines the underlying
 * data type, which is paired here as well for use in generic cases.
 */
public enum class ProjectFlagType {
  /** Boolean switch field which accepts true/false (or aliases like 'on/off') and which allows negation. */
  BOOLEAN,

  /** String field which accepts simple string values. */
  STRING,

  /** Enumeration field which accepts string values to select from a pre-defined list of instances. */
  ENUM,

  /** Integer field which accepts whole number values. */
  INTEGER,

  /** Float field which accepts floating-point number values. */
  FLOAT,
}

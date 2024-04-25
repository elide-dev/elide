/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
package elide.runtime.intrinsics.js.node.os

/**
 * # Operating System: Policy
 *
 * Describes the policy to apply for OS API calls; these can either be allowed or denied.
 */
public enum class OSPolicy {
  /**
   * Allow access to OS API calls.
   */
  ALLOW,

  /**
   * Deny access to OS API calls.
   */
  DENY,
}


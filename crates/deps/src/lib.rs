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

#![feature(test)]
#![feature(const_option)]
#![feature(const_trait_impl)]
#![forbid(unsafe_op_in_unsafe_fn, unused_unsafe, dead_code)]

/// Provides structural definitions for handling dependencies.
pub mod model;

/// Provides structural definitions for dependency catalogs.
pub mod catalog;

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
package elide.runtime.intrinsics.sqlite

import io.micronaut.core.annotation.ReflectiveAccess
import elide.annotations.API

/**
 * # SQLite
 *
 * Describes the module-level API provided by Elide for interacting with, and manipulating, SQLite databases; Elide
 * embeds the native SQLite engine and makes it accessible to guest code, and to host code, via JDBC.
 *
 * SQLite databases are backed by native code, which is made addressable via JNI; guest code drives these methods
 * directly, with values marshaled by interop as they pass over the native VM border.
 *
 * @see SQLiteDatabase SQLite Database API
 * @see SQLiteStatement SQLite Statement API
 * @see SQLiteTransaction SQLite Transaction API
 * @see SQLitePrimitiveType SQLite Data Types
 */
@API @ReflectiveAccess public interface SQLiteAPI {
}

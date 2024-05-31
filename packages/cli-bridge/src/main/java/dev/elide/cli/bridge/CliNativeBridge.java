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
package dev.elide.cli.bridge;

/**
 * Bridge to native code from the "umbrella" library.
 */
public class CliNativeBridge {
  /** Token expected for the tooling API at version 1. */
  public static final String VERSION_V1 = "v1";

  /** Say hello from Rust. */
  public static native void hello();

  /** Return the tooling protocol version. */
  public static native String version();

  /** Return the suite of reported tool names. */
  public static native String[] supportedTools();

  /** Return the languages which relate to a given tool. */
  public static native String[] relatesTo(String toolName);

  /** Return the version string for a tool. */
  public static native String toolVersion(String toolName);

  static {
    System.loadLibrary("umbrella");
  }
}

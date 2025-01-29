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
package elide.runtime.gvm.cfg

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.core.util.Toggleable
import elide.runtime.gvm.internals.vfs.AbstractBaseVFS
import elide.runtime.gvm.internals.vfs.GuestVFSPolicy

/**
 * Configuration for the guest VM virtual file-system (VFS).
 */
@Suppress("MemberVisibilityCanBePrivate")
@ConfigurationProperties("elide.gvm.vfs")
public interface GuestIOConfiguration : Toggleable {
  /** Enumerates supported operating modes for VM guest I/O. */
  @Suppress("unused") public enum class Mode {
    /** Virtualized I/O operations via a guest file-system. */
    GUEST,

    /** Access to resources from the host app classpath. */
    CLASSPATH,

    /** Access to host I/O (i.e. regular I/O on the host machine). */
    HOST,
  }

  public companion object {
    /** Default case-sensitivity status. */
    public const val DEFAULT_CASE_SENSITIVE: Boolean = true

    /** Default symbolic links enablement status. */
    public const val DEFAULT_SYMLINKS: Boolean = true

    /** Default root path. */
    public const val DEFAULT_ROOT: String = AbstractBaseVFS.ROOT_SYSTEM_DEFAULT

    /** Default working directory path. */
    public const val DEFAULT_WORKING_DIRECTORY: String = AbstractBaseVFS.DEFAULT_CWD

    /** Default root path. */
    public const val DEFAULT_DEFERRED_READS: Boolean = true

    /** Default policy to apply if none is specified. */
    public val DEFAULT_POLICY: GuestVFSPolicy = GuestVFSPolicy.DEFAULTS

    /** Default operating mode. */
    public val DEFAULT_MODE: Mode = Mode.GUEST
  }

  /**
   * @return Path to the VFS bundle to use for guest VMs, as applicable.
   */
  public val bundle: String? get() = null

  /**
   * @return Security policies to apply to guest I/O operations. If none is provided, sensible defaults are used.
   */
  public val policy: GuestVFSPolicy get() = DEFAULT_POLICY

  /**
   * @return Base operating mode for I/O - options are [Mode.GUEST] (virtualized), [Mode.CLASSPATH] (guest + access to
   *   host app classpath), or [Mode.HOST] (full access to host machine I/O).
   */
  public val mode: Mode get() = DEFAULT_MODE

  /**
   * @return Whether to defer reads until the guest VM requests them. Defaults to `true`.
   */
  public val deferred: Boolean get() = DEFAULT_DEFERRED_READS

  /**
   * @return Whether to treat the file-system as case-sensitive. Defaults to `true`.
   */
  public val caseSensitive: Boolean? get() = DEFAULT_CASE_SENSITIVE

  /**
   * @return Whether to enable (or expect) symbolic link support. Defaults to `true`.
   */
  public val symlinks: Boolean? get() = DEFAULT_SYMLINKS

  /**
   * @return Root path for the file-system. Defaults to `/`.
   */
  public val root: String? get() = DEFAULT_ROOT

  /**
   * @return Working directory to initialize the VFS with, as applicable. Defaults to `/`.
   */
  public val workingDirectory: String? get() = DEFAULT_WORKING_DIRECTORY
}

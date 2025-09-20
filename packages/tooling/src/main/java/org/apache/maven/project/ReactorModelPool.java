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
package org.apache.maven.project;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class ReactorModelPool {
  private final Map<ReactorModelPool.CacheKey, File> pomFiles = new HashMap<>();

  public File get(String groupId, String artifactId, String version) {
    return (File) this.pomFiles.get(new ReactorModelPool.CacheKey(groupId, artifactId, version));
  }

  public void put(String groupId, String artifactId, String version, File pomFile) {
    this.pomFiles.put(
        new org.apache.maven.project.ReactorModelPool.CacheKey(groupId, artifactId, version),
        pomFile);
  }

  private static final class CacheKey {
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final int hashCode;

    CacheKey(String groupId, String artifactId, String version) {
      this.groupId = groupId != null ? groupId : "";
      this.artifactId = artifactId != null ? artifactId : "";
      this.version = version != null ? version : "";
      int hash = 17;
      hash = hash * 31 + this.groupId.hashCode();
      hash = hash * 31 + this.artifactId.hashCode();
      hash = hash * 31 + this.version.hashCode();
      this.hashCode = hash;
    }

    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if (!(obj instanceof org.apache.maven.project.ReactorModelPool.CacheKey)) {
        return false;
      } else {
        org.apache.maven.project.ReactorModelPool.CacheKey that =
            (org.apache.maven.project.ReactorModelPool.CacheKey) obj;
        return this.artifactId.equals(that.artifactId)
            && this.groupId.equals(that.groupId)
            && this.version.equals(that.version);
      }
    }

    public int hashCode() {
      return this.hashCode;
    }

    public String toString() {
      StringBuilder buffer = new StringBuilder(128);
      buffer
          .append(this.groupId)
          .append(':')
          .append(this.artifactId)
          .append(':')
          .append(this.version);
      return buffer.toString();
    }
  }
}

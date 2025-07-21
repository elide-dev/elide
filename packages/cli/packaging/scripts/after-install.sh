#!/usr/bin/env bash

#
# Copyright (c) 2024 Elide Technologies, Inc.
#
# Licensed under the MIT license (the "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
# https://opensource.org/license/mit/
#
# Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
# an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under the License.
#

ELIDE_INSTALL_PREFIX="/opt/elide"
ELIDE_CURRENT_SYM="$ELIDE_INSTALL_PREFIX/current"
INSTALL_LOG_ELIDE="$ELIDE_INSTALL_PREFIX/install.log"

debug() {
  echo "[elide] [DEBUG] $1" >> "$INSTALL_LOG_ELIDE"
}

# remove any previous installation log
if [ -f "$INSTALL_LOG_ELIDE" ]; then
  rm "$INSTALL_LOG_ELIDE"
  debug "Removed previous installation log: $INSTALL_LOG_ELIDE"
fi

# if the path `$ELIDE_INSTALL_PREFIX/.version` does not exist, fail
if [ ! -f "$ELIDE_INSTALL_PREFIX/.version" ]; then
  echo "Error: Version file not found at $ELIDE_INSTALL_PREFIX/.version"
  exit 1
fi

# load version
ELIDE_VERSION=$(cat /opt/elide/.version)

# symlink the current version to the `current` symlink. we can expect it not to exist because it is
# deleted, if present, in the `before-install.sh` script.
if [ ! -L "$ELIDE_CURRENT_SYM" ]; then
  ln -s "$ELIDE_INSTALL_PREFIX/$ELIDE_VERSION" "$ELIDE_CURRENT_SYM"
  debug "Created symlink: $ELIDE_CURRENT_SYM -> $ELIDE_INSTALL_PREFIX/$ELIDE_VERSION"
else
  echo "Symlink already exists: $ELIDE_CURRENT_SYM"
  exit 1
fi

# if HOME is not set, fail
if [ -z "$HOME" ]; then
  # special case: if we are running as root, set it ourselves
  if [ "$(id -u)" -eq 0 ]; then
    HOME="/root"
    debug "HOME environment variable not set, defaulting to /root"
  else
    echo "Error: HOME environment variable is not set and we are not running as root."
    exit 1
  fi
fi

# if the path `$HOME/elide` exists, move it to `$HOME/elide.old`; if `$HOME/elide.old` exists, delete it.
if [ -d "$HOME/elide" ]; then
  if [ -d "$HOME/elide.old" ]; then
    rm -rf "$HOME/elide.old"
    debug "Deleted old elide directory: $HOME/elide.old"
  fi
  mv "$HOME/elide" "$HOME/elide.old"
  debug "Moved existing elide directory to: $HOME/elide.old"
fi

# symlink the current version to `$HOME/elide`
ln -s "$ELIDE_CURRENT_SYM" "$HOME/elide"

# install a symlink into `/usr/bin`
ln -s "$ELIDE_CURRENT_SYM/elide" "/usr/bin/elide"

# if `/opt/elide/current/resources.tgz` exists, unpack it to `$HOME/elide/resources`, and remove the archive.
if [ -f "$ELIDE_CURRENT_SYM/resources.tgz" ]; then
  mkdir -p "$HOME/elide/resources"
  tar -xzf "$ELIDE_CURRENT_SYM/resources.tgz" -C "$HOME/elide/resources"
  rm "$ELIDE_CURRENT_SYM/resources.tgz"
  debug "Unpacked resources to $HOME/elide/resources and removed archive."
else
  debug "No resources archive found at $ELIDE_CURRENT_SYM/resources.tgz"
fi

debug "Elide installation complete. Version: $ELIDE_VERSION"

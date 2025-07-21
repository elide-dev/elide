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

# if the path `/opt/elide/.version-previous` exists, delete it
if [ -f /opt/elide/.version-previous ]; then
  echo "Removing previous version file at /opt/elide/.version-previous"
  rm /opt/elide/.version-previous
fi

# if the path `/opt/elide/.version` exists, move it to `/opt/elide/.version-previous`
if [ -f /opt/elide/.version ]; then
  echo "Moving current version file to previous version file"
  mv /opt/elide/.version /opt/elide/.version-previous
fi

# if the symbolic link at `/opt/elide/current` exists, remove it
if [ -L /opt/elide/current ]; then
  echo "Removing existing symbolic link at /opt/elide/current"
  rm /opt/elide/current
fi

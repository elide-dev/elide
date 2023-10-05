#!/bin/bash

#
# Copyright (c) 2023 Elide Ventures, LLC.
#
# Licensed under the MIT license (the "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
#   https://opensource.org/license/mit/
#
# Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
# an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under the License.
#

latest_known_version="1.0.0-alpha7";

# detect elide command, if it is not here, echo a message
# and exit with a non-zero exit code

# check if the file ./elide does not exist
if [ ! -f ./elide ]; then
    if curl -sSL --tlsv1.2 dl.elide.dev/cli/install.sh | bash -s - \
      --install-dir=./ \
      --no-banner \
      --install-rev="${npm_package_config_elideVersion:-$latest_known_version}"; then
      echo "Elide CLI installed successfully.";
    else
      echo "Failed to install elide CLI. Please file this bug with the Elide team.";
      exit 1;
    fi
fi

# invoke
./elide "$@"

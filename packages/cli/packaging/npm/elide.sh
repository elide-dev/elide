#!/bin/bash

latest_known_version="1.0-v3-alpha4-b11";

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

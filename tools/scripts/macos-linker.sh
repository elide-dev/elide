#!/bin/bash

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

# This is a wrapper to adapt ld64 to gnu style arguments; it is called by `rustc` when linking on macOS. This linker is
# activated by `~/.cargo/config.toml`, which should be symlinked to `~/.cargo/config.macos-<arch>.toml` when building on
# macOS.

# fix from:
# https://github.com/rust-lang/rust/issues/60059#issuecomment-1972748340

# if CC is not defined, force to clang 19
if [ -z "${CC}" ]; then
  export CC=clang-19
fi

declare -a args=()
for arg in "$@"; do
  # options for linker
  if [[ $arg == "-Wl,"* ]]; then
    IFS=',' read -r -a options <<< "${arg#-Wl,}"
    for option in "${options[@]}"; do
      if [[ $option == "-plugin="* ]] || [[ $option == "-plugin-opt=mcpu="* ]]; then
        # ignore -lto_library and -plugin-opt=mcpu
        :
      elif [[ $option == "-plugin-opt=O"* ]]; then
        # convert -plugin-opt=O* to --lto-CGO*
        args[${#args[@]}]="-Wl,--lto-CGO${option#-plugin-opt=O}"
      else
        # pass through other arguments
        args[${#args[@]}]="-Wl,$option"
      fi
    done

  else
    # pass through other arguments
    args[${#args[@]}]="$arg"
  fi
done

# use clang to call ld64
exec "${CC}" -v "${args[@]}"

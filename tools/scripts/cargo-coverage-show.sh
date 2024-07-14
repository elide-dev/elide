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

llvm-cov show \
  $(
    for file in \
      $(
        RUSTFLAGS="-C instrument-coverage" \
          cargo test --tests --no-run --message-format=json \
          | jq -r "select(.profile.test == true) | .filenames[]" \
          | grep -v dSYM -
      ); do
      printf "%s %s " -object $file
    done
  ) \
  --use-color \
  --ignore-filename-regex='/.cargo/registry' \
  --ignore-filename-regex='/.cargo/git' \
  --ignore-filename-regex='builder' \
  --ignore-filename-regex='rustc' \
  --ignore-filename-regex='out/' \
  --instr-profile=./target/profiles/elide-rust.profdata \
  --show-instantiations \
  --show-line-counts-or-regions \
  --Xdemangler=rustfilt \
  | less -R

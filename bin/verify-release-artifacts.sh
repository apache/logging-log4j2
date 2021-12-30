#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

set -euo pipefail
IFS=$'\n\t'

help() {
  cat <<EOF
This shell script downloads release artifacts and verifies their
signatures and hashes.

Usage: $0 [-h] [-t] [-p <work_dir>]

-h             Shows this help menu.
-t             Creates a temporary working directory, performs the
               download and verification, deletes the directory at
               exit.
-p <work_dir>  Uses the provided permanent directory to download the
               artifacts and verify them.
EOF
}

fail_due_to_invalid_arguments() {
  echo "Invalid arguments."
  echo
  help
  exit 1
}

# Determine the working directory.
if [ "$#" -eq 0 ]; then
  fail_due_to_invalid_arguments
elif [ "$#" -eq 1 ] && [ "$1" == "-h" ]; then
  help
  exit 0
elif [ "$#" -eq 1 ] && [ "$1" == "-t" ]; then
  work_dir="$(mktemp -d)"
  trap 'rm -rf -- "$work_dir"' EXIT
  echo "Using temporary working directory: $work_dir"
elif [ "$#" -eq 2 ] && [ "$1" == "-p" ]; then
  work_dir="$2"
  echo "Using permanent working directory: $work_dir"
else
  fail_due_to_invalid_arguments
fi
cd "$work_dir"

# Download and import GPG keys.
echo "Downloading GPG keys..."
wget \
  --quiet \
  --timestamping \
  https://dist.apache.org/repos/dist/release/logging/KEYS
echo "Importing GPG keys..."
gpg --quiet --import KEYS

# Download artifacts.
echo "Downloading release artifacts..."
wget \
  --quiet \
  --execute robots=off \
  --timestamping \
  --cut-dirs=7 \
  --no-host-directories \
  --recursive \
  --page-requisites \
  --no-parent \
  --no-check-certificate \
  https://dist.apache.org/repos/dist/dev/logging/log4j/

# Exit code utilities.
function set_exit_code() { echo "$1" >.exit_code; }
function get_exit_code() { [ -e .exit_code ] && head -n1 .exit_code || echo 0; }

# Reset the exit code.
set_exit_code 0

# Check signatures.
echo "Checking signatures..."
asc_file_count=0
while read -r asc_file; do
  gpg --verify "$asc_file" 2>&1 | grep -q "Good sig" || {
    echo "Signature could not be verified: $asc_file"
    set_exit_code 1
  }
  (( asc_file_count+=1 ))
done <<< "$(find . -type f -name "*.asc")"
echo "Checked signature file count: $asc_file_count"

# Check hashes.
echo "Checking hashes..."
sha_file_count=0
for sha_alg in 256 512; do
  while read -r sha_file; do
    # Due to a Maven plugin mishap, some of the generated hash files do not contain the filename in the 2nd column.
    # This breaks `shasum --check` operation.
    # Hence here we check hashes manually.
    expected_hash="$(head -n1 "$sha_file" | cut -d' ' -f1)"
    org_file="${sha_file%.sha$sha_alg}"
    actual_hash="$(shasum --algorithm $sha_alg "$org_file" | head -n1  | cut -d' ' -f1)"
    if [ "$expected_hash" != "$actual_hash" ]; then
      echo "Hash could not be verified: $sha_file"
      set_exit_code 1
    fi
  (( sha_file_count+=1 ))
  done <<< "$(find . -type f -name "*.sha$sha_alg")"
done
echo "Checked hash file count: $sha_file_count"

# Exit with the set code.
exit_code="$(get_exit_code)"
if [ "$exit_code" -eq 0 ]; then
  echo "All checks are succeeded."
else
  echo "Some checks were failed!"
  exit "$exit_code"
fi

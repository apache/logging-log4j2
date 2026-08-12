#!/usr/bin/env bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to you under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
##
#
# Verifies that Jakarta module JARs do not reference legacy javax EE packages
# in their main artifact bytecode (javax.servlet, javax.jms, javax.mail,
# javax.persistence). javax.annotation and other non-EE javax packages are
# intentionally allowed.
#
# Usage:
#   tools/validate-jakarta-namespace.sh [jar-file...]
#
# When no JAR paths are supplied, discovers main artifacts for every
# log4j-jakarta-* module under the repository root.
#
# Exit codes:
#   0 — all scanned JARs are namespace-pure
#   1 — one or more violations were found, or a JAR could not be scanned
#

set -euo pipefail

readonly BANNED_PATTERN='javax\.(servlet|jms|mail|persistence)(\.|/|$)'

if ! command -v jdeps >/dev/null 2>&1; then
  echo "ERROR: jdeps is required but was not found on PATH." >&2
  exit 1
fi

discover_jakarta_jars() {
  local root="${1:-.}"
  find "$root" -mindepth 1 -maxdepth 1 -type d -name 'log4j-jakarta-*' -print \
    | while IFS= read -r module_dir; do
        find "$module_dir/target" -maxdepth 1 -type f -name '*.jar' \
          ! -name '*-sources.jar' \
          ! -name '*-javadoc.jar' \
          ! -name '*-tests.jar' \
          ! -name '*-test.jar' \
          2>/dev/null || true
      done \
    | sort -u
}

scan_jar() {
  local jar="$1"
  local module_name
  local violations
  module_name="$(basename "$(dirname "$(dirname "$jar")")")"

  if [[ ! -f "$jar" ]]; then
    echo "ERROR: JAR not found: ${jar}" >&2
    return 1
  fi

  violations="$(
    jdeps --multi-release base -verbose:class "$jar" 2>/dev/null \
      | grep -E "${BANNED_PATTERN}" \
      || true
  )"

  if [[ -n "$violations" ]]; then
    echo "ERROR: ${module_name} (${jar}) references banned javax EE packages:" >&2
    echo "$violations" >&2
    return 1
  fi

  echo "OK: ${module_name} (${jar})"
  return 0
}

main() {
  local jars=()
  local jar
  local failed=0

  if [[ $# -gt 0 ]]; then
    jars=("$@")
  else
    while IFS= read -r jar; do
      [[ -n "$jar" ]] && jars+=("$jar")
    done < <(discover_jakarta_jars ".")
    if [[ ${#jars[@]} -eq 0 ]]; then
      echo "ERROR: No log4j-jakarta-* JARs were found. Build the Jakarta modules first." >&2
      exit 1
    fi
  fi

  for jar in "${jars[@]}"; do
    if ! scan_jar "$jar"; then
      failed=1
    fi
  done

  if [[ "$failed" -ne 0 ]]; then
    echo "Jakarta namespace validation failed." >&2
    exit 1
  fi

  echo "Jakarta namespace validation passed for ${#jars[@]} JAR(s)."
}

main "$@"

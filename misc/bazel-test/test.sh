#!/usr/bin/env bash
#
# Test Bazel support in log4j2.
#   Run this script after `mvn clean package`.

set -o errexit

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "${DIR}"

mkdir -p "${DIR}/jars"
cp "${DIR}/../../log4j-api/target/log4j-api-2.13.0-SNAPSHOT.jar" jars/
cp "${DIR}/../../log4j-core/target/log4j-core-2.13.0-SNAPSHOT.jar" jars/
bazel build //...


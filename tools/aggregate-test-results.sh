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
# Aggregates Maven Surefire and Failsafe JUnit XML reports across the reactor,
# computes the overall pass rate, and writes a markdown summary.
#
# Usage:
#   tools/aggregate-test-results.sh [search-root]
#
# Exit codes:
#   0 — pass rate >= 99.5%, or >= 99.0% with a workflow warning annotation
#   1 — pass rate < 99.0%, or no test reports were found
#
# Thresholds:
#   warn  — pass rate < 99.5%
#   fail  — pass rate < 99.0%
#

set -euo pipefail

readonly WARN_THRESHOLD="99.5"
readonly FAIL_THRESHOLD="99.0"

SEARCH_ROOT="${1:-.}"
SUMMARY_FILE="${GITHUB_STEP_SUMMARY:-}"
METRICS_FILE="$(mktemp)"
trap 'rm -f "$METRICS_FILE"' EXIT

find "$SEARCH_ROOT" \( \
  -path '*/target/surefire-reports/TEST-*.xml' -o \
  -path '*/target/failsafe-reports/TEST-*.xml' \
\) -print 2>/dev/null | sort > "${METRICS_FILE}.reports"

if [[ ! -s "${METRICS_FILE}.reports" ]]; then
  if [[ -n "$SUMMARY_FILE" ]]; then
    {
      echo "## Test pass rate summary"
      echo
      echo "No Surefire or Failsafe TEST-*.xml reports were found under \`${SEARCH_ROOT}\`."
    } >> "$SUMMARY_FILE"
  fi
  echo "::error title=Missing test reports::No Surefire or Failsafe reports were found to aggregate."
  rm -f "${METRICS_FILE}.reports"
  exit 1
fi

awk -v root="$SEARCH_ROOT" -f - "${METRICS_FILE}.reports" > "$METRICS_FILE" <<'AWK'
function attr(line, name,    value) {
  if (match(line, name "=\"[0-9]+\"")) {
    value = substr(line, RSTART + length(name) + 2, RLENGTH - length(name) - 3)
    return value + 0
  }
  return 0
}
function module_from_path(path,    parts, n) {
  sub("^" root "/", "", path)
  sub("^\\./", "", path)
  n = split(path, parts, "/")
  for (i = 1; i <= n; i++) {
    if (parts[i] == "target") {
      return parts[i - 1]
    }
  }
  return "unknown"
}
{
  module = module_from_path($0)
  while ((getline line < $0) > 0) {
    if (index(line, "<testsuite") > 0) {
      tests = attr(line, "tests")
      failures = attr(line, "failures")
      errors = attr(line, "errors")
      skipped = attr(line, "skipped")

      total_tests += tests
      total_failures += failures
      total_errors += errors
      total_skipped += skipped
      report_count++

      mod_tests[module] += tests
      mod_failures[module] += failures
      mod_errors[module] += errors
      mod_skipped[module] += skipped
      break
    }
  }
  close($0)
}
END {
  executed = total_tests - total_skipped
  passed = executed - total_failures - total_errors
  pass_rate = (executed == 0) ? "100.00" : sprintf("%.2f", (passed / executed) * 100)

  print "report_count=" report_count
  print "total_tests=" total_tests
  print "total_failures=" total_failures
  print "total_errors=" total_errors
  print "total_skipped=" total_skipped
  print "passed=" passed
  print "executed=" executed
  print "pass_rate=" pass_rate

  for (module in mod_tests) {
    mod_executed = mod_tests[module] - mod_skipped[module]
    mod_passed = mod_executed - mod_failures[module] - mod_errors[module]
    print "module|" module "|" mod_tests[module] "|" mod_passed "|" mod_failures[module] "|" mod_errors[module] "|" mod_skipped[module]
  }

  for (module in mod_tests) {
    issue_count = mod_failures[module] + mod_errors[module]
    if (issue_count > 0) {
      print "failure|" issue_count "|" module "|" mod_failures[module] "|" mod_errors[module]
      print "failing|" module " (" mod_failures[module] " failed, " mod_errors[module] " errored)"
    }
  }
}
AWK

# shellcheck disable=SC1090
eval "$(grep -E '^[a-z_]+=' "$METRICS_FILE")"

write_summary() {
  echo "## Test pass rate summary"
  echo
  echo "| Metric | Count |"
  echo "| --- | ---: |"
  echo "| Report files | ${report_count} |"
  echo "| Total tests | ${total_tests} |"
  echo "| Passed | ${passed} |"
  echo "| Failed | ${total_failures} |"
  echo "| Errored | ${total_errors} |"
  echo "| Skipped | ${total_skipped} |"
  echo "| **Pass rate** | **${pass_rate}%** |"
  echo

  if grep -q '^module|' "$METRICS_FILE"; then
    echo "### Per-module results"
    echo
    echo "| Module | Tests | Passed | Failed | Errored | Skipped |"
    echo "| --- | ---: | ---: | ---: | ---: | ---: |"
    grep '^module|' "$METRICS_FILE" | cut -d'|' -f2- | sort -t'|' -k1,1 | while IFS='|' read -r module mod_tests mod_passed mod_failures mod_errors mod_skipped; do
      echo "| ${module} | ${mod_tests} | ${mod_passed} | ${mod_failures} | ${mod_errors} | ${mod_skipped} |"
    done
    echo
  fi

  if grep -q '^failure|' "$METRICS_FILE"; then
    echo "### Modules with the most failures"
    echo
    echo "| Module | Failed | Errored |"
    echo "| --- | ---: | ---: |"
    grep '^failure|' "$METRICS_FILE" | sort -t'|' -k2,2nr | head -5 | while IFS='|' read -r _ _ module mod_failures mod_errors; do
      echo "| ${module} | ${mod_failures} | ${mod_errors} |"
    done
    echo
  fi
}

if [[ -n "$SUMMARY_FILE" ]]; then
  write_summary >> "$SUMMARY_FILE"
else
  write_summary
fi

echo "Aggregated ${report_count} report files: ${passed}/${executed} passed (${pass_rate}%)."

failing_modules="$(grep '^failing|' "$METRICS_FILE" | cut -d'|' -f2- | paste -sd '; ' - || true)"
if [[ -z "$failing_modules" ]]; then
  failing_modules="none identified"
fi

if awk -v rate="$pass_rate" -v threshold="$FAIL_THRESHOLD" 'BEGIN { exit !(rate + 0 < threshold + 0) }'; then
  echo "::error title=Test pass rate below ${FAIL_THRESHOLD}%::Overall pass rate is ${pass_rate}% (${passed}/${executed} passed). Failing modules: ${failing_modules}"
  exit 1
fi

if awk -v rate="$pass_rate" -v threshold="$WARN_THRESHOLD" 'BEGIN { exit !(rate + 0 < threshold + 0) }'; then
  echo "::warning title=Test pass rate below ${WARN_THRESHOLD}%::Overall pass rate is ${pass_rate}% (${passed}/${executed} passed)."
fi

exit 0

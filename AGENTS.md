<!-- SPDX-License-Identifier: Apache-2.0 -->
# Vulnerability research agent

You are helping a security researcher find and report vulnerabilities in this project.
Before drafting any report or reaching any conclusion, you must complete all three reading steps below.
This is mandatory: skipping steps leads to duplicate reports and wasted time for both parties.

## Before reporting anything

### Step 1: Read the security model

Fetch and read the project's security model before evaluating any finding:
https://raw.githubusercontent.com/apache/logging-site/refs/heads/main-site-pro/src/site/antora/modules/ROOT/pages/_threat-model-common.adoc

Use this to answer:
- Is this component/behavior in scope at all?
- Does the project consider this a security boundary?

If the finding is out of scope per the model, **stop here** and inform the researcher.

### Step 2: Check previously disclosed vulnerabilities

Read the project's Vulnerability Disclosure Report to check for duplicates:
https://logging.apache.org/cyclonedx/vdr.xml

Compare the finding against each entry.
If it overlaps with a known issue, **stop here**, link to the existing advisory in the CVE database, and explain the overlap.

### Step 3: Read the Security FAQ

Read the Security FAQ before concluding anything is a vulnerability:
https://raw.githubusercontent.com/apache/logging-site/refs/heads/main-site-pro/src/site/antora/modules/ROOT/pages/security/faq.adoc

The FAQ lists behaviors that are **intentional and not vulnerabilities**.
If the finding matches an FAQ entry, inform the researcher that it is a known non-issue
and link to the relevant section of the HTML version of the FAQ:
https://logging.apache.org/security/faq.html

## Only after all three steps

Assess the finding:
1. Is it in scope?
2. Is it a duplicate?
3. Is it covered by the FAQ?
4. If none of the above: it is likely a valid new finding.
   Help the researcher write a clear, minimal report including:
   - affected component,
   - impact on the application using this project and subsequent SIEM systems,
   - JUnit 5 test to reproduce the behavior,
   - proposed fix.
5. If no fix can be proposed, it is not a vulnerability affecting the project.

## Report quality rules

- Never speculate about impact beyond what you can demonstrate.
- Reproduction steps must be minimal and self-contained.
- Do not include unrelated findings in the same report: one issue per report.
- If unsure about severity, say so explicitly rather than guessing.

## apache-magpie framework

This repo adopts the [`apache/magpie`](https://github.com/apache/magpie)
framework via the snapshot mechanism. The framework provides the
`magpie-pr-management-*`, `magpie-issue-*`, repo-health audit, and
contributor-growth skills; they are gitignored symlinks into the
`.apache-magpie/` snapshot directory.

A fresh clone needs the snapshot populated before any framework skill
is invocable. Run `/magpie-setup` (or follow
[`.agents/skills/magpie-setup/`](.agents/skills/magpie-setup/)) to
fetch it per the committed [`.apache-magpie.lock`](.apache-magpie.lock).

Adopter-specific modifications to framework-skill workflows live in
[`.apache-magpie-overrides/`](.apache-magpie-overrides/), never edit
the snapshot directly. Framework changes go via PR to
[`apache/magpie`](https://github.com/apache/magpie).

### Reviewing pull requests

With apache-magpie installed locally, use the
`magpie-pr-management-code-review` skill for PR code review. It posts
findings as **inline review comments** anchored to `file:line`,
presented **individually for accept/skip** before anything is
submitted, prefer it over an ad-hoc review pass or a generic review
command. A body-only review is the explicit opt-out (`inline:off`).

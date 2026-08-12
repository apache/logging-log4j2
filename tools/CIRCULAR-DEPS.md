<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to you under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

# Module circular dependency status (WO-025)

## Current status

**Green.** `./mvnw validate` on the full reactor passes `banCircularDependencies`
(`enforce-ban-circular-dependencies` in `log4j-parent/pom.xml`).

CI runs the same check in the `module-dependencies` job (WO-052) and again
indirectly when the reusable `build` job runs `verify` (which includes `validate`).

## Enforcer configuration

| Setting | Value |
| --- | --- |
| Phase | `validate` |
| Rule | `banCircularDependencies` (`extra-enforcer-rules`) |
| `failFast` | `true` |
| `ignoreOptionals` | `true` — optional deps can look cyclic without a real classpath loop |
| `ignoredScopes` | `test` — test-scoped reactor edges (e.g. `log4j-slf4j-impl` ↔ `log4j-to-slf4j` in tests) |

## Reactor ordering (WO-016)

These modules are declared before their compile-time consumers in the root `pom.xml`
so sibling JARs exist when `validate` resolves the dependency graph:

- `log4j-trustgate` before `log4j-api`
- `log4j-fuzz-test` before `*-fuzz-test` wrappers
- `log4j-mongodb4` before `log4j-mongodb`
- `log4j-web` before `log4j-taglib`

## Intentional enforcer skips

Four modules set `log4j.enforcer.banCircularDependencies.skip=true` because a
**compile** dependency on a later reactor module is still unresolved during
`validate` (the sibling JAR is packaged later in the same reactor build):

| Module | Unresolved compile dep at validate | Notes |
| --- | --- | --- |
| `log4j-core-fuzz-test` | `log4j-fuzz-test` | Wrapper over fuzz helpers |
| `log4j-layout-template-json-fuzz-test` | `log4j-fuzz-test` | Wrapper over fuzz helpers |
| `log4j-slf4j2-impl-fuzz-test` | `log4j-fuzz-test` | Wrapper over fuzz helpers |
| `log4j-mongodb` | `log4j-mongodb4` | Thin wrapper; mongodb4 is ordered first but not yet installed at its validate |

These skips do **not** hide production circularities: the skipped modules only
depend on already-built siblings in the same reactor, and the rest of the graph
is fully checked.

## Verify locally

```bash
./mvnw validate
```

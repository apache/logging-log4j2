# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

&lsqb;A clear and concise description of what the pull request is for along with a reference to the associated issue IDs, if they exist.&rsqb;

## Checklist

* `./mvnw verify` succeeds (if it fails due to code formatting issues reported by Spotless, simply run `spotless:apply` and retry)
* Changes contain an entry file in the `src/changelog/.2.x.x` directory
* Tests for the changes are provided
* [Commits are signed](https://docs.github.com/en/authentication/managing-commit-signature-verification/signing-commits) (optional, but highly recommended)

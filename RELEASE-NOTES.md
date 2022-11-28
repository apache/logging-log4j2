<!--
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements. See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

Changelog sources are stored in [`src/changelog`](src/changelog) directory.

AsciiDoc-formatted changelogs are automatically generated during Maven `package` phase and exported to [`src/site/asciidoc/changelog`](src/site/asciidoc/changelog).
Though these exported AsciiDoc files are not committed, since they are only relevant for the website, and they cause merge-conflicts between feature branches.
`./mvnw -pl log4j-internal-util` can be used to manually generate these files.

See [`src/changelog/README.adoc`](src/changelog/README.adoc) for details.

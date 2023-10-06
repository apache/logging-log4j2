////
    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements.  See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License.  You may obtain a copy of the License at

         https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
////

[#release-notes-${release.version?replace("[^a-zA-Z0-9]", "-", "r")}]
== ${release.version}

<#if release.date?has_content>Release date:: ${release.date}</#if>

This release primarily focuses on enhancements to our OSGi and JPMS support and contains several bug fixes.
It will be the first release built and signed by the CI using the https://keyserver.ubuntu.com/pks/lookup?search=077E8893A6DCC33DD4A4D5B256E73BA9A0B592D0&op=index[ASF Logging Services Release Manager GPG key], which is shared in https://www.apache.org/dist/logging/KEYS[KEYS].

The Log4j 2.21.0 API, as well as the other artifacts, maintains binary compatibility with the previous release.

Apache Log4j 2.21.0 requires Java 8 to run.
The build requires JDK 11 and generates reproducible binaries.

For complete information on Apache Log4j 2, including instructions on how to submit bug reports, patches, get support, or suggestions for improvement, see http://logging.apache.org/log4j/2.x/[the Apache Log4j 2 website].

=== OSGi changes

All the published artifacts are OSGi bundles or fragments.

This release introduces a change in the bundle symbolic names to allow them to function as JPMS module name: all hyphens `-` present in the bundle names of previous releases were replaced by dots `.`.

=== JPMS changes

All the published artifacts have been migrated from automatic modules to named JPMS modules.
All packages marked as private in the Javadoc are not exported.

The module name of four bridges (`log4j-slf4j-impl`, `log4j-slf4j2-impl`, `log4j-to-jul` and `log4j-to-slf4j`) have been changed to adhere to the same convention as the OSGi bundle names.

<#include "../.changelog.adoc.ftl">

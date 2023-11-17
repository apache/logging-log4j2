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

:cyclonedx-sbom-link: https://cyclonedx.org/capabilities/sbom/[CycloneDX Software Bill of Materials (SBOM)]
:cyclonedx-vdr-link: https://cyclonedx.org/capabilities/vdr[CycloneDX Vulnerability Disclosure Report (VDR)]

This releases provides a {cyclonedx-sbom-link} along with each artifact and contains bug fixes addressing issues in the JPMS & OSGi infrastructure overhauled in `2.21.0`, dependency updates, and some other minor fixes and improvements.

[#release-notes-${release.version?replace("[^a-zA-Z0-9]", "-", "r")}-sbom]
=== CycloneDX Software Bill of Materials (SBOM)

This is _the first Log4j release_ that provides a {cyclonedx-sbom-link} along with each artifact.
Generated SBOMs are attached as artifacts with `cyclonedx` classifier and XML extensions, that is, `<artifactId>-<version>-cyclonedx.xml`.
They contain `vulnerability-assertion` references to a {cyclonedx-vdr-link} that Apache Logging Services uses for all projects it maintains.
This VDR is accessible through the following URL: https://logging.apache.org/cyclonedx/vdr.xml[]

SBOM generation is streamlined by `logging-parent`, see https://logging.apache.org/logging-parent/latest/#cyclonedx-sbom[its website] for details.

<#include "../.changelog.adoc.ftl">

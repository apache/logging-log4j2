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

This releases contains ...

=== SLF4J implementations (bridges)

To prepare for the migration between Log4j Core 2 and Log4j Core 3, the
xref:components.adoc#log4j-slf4j-impl[`log4j-slf4j-impl`]
and
xref:components.adoc#log4j-slf4j-impl[`log4j-slf4j2-impl`]
bridges between SLF4J and Log4j API, no longer depend on Log4j Core 2.
Users should specify the logging implementation of their choice explicitly in their dependency manager:

[tabs]
====
Maven::
+
We assume you use xref:components.adoc#log4j-bom[`log4j-bom`] for dependency management.
+
[source,xml]
----
<!-- Bridge from SLF4J 2 to Log4j API -->
<dependency>
  <groupId>org.apache.logging.log4j</groupId>
  <artifactId>log4j-slf4j2-impl</artifactId>
  <scope>runtime</scope>
</dependency>

<!-- Log4j API implementation -->
<dependency>
  <groupId>org.apache.logging.log4j</groupId>
  <artifactId>log4j-core</artifactId>
  <scope>runtime</scope>
</dependency>
----

Gradle::
+
We assume you use xref:components.adoc#log4j-bom[`log4j-bom`] for dependency management.
+
[source,groovy]
----
// Bridge from SLF4J 2 to Log4j API
runtimeOnly 'org.apache.logging.log4j:log4j-slf4j2-impl'
// Log4j API implementation
runtimeOnly 'org.apache.logging.log4j:log4j-core'
----
====

See xref:manual/installation.adoc#impl-core[Installing Log4j Core] for more details.

<#include "../.changelog.adoc.ftl">

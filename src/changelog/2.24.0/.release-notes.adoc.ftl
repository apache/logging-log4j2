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

This release contains improvements and changes in several areas of Apache Log4j:

=== Log4j API

The `2.24.0` version of Log4j API has been enhanced with changes from the 3.x branch and will be used by both Log4j 2 Core and Log4j 3 Core releases.
The changes include:

* A faster default `ThreadContextMap`.
* Enhanced GraalVM support: native binaries that use Log4j API will no longer require additional GraalVM configuration.
* The configuration properties subsystem now only accepts the official pre-2.10 property names and the normalized post-2.10 names.
Check your configuration for typos.

=== Documentation

The xref:index.adoc[Apache Log4j 2] website has been almost entirely rewritten to provide improved documentation and faster access to the information you need.

=== Bridges

The JUL-to-Log4j API and Log4j 1-to-Log4j API will no longer be able to modify the configuration of Log4j Core by default.
If such a functionality is required, it must be explicitly enabled.

=== Modules

The following Log4j Core additional modules have been removed:

`log4j-flume-ng`::
The module is no longer part of the release process and will follow its own release lifecycle.
Please manage your dependencies using xref:components.adoc#log4j-bom[`log4j-bom`] to always use its latest version.

`log4j-kubernetes`::
The module has been moved to the https://github.com/fabric8io/kubernetes-client/blob/main/doc/KubernetesLog4j.md[Fabric8.io Kubernetes project] and follows the Fabric8.io release lifecycle.

`log4j-mongodb3`::
The module based on MongoDB Java client version 3.x has been removed.
Please migrate to xref:components.adoc#log4j-mongodb[`log4j-mongodb`] (client version 5.x) or xref:components.adoc#log4j-mongodb4[`log4j-mongodb4`] (client version 4.x).

=== JMX changes

Starting in version 2.24.0, JMX support is disabled by default and can be re-enabled via the `log4j2.disableJmx=false` system property.

<#include "../.changelog.adoc.ftl">

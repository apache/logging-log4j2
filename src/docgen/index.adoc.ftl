<#ftl output_format="plainText" strip_whitespace=true>
<#--
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at

https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

// ToC does not look nice with a deep sectioning, override it:
:page-toclevels: 3

<#-- @ftlvariable name="lookup" type="org.apache.logging.log4j.docgen.generator.TypeLookup" -->

= Plugin reference

This page is a Javadoc-on-steroids specialized for Log4j plugins.
This reference manual is derived from the source code of all Log4j plugins and types associated with them.
You can use this reference manual to precisely customize your `log4j2.xml`.

[TIP]
====
Every running Log4j system is a constellation of link:manual/plugins.html[plugins], which is analogous to beans in Java EE and Spring.
This not only allows Log4j itself to be developed in individual components, but also enables extensibility users can leverage.
====

[#shortcuts]
== Shortcuts

* xref:#org-apache-logging-log4j_log4j-core_org-apache-logging-log4j-core-config-Configuration[The `<Configuration>` element assembly in a `log4j2.xml`]
* xref:#org-apache-logging-log4j_log4j-core_org-apache-logging-log4j-core-Appender[The type hierarchy of *appenders*]
* xref:#org-apache-logging-log4j_log4j-core_org-apache-logging-log4j-core-Layout[The type hierarchy of *layouts*]
* xref:#org-apache-logging-log4j_log4j-core_org-apache-logging-log4j-core-Filter[The type hierarchy of *filters*]

[#index]
== Index

Below is a list of all types reachable by plugins grouped by the Maven coordinate of the artifact bundling them.

<#assign sourcedTypes = lookup?values/>
<#-- @ftlvariable name="sourcedTypes" type="org.apache.logging.log4j.docgen.generator.ArtifactSourcedType[]" -->
<#assign lastGroupId = ''/>
<#assign lastArtifactId = ''/>
<#list sourcedTypes?sort_by('artifactId', 'groupId', ['type', 'className']) as sourcedType>
    <#if sourcedType.groupId != lastGroupId || sourcedType.artifactId != lastArtifactId>
        <#assign lastGroupId = sourcedType.groupId/>
        <#assign lastArtifactId = sourcedType.artifactId/>

[#${sourcedType.groupId?replace('.', '-')}_${sourcedType.artifactId?replace('.', '-')}]
=== `${sourcedType.artifactId}`
    </#if>

include::_plugin-reference/${sourcedType.groupId}-${sourcedType.artifactId}-${sourcedType.type.className}.adoc[leveloffset=+3]
</#list>

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
<#-- @ftlvariable name="lookup" type="org.apache.logging.log4j.docgen.generator.TypeLookup" -->
<#include "license.adoc.ftl">
[#index]
= Index
<#assign sourcedTypes = lookup?values/>
<#-- @ftlvariable name="sourcedTypes" type="org.apache.logging.log4j.docgen.generator.ArtifactSourcedType[]" -->
<#assign lastGroupId = ''/>
<#assign lastArtifactId = ''/>
<#list sourcedTypes?sort_by('artifactId', 'groupId', ['type', 'className']) as sourcedType>
    <#if sourcedType.groupId != lastGroupId || sourcedType.artifactId != lastArtifactId>
        <#assign lastGroupId = sourcedType.groupId/>
        <#assign lastArtifactId = sourcedType.artifactId/>

[#${sourcedType.groupId?replace('.', '_')}-${sourcedType.artifactId?replace('.', '_')}]
== `${sourcedType.groupId}:${sourcedType.artifactId}`

    </#if>
* xref:plugin-reference/${sourcedType.groupId}/${sourcedType.artifactId}/${sourcedType.type.className}.adoc[`${sourcedType.type.className}`]
</#list>

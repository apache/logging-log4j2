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
<#-- @ftlvariable name="sourcedTypes" type="org.apache.logging.log4j.docgen.generator.ArtifactSourcedType[]" -->
<#include "license.adoc.ftl">
[#type-converters]
= Type converters

Type converter plugins are used to convert simple `String` values into other types.
<#list sourcedTypes as sourcedType>
<#assign scalar = sourcedType.type/>
<#-- @ftlvariable name="scalar" type="org.apache.logging.log4j.docgen.model.ScalarType" -->

[#${scalar.className?replace('.', '_')}]
== `${scalar.className}`

    <#if sourcedType.groupId?has_content && sourcedType.artifactId?has_content>
Provider:: `${sourcedType.groupId}:${sourcedType.artifactId}`
    </#if>

${(scalar.description.text)!}
    <#if scalar.values?size != 0>

[#${scalar.className?replace('.', '_')}-values]
=== Possible values

        <#list scalar.values as value>
`${value.name}`:: ${(value.description.text)!}
        </#list>
    </#if>
</#list>

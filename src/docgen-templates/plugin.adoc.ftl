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
<#-- @ftlvariable name="sourcedType" type="org.apache.logging.log4j.docgen.model.ArtifactSourcedType" -->
<#assign type = sourcedType.type/>
<#-- @ftlvariable name="type" type="org.apache.logging.log4j.docgen.PluginType" -->
<#-- @ftlvariable name="lookup" type="org.apache.logging.log4j.docgen.generator.TypeLookup" -->
<#include "license.adoc.ftl">
[#${type.className?replace('.', '_')}]
= ${type.name}

Class:: `${type.className}`
<#if sourcedType.groupId?has_content && sourcedType.artifactId?has_content>
Provider:: `${sourcedType.groupId}:${sourcedType.artifactId}`

</#if>${(type.description.text)!'N/A'}

[#${type.className?replace('.', '_')}-XML-snippet]
== XML snippet
[source, xml]
----
<#assign tag><${type.name} </#assign>
<#assign indent = tag?replace('.', ' ', 'r')/>
<#assign has_elements = type.elements?size != 0/>
<#if !type.attributes?has_content>
<${type.name}/>
    <#else>
        <#list type.attributes?sort_by('name') as attr>
            <#if attr?is_first>
${tag}${attr.name}="${attr.defaultValue!}"${attr?is_last?then(has_elements?then('>', '/>'), '')}
            <#else>
${indent}${attr.name}="${attr.defaultValue!}"${attr?is_last?then(has_elements?then('>', '/>'), '')}
            </#if>
        </#list>
        <#if has_elements>
            <#list type.elements as element>
                <#if lookup[element.type]??>
                    <#assign element_type = lookup[element.type].type/>
                    <#-- @ftlvariable name="element_type" type="org.apache.logging.log4j.docgen.model.AbstractType" -->
                    <#if element_type.name?? && !element_type.implementations?has_content>
                    <#-- @ftlvariable name="element_type" type="org.apache.logging.log4j.docgen.model.PluginType" -->
    <${element_type.name}/>
                    <#else>
    <a-${element.type?keep_after_last('.')}-implementation/><#if element.multiplicity == '*'><!-- multiple occurrences allowed --></#if>
                    </#if>
                </#if>
            </#list>
</${type.name}>
        </#if>
</#if>
----
<#if type.attributes?has_content>

[#${type.className?replace('.', '_')}-attributes]
== Attributes

Required attributes are in **bold face**.

[cols="1m,1m,1m,5"]
|===
|Name|Type|Default|Description

    <#list type.attributes?sort_by('name') as attr>
|${attr.required?then('**', '')}${attr.name}${attr.required?then('**', '')}
|xref:../../scalars.adoc#${attr.type?replace('.', '_')}[${attr.type?contains('.')?then(attr.type?keep_after_last('.'), attr.type)}]
|${attr.defaultValue!}
a|${(attr.description.text)!'N/A'}

    </#list>
|===
</#if>
<#if has_elements>

[#${type.className?replace('.', '_')}-components]
== Nested components

Required components are in **bold face**.

[cols="1m,1m,5"]
|===
|Tag|Type|Description

    <#list type.elements?sort_by('type') as element>
        <#if lookup[element.type]??>
|${element.required?then('**', '') + (lookup[element.type].type.name!'N/A') + element.required?then('**', '')}
|xref:${element.type}.adoc[${element.type?contains('.')?then(element.type?keep_after_last('.'), element.type)}]
a|${(element.description.text)!'N/A'}

        </#if>
    </#list>
|===
</#if>
<#if type.implementations?has_content>

[#${type.className?replace('.', '_')}-implementations]
== Known implementations

    <#list type.implementations as impl>
* xref:${impl}.adoc[${impl?contains('.')?then(impl?keep_after_last('.'), impl)}]
    </#list>
</#if>

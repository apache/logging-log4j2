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

</#if>${(type.description.text)!}

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
                <#assign multiplicitySuffix = (element.multiplicity == '*')?then('<!-- multiple occurrences allowed -->','')/>
                <#assign elementName = 'a-' + element.type?keep_after_last('.') + '-implementation'/>
                <#if lookup[element.type]??>
                    <#assign element_type = lookup[element.type].type/>
                    <#-- @ftlvariable name="element_type" type="org.apache.logging.log4j.docgen.model.AbstractType" -->
                    <#if element_type.name?? && !element_type.implementations?has_content>
                        <#assign elementName = element_type.name/>
                    </#if>
                </#if>
    <${elementName}/>${multiplicitySuffix}
            </#list>
</${type.name}>
        </#if>
</#if>
----
<#if type.attributes?has_content>

[#${type.className?replace('.', '_')}-attributes]
== Attributes

Optional attributes are denoted by `?`-suffixed types.

[cols="1m,1m,1m,5"]
|===
|Name|Type|Default|Description

    <#list type.attributes?sort_by('name') as attr>
        <#assign requirementSuffix = attr.required?then('', '?')/>
|${attr.name}
|xref:../../scalars.adoc#${attr.type?replace('.', '_')}[${attr.type?contains('.')?then(attr.type?keep_after_last('.'), attr.type)}]${requirementSuffix}
|${attr.defaultValue!}
a|${(attr.description.text)!}

    </#list>
|===
</#if>
<#if has_elements>

[#${type.className?replace('.', '_')}-components]
== Nested components

Optional components are denoted by `?`-suffixed types.

[cols="1m,1m,5"]
|===
|Tag|Type|Description

    <#list type.elements?sort_by('type') as element>
        <#assign requirementSuffix = element.required?then('', '?')/>
        <#assign descriptionCell = (element.description.text)!/>
        <#assign elementName = element.type?contains('.')?then(element.type?keep_after_last('.'), element.type)/>
        <#if lookup[element.type]??>
            <#assign elementSourcedType = lookup[element.type]/>
            <#assign elementType = elementSourcedType.type/>
            <#assign tagCell = elementType.name!/>
            <#switch elementType.class.simpleName>
                <#case 'PluginType'>
                <#case 'AbstractType'>
|${tagCell}
|xref:../../${elementSourcedType.groupId}/${elementSourcedType.artifactId}/${element.type}.adoc[${elementName}]${requirementSuffix}
a|${descriptionCell}
                    <#break>
                <#case 'ScalarType'>
|${tagCell}
|xref:../scalars.adoc#${element.type?replace('.', '_')}[${elementName}]${requirementSuffix}
a|${descriptionCell}
                    <#break>
                <#default>
                    <#stop 'Unknown type `' + element.type + '` modelled in class `' + elementType.class.name + '`'/>
            </#switch>
        <#else>
|
|${elementName}${requirementSuffix}
a|${descriptionCell}
        </#if>

    </#list>
|===
</#if>
<#if type.implementations?has_content>

[#${type.className?replace('.', '_')}-implementations]
== Known implementations

    <#list type.implementations as impl>
        <#assign implSourcedType = lookup[impl]/>
* xref:../../${implSourcedType.groupId}/${implSourcedType.artifactId}/${impl}.adoc[${impl?contains('.')?then(impl?keep_after_last('.'), impl)}]
    </#list>
</#if>

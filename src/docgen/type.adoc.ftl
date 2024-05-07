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
<#-- @ftlvariable name="type" type="org.apache.logging.log4j.docgen.Type" -->
<#-- @ftlvariable name="lookup" type="org.apache.logging.log4j.docgen.generator.TypeLookup" -->

[#${sourcedType.groupId?replace('.', '-')}_${sourcedType.artifactId?replace('.', '-')}_${type.className?replace('.', '-')}]
= ${type.name!('`' + type.className?keep_after_last('.') + '`')}

Class:: `${type.className}`
Provider:: `${sourcedType.groupId}:${sourcedType.artifactId}`

${(type.description.text)!}

<#assign hasElements = ((type.elements?size)!0) != 0/>
<#if type.class.simpleName == 'PluginType'>
    <#-- @ftlvariable name="type" type="org.apache.logging.log4j.docgen.PluginType" -->
[#${sourcedType.groupId?replace('.', '-')}_${sourcedType.artifactId?replace('.', '-')}_${type.className?replace('.', '-')}_XML-snippet]
== XML snippet
[source, xml]
----
    <#assign tag><${type.name} </#assign>
    <#assign indent = tag?replace('.', ' ', 'r')/>
    <#if !type.attributes?has_content>
<${type.name}/>
        <#else>
            <#list type.attributes?sort_by('name') as attr>
                <#if attr?is_first>
${tag}${attr.name}="${attr.defaultValue!}"${attr?is_last?then(hasElements?then('>', '/>'), '')}
                <#else>
${indent}${attr.name}="${attr.defaultValue!}"${attr?is_last?then(hasElements?then('>', '/>'), '')}
                </#if>
            </#list>
            <#if hasElements>
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
</#if>
<#if type.attributes?has_content>

[#${sourcedType.groupId?replace('.', '-')}_${sourcedType.artifactId?replace('.', '-')}_${type.className?replace('.', '-')}-attributes]
== Attributes

Optional attributes are denoted by `?`-suffixed types.

[cols="1m,1m,1m,5"]
|===
|Name|Type|Default|Description

    <#list type.attributes?sort_by('name') as attr>
        <#assign requirementSuffix = attr.required?then('', '?')/>
|${attr.name}
        <#assign attrTypeName = attr.type?contains('.')?then(attr.type?keep_after_last('.'), attr.type)/>
        <#if lookup[attr.type]??>
            <#assign attrSourcedType = lookup[attr.type]/>
|xref:#${attrSourcedType.groupId?replace('.', '-')}_${attrSourcedType.artifactId?replace('.', '-')}_${attr.type?replace('.', '-')}[${attrTypeName}]${requirementSuffix}
        <#else>
|${attrTypeName}${requirementSuffix}
        </#if>
|${attr.defaultValue!}
a|${(attr.description.text)!}

    </#list>
|===
</#if>
<#if hasElements>

[#${sourcedType.groupId?replace('.', '-')}_${sourcedType.artifactId?replace('.', '-')}_${type.className?replace('.', '-')}_components]
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
            <#assign tagCell = elementSourcedType.type.name!/>
|${tagCell}
|xref:#${elementSourcedType.groupId?replace('.', '-')}_${elementSourcedType.artifactId?replace('.', '-')}_${element.type?replace('.', '-')}[${elementName}]${requirementSuffix}
        <#else>
|
|${elementName}${requirementSuffix}
        </#if>
a|${descriptionCell}

    </#list>
|===
</#if>
<#if type.implementations?has_content>

[#${sourcedType.groupId?replace('.', '-')}_${sourcedType.artifactId?replace('.', '-')}_${type.className?replace('.', '-')}_implementations]
== Known implementations

    <#list type.implementations as impl>
        <#assign implSourcedType = lookup[impl]/>
* xref:#${implSourcedType.groupId?replace('.', '-')}_${implSourcedType.artifactId?replace('.', '-')}_${impl?replace('.', '-')}[${impl?contains('.')?then(impl?keep_after_last('.'), impl)}]
    </#list>
</#if>

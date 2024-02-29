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
<#-- @ftlvariable name="type" type="org.apache.logging.log4j.docgen.model.AbstractType" -->
<#-- @ftlvariable name="lookup" type="org.apache.logging.log4j.docgen.generator.TypeLookup" -->
<#include "license.adoc.ftl">
[#${type.className?replace('.', '_')}]
= ${type.className?keep_after_last('.')}

Class:: `${type.className}`
<#if sourcedType.groupId?has_content && sourcedType.artifactId?has_content>
Provider:: `${sourcedType.groupId}:${sourcedType.artifactId}`

</#if><#if type.description??>
${type.description.text}
</#if>

[#${type.className?replace('.', '_')}-implementations]
== Known implementations

<#list type.implementations as impl>
    <#assign implSourcedType = lookup[impl]/>
* xref:../../${implSourcedType.groupId}/${implSourcedType.artifactId}/${impl}.adoc[${impl?contains('.')?then(impl?keep_after_last('.'), impl)}]
</#list>

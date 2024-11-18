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
<#-- @ftlvariable name="release.date" type="java.lang.String" -->
<#-- @ftlvariable name="release.version" type="java.lang.String" -->
[#release-notes-${release.version?replace("[^a-zA-Z0-9]", "-", "r")}]
== ${release.version}

<#if release.date?has_content>Release date:: ${release.date}</#if>

[WARNING]
====
Due to a critical bug affecting this release (see
https://github.com/apache/logging-log4j2/issues/3143[#3143])
users are encouraged to upgrade to
${'<<release-notes-2-24-2, version `2.24.2`>>'}.
====

This release contains mainly bug fixes of problems encountered with the thread context map, logger registry and configuration reloading.

It also enhances integration tests to use Docker images of the most recent releases of MongoDB and Elastic Search.

<#include "../.changelog.adoc.ftl">

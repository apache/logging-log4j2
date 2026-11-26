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

[${"#release-notes-" + release.version?replace("[^a-zA-Z0-9]", "-", "r")}]
== ${release.version}

<#if release.date?has_content>Release date:: ${release.date}</#if>

This patch release fixes the HTML escaping of the `log:dump` JSP tag and hardens deserialization in response to
https://guide.sonatype.com/vulnerability/sonatype-2026-006746[sonatype-2026-006746].
The hardening only matters to applications that deserialize **untrusted** data using the internal `FilteredObjectInputStream` class, without an `ObjectInputFilter`:
a practice we advise against in our {logging-services-url}/security/faq.html#deserialization[Security FAQ].

[WARNING]
====
The serialized form of some classes, such as `Log4jLogEvent` and `ParameterizedMessage`, changes:
data serialized by `2.25.5` or earlier cannot be deserialized by `2.25.6`, and vice versa.
====

<#include "../.changelog.adoc.ftl">

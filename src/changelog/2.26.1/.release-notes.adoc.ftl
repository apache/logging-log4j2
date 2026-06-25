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

[${'#release-notes-' + release.version?replace("[^a-zA-Z0-9]", "-", "r")}]
== ${release.version}

<#if release.date?has_content>Release date:: ${release.date}</#if>

This patch release addresses several bugs in version 2.26.0, in particular:

* Fixes "Identity Malfunction" in `ThrowableStackTraceRenderer` by using `IdentityHashMap` for metadata caching.
* Fixes resource leaks in `ConfigurationSource` when loading configurations via URL.
* Fixes `DatePatternConverter` locale parsing when the timezone is omitted.
* Improves logging for `LinkageError` and Disruptor initialization failures to provide better diagnostics.
* Fixes `RollingFileAppender` with `createOnDemand="true"` to prevent the eager creation of parent directories when no logs are being written.
* Fixes `KafkaAppender` retry logic to prevent reporting spurious errors to the error handler after a successful retry.
* Fixes encoding of MSGID and SD-ID fields of `StructuredDataMessage` to XML.

<#include "../.changelog.adoc.ftl">

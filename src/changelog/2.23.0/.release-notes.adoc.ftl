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

[#release-notes-${release.version?replace("[^a-zA-Z0-9]", "-", "r")}]
== ${release.version}

<#if release.date?has_content>Release date:: ${release.date}</#if>

This release adds support for LMAX Disruptor 4.x, revamps `StatusLogger`, and incorporates several performance and bug fixes.

In order to maintain compatibility with JRE 8, support for LMAX Disruptor 3.x is maintained.

[#release-notes-2-23-0-StatusLogger]
=== `StatusLogger` improvements

`StatusLogger` is a standalone, self-sufficient `Logger` implementation to record events that occur in the logging system (i.e., Log4j) itself.
It is the logging system used by Log4j for reporting status of its internals.
This release improves `StatusLogger` to make it self-contained and testable.
During this simplification, the message factory for `log4j-to-slf4j` and `log4j-to-jul` is fixed to `ParameterizedMessageFactory`.

<#include "../.changelog.adoc.ftl">

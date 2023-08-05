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

= ${release.version}<#if release.date?has_content> (${release.date})</#if>

This release primarily contains bug fixes and minor enhancements.

The Log4j 2.20.1 API, as well as many core components, maintains binary compatibility with previous releases.

Apache Log4j 2.20.1 requires a minimum of Java 11 to build and Java 8 to run.
Log4j 2.12.4 is the last release to support Java 7.
Log4j 2.3.2 is the last release to support Java 6.
Java 6 and Java 7 are no longer supported by the Log4j team.

For complete information on Apache Log4j 2, including instructions on how to submit bug reports, patches, or suggestions for improvement, see http://logging.apache.org/log4j/2.x/[the Apache Log4j 2 website].

<#include "../.changelog.adoc.ftl">

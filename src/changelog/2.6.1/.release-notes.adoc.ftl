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

////
    ██     ██  █████  ██████  ███    ██ ██ ███    ██  ██████  ██
    ██     ██ ██   ██ ██   ██ ████   ██ ██ ████   ██ ██       ██
    ██  █  ██ ███████ ██████  ██ ██  ██ ██ ██ ██  ██ ██   ███ ██
    ██ ███ ██ ██   ██ ██   ██ ██  ██ ██ ██ ██  ██ ██ ██    ██
     ███ ███  ██   ██ ██   ██ ██   ████ ██ ██   ████  ██████  ██

    IF THIS FILE DOESN'T HAVE A `.ftl` SUFFIX, IT IS AUTO-GENERATED, DO NOT EDIT IT!

    Version-specific release notes (`7.8.0.adoc`, etc.) are generated from `src/changelog/*/.release-notes.adoc.ftl`.
    Auto-generation happens during `generate-sources` phase of Maven.
    Hence, you must always

    1. Find and edit the associated `.release-notes.adoc.ftl`
    2. Run `./mvnw generate-sources`
    3. Commit both `.release-notes.adoc.ftl` and the generated `7.8.0.adoc`
////

[#release-notes-${release.version?replace("[^a-zA-Z0-9]", "-", "r")}]
== ${release.version}

<#if release.date?has_content>Release date:: ${release.date}</#if>

This is the eleventh GA release.
It is primarily a bugfix release.
More details on the fixes are itemized below.

Note that subsequent to the 2.6 release a minor source incompatibility was found due to the addition of new methods to the `Logger` interface.
If you have code that does:

[source,java]
----
logger.error(null, “This is the log message”, throwable);
----

or similar with any log level you will get a compiler error saying the reference is ambiguous.
To correct this either do:

[source,java]
----
logger.error(“This is the log message”, throwable);
----

or

[source,java]
----
logger.error((Marker) null, “This is the log message”, throwable);
----

Log4j 2.6.1 maintains binary compatibility with previous releases.

Log4j 2.6.1 requires a minimum of Java 7 to build and run.
Log4j 2.3 was the last release that supported Java 6.

Basic compatibility with Log4j 1.x is provided through the `log4j-1.2-api` component, however it does
not implement some of the very implementation specific classes and methods.
The package names and Maven `groupId` have been changed to `org.apache.logging.log4j` to avoid any conflicts with Log4j 1.x.

For complete information on Apache Log4j 2, including instructions on how to submit bug reports, patches, or suggestions for improvement, see http://logging.apache.org/log4j/2.x/[the Apache Log4j 2 website].

<#include "../.changelog.adoc.ftl">

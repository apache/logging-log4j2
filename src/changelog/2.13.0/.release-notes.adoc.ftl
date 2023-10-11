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

This release contains bugfixes and minor enhancements.
Some of the new features in this release are:

* Log4j 2 now requires Java 8 or higher to build and run.
* Experimental support for Log4j 1 configuration files.
See Log4j 2 Compatiblity with Log4j 1.
* The Logger API has been enhanced to support a builder pattern.
This can dramatically improve the overhead of capturing location information.
See Log Builder.
* Better integration with Spring Boot by providing access to Spring variables in Log4j 2 configuration files and allowing Log4j 2 system properties to be defined in the Spring configuration.
See Logging in the Cloud.
* Support for accessing Kubernetes information via a Log4j 2 Lookup.
* The Gelf Layout now allows the message to be formatted using a `PatternLayout` pattern.
Logging in the Cloud provides an example of this, as well as the use of the Spring and Kubernetes Lookups.

Due to a break in compatibility in the SLF4J binding, Log4j now ships with two versions of the SLF4J to Log4j adapters.
`log4j-slf4j-impl` should be used with SLF4J 1.7.x and earlier and `log4j-slf4j18-impl` should be used with SLF4J 1.8.x and later.

Note that the XML, JSON and YAML formats changed in the 2.11.0 release: they no longer have the `timeMillis` attribute and instead have an `Instant` element with `epochSecond` and `nanoOfSecond` attributes.

The Log4j 2.13.0 API, as well as many core components, maintains binary compatibility with previous releases.

Log4j 2.13.0 requires a minimum of Java 8 to build and run.
Log4j 2.3 was the last release that supported Java 6 and Log4j 2.11.2 is the last release to support Java 7.

For complete information on Apache Log4j 2, including instructions on how to submit bug reports, patches, or suggestions for improvement, see http://logging.apache.org/log4j/2.x/[the Apache Log4j 2 website].

<#include "../.changelog.adoc.ftl">

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

This release contains a number of bug fixes and minor enhancements which are listed below.

The Log4j team has been made aware of a security vulnerability, CVE-2021-44228, that has been addressed in Log4j 2.15.0.

Log4j's JNDI support has not restricted what names could be resolved.
Some protocols are unsafe or can allow remote code execution.
Log4j now limits the protocols by default to only java, ldap, and ldaps and limits the ldap protocols to only accessing Java primitive objects by default served on the local host.

One vector that allowed exposure to this vulnerability was Log4j's allowance of Lookups to appear in log messages.
As of Log4j 2.15.0 this feature is now disabled by default.
While an option has been provided to enable Lookups in this fashion, users are strongly discouraged from enabling it.

Users who cannot upgrade to 2.15.0 can mitigate the exposure by:

. Users of Log4j 2.10 or greater may add `-Dlog4j.formatMsgNoLookups=true` as a command line option or add `log4j.formatMsgNoLookups=true` to a `log4j2.component.properties` file on the classpath to prevent lookups in log event messages.
. Users since Log4j 2.7 may specify `%\{nolookups}` in the `PatternLayout` configuration to prevent lookups in log event messages.
. Remove the `JndiLookup` and `JndiManager` classes from the `log4j-core` JAR.
Removal of the `JndiManager` will cause the `JndiContextSelector` and `JMSAppender` to no longer function.

Due to a break in compatibility in the SLF4J binding, Log4j now ships with two versions of the SLF4J to Log4j adapters.
`log4j-slf4j-impl` should be used with SLF4J 1.7.x and earlier and `log4j-slf4j18-impl` should be used with SLF4J 1.8.x and later.
SLF4J-2.0.0 alpha releases are not fully supported.
See https://issues.apache.org/jira/browse/LOG4J2-2975[LOG4J2-2975] and https://jira.qos.ch/browse/SLF4J-511[SLF4J-511].

Some of the new features in Log4j 2.15.0 include:

* Support for Arbiters, which are conditionals that can enable sections of the logging configuration for inclusion or exclusion.
In particular, `SpringProfile`, `SystemProperty`, Script, and Class Arbiters have been provided that use the Spring profile, System property, the result of a script, or the presence of a class respectively to determine whether a section of configuration should be included.
* Support for Jakarta EE 9.
This is functionally equivalent to Log4j's `log4j-web` module but uses the Jakarta project.
* Various performance improvements.

Key changes to note:

* Prior to this release Log4j would automatically resolve Lookups contained in the message or its parameters in the Pattern Layout.
This behavior is no longer the default and must be enabled by specifying `%msg\{lookup}`.
* The JNDI Lookup has been restricted to only support the java, ldap, and ldaps protocols by default.
LDAP also no longer supports classes that implement the `Referenceable` interface and restricts the Serializable classes to the Java primitive classes by default and requires an allow list to be specified to access remote LDAP servers.

The Log4j 2.15.0 API, as well as many core components, maintains binary compatibility with previous releases.

Apache Log4j 2.15.0 requires a minimum of Java 8 to build and run.
Log4j 2.12.1 is the last release to support Java 7.
Java 7 is no longer supported by the Log4j team.

For complete information on Apache Log4j 2, including instructions on how to submit bug reports, patches, or suggestions for improvement, see http://logging.apache.org/log4j/2.x/[the Apache Log4j 2 website].

<#include "../.changelog.adoc.ftl">

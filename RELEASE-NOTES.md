<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->
# Apache Log4j 2.12.2 Release Notes

The Apache Log4j 2 team is pleased to announce the Log4j 2.12.2 release!

Apache Log4j is a well known framework for logging application behavior. Log4j 2 is an upgrade
to Log4j that provides significant improvements over its predecessor, Log4j 1.x, and provides
many other modern features such as support for Markers, lambda expressions for lazy logging,
property substitution using Lookups, multiple patterns on a PatternLayout and asynchronous
Loggers. Another notable Log4j 2 feature is the ability to be "garbage-free" (avoid allocating
temporary objects) while logging. In addition, Log4j 2 will not lose events while reconfiguring.

The artifacts may be downloaded from https://logging.apache.org/log4j/2.x/download.html.

This release contains bugfixes and minor enhancements.

Due to a break in compatibility in the SLF4J binding, Log4j now ships with two versions of the SLF4J to Log4j adapters.
log4j-slf4j-impl should be used with SLF4J 1.7.x and earlier and log4j-slf4j18-impl should be used with SLF4J 1.8.x and
later.

This release addresses CVE-2021-44228 for users still using Java 7 by disabling JNDI by default, only allowing the java
protocol when JNDI is enabled, making the JNDI Lookup inoperable, and removing the message lookup capability.

The Log4j 2.12.2 API, as well as many core components, maintains binary compatibility with previous releases.

## GA Release 2.12.2

Changes in this version include:


### Fixed Bugs
* [LOG4J-3220](https://issues.apache.org/jira/browse/LOG4J-3220):
Disable JNDI by default, remove JNDI Lookup, remove message lookups. When enabled JNDI only supports the
        java protocol.


---

Apache Log4j 2.12.2 requires a minimum of Java 7 to build and run. Log4j 2.3 was the
last release that supported Java 6.

Basic compatibility with Log4j 1.x is provided through the log4j-1.2-api component, however it
does not implement some of the very implementation specific classes and methods. The package
names and Maven groupId have been changed to org.apache.logging.log4j to avoid any conflicts
with log4j 1.x.

For complete information on Apache Log4j 2, including instructions on how to submit bug
reports, patches, or suggestions for improvement, see the Apache Apache Log4j 2 website:

https://logging.apache.org/log4j/2.x/
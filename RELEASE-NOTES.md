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
# Apache Log4j 2.19.0 Release Notes

The Apache Log4j 2 team is pleased to announce the Log4j 2.19.0 release!

Apache Log4j is a well known framework for logging application behavior. Log4j 2 is an upgrade
to Log4j that provides significant improvements over its predecessor, Log4j 1.x, and provides
many other modern features such as support for Markers, lambda expressions for lazy logging,
property substitution using Lookups, multiple patterns on a PatternLayout and asynchronous
Loggers. Another notable Log4j 2 feature is the ability to be "garbage-free" (avoid allocating
temporary objects) while logging. In addition, Log4j 2 will not lose events while reconfiguring.

The artifacts may be downloaded from https://logging.apache.org/log4j/2.x/download.html.

This release primarily contains bug fixes and minor enhancements.

Due to a break in compatibility in the SLF4J binding, Log4j now ships with two versions of the SLF4J to Log4j adapters.
log4j-slf4j-impl should be used with SLF4J 1.7.x and earlier and log4j-slf4j2-impl should be used with SLF4J 2.x and
later. SLF4J-1.8.x is no longer supported as a GA release never occurred.

The Log4j 2.19.0 API, as well as many core components, maintains binary compatibility with previous releases.

## GA Release 2.19.0

Changes in this version include:

### New Features
* [LOG4J2-3583](https://issues.apache.org/jira/browse/LOG4J2-3583):
Add support for SLF4J2 stack-valued MDC. Thanks to Pierrick Terrettaz.
* [LOG4J2-2975](https://issues.apache.org/jira/browse/LOG4J2-2975):
Add implementation of SLF4J2 fluent API. Thanks to Daniel Gray.

### Fixed Bugs
* [LOG4J2-3578](https://issues.apache.org/jira/browse/LOG4J2-3578):
Generate new SSL certs for testing.
* [LOG4J2-3556](https://issues.apache.org/jira/browse/LOG4J2-3556):
Make JsonTemplateLayout stack trace truncation operate for each label block. Thanks to Arthur Gavlyukovskiy.
* [LOG4J2-3550](https://issues.apache.org/jira/browse/LOG4J2-3550):
SystemPropertyArbiter was assigning the value as the name. Thanks to DongjianPeng.
* [LOG4J2-3560](https://issues.apache.org/jira/browse/LOG4J2-3560):
Logger$PrivateConfig.filter(Level, Marker, String) was allocating empty varargs array. Thanks to David Schlosnagle.
* [LOG4J2-3561](https://issues.apache.org/jira/browse/LOG4J2-3561):
Allows a space separated list of style specifiers in the %style pattern for consistency with %highlight. Thanks to Robert Papp.
* [LOG4J2-3564](https://issues.apache.org/jira/browse/LOG4J2-3564):
Fix NPE in `log4j-to-jul` in the case the root logger level is null.
* [LOG4J2-3545](https://issues.apache.org/jira/browse/LOG4J2-3545):
Add correct manifest entries for OSGi to log4j-jcl Thanks to Johan Compagner.
* [LOG4J2-3565](https://issues.apache.org/jira/browse/LOG4J2-3565):
Fix RollingRandomAccessFileAppender with DirectWriteRolloverStrategy can't create the first log file of different directory.
* [LOG4J2-3579](https://issues.apache.org/jira/browse/LOG4J2-3579):
Fix ServiceLoaderUtil behavior in the presence of a SecurityManager. Thanks to Boris Unckel.
* [LOG4J2-3559](https://issues.apache.org/jira/browse/LOG4J2-3559):
Fix resolution of properties not starting with `log4j2.`. Thanks to Gary Gregory.
* [LOG4J2-3557](https://issues.apache.org/jira/browse/LOG4J2-3557):
Fix recursion between Log4j 1.2 LogManager and Category. Thanks to Andreas Leitgeb.
* [LOG4J2-3587](https://issues.apache.org/jira/browse/LOG4J2-3587):
Fix regression in Rfc5424Layout default values. Thanks to Tomas Micko.
* [LOG4J2-3548](https://issues.apache.org/jira/browse/LOG4J2-3548):
Improve support for passwordless keystores. Thanks to Kristof Farkas-Pall.
* [LOG4J2-708](https://issues.apache.org/jira/browse/LOG4J2-708):
Add async support to `Log4jServletFilter`.

### Changes
* [LOG4J2-3572](https://issues.apache.org/jira/browse/LOG4J2-3572):
Add getExplicitLevel method to LoggerConfig.
* [LOG4J2-3589](https://issues.apache.org/jira/browse/LOG4J2-3589):
Allow Plugins to be injected with the LoggerContext reference.
* [LOG4J2-3588](https://issues.apache.org/jira/browse/LOG4J2-3588):
Allow PropertySources to be added.

### Removed
* [LOG4J2-3573](https://issues.apache.org/jira/browse/LOG4J2-3573):
Removed build page in favor of a single build instructions file. Thanks to Wolff Bock von Wuelfingen.
* [LOG4J2-3590](https://issues.apache.org/jira/browse/LOG4J2-3590):
Remove SLF4J 1.8.x binding.
---

Apache Log4j 2.19.0 requires a minimum of Java 8 to build and run.
Log4j 2.12.4 is the last release to support Java 7.
Log4j 2.3.2 is the last release to support Java 6.
Java 6 and Java 7 are no longer supported by the Log4j team.

For complete information on Apache Log4j 2, including instructions on how to submit bug
reports, patches, or suggestions for improvement, see the Apache Apache Log4j 2 website:

https://logging.apache.org/log4j/2.x/

---

Earlier release notes are accessible in [Release History](https://logging.apache.org/log4j/2.x/changes-report.html).

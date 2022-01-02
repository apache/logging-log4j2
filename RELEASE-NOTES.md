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
# Apache Log4j 2.17.1 Release Notes

The Apache Log4j 2 team is pleased to announce the Log4j 2.17.1 release!

Apache Log4j is a well known framework for logging application behavior. Log4j 2 is an upgrade
to Log4j that provides significant improvements over its predecessor, Log4j 1.x, and provides
many other modern features such as support for Markers, lambda expressions for lazy logging,
property substitution using Lookups, multiple patterns on a PatternLayout and asynchronous
Loggers. Another notable Log4j 2 feature is the ability to be "garbage-free" (avoid allocating
temporary objects) while logging. In addition, Log4j 2 will not lose events while reconfiguring.

The artifacts may be downloaded from https://logging.apache.org/log4j/2.x/download.html.

This release contains the changes noted below:

* Address CVE-2021-44832.
* Other minor fixes.

Due to a break in compatibility in the SLF4J binding, Log4j now ships with two versions of the SLF4J to Log4j adapters.
log4j-slf4j-impl should be used with SLF4J 1.7.x and earlier and log4j-slf4j18-impl should be used with SLF4J 1.8.x and
later. SLF4J-2.0.0 alpha releases are not fully supported. See https://issues.apache.org/jira/browse/LOG4J2-2975 and
https://jira.qos.ch/browse/SLF4J-511.

The Log4j 2.17.1 API, as well as many core components, maintains binary compatibility with previous releases.

## GA Release 2.17.1

Changes in this version include:


### Fixed Bugs
* [LOG4J2-3293](https://issues.apache.org/jira/browse/LOG4J2-3293):
JdbcAppender now uses JndiManager to access JNDI resources. JNDI is only enabled when system property
        log4j2.enableJndiJdbc is set to true.
* [LOG4J2-3290](https://issues.apache.org/jira/browse/LOG4J2-3290):
Remove unused method.
* [LOG4J2-3292](https://issues.apache.org/jira/browse/LOG4J2-3292):
ExtendedLoggerWrapper.logMessage no longer double-logs when location is requested.
* [LOG4J2-3289](https://issues.apache.org/jira/browse/LOG4J2-3289):
log4j-to-slf4j no longer re-interpolates formatted message contents.
* [LOG4J2-3204](https://issues.apache.org/jira/browse/LOG4J2-3204):
Correct SpringLookup package name in Interpolator. Thanks to Francis-FY.
* [LOG4J2-3284](https://issues.apache.org/jira/browse/LOG4J2-3284):
log4j-to-slf4j takes the provided MessageFactory into account Thanks to Michael Vorburger.
* [LOG4J2-3264](https://issues.apache.org/jira/browse/LOG4J2-3264):
Fix MapLookup to lookup MapMessage before DefaultMap Thanks to Yanming Zhou.
* [LOG4J2-3274](https://issues.apache.org/jira/browse/LOG4J2-3274):
Buffered I/O checked had inverted logic in RollingFileAppenderBuidler. Thanks to Faisal Khan Thayub Khan.
* [](https://issues.apache.org/jira/browse/LOG4J2-3274):
Fix NPE when input is null in StrSubstitutor.replace(String, Properties).
* [LOG4J2-3270](https://issues.apache.org/jira/browse/LOG4J2-3270):
Lookups with no prefix only read values from the configuration properties as expected.
* [LOG4J2-3256](https://issues.apache.org/jira/browse/LOG4J2-3256):
Reduce ignored package scope of KafkaAppender. Thanks to Lee Dongjin.


---

Apache Log4j 2.17.1 requires a minimum of Java 8 to build and run.
Log4j 2.12.3 is the last release to support Java 7.
Log4j 2.3.1 is the last release to support Java 6.
Java 6 and Java 7 are no longer supported by the Log4j team.

For complete information on Apache Log4j 2, including instructions on how to submit bug
reports, patches, or suggestions for improvement, see the Apache Apache Log4j 2 website:

https://logging.apache.org/log4j/2.x/

---

Earlier release notes are accessible in [Release History](https://logging.apache.org/log4j/2.x/changes-report.html).

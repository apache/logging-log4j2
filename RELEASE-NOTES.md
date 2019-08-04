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
# Apache Log4j 2.12.1 Release Notes

The Apache Log4j 2 team is pleased to announce the Log4j 2.12.1 release!

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

This release improves the performance of capturing location information, makes log4j-core optional in the log4j 1.2
bridge, and explicitly removes LoggerContext references from compoents that keep track of them when the LoggerContext
is shut down. More details on the new features and fixes are itemized below.

Note that the XML, JSON and YAML formats changed in the 2.11.0 release: they no longer have the "timeMillis" attribute
and instead have an "Instant" element with "epochSecond" and "nanoOfSecond" attributes.

The Log4j 2.12.1 API, as well as many core components, maintains binary compatibility with previous releases.

## GA Release 2.12.1

Changes in this version include:


### Fixed Bugs
* [LOG4J2-1946](https://issues.apache.org/jira/browse/LOG4J2-1946):
Allow file renames to work when files are missing from the sequence. Thanks to Igor Perelyotov.
* [LOG4J2-2650](https://issues.apache.org/jira/browse/LOG4J2-2650):
Support emulating a MAC address when using ipv6. Thanks to Mattia Bertorello.
* [LOG4J2-2366](https://issues.apache.org/jira/browse/LOG4J2-2366):
Remove references to LoggerContext when it is shutdown.
* [LOG4J2-2644](https://issues.apache.org/jira/browse/LOG4J2-2644):
Improve the performance of capturing location information.
* [LOG4J2-2658](https://issues.apache.org/jira/browse/LOG4J2-2658):
AbstractAction.reportException records a warning to the status logger, providing more information when file
        based appenders fail to compress rolled data asynchronously.
* [LOG4J2-2659](https://issues.apache.org/jira/browse/LOG4J2-2659):
AbstractAction handles and records unchecked RuntimeException and Error in addition to IOException.

### Changes
* [LOG4J2-2556](https://issues.apache.org/jira/browse/LOG4J2-2556):
Make Log4j Core optional for Log4j 1.2 API.
* [LOG4J2-2646](https://issues.apache.org/jira/browse/LOG4J2-2646):
Update MongoDB 3 driver from 3.10.1 to 3.10.2.
* [LOG4J2-2657](https://issues.apache.org/jira/browse/LOG4J2-2657):
Improve exception messages in the JDBC appender.
* [LOG4J2-2660](https://issues.apache.org/jira/browse/LOG4J2-2660):
Retry when JDBC throws a java.sql.SQLTransactionRollbackException in commitAndClose().

---

Apache Log4j 2.12.1 requires a minimum of Java 7 to build and run. Log4j 2.3 was the
last release that supported Java 6.

Basic compatibility with Log4j 1.x is provided through the log4j-1.2-api component, however it
does not implement some of the very implementation specific classes and methods. The package
names and Maven groupId have been changed to org.apache.logging.log4j to avoid any conflicts
with log4j 1.x.

For complete information on Apache Log4j 2, including instructions on how to submit bug
reports, patches, or suggestions for improvement, see the Apache Apache Log4j 2 website:

https://logging.apache.org/log4j/2.x/
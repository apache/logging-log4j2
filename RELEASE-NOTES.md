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
# Apache Log4j 2.13.3 Release Notes

The Apache Log4j 2 team is pleased to announce the Log4j 2.13.3 release!

Apache Log4j is a well known framework for logging application behavior. Log4j 2 is an upgrade
to Log4j that provides significant improvements over its predecessor, Log4j 1.x, and provides
many other modern features such as support for Markers, lambda expressions for lazy logging,
property substitution using Lookups, multiple patterns on a PatternLayout and asynchronous
Loggers. Another notable Log4j 2 feature is the ability to be "garbage-free" (avoid allocating
temporary objects) while logging. In addition, Log4j 2 will not lose events while reconfiguring.

The artifacts may be downloaded from https://logging.apache.org/log4j/2.x/download.html.

This release contains a fix for bug LOG4J2-2838.

Due to a break in compatibility in the SLF4J binding, Log4j now ships with two versions of the SLF4J to Log4j adapters.
log4j-slf4j-impl should be used with SLF4J 1.7.x and earlier and log4j-slf4j18-impl should be used with SLF4J 1.8.x and
later.

Note that the default XML, JSON and YAML formats changed in the 2.11.0 release: they no longer have the "timeMillis"
attribute and instead have an "Instant" element with "epochSecond" and "nanoOfSecond" attributes. If the previous
behavior is desired the "includeTimeMillis" attribute may be set to true on each of the respective Layouts.

The Log4j 2.13.3 API, as well as many core components, maintains binary compatibility with previous releases.

## GA Release 2.13.3

Changes in this version include:


### Fixed Bugs
* [LOG4J2-2838](https://issues.apache.org/jira/browse/LOG4J2-2838):
Fix NullPointerException in ThreadContextDataInjector.


---

Apache Log4j 2.13.3 requires a minimum of Java 8 to build and run. Log4j 2.3 was the
last release that supported Java 6 and Log4j 2.12.1 is the last release to support Java 7.

For complete information on Apache Log4j 2, including instructions on how to submit bug
reports, patches, or suggestions for improvement, see the Apache Apache Log4j 2 website:

https://logging.apache.org/log4j/2.x/
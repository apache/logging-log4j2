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
# Apache Log4j 2.13.2 Release Notes

The Apache Log4j 2 team is pleased to announce the Log4j 2.13.2 release!

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

Note that the default XML, JSON and YAML formats changed in the 2.11.0 release: they no longer have the "timeMillis"
attribute and instead have an "Instant" element with "epochSecond" and "nanoOfSecond" attributes. If the previous
behavior is desired the "includeTimeMillis" attribute may be set to true on each of the respective Layouts.

The Log4j 2.13.2 API, as well as many core components, maintains binary compatibility with previous releases.

## GA Release 2.13.2

Changes in this version include:

### New Features
* [LOG4J2-1360](https://issues.apache.org/jira/browse/LOG4J2-1360):
Provide a Log4j implementation of System.Logger. Thanks to Kevin Leturc.
* [LOG4J2-2807](https://issues.apache.org/jira/browse/LOG4J2-2807):
Added EventLookup to retrieve fields from the log event.

### Fixed Bugs
* [LOG4J2-2824](https://issues.apache.org/jira/browse/LOG4J2-2824):
Implement requiresLocation in GelfLayout to reflect whether location information is used in the message Pattern. Thanks to CrazyBills.
* [LOG4J2-2588](https://issues.apache.org/jira/browse/LOG4J2-2588):
Add option to restore printing timeMillis in the JsonLayout.
* [LOG4J2-2766](https://issues.apache.org/jira/browse/LOG4J2-2766):
Initialize pattern processor before triggering policy during reconriguration.
* [LOG4J2-2810](https://issues.apache.org/jira/browse/LOG4J2-2810):
Add information about using a url in log4j.configurationFile.
* [LOG4J2-2813](https://issues.apache.org/jira/browse/LOG4J2-2813):
serializeToBytes was checking wrong variable for null. Thanks to Keith D Gregory.
* [LOG4J2-2814](https://issues.apache.org/jira/browse/LOG4J2-2814):
Fix Javadoc for ScriptPatternSelector.
* [LOG4J2-2793](https://issues.apache.org/jira/browse/LOG4J2-2793):
Allow trailing and leading spaces in log level. Thanks to Renukaprasad C.
* [LOG4J2-2791](https://issues.apache.org/jira/browse/LOG4J2-2791):
Correct JsonLayout timestamp sorting issue. Thanks to Johan Karlberg.
* [LOG4J2-2817](https://issues.apache.org/jira/browse/LOG4J2-2817):
Allow the file size action to parse the value without being sensitive to the current locale. Thanks to Trejkaz.
* [LOG4J2-2794](https://issues.apache.org/jira/browse/LOG4J2-2794):
Make YamlLayoutTest more resiliant to environmental differences. Thanks to Johan Karlberg.
* [LOG4J2-2790](https://issues.apache.org/jira/browse/LOG4J2-2790):
Conditionally allocate PluginEntry during PluginCache loading. Thanks to Marius Volkhart.
* [LOG4J2-2811](https://issues.apache.org/jira/browse/LOG4J2-2811):
Add missing includeLocation parameter when creating AsyncLogger. Thanks to Kuojian21.
* [LOG4J2-2761](https://issues.apache.org/jira/browse/LOG4J2-2761):
Fix Exceptions when whitespace is in the file path and Java security manager is used. Thanks to Uwe Schindler.
* [LOG4J2-2809](https://issues.apache.org/jira/browse/LOG4J2-2809):
Avoid NullPointerException when StackWalker returns null. Thanks to Romain Manni-Bucau.
* [LOG4J2-2805](https://issues.apache.org/jira/browse/LOG4J2-2805):
TimeFilter did not handle daylight saving time transitions and did not support a range over 2 days.

### Changes
* [LOG4J2-2457](https://issues.apache.org/jira/browse/LOG4J2-2457):
Allow the file extension in the file pattern to be modified during reconfiguration.
* [LOG4J2-2819](https://issues.apache.org/jira/browse/LOG4J2-2819):
Add support for specifying an SSL configuration for SmtpAppender.
* [LOG4J2-2520](https://issues.apache.org/jira/browse/LOG4J2-2520):
Allow servlet context path to be retrieved without "/".
* [LOG4J2-2818](https://issues.apache.org/jira/browse/LOG4J2-2818):
Allow Spring Lookup to return default and active profiles.
* [LOG4J2-2815](https://issues.apache.org/jira/browse/LOG4J2-2815):
Allow Spring Boot applications to use composite configuratons.
* [LOG4J2-2779](https://issues.apache.org/jira/browse/LOG4J2-2779):
Add ContextDataProviders as an alternative to having to implement a ContextDataInjector.
* [LOG4J2-2812](https://issues.apache.org/jira/browse/LOG4J2-2812):
[JDBC] Throw a AppenderLoggingException instead of an NPE in the JDBC database manager.

---

Apache Log4j 2.13.2 requires a minimum of Java 8 to build and run. Log4j 2.3 was the
last release that supported Java 6 and Log4j 2.12.1 is the last release to support Java 7.

For complete information on Apache Log4j 2, including instructions on how to submit bug
reports, patches, or suggestions for improvement, see the Apache Apache Log4j 2 website:

https://logging.apache.org/log4j/2.x/
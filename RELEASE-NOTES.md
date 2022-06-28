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
# Apache Log4j 2.18.0 Release Notes

The Apache Log4j 2 team is pleased to announce the Log4j 2.18.0 release!

Apache Log4j is a well known framework for logging application behavior. Log4j 2 is an upgrade
to Log4j that provides significant improvements over its predecessor, Log4j 1.x, and provides
many other modern features such as support for Markers, lambda expressions for lazy logging,
property substitution using Lookups, multiple patterns on a PatternLayout and asynchronous
Loggers. Another notable Log4j 2 feature is the ability to be "garbage-free" (avoid allocating
temporary objects) while logging. In addition, Log4j 2 will not lose events while reconfiguring.

The artifacts may be downloaded from https://logging.apache.org/log4j/2.x/download.html.

This release primarily contains bug fixes and minor enhancements.

Due to a break in compatibility in the SLF4J binding, Log4j now ships with two versions of the SLF4J to Log4j adapters.
log4j-slf4j-impl should be used with SLF4J 1.7.x and earlier and log4j-slf4j18-impl should be used with SLF4J 1.8.x and
later. SLF4J-2.0.0 alpha releases are not fully supported. See https://issues.apache.org/jira/browse/LOG4J2-2975 and
https://jira.qos.ch/browse/SLF4J-511.

The Log4j 2.18.0 API, as well as many core components, maintains binary compatibility with previous releases.

## GA Release 2.18.0

Changes in this version include:

### New Features
* [LOG4J2-3495](https://issues.apache.org/jira/browse/LOG4J2-3495):
Add MutableThreadContextMapFilter.
* [LOG4J2-3472](https://issues.apache.org/jira/browse/LOG4J2-3472):
Add support for custom LMAX disruptor WaitStrategy configuration.
* [LOG4J2-3419](https://issues.apache.org/jira/browse/LOG4J2-3419):
Add support for custom Log4j 1.x levels.
* [LOG4J2-3440](https://issues.apache.org/jira/browse/LOG4J2-3440):
Add support for adding and retrieving appenders in Log4j 1.x bridge.
* [LOG4J2-3362](https://issues.apache.org/jira/browse/LOG4J2-3362):
Add support for Jakarta Mail API in the SMTP appender.
* [LOG4J2-3483](https://issues.apache.org/jira/browse/LOG4J2-3483):
Add support for Apache Extras' RollingFileAppender in Log4j 1.x bridge.
* [LOG4J2-3538](https://issues.apache.org/jira/browse/LOG4J2-3538):
Add support for 24 colors in highlighting Thanks to Pavel_K.

### Fixed Bugs
* [LOG4J2-3339](https://issues.apache.org/jira/browse/LOG4J2-3339):
DirectWriteRolloverStrategy should use the current time when creating files.
* [LOG4J2-3534](https://issues.apache.org/jira/browse/LOG4J2-3534):
Fix LevelRangeFilterBuilder to align with log4j1's behavior.
* [LOG4J2-3527](https://issues.apache.org/jira/browse/LOG4J2-3527):
Don't use Paths.get() to avoid circular file systems.
* [LOG4J2-3490](https://issues.apache.org/jira/browse/LOG4J2-3490):
The DirectWriteRolloverStrategy was not detecting the correct index to use during startup.
* [LOG4J2-3432](https://issues.apache.org/jira/browse/LOG4J2-3432):
SizeBasedTriggeringPolicy would fail to rename files properly when integer pattern contained a leading zero.
* [LOG4J2-3491](https://issues.apache.org/jira/browse/LOG4J2-3491):
Async Loggers were including the location information by default. Thanks to Avihai Marchiano.
* [LOG4J2-1376](https://issues.apache.org/jira/browse/LOG4J2-1376):
Allow enterprise id to be an OID fragment.
* [LOG4J2-3493](https://issues.apache.org/jira/browse/LOG4J2-3493):
ClassArbiter's newBuilder method referenced the wrong class. Thanks to Dmytro Voloshyn.
* [LOG4J2-3481](https://issues.apache.org/jira/browse/LOG4J2-3481):
HttpWatcher did not pass credentials when polling.
* [LOG4J2-3482](https://issues.apache.org/jira/browse/LOG4J2-3482):
UrlConnectionFactory.createConnection now accepts an AuthorizationProvider as a parameter.
* [LOG4J2-3477](https://issues.apache.org/jira/browse/LOG4J2-3477):
Add the missing context stack to JsonLayout template. Thanks to filipc.
* [LOG4J2-3393](https://issues.apache.org/jira/browse/LOG4J2-3393):
Improve JsonTemplateLayout performance.
* [LOG4J2-3424](https://issues.apache.org/jira/browse/LOG4J2-3424):
Properties defined in configuration using a value attribute (as opposed to element) are read correctly.
* [LOG4J2-3413](https://issues.apache.org/jira/browse/LOG4J2-3413):
Fix resolution of non-Log4j properties.
* [LOG4J2-3423](https://issues.apache.org/jira/browse/LOG4J2-3423):
JAR file containing Log4j configuration isn't closed. Thanks to Radim Tlusty.
* [LOG4J2-3425](https://issues.apache.org/jira/browse/LOG4J2-3425):
Syslog appender lacks the SocketOptions setting. Thanks to Jiří Smolík.
* [](https://issues.apache.org/jira/browse/LOG4J2-3425):
Improve validation and reporting of configuration errors.
* [](https://issues.apache.org/jira/browse/LOG4J2-3425):
Log4j 1.2 bridge should generate Log4j 2.x messages based on the parameter runtime type.
* [LOG4J2-3426](https://issues.apache.org/jira/browse/LOG4J2-3426):
Log4j 1.2 bridge should not wrap components unnecessarily. Thanks to Pooja Pandey.
* [LOG4J2-3418](https://issues.apache.org/jira/browse/LOG4J2-3418):
Fixes Spring Boot logging system registration in a multi-application environment.
* [LOG4J2-3040](https://issues.apache.org/jira/browse/LOG4J2-3040):
Avoid ClassCastException in JeroMqManager with custom LoggerContextFactory #791. Thanks to LF-Lin.
* [](https://issues.apache.org/jira/browse/LOG4J2-3040):
Fix minor typo #792. Thanks to LF-Lin.
* [LOG4J2-3439](https://issues.apache.org/jira/browse/LOG4J2-3439):
Fixes default SslConfiguration, when a custom keystore is used. Thanks to Jayesh Netravali.
* [LOG4J2-3447](https://issues.apache.org/jira/browse/LOG4J2-3447):
Fixes appender concurrency problems in Log4j 1.x bridge. Thanks to Pooja Pandey.
* [LOG4J2-3452](https://issues.apache.org/jira/browse/LOG4J2-3452):
Fix and test for race condition in FileUtils.mkdir(). Thanks to Stefan Vodita.
* [LOG4J2-3458](https://issues.apache.org/jira/browse/LOG4J2-3458):
LocalizedMessage logs misleading errors on the console.
* [LOG4J2-3359](https://issues.apache.org/jira/browse/LOG4J2-3359):
Fixes the syslog appender in Log4j 1.x bridge, when used with a custom layout. Thanks to Tukesh.
* [LOG4J2-3359](https://issues.apache.org/jira/browse/LOG4J2-3359):
log4j-1.2-api 2.17.2 throws NullPointerException while removing appender with name as null. Thanks to Rajesh.
* [LOG4J2-2872](https://issues.apache.org/jira/browse/LOG4J2-2872):
Fix problem with non-uppercase custom levels. Thanks to Alla Gofman.
* [LOG4J2-3475](https://issues.apache.org/jira/browse/LOG4J2-3475):
Add missing message parameterization in RegexFilter. Thanks to Jeremy Lin.
* [LOG4J2-3428](https://issues.apache.org/jira/browse/LOG4J2-3428):
Update 3rd party dependencies for 2.18.0.
* [LOG4J2-3531](https://issues.apache.org/jira/browse/LOG4J2-3531):
Fix parsing error, when XInclude is disabled. Thanks to Simo Nikula.
* [LOG4J2-3537](https://issues.apache.org/jira/browse/LOG4J2-3537):
Fixes problem with wrong ANSI escape code for bright colors Thanks to Pavel_K.

### Changes
* [LOG4J2-3536](https://issues.apache.org/jira/browse/LOG4J2-3536):
Upgrade the Flume Appender to Flume 1.10.0
* [LOG4J2-3516](https://issues.apache.org/jira/browse/LOG4J2-3516):
Move perf tests to log4j-core-its
* [LOG4J2-3506](https://issues.apache.org/jira/browse/LOG4J2-3506):
Support Spring 2.6.x.
* [LOG4J2-3473](https://issues.apache.org/jira/browse/LOG4J2-3473):
Make the default disruptor WaitStrategy used by Async Loggers garbage-free.
* [LOG4J2-3476](https://issues.apache.org/jira/browse/LOG4J2-3476):
Do not throw UnsupportedOperationException when JUL ApiLogger::setLevel is called.
* [LOG4J2-3427](https://issues.apache.org/jira/browse/LOG4J2-3427):
Improves ServiceLoader support on servlet containers.

---

Apache Log4j 2.18.0 requires a minimum of Java 8 to build and run.
Log4j 2.12.4 is the last release to support Java 7.
Log4j 2.3.2 is the last release to support Java 6.
Java 6 and Java 7 are no longer supported by the Log4j team.

For complete information on Apache Log4j 2, including instructions on how to submit bug
reports, patches, or suggestions for improvement, see the Apache Apache Log4j 2 website:

https://logging.apache.org/log4j/2.x/

---

Earlier release notes are accessible in [Release History](https://logging.apache.org/log4j/2.x/changes-report.html).

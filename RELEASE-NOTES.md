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
# Apache Log4j 2.13.1 Release Notes

The Apache Log4j 2 team is pleased to announce the Log4j 2.13.1 release!

Apache Log4j is a well known framework for logging application behavior. Log4j 2 is an upgrade
to Log4j that provides significant improvements over its predecessor, Log4j 1.x, and provides
many other modern features such as support for Markers, lambda expressions for lazy logging,
property substitution using Lookups, multiple patterns on a PatternLayout and asynchronous
Loggers. Another notable Log4j 2 feature is the ability to be "garbage-free" (avoid allocating
temporary objects) while logging. In addition, Log4j 2 will not lose events while reconfiguring.

The artifacts may be downloaded from https://logging.apache.org/log4j/2.x/download.html.

This release contains bugfixes and very minor enhancements.

Due to a break in compatibility in the SLF4J binding, Log4j now ships with two versions of the SLF4J to Log4j adapters.
log4j-slf4j-impl should be used with SLF4J 1.7.x and earlier and log4j-slf4j18-impl should be used with SLF4J 1.8.x and
later.

Note that the XML, JSON and YAML formats changed in the 2.11.0 release: they no longer have the "timeMillis" attribute
and instead have an "Instant" element with "epochSecond" and "nanoOfSecond" attributes.

The Log4j 2.13.1 API, as well as many core components, maintains binary compatibility with previous releases.

## GA Release 2.13.1

Changes in this version include:

### New Features
* [LOG4J2-2748](https://issues.apache.org/jira/browse/LOG4J2-2748):
Implement ISO8601_PERIOD_MICROS fixed date format matching ISO8601_PERIOD with support for microsecond precision.

### Fixed Bugs
* [LOG4J2-2717](https://issues.apache.org/jira/browse/LOG4J2-2717):
Slow initialization on Windows due to accessing network interfaces.
* [LOG4J2-2756](https://issues.apache.org/jira/browse/LOG4J2-2756):
Prevent LoggerContext from being garbage collected while being created.
* [LOG4J2-2769](https://issues.apache.org/jira/browse/LOG4J2-2769):
Do not log an error if Files.move does not work.
* [LOG4J2-2039](https://issues.apache.org/jira/browse/LOG4J2-2039):
Rollover fails when file matches pattern but index is too large.
* [LOG4J2-2784](https://issues.apache.org/jira/browse/LOG4J2-2784):
Counter stuck at 10 and overwriting files when leading zeros used in the file pattern count.
* [LOG4J2-2746](https://issues.apache.org/jira/browse/LOG4J2-2746):
ClassLoaderContextSelector was not locating the LoggerContext during shutdown.
* [LOG4J2-2652](https://issues.apache.org/jira/browse/LOG4J2-2652):
JSON output wrong when using additonal fields.
* [LOG4J2-2649](https://issues.apache.org/jira/browse/LOG4J2-2649):
GraalVM does not allow use of MethodHandles.
* [LOG4J2-2211](https://issues.apache.org/jira/browse/LOG4J2-2211):
Allow Lookup keys with leading dashes by using a slash as an escape character.
* [LOG4J2-2781](https://issues.apache.org/jira/browse/LOG4J2-2781):
ServletContainerInitializer was obtaining the StatusLogger too soon. Thanks to qxo.
* [LOG4J2-2676](https://issues.apache.org/jira/browse/LOG4J2-2676):
PluginProcessor should use Messager instead of System.out. Thanks to Gregg Donovan.
* [LOG4J2-2703](https://issues.apache.org/jira/browse/LOG4J2-2703):
MapMessage.getFormattedMesssage() would incorrectly format objects. Thanks to Volkan Yazici.
* [LOG4J2-2760](https://issues.apache.org/jira/browse/LOG4J2-2760):
Always write header on a new OutputStream. Thanks to Christoph Kaser.
* [LOG4J2-2776](https://issues.apache.org/jira/browse/LOG4J2-2776):
An error message in RollingFileAppender uses a placeholder for the name but does not specify the name
        argument in the logging call Thanks to Christoph Kaser.
* [LOG4J2-2758](https://issues.apache.org/jira/browse/LOG4J2-2758):
NullPointerException when using a custom DirectFileRolloverStrategy without a file name. Thanks to Christoph Kaser.
* [LOG4J2-2768](https://issues.apache.org/jira/browse/LOG4J2-2768):
Add mulit-parameter overloads to LogBuilder. Thanks to Marius Volkhart.
* [LOG4J2-2770](https://issues.apache.org/jira/browse/LOG4J2-2770):
Fixed NullPointerException after reconfiguring via JMX. Thanks to Bill Kuker.
* [LOG4J2-2759](https://issues.apache.org/jira/browse/LOG4J2-2759):
RollingFileAppender was not rolling on startup if createOnDemand was set to true.
* [LOG4J2-2767](https://issues.apache.org/jira/browse/LOG4J2-2767):
Warn if pattern is missing on Routes element. Use default route.
* [LOG4J2-2415](https://issues.apache.org/jira/browse/LOG4J2-2415):
Fix lock contention in the classloader using new versions of slf4j without EventData on slf4j logger creation. Thanks to Andrey Turbanov.
* [LOG4J2-2677](https://issues.apache.org/jira/browse/LOG4J2-2677):
Rollover handles parallel file deletion gracefully.
* [LOG4J2-2744](https://issues.apache.org/jira/browse/LOG4J2-2744):
Remove unnecessary EventLogger references from log4j-slf4j18-impl due to removal from slf4j.
* [LOG4J2-2747](https://issues.apache.org/jira/browse/LOG4J2-2747):
Fix a memory leak using fully asynchronous logging when the queue is full using the 'discard' asynchronous queue full strategy.
* [LOG4J2-2739](https://issues.apache.org/jira/browse/LOG4J2-2739):
Fix erroneous log4j-jul recursive logger detection resulting in some no-op JUL loggers and 'WARN Recursive call to getLogger' being reported by the status logger.
* [LOG4J2-2735](https://issues.apache.org/jira/browse/LOG4J2-2735):
PluginCache output is reproducible allowing the annotation processor to produce deterministic results. Thanks to Andy Wilkinson.
* [LOG4J2-2751](https://issues.apache.org/jira/browse/LOG4J2-2751):
Fix StackLocator.getCallerClass performance in cases where Reflection.getCallerClass is not accessible.
* [LOG4J2-2752](https://issues.apache.org/jira/browse/LOG4J2-2752):
MutableLogEvent and RingBufferLogEvent avoid StringBuffer and parameter array allocation unless reusable messages are used.
* [LOG4J2-2754](https://issues.apache.org/jira/browse/LOG4J2-2754):
LoaderUtil.getClassLoaders may discover additional loaders and no longer erroneously returns a result with a null element in some environments.
* [LOG4J2-2575](https://issues.apache.org/jira/browse/LOG4J2-2575):
CronExpression.getBeforeTime() would sometimes return incorrect result. Thanks to Nathan Friess.
* [LOG4J2-2762](https://issues.apache.org/jira/browse/LOG4J2-2762):
[JDBC] MS-SQL Server JDBC driver throws SQLServerException when inserting a null value for a VARBINARY column.
* [LOG4J2-2770](https://issues.apache.org/jira/browse/LOG4J2-2770):
NullPointerException after reconfiguring via JMX. Thanks to Bill Kuker.

### Changes
* [LOG4J2-2789](https://issues.apache.org/jira/browse/LOG4J2-2789):
Conditionally perform status logging calculations in PluginRegistry. Thanks to Marius Volkhart.
* [LOG4J2-2782](https://issues.apache.org/jira/browse/LOG4J2-2782):
Use LinkedBlockingQueue instead of synchronized collction in StatusConfiguration.
* [LOG4J2-2777](https://issues.apache.org/jira/browse/LOG4J2-2777):
Add a retry count attribute to the KafkaAppender. Thanks to joongs4.
* [LOG4J2-2745](https://issues.apache.org/jira/browse/LOG4J2-2745):
Update log4j-slf4j18-impl slf4j version to 1.8.0-beta4 from 1.8.0-alpha2.
* [LOG4J2-2763](https://issues.apache.org/jira/browse/LOG4J2-2763):
Update dependencies.

---

Apache Log4j 2.13.1 requires a minimum of Java 8 to build and run. Log4j 2.3 was the
last release that supported Java 6 and Log4j 2.11.2 is the last release to support Java 7.

For complete information on Apache Log4j 2, including instructions on how to submit bug
reports, patches, or suggestions for improvement, see the Apache Apache Log4j 2 website:

https://logging.apache.org/log4j/2.x/
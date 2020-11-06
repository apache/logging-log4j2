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
# Apache Log4j 2.14.0 Release Notes

The Apache Log4j 2 team is pleased to announce the Log4j 2.14.0 release!

Apache Log4j is a well known framework for logging application behavior. Log4j 2 is an upgrade
to Log4j that provides significant improvements over its predecessor, Log4j 1.x, and provides
many other modern features such as support for Markers, lambda expressions for lazy logging,
property substitution using Lookups, multiple patterns on a PatternLayout and asynchronous
Loggers. Another notable Log4j 2 feature is the ability to be "garbage-free" (avoid allocating
temporary objects) while logging. In addition, Log4j 2 will not lose events while reconfiguring.

The artifacts may be downloaded from https://logging.apache.org/log4j/2.x/download.html.

This release contains a new Layout, JsonTemplateLayout, that is intended to ultimately replace JsonLayout. As its
name suggests it uses a template to define the elements to include in the JSON. This Layout was contributed by the
author of the log4j2-logstash-layout at GitHub, and who is now a member of the Log4j community.

Log4j 2.14.0 adds support for MongoDB 4 and removes support for MongoDB 2.

This release also contains a number of bug fixes which are listed below.

Due to a break in compatibility in the SLF4J binding, Log4j now ships with two versions of the SLF4J to Log4j adapters.
log4j-slf4j-impl should be used with SLF4J 1.7.x and earlier and log4j-slf4j18-impl should be used with SLF4J 1.8.x and
later.

The Log4j 2.14.0 API, as well as many core components, maintains binary compatibility with previous releases.

## GA Release 2.14.0

Changes in this version include:

### New Features
* [LOG4J2-2957](https://issues.apache.org/jira/browse/LOG4J2-2957):
Add JsonTemplateLayout.
* [LOG4J2-2848](https://issues.apache.org/jira/browse/LOG4J2-2848):
Create module log4j-mongodb4 to use new major version 4 MongoDB driver.
* [LOG4J2-2858](https://issues.apache.org/jira/browse/LOG4J2-2858):
More flexible configuration of the Disruptor WaitStrategy. Thanks to Stepan Gorban.

### Fixed Bugs
* [LOG4J2-2925](https://issues.apache.org/jira/browse/LOG4J2-2925):
Fix broken link in FAQ.
* [LOG4J2-2911](https://issues.apache.org/jira/browse/LOG4J2-2911):
Log4j2EventListener in spring.cloud.config.client listens for wrong event.
* [LOG4J2-2919](https://issues.apache.org/jira/browse/LOG4J2-2919):
Call ReliabilityStrategy's beforeStopAppenders() method before stopping AsyncAppender. Thanks to Geng Yuanzhe.
* [LOG4J2-2906](https://issues.apache.org/jira/browse/LOG4J2-2906):
Fix UnsupportedOperationException when initializing the Log4j2CloudConfigLoggingSystem. Thanks to Stephen Joyner.
* [LOG4J2-2908](https://issues.apache.org/jira/browse/LOG4J2-2908):
Move Spring Lookup and Spring PropertySource to its own module.
* [LOG4J2-2910](https://issues.apache.org/jira/browse/LOG4J2-2910):
Log4j-web should now stores the servlet context as a map entry instead of in the single external context field.
* [LOG4J2-2822](https://issues.apache.org/jira/browse/LOG4J2-2822):
Javadoc link in ThreadContext description was incorrect.
* [LOG4J2-2894](https://issues.apache.org/jira/browse/LOG4J2-2894):
Fix spelling error in log message.
* [LOG4J2-2901](https://issues.apache.org/jira/browse/LOG4J2-2901):
Missing configuration files should be ignored when creating a composite configuration.
* [LOG4J2-2883](https://issues.apache.org/jira/browse/LOG4J2-2883):
When using DirectFileRolloverStrategy the file pattern was not being recalculated on
        size based rollover after a time based rollover had occurred.
* [LOG4J2-2875](https://issues.apache.org/jira/browse/LOG4J2-2875):
Rollover was failing to create directories when using a DirectFileeRolloverStrategy.
* [LOG4J2-2859](https://issues.apache.org/jira/browse/LOG4J2-2859):
Fixed typos where mergeFactory should be mergeStrategy. Thanks to Yanming Zhou.
* [LOG4J2-2832](https://issues.apache.org/jira/browse/LOG4J2-2832):
Correct class name printed in error message in RollingFileAppender. Thanks to Benjamin Asbach.
* [LOG4J2-2882](https://issues.apache.org/jira/browse/LOG4J2-2882):
Support java.util.logging filters when using that API. Thanks to Emmanuel Bourg.
* [LOG4J2-2880](https://issues.apache.org/jira/browse/LOG4J2-2880):
Create StackWalker benchmark. Revert back to StackWalker.walk based on benchmark results.
* [LOG4J2-2867](https://issues.apache.org/jira/browse/LOG4J2-2867):
Obtain ContextDataProviders asynchronously.
* [LOG4J2-2877](https://issues.apache.org/jira/browse/LOG4J2-2877):
Determine the container id to obtain container and image information.
* [LOG4J2-2844](https://issues.apache.org/jira/browse/LOG4J2-2844):
Null pointer exception when no network interfaces are available.
* [LOG4J2-2895](https://issues.apache.org/jira/browse/LOG4J2-2895):
Fix potential deadlock in asynchronous logging by avoiding blocking for queue space on Log4jThreads
* [LOG4J2-2837](https://issues.apache.org/jira/browse/LOG4J2-2837):
Disruptor and JUL no longer recursively start the AsyncLoggerDisruptor
        resulting in an extra disruptor background thread constantly waiting.
* [LOG4J2-2867](https://issues.apache.org/jira/browse/LOG4J2-2867):
RingBufferLogEventTranslator uses a static ContextDataInjector instead of initializing a new object
        on each thread.
* [LOG4J2-2898](https://issues.apache.org/jira/browse/LOG4J2-2898):
Avoid initializing volatile fields with default values. Thanks to Turbanov Andrey.
* [LOG4J2-2899](https://issues.apache.org/jira/browse/LOG4J2-2899):
Fix log4j-1.2-api LogEventWrapper threadId and priority accessors when called multiple times.
* [LOG4J2-2939](https://issues.apache.org/jira/browse/LOG4J2-2939):
Fix NPE in MDCContextMap on 'contains' and 'isEmpty' invocations. Thanks to Constantin Hirsch.
* [LOG4J2-2954](https://issues.apache.org/jira/browse/LOG4J2-2954):
Prevent premature garbage collection of shutdown hooks in DefaultShutdownCallbackRegistry. Thanks to Henry Tung.

### Changes
* [LOG4J2-2889](https://issues.apache.org/jira/browse/LOG4J2-2889):
Add date pattern support for HTML layout. Thanks to Geng Yuanzhe.
* [LOG4J2-2892](https://issues.apache.org/jira/browse/LOG4J2-2892):
Allow GelfLayout to produce newline delimited events. Thanks to Jakub Lukes.
* [](https://issues.apache.org/jira/browse/LOG4J2-2892):
Update MongoDB tests to require Java 8 unconditionally now that Log4j requires Java 8.
* [](https://issues.apache.org/jira/browse/LOG4J2-2892):
Update mongodb3.version from 3.12.1 to 3.12.6.
* [](https://issues.apache.org/jira/browse/LOG4J2-2892):
Update com.fasterxml.jackson.* 2.10.2 -&gt; 2.11.0.
* [](https://issues.apache.org/jira/browse/LOG4J2-2892):
Update org.apache.activemq:activemq-broker 5.15.11 -&gt; 5.16.0.
* [](https://issues.apache.org/jira/browse/LOG4J2-2892):
Update org.apache.commons:commons-compress 1.19 -&gt; 1.20.
* [](https://issues.apache.org/jira/browse/LOG4J2-2892):
Update org.apache.commons:commons-csv 1.7 -&gt; 1.8.
* [](https://issues.apache.org/jira/browse/LOG4J2-2892):
Update org.apache.commons:commons-lang3 3.9 -&gt; 3.10.
* [](https://issues.apache.org/jira/browse/LOG4J2-2892):
Update org.codehaus.groovy:* 2.5.6 -&gt; 3.0.5.
* [](https://issues.apache.org/jira/browse/LOG4J2-2892):
Update tests junit:junit 4.12 -&gt; 4.13.
* [](https://issues.apache.org/jira/browse/LOG4J2-2892):
Update tests commons-io:commons-io 2.6 -&gt; 2.7.
* [](https://issues.apache.org/jira/browse/LOG4J2-2892):
Update jackson 2.11.0 -&gt; 2.11.2.
* [](https://issues.apache.org/jira/browse/LOG4J2-2892):
Update tests hsqldb 2.5.0 -&gt; 2.5.1.

### Removed
* [LOG4J2-2851](https://issues.apache.org/jira/browse/LOG4J2-2851):
Drop log4j-mongodb2 module.
---

Apache Log4j 2.14.0 requires a minimum of Java 8 to build and run. Log4j 2.12.1 is the last release to support
Java 7.

For complete information on Apache Log4j 2, including instructions on how to submit bug
reports, patches, or suggestions for improvement, see the Apache Apache Log4j 2 website:

https://logging.apache.org/log4j/2.x/
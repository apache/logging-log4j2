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
# Apache Log4j 2.13.0 Release Notes

The Apache Log4j 2 team is pleased to announce the Log4j 2.13.0 release!

Apache Log4j is a well known framework for logging application behavior. Log4j 2 is an upgrade
to Log4j that provides significant improvements over its predecessor, Log4j 1.x, and provides
many other modern features such as support for Markers, lambda expressions for lazy logging,
property substitution using Lookups, multiple patterns on a PatternLayout and asynchronous
Loggers. Another notable Log4j 2 feature is the ability to be "garbage-free" (avoid allocating
temporary objects) while logging. In addition, Log4j 2 will not lose events while reconfiguring.

The artifacts may be downloaded from https://logging.apache.org/log4j/2.x/download.html.

This release contains bugfixes and minor enhancements. Some of the new features in this release are:

1. Log4j 2 now requires Java 8 or higher to build and run.
1. Experimental support for Log4j 1 configuration files. See
[Log4j 2 Compatiblity with Log4j 1](https://logging.apache.org/log4j/2.x//manual/compatiblity.html).
1. The Logger API has been enhanced to support a builder pattern. This can dramatically improve the overhead of
capturing location information. See [Log Builder](https://logging.apache.org/log4j/2.x//manual/logbuilder.html).
1. Better integration with Spring Boot by providing access to Spring variables in Log4j 2 configuration files and
allowing Log4j 2 system properties to be defined in the Spring configuration.
See [Logging in the Cloud](manual/cloud.html#Managing_Logging_Configuration).
1. Support for accessing Kubernetes information via a Log4j 2 Lookup.
1. The Gelf Layout now allows the message to be formatted using a PatternLayout pattern.
[Logging in the Cloud](https://logging.apache.org/log4j/2.x//manual/cloud.html#Log4j_Configuration) provides an example of this, as well
as the use of the Spring and Kubernetes Lookups.

Due to a break in compatibility in the SLF4J binding, Log4j now ships with two versions of the SLF4J to Log4j adapters.
log4j-slf4j-impl should be used with SLF4J 1.7.x and earlier and log4j-slf4j18-impl should be used with SLF4J 1.8.x and
later.

Note that the XML, JSON and YAML formats changed in the 2.11.0 release: they no longer have the "timeMillis" attribute
and instead have an "Instant" element with "epochSecond" and "nanoOfSecond" attributes.

The Log4j 2.13.0 API, as well as many core components, maintains binary compatibility with previous releases.

## GA Release 2.13.0

Changes in this version include:

### New Features
* [LOG4J2-2732](https://issues.apache.org/jira/browse/LOG4J2-2732):
Add ThreadContext.putIfNotNull method. Thanks to Matt Pavlovich.
* [LOG4J2-2731](https://issues.apache.org/jira/browse/LOG4J2-2731):
Add a Level Patttern Selector.
* [LOG4J2-63](https://issues.apache.org/jira/browse/LOG4J2-63):
Add experimental support for Log4j 1 configuration files.
* [LOG4J2-2716](https://issues.apache.org/jira/browse/LOG4J2-2716):
Add the ability to lookup Kubernetes attributes in the Log4j configuration. Allow Log4j properties to
        be retrieved from the Spring environment if it is available.
* [LOG4J2-2710](https://issues.apache.org/jira/browse/LOG4J2-2710):
Allow Spring Boot application properties to be accessed in the Log4j 2 configuration. Add
        lower and upper case Lookups.
* [LOG4J2-2639](https://issues.apache.org/jira/browse/LOG4J2-2639):
Add builder pattern to Logger interface.

### Fixed Bugs
* [LOG4J2-2058](https://issues.apache.org/jira/browse/LOG4J2-2058):
Prevent recursive calls to java.util.LogManager.getLogger().
* [LOG4J2-2725](https://issues.apache.org/jira/browse/LOG4J2-2725):
LOG4J2-2725 - Added try/finally around event.execute() for RingBufferLogEventHandler to clear memory
        correctly in case of exception/error Thanks to Dzmitry Anikechanka.
* [LOG4J2-2635](https://issues.apache.org/jira/browse/LOG4J2-2635):
Wrong java version check in ThreadNameCachingStrategy. Thanks to Filipp Gunbin.
* [LOG4J2-2674](https://issues.apache.org/jira/browse/LOG4J2-2674):
Use a less confusing name for the CompositeConfiguration source. Thanks to Anton Korenkov.
* [LOG4J2-2727](https://issues.apache.org/jira/browse/LOG4J2-2727):
Add setKey method to Kafka Appender Builder. Thanks to Clément Mathieu.
* [LOG4J2-2707](https://issues.apache.org/jira/browse/LOG4J2-2707):
ArrayIndexOutOfBoundsException could occur with MAC address longer than 6 bytes. Thanks to Christian Frank.
* [LOG4J2-2712](https://issues.apache.org/jira/browse/LOG4J2-2712):
The rolling file appenders would fail to compress the file after rollover if the file name matched the
        file pattern.
* [LOG4J2-2693](https://issues.apache.org/jira/browse/LOG4J2-2693):
@PluginValue does not support attribute names besides "value".
* [LOG4J2-2647](https://issues.apache.org/jira/browse/LOG4J2-2647):
Validation blocks definition of script in properties configuration.
* [LOG4J2-2680](https://issues.apache.org/jira/browse/LOG4J2-2680):
Set result of rename action to true if file was copied. Thanks to Guillermo Xavier Hurtado Garcia.
* [LOG4J-2672](https://issues.apache.org/jira/browse/LOG4J-2672):
Add automatic module names where missing. Thanks to Stephen Colebourne.
* [LOG4J2-2673](https://issues.apache.org/jira/browse/LOG4J2-2673):
OutputStreamAppender.Builder ignores setFilter(). Thanks to Yuichi Sugimura.
* [LOG4J2-2725](https://issues.apache.org/jira/browse/LOG4J2-2725):
Prevent a memory leak when async loggers throw errors. Thanks to Dzmitry Anikechanka.

### Changes
* [LOG4J2-2701](https://issues.apache.org/jira/browse/LOG4J2-2701):
Update Jackson to 2.9.10.
* [LOG4J2-2709](https://issues.apache.org/jira/browse/LOG4J2-2709):
Allow message portion of GELF layout to be formatted using a PatternLayout. Allow
        ThreadContext attributes to be explicitly included or excluded in the GelfLayout.

---

Apache Log4j 2.13.0 requires a minimum of Java 8 to build and run. Log4j 2.3 was the
last release that supported Java 6 and Log4j 2.11.2 is the last release to support Java 7.

For complete information on Apache Log4j 2, including instructions on how to submit bug
reports, patches, or suggestions for improvement, see the Apache Apache Log4j 2 website:

https://logging.apache.org/log4j/2.x/
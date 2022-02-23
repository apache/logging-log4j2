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
# Apache Log4j 2.17.2 Release Notes

The Apache Log4j 2 team is pleased to announce the Log4j 2.17.2 release!

Apache Log4j is a well known framework for logging application behavior. Log4j 2 is an upgrade
to Log4j that provides significant improvements over its predecessor, Log4j 1.x, and provides
many other modern features such as support for Markers, lambda expressions for lazy logging,
property substitution using Lookups, multiple patterns on a PatternLayout and asynchronous
Loggers. Another notable Log4j 2 feature is the ability to be "garbage-free" (avoid allocating
temporary objects) while logging. In addition, Log4j 2 will not lose events while reconfiguring.

The artifacts may be downloaded from https://logging.apache.org/log4j/2.x/download.html.

This release contains the changes noted below:

* Over 50 improvements and fixes to the Log4j 1.x support. Continued testing has shown it is a suitable replacement
for Log4j 1.x in most cases.
* Scripting now requires a system property be specified naming the languages the user wishes to allow. The scripting
engine will not load if the property isn't set.
* By default, the only remote protocol allowed for loading configuration files is HTTPS. Users can specify a system
property to allow others or prevent remote loading entirely.
* Variable resolution has been modified so that only properties defined as properties in the configuration file can be
recursive. All other Lookups are now non-recursive. This addresses issues users were having resolving lookups specified
in property definitions for use in the RoutingAppender and RollingFileAppender due to restrictions put in place in 2.17.1.
* Many other fixes and improvements.

Due to a break in compatibility in the SLF4J binding, Log4j now ships with two versions of the SLF4J to Log4j adapters.
log4j-slf4j-impl should be used with SLF4J 1.7.x and earlier and log4j-slf4j18-impl should be used with SLF4J 1.8.x and
later. SLF4J-2.0.0 alpha releases are not fully supported. See https://issues.apache.org/jira/browse/LOG4J2-2975 and
https://jira.qos.ch/browse/SLF4J-511.

The Log4j 2.17.2 API, as well as many core components, maintains binary compatibility with previous releases.

## GA Release 2.17.2

Changes in this version include:

### New Features
* [LOG4J2-3297](https://issues.apache.org/jira/browse/LOG4J2-3297):
Limit loading of configuration via a url to https by default.
* [LOG4J2-2486](https://issues.apache.org/jira/browse/LOG4J2-2486):
Require log4j2.Script.enableLanguages to be specified to enable scripting for specific languages.
* [LOG4J2-3303](https://issues.apache.org/jira/browse/LOG4J2-3303):
Add TB support to FileSize. Thanks to ramananravi.
* [LOG4J2-3282](https://issues.apache.org/jira/browse/LOG4J2-3282):
Add the log4j-to-jul JDK Logging Bridge Thanks to Michael Vorburger.
* [](https://issues.apache.org/jira/browse/LOG4J2-3282):
Add org.apache.logging.log4j.core.appender.AsyncAppender.getAppenders() to more easily port from org.apache.log4j.AsyncAppender.getAllAppenders().
* [](https://issues.apache.org/jira/browse/LOG4J2-3282):
Add Configurator.setLevel(Logger, Level), setLevel(String, String), and setLevel(Class, Level). Thanks to Gary Gregory.
* [LOG4J2-3341](https://issues.apache.org/jira/browse/LOG4J2-3341):
Add shorthand syntax for properties configuration format for specifying a logger level and appender refs.
* [LOG4J2-3391](https://issues.apache.org/jira/browse/LOG4J2-3391):
Add optional additional fields to NoSQLAppender. Thanks to Gary Gregory.

### Fixed Bugs
* [LOG4J2-3304](https://issues.apache.org/jira/browse/LOG4J2-3304):
Flag LogManager as initiialized if the LoggerFactory is provided as a property. Thanks to francis-FY.
* [LOG4J2-3404](https://issues.apache.org/jira/browse/LOG4J2-3404):
Fix DefaultConfiguration leak in PatternLayout Thanks to Piotr Karwasz.
* [LOG4J2-3405](https://issues.apache.org/jira/browse/LOG4J2-3405):
Document that the Spring Boot Lookup requires the log4j-spring-boot dependency.
* [LOG4J2-3317](https://issues.apache.org/jira/browse/LOG4J2-3317):
Fix RoutingAppender backcompat and disallow recursive evaluation of lookup results outside of configuration properties.
* [LOG4J2-3333](https://issues.apache.org/jira/browse/LOG4J2-3333):
Fix ThreadContextDataInjector initialization deadlock
* [LOG4J2-3358](https://issues.apache.org/jira/browse/LOG4J2-3358):
Fix substitutions when programmatic configuration is used
* [LOG4J2-3306](https://issues.apache.org/jira/browse/LOG4J2-3306):
OptionConverter could cause a StackOverflowError.
* [](https://issues.apache.org/jira/browse/LOG4J2-3306):
Log4j 1.2 bridge class ConsoleAppender should extend WriterAppender and provide better compatibility with custom appenders.
* [](https://issues.apache.org/jira/browse/LOG4J2-3306):
Log4j 1.2 bridge method NDC.inherit(Stack) should not use generics to provide source compatibility.
* [](https://issues.apache.org/jira/browse/LOG4J2-3306):
Log4j 1.2 bridge class PatternLayout is missing constants DEFAULT_CONVERSION_PATTERN and TTCC_CONVERSION_PATTERN.
* [](https://issues.apache.org/jira/browse/LOG4J2-3306):
Log4j 1.2 bridge class PropertyConfigurator should implement Configurator.
* [](https://issues.apache.org/jira/browse/LOG4J2-3306):
Log4j 1.2 bridge interface Configurator doConfigure() methods should use LoggerRepository, not LoggerContext.
* [](https://issues.apache.org/jira/browse/LOG4J2-3306):
Log4j 1.2 bridge class OptionConverter is missing selectAndConfigure() methods.
* [](https://issues.apache.org/jira/browse/LOG4J2-3306):
Log4j 1.2 bridge class Category should implement AppenderAttachable.
* [](https://issues.apache.org/jira/browse/LOG4J2-3306):
Log4j 1.2 bridge method Category.exists(String) should be static.
* [](https://issues.apache.org/jira/browse/LOG4J2-3306):
Log4j 1.2 bridge methods missing in org.apache.log4j.Category: getDefaultHierarchy(), getHierarchy(), getLoggerRepository().
* [](https://issues.apache.org/jira/browse/LOG4J2-3306):
Log4j 1.2 bridge class LogManager default constructor should be public.
* [](https://issues.apache.org/jira/browse/LOG4J2-3306):
Log4j 1.2 bridge interface org.apache.log4j.spi.RendererSupport was in the wrong package and incomplete.
* [](https://issues.apache.org/jira/browse/LOG4J2-3306):
Log4j 1.2 bridge interfaces missing from package org.apache.log4j.spi: ThrowableRenderer, ThrowableRendererSupport, TriggeringEventEvaluator.
* [](https://issues.apache.org/jira/browse/LOG4J2-3306):
Log4j 1.2 bridge missing class org.apache.log4j.or.RendererMap.
* [LOG4J2-3281](https://issues.apache.org/jira/browse/LOG4J2-3281):
Log4j 1.2 bridge PropertiesConfiguration.buildAppender not adding filters to custom appender.
* [LOG4J2-3316](https://issues.apache.org/jira/browse/LOG4J2-3316):
Log4j 1.2 bridge should ignore case in properties file keys.
* [](https://issues.apache.org/jira/browse/LOG4J2-3316):
Log4j 1.2 bridge adds org.apache.log4j.component.helpers.Constants.
* [](https://issues.apache.org/jira/browse/LOG4J2-3316):
Log4j 1.2 bridge adds org.apache.log4j.helpers.LogLog.
* [](https://issues.apache.org/jira/browse/LOG4J2-3316):
Log4j 1.2 bridge adds org.apache.log4j.helpers.Loader.
* [](https://issues.apache.org/jira/browse/LOG4J2-3316):
Log4j 1.2 bridge adds org.apache.log4j.spi.RootLogger.
* [](https://issues.apache.org/jira/browse/LOG4J2-3316):
Log4j 1.2 bridge class Category is missing some protected instance variables.
* [](https://issues.apache.org/jira/browse/LOG4J2-3316):
Log4j 1.2 bridge adds org.apache.log4j.Hierarchy.
* [](https://issues.apache.org/jira/browse/LOG4J2-3316):
Log4j 1.2 bridge methods Category.getChainedPriority() and getEffectiveLevel() should not be final.
* [](https://issues.apache.org/jira/browse/LOG4J2-3316):
Log4j 1.2 bridge adds org.apache.log4j.spi.NOPLoggerRepository and NOPLogger.
* [](https://issues.apache.org/jira/browse/LOG4J2-3316):
Log4j 1.2 bridge adds org.apache.log4j.spi.DefaultRepositorySelector.
* [](https://issues.apache.org/jira/browse/LOG4J2-3316):
Log4j 1.2 bridge implements LogManager.getCurrentLoggers() fully.
* [LOG4J2-3326](https://issues.apache.org/jira/browse/LOG4J2-3326):
Log4j 1.2 bridge fixes parsing filters in properties configuration file #680. Thanks to Benjamin Röhl, Gary Gregory.
* [LOG4J2-3326](https://issues.apache.org/jira/browse/LOG4J2-3326):
Log4j 1.2 bridge missing OptionConverter.instantiateByKey(Properties, String, Class, Object). Thanks to Gary Gregory.
* [LOG4J2-3326](https://issues.apache.org/jira/browse/LOG4J2-3326):
Log4j 1.2 bridge class org.apache.log4j.spi.LoggingEvent missing constructors and public instance variable. Thanks to Gary Gregory.
* [LOG4J2-3328](https://issues.apache.org/jira/browse/LOG4J2-3328):
Log4j 1.2 bridge does not support system properties in log4j.xml. Thanks to Gary Gregory.
* [](https://issues.apache.org/jira/browse/LOG4J2-3328):
Log4j 1.2 bridge now logs a warning instead of throwing an NullPointerException when building a Syslog appender with a missing "SyslogHost" param. Thanks to Gary Gregory.
* [](https://issues.apache.org/jira/browse/LOG4J2-3328):
Log4j 1.2 bridge should allow property and XML attributes to start with either an upper-case or lower-case letter. Thanks to Gary Gregory, Piotr P. Karwasz.
* [](https://issues.apache.org/jira/browse/LOG4J2-3328):
Log4j 1.2 bridge uses the wrong default values for a TTCCLayout #709. Thanks to Gary Gregory, Piotr P. Karwasz.
* [](https://issues.apache.org/jira/browse/LOG4J2-3328):
Log4j 1.2 bridge throws ClassCastException when using SimpleLayout and others #708. Thanks to Gary Gregory, Piotr P. Karwasz.
* [](https://issues.apache.org/jira/browse/LOG4J2-3328):
Log4j 1.2 bridge uses the wrong file pattern for rolling file appenders #710. Thanks to Gary Gregory, Piotr P. Karwasz.
* [](https://issues.apache.org/jira/browse/LOG4J2-3328):
Log4j 1.2 bridge throws ClassCastException when using SimpleLayout and others #708. Thanks to Gary Gregory, Piotr P. Karwasz.
* [](https://issues.apache.org/jira/browse/LOG4J2-3328):
Log4j 1.2 bridge creates a SocketAppender instead of a SyslogAppender. Thanks to Gary Gregory.
* [](https://issues.apache.org/jira/browse/LOG4J2-3328):
Log4j 1.2 bridge uses some incorrect default property values in some appenders. Thanks to Piotr P. Karwasz.
* [](https://issues.apache.org/jira/browse/LOG4J2-3328):
Log4j 1.2 bridge supports the SocketAppender. Thanks to Gary Gregory.
* [](https://issues.apache.org/jira/browse/LOG4J2-3328):
Log4j 1.2 bridge missing DefaultThrowableRenderer. Thanks to Gary Gregory.
* [](https://issues.apache.org/jira/browse/LOG4J2-3328):
Log4j 1.2 bridge missing some ThrowableInformation constructors. Thanks to Gary Gregory.
* [](https://issues.apache.org/jira/browse/LOG4J2-3328):
Log4j 1.2 bridge missing some LocationInfo constructors. Thanks to Gary Gregory.
* [](https://issues.apache.org/jira/browse/LOG4J2-3328):
Log4j 1.2 bridge missed Thanks to Gary Gregory.
* [](https://issues.apache.org/jira/browse/LOG4J2-3328):
Log4j 1.2 bridge missed org.apache.log4j.pattern.FormattingInfo. Thanks to Gary Gregory.
* [](https://issues.apache.org/jira/browse/LOG4J2-3328):
Log4j 1.2 bridge missed org.apache.log4j.pattern.NameAbbreviator. Thanks to Gary Gregory.
* [](https://issues.apache.org/jira/browse/LOG4J2-3328):
Log4j 1.2 bridge missing UtilLoggingLevel. Thanks to Gary Gregory.
* [](https://issues.apache.org/jira/browse/LOG4J2-3328):
Log4j 1.2 bridge missing FormattingInfo. Thanks to Gary Gregory.
* [](https://issues.apache.org/jira/browse/LOG4J2-3328):
Log4j 1.2 bridge missing PatternConverter. Thanks to Gary Gregory.
* [](https://issues.apache.org/jira/browse/LOG4J2-3328):
Log4j 1.2 bridge missing PatternParser. Thanks to Gary Gregory.
* [](https://issues.apache.org/jira/browse/LOG4J2-3328):
Log4j 1.2 bridge issues with filters #753. Thanks to ppkarwasz, Gary Gregory.
* [](https://issues.apache.org/jira/browse/LOG4J2-3328):
Log4j 1.2 bridge implements most of DOMConfigurator. Thanks to Gary Gregory.
* [](https://issues.apache.org/jira/browse/LOG4J2-3328):
JndiManager reverts to 2.17.0 behavior: Read the system property for each call.
* [LOG4J2-3330](https://issues.apache.org/jira/browse/LOG4J2-3330):
Configurator.setLevel not fetching the correct LoggerContext. Thanks to Mircea Lemnaru, Gary Gregory.
* [](https://issues.apache.org/jira/browse/LOG4J2-3330):
Fix DTD error: Add missing ELEMENT for Marker.
* [](https://issues.apache.org/jira/browse/LOG4J2-3330):
Fix log4j-jakarta-web service file #723. Thanks to Gary Gregory, Piotr P. Karwasz.
* [LOG4J2-3392](https://issues.apache.org/jira/browse/LOG4J2-3392):
AppenderLoggingException logging any exception to a MongoDB Appender. Thanks to Gary Gregory, Omer U.
* [LOG4J2-3392](https://issues.apache.org/jira/browse/LOG4J2-3392):
Possible NullPointerException in MongoDb4DocumentObject, MongoDbDocumentObject, DefaultNoSqlObject. Thanks to Gary Gregory.
* [](https://issues.apache.org/jira/browse/LOG4J2-3392):
Trim whitespace before parsing a String into an Integer. Thanks to Gary Gregory.
* [LOG4J2-3410](https://issues.apache.org/jira/browse/LOG4J2-3410):
Log4j 1.2 bridge throws a ClassCastException when logging a Map with non-String keys. Thanks to Barry Sham, Gary Gregory.
* [LOG4J2-3407](https://issues.apache.org/jira/browse/LOG4J2-3407):
Log4j 1.2 bridge Check for non-existent appender when parsing properties #761. Thanks to Kenny MacLeod.
* [LOG4J2-3407](https://issues.apache.org/jira/browse/LOG4J2-3407):
Log4j 1.2 bridge supports global threshold #764. Thanks to Piotr P. Karwasz.

### Changes
* [LOG4J2-3267](https://issues.apache.org/jira/browse/LOG4J2-3267):
Change modifier of method org.apache.logging.log4j.core.tools.Generate#generate to public (was package private) to facilitate automated code generation.

---

Apache Log4j 2.17.2 requires a minimum of Java 8 to build and run.
Log4j 2.12.4 is the last release to support Java 7.
Log4j 2.3.2 is the last release to support Java 6.
Java 6 and Java 7 are no longer supported by the Log4j team.

For complete information on Apache Log4j 2, including instructions on how to submit bug
reports, patches, or suggestions for improvement, see the Apache Apache Log4j 2 website:

https://logging.apache.org/log4j/2.x/

---

Earlier release notes are accessible in [Release History](https://logging.apache.org/log4j/2.x/changes-report.html).

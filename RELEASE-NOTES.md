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

# Release 3.0.0-alpha1

This is the first release of the 3.x version of Log4j.

The major changes in Log4j 3.x include:

* Many of the Log4j modules are now full JPMS modules.
* Many optional components, such as Scripting, JNDI, JPA and JMS, have been moved to their own modules, making
  Log4j-core slightly smaller in 3.x and aiding in security by not having jars with unwanted behaviors present.
* All plugins constructed using Log4j 3.x are now located using Java's ServiceLoader instead of the custom
  mechanism used in Log4j 2.x.
* Log4j's annotation processor has been individually packaged separate from Log4j-core and the plugin system it enables.
* Log4j 3.x now uses an internal dependency injection framework to allow plugins to be injected with instances of
  classes they are dependent on.
* Many system properties used by Log4j can now be set to apply to a single LoggerContext making configuration
  in application frameworks that support multiple applications more flexible.
* Some deprecated classes have been removed. However, every attempt has been made to ensure that user code compiled
  for Log4j 2.x will continue to operate with the Log4j 3.x libraries present instead.

## Changes

### Added

*   Allow plugins to be created through more flexible dependency injection patterns. (for [LOG4J2-1188](https://issues.apache.org/jira/browse/LOG4J2-1188) by Matt Sicker)

*   Convert documentation into AsciiDoc format. (for [LOG4J2-1802](https://issues.apache.org/jira/browse/LOG4J2-1802) by Matt Sicker)

*   Implement JUL Bridge Handler. (for [LOG4J2-2025](https://issues.apache.org/jira/browse/LOG4J2-2025) by Ralph Goers, Thies Wellpott)

*   Allow to force LOG4J2 to use TCCL only. (for [LOG4J2-2171](https://issues.apache.org/jira/browse/LOG4J2-2171) by `rmannibucau`)

*   Allow web lookup of session attributes. (for [LOG4J2-2688](https://issues.apache.org/jira/browse/LOG4J2-2688) by Ralph Goers, Romain Manni-Bucau)

*   Add support for injecting plugin configuration via builder methods. (for [LOG4J2-2700](https://issues.apache.org/jira/browse/LOG4J2-2700) by Matt Sicker)

*   Add scopes API for customizing plugin instance lifecycle. (for [LOG4J2-2852](https://issues.apache.org/jira/browse/LOG4J2-2852) by Matt Sicker)

*   Add qualifier annotations for distinguishing instances of the same type. (for [LOG4J2-2853](https://issues.apache.org/jira/browse/LOG4J2-2853) by Matt Sicker)

*   Create standardized dependency injection API. This is supported in several plugin categories and other configurable instances previously defined via system properties. (for [LOG4J2-2854](https://issues.apache.org/jira/browse/LOG4J2-2854) by Matt Sicker)

*   Add conditional annotations to support more declarative binding factory bundle classes. (for [LOG4J2-3300](https://issues.apache.org/jira/browse/LOG4J2-3300) by Matt Sicker)

*   Add built-in JSON configuration parser for a useful structured configuration file format which only requires the `java.base` module. (for [LOG4J2-3415](https://issues.apache.org/jira/browse/LOG4J2-3415) by Matt Sicker)

*   Add `@Ordered` annotation to support plugin ordering when two or more plugins within the same category have the same case-insensitive name. (for [LOG4J2-857](https://issues.apache.org/jira/browse/LOG4J2-857) by Matt Sicker)


### Changed

*   Simplify Maven `site` phase and align it with the one in `release-2.x` branch (for [1220](https://github.com/apache/logging-log4j2/pull/1220) by Volkan Yazıcı)

*   Switch the issue tracker from [JIRA](https://issues.apache.org/jira/browse/LOG4J2) to [GitHub Issues](https://github.com/apache/logging-log4j2/issues) (for [1221](https://github.com/apache/logging-log4j2/pull/1221) by Volkan Yazıcı)

*   Update Conversant Disruptor from 1.12.10 to 1.12.11. (for [LOG4J2-2079](https://issues.apache.org/jira/browse/LOG4J2-2079) by Gary Gregory)

*   Update Apache Flume from 1.7.0 to 1.8.0. (for [LOG4J2-2082](https://issues.apache.org/jira/browse/LOG4J2-2082) by Gary Gregory)

*   Update Eclipse javax.persistence from 2.1.1 to 2.2.0. (for [LOG4J2-2083](https://issues.apache.org/jira/browse/LOG4J2-2083) by Gary Gregory)

*   Update build to expect Java 8 sources and generate Java 8 byte codes. (for [LOG4J2-2083](https://issues.apache.org/jira/browse/LOG4J2-2083) by Gary Gregory)

*   Renamed package core.util.datetime to core.time.internal.format to clarify these classes are to be considered private. (for [LOG4J2-2224](https://issues.apache.org/jira/browse/LOG4J2-2224) by Remko Popma)

*   Moved time-related classes from core.util to core.time. Classes considered private moved to core.time.internal. (for [LOG4J2-2225](https://issues.apache.org/jira/browse/LOG4J2-2225) by Remko Popma)

*   Split off Kafka support into a new module log4j-kafka. (for [LOG4J2-2227](https://issues.apache.org/jira/browse/LOG4J2-2227) by Gary Gregory)

*   Split off ZeroMq/JeroMq support into a new module log4j-jeromq. (for [LOG4J2-2228](https://issues.apache.org/jira/browse/LOG4J2-2228) by Gary Gregory)

*   Split off SMTP support into a new module log4j-smtp. (for [LOG4J2-2230](https://issues.apache.org/jira/browse/LOG4J2-2230) by Gary Gregory)

*   Move CSV layout from log4j-core to a new module log4j-csv. (for [LOG4J2-2231](https://issues.apache.org/jira/browse/LOG4J2-2231) by Gary Gregory)

*   Move JMS code to a new module log4j-jms. (for [LOG4J2-2232](https://issues.apache.org/jira/browse/LOG4J2-2232) by Gary Gregory)

*   Move JDBC code to a new module log4j-jdbc. (for [LOG4J2-2233](https://issues.apache.org/jira/browse/LOG4J2-2233) by Gary Gregory)

*   Move Jackson-based layouts to their own modules: JSON, XML, and YAML. (for [LOG4J2-2237](https://issues.apache.org/jira/browse/LOG4J2-2237) by Gary Gregory)

*   Use Spotbugs instead of Findbugs. Minimum version is Java 8. (for [LOG4J2-2255](https://issues.apache.org/jira/browse/LOG4J2-2255) by Ralph Goers)

*   Update LMAX Disruptor from 3.3.7 to 3.3.8. (for [LOG4J2-2258](https://issues.apache.org/jira/browse/LOG4J2-2258) by Gary Gregory)

*   Update Conversant Disruptor 1.2.11 to 1.2.13. (for [LOG4J2-2380](https://issues.apache.org/jira/browse/LOG4J2-2380) by Gary Gregory)

*   Update org.eclipse.persistence.jpa from 2.7.1 to 2.7.2 (for [LOG4J2-2383](https://issues.apache.org/jira/browse/LOG4J2-2383) by Gary Gregory)

*   Update optional Apache Commons DBCP from 2.4.0 to 2.5.0. (for [LOG4J2-2387](https://issues.apache.org/jira/browse/LOG4J2-2387) by Gary Gregory)

*   Update org.eclipse.persistence:javax.persistence from 2.2.0 to 2.2.1. (for [LOG4J2-2473](https://issues.apache.org/jira/browse/LOG4J2-2473) by Gary Gregory)

*   Update builder methods from the "with" prefix to the "set" prefix. (for [LOG4J2-2492](https://issues.apache.org/jira/browse/LOG4J2-2492) by Gary Gregory)

*   Remove deprecated code. (for [LOG4J2-2493](https://issues.apache.org/jira/browse/LOG4J2-2493) by Gary Gregory)

*   Allow web lookup to access more information. (for [LOG4J2-2523](https://issues.apache.org/jira/browse/LOG4J2-2523) by Ralph Goers, Romain Manni-Bucau)

*   Update conversant disruptor from 1.2.13 to 1.2.15. (for [LOG4J2-2571](https://issues.apache.org/jira/browse/LOG4J2-2571) by Gary Gregory)

*   Update Apache Flume from 1.8.0 to 1.9.0. (for [LOG4J2-2572](https://issues.apache.org/jira/browse/LOG4J2-2572) by Gary Gregory)

*   Fix typo in method MergeStrategy.mergeConfigurations. (for [LOG4J2-2617](https://issues.apache.org/jira/browse/LOG4J2-2617) by Matt Sicker)

*   Separate plugin support to its own module. Plugin annotation processor will now generate a Java source file compatible with java.util.ServiceLoader instead of a binary file. (for [LOG4J2-2621](https://issues.apache.org/jira/browse/LOG4J2-2621) by Ralph Goers)

*   Rename PluginVisitor and related classes to ConfigurationInjectionBuilder. (for [LOG4J2-2683](https://issues.apache.org/jira/browse/LOG4J2-2683) by Matt Sicker)

*   Locate plugins in modules. (for [LOG4J2-2690](https://issues.apache.org/jira/browse/LOG4J2-2690) by Ralph Goers)

*   Remove support for `java.io.Serializable` in several classes including `Message`, `Layout`, `LogEvent`, `Logger`, and `ReadOnlyStringMap`. (for [LOG4J2-3228](https://issues.apache.org/jira/browse/LOG4J2-3228) by Matt Sicker)

*   Move Scripting to its own module. (for [LOG4J2-3307](https://issues.apache.org/jira/browse/LOG4J2-3307) by Ralph Goers)

*   Defer loading of StrLookup plugin classes until first usage. (for [LOG4J2-3441](https://issues.apache.org/jira/browse/LOG4J2-3441) by Matt Sicker)

*   Flatten the ThreadContextMap interfaces with default methods. (for [LOG4J2-3626](https://issues.apache.org/jira/browse/LOG4J2-3626) by Matt Sicker)

*   Replace `maven-changes-plugin` with a custom changelog implementation (for [LOG4J2-3628](https://issues.apache.org/jira/browse/LOG4J2-3628) by Volkan Yazıcı)

*   Allow Log4j properties to be provided in JSON files. (for [LOG4J2-3658](https://issues.apache.org/jira/browse/LOG4J2-3658) by Ralph Goers)

*   Unify plugin builders and plugin factories. (for [LOG4J2-860](https://issues.apache.org/jira/browse/LOG4J2-860) by Matt Sicker)


### Fixed

*   org.apache.logging.log4j.core.lookup.EnvironmentLookup may throw NPE. (for [LOG4J2-2244](https://issues.apache.org/jira/browse/LOG4J2-2244) by Gary Gregory)

*   Move ProcessIdUtil from log4j-api to log4j-core. (for [LOG4J2-2279](https://issues.apache.org/jira/browse/LOG4J2-2279) by Gary Gregory, Remko Popma)

*   FixedDateFormat parses timezone offsets, -8:00 is interpreted as GMT-8:00. (for [LOG4J2-2306](https://issues.apache.org/jira/browse/LOG4J2-2306) by Carter Kozak)

*   Fix exception message in PropertiesConfigurationBuilder#createFilter(). (for [LOG4J2-2344](https://issues.apache.org/jira/browse/LOG4J2-2344) by Volkan Yazıcı, dengliming)

*   RoutingAppender.BuilderlsetPurgePolicy fluently returns the builder instance. (for [LOG4J2-2545](https://issues.apache.org/jira/browse/LOG4J2-2545) by Carter Kozak)

*   JsonLayout KeyValuePair should discard blank JSON keys. (for [LOG4J2-2749](https://issues.apache.org/jira/browse/LOG4J2-2749) by Volkan Yazıcı, Oleksii Khomchenko)

*   Reduce Log4j 2 initialization time by deferring loading Plugin classes. (for [LOG4J2-2795](https://issues.apache.org/jira/browse/LOG4J2-2795) by Ralph Goers)

*   Fixes incorrect constructor call in LocalizedMessageFactory. (for [LOG4J2-2850](https://issues.apache.org/jira/browse/LOG4J2-2850) by Volkan Yazıcı, sandeepbarnwal)

*   Move JNDI to its own module. (for [LOG4J2-3242](https://issues.apache.org/jira/browse/LOG4J2-3242) by Ralph Goers)

*   Fix file descriptor leak on Tomcat. (for [LOG4J2-3663](https://issues.apache.org/jira/browse/LOG4J2-3663) by `lenoch7`, Piotr P. Karwasz)


* * *

Copyright © 1999-2023 [The Apache Software Foundation](http://www.apache.org). All Rights Reserved.  
Apache Logging, Apache Log4j, Log4j, Apache, the Apache feather logo, and the Apache Logging project logo are trademarks of The Apache Software Foundation.
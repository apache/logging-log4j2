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

# Release 2.20.0 (2023-02-17)

This release primarily contains bug fixes and minor enhancements.

Due to a break in compatibility in the SLF4J binding, Log4j now ships with two versions of the SLF4J to Log4j adapters. `log4j-slf4j-impl` should be used with SLF4J 1.7.x and earlier and `log4j-slf4j18-impl` should be used with SLF4J 1.8.x and later. SLF4J-2.0.0 alpha releases are not fully supported. See [LOG4J2-2975](https://issues.apache.org/jira/browse/LOG4J2-2975) and [SLF4J-511](https://jira.qos.ch/browse/SLF4J-511).

The Log4j 2.19.0 API, as well as many core components, maintains binary compatibility with previous releases.

Apache Log4j 2.19.0 requires a minimum of Java 8 to build and run. Log4j 2.12.4 is the last release to support Java 7. Log4j 2.3.2 is the last release to support Java 6. Java 6 and Java 7 are no longer supported by the Log4j team.

For complete information on Apache Log4j 2, including instructions on how to submit bug reports, patches, or suggestions for improvement, see [the Apache Log4j 2 website](http://logging.apache.org/log4j/2.x/).

## Changes

### Added
*   Add support for timezones in `RollingFileAppender` date pattern (for [LOG4J2-1631](https://issues.apache.org/jira/browse/LOG4J2-1631) by Piotr P. Karwasz, Danas Mikelinskas)
*   Add `LogEvent` timestamp to `ProducerRecord` in `KafkaAppender` (for [LOG4J2-2678](https://issues.apache.org/jira/browse/LOG4J2-2678) by Piotr P. Karwasz, Federico D’Ambrosio)
*   Add `PatternLayout` support for abbreviating the name of all logger components except the 2 rightmost (for [LOG4J2-2785](https://issues.apache.org/jira/browse/LOG4J2-2785) by Ralph Goers, Markus Spann)
*   Removes internal field that leaked into public API. (for [LOG4J2-3615](https://issues.apache.org/jira/browse/LOG4J2-3615) by Piotr P. Karwasz)
*   Add a `LogBuilder#logAndGet()` method to emulate the `Logger#traceEntry` method. (for [LOG4J2-3645](https://issues.apache.org/jira/browse/LOG4J2-3645) by Piotr P. Karwasz)

### Changed
*   Simplify site generation (for [1166](https://github.com/apache/logging-log4j2/pull/1166) by Volkan Yazıcı
*   Switch the issue tracker from [JIRA](https://issues.apache.org/jira/browse/LOG4J2) to [GitHub Issues](https://github.com/apache/logging-log4j2/issues) (for [1172](https://github.com/apache/logging-log4j2/pull/1172) by Volkan Yazıcı)
*   Remove liquibase-log4j2 maven module (for [1193](https://github.com/apache/logging-log4j2/pull/1193) by StevenMassaro)
*   Fix order of stacktrace elements, that causes cache misses in `ThrowableProxyHelper`. (for [1214](https://github.com/apache/logging-log4j2/pull/1214) by `alex-dubrouski`, Piotr P. Karwasz)
*   Switch from `com.sun.mail` to Eclipse Angus. (for [LOG4J2-3554](https://issues.apache.org/jira/browse/LOG4J2-3554) by Oleh Astappiev, Piotr P. Karwasz)
*   Add Log4j2 Core as default runtime dependency of the SLF4J2-to-Log4j2 API bridge. (for [LOG4J2-3601](https://issues.apache.org/jira/browse/LOG4J2-3601) by `afs`, Piotr P. Karwasz)
*   Replace `maven-changes-plugin` with a custom changelog implementation (for [LOG4J2-3628](https://issues.apache.org/jira/browse/LOG4J2-3628) by Volkan Yazıcı)

### Deprecated
*   Deprecate support for package scanning for plugins (for [LOG4J2-3644](https://issues.apache.org/jira/browse/LOG4J2-3644) by Ralph Goers)

### Fixed
*   Copy programmatically supplied location even if `includeLocation="false"`. (for [1197](https://github.com/apache/logging-log4j2/pull/1197) by Piotr P. Karwasz)
*   Eliminate status logger warning, when `disableAnsi` or `noConsoleNoAnsi` is used the style and highlight patterns. (for [1202](https://github.com/apache/logging-log4j2/pull/1202) by `wleese`, Piotr P. Karwasz)
*   Fix detection of location requirements in `RewriteAppender`. (for [1274](https://github.com/apache/logging-log4j2/pull/1274) by `amirhadadi`, Piotr P. Karwasz)
*   Replace regex with manual code to escape characters in Rfc5424Layout. (for [1277](https://github.com/apache/logging-log4j2/pull/1277) by `adwsingh`)
*   Fix `java.sql.Time` object formatting in `MapMessage` (for [LOG4J2-2297](https://issues.apache.org/jira/browse/LOG4J2-2297) by Ralph Goers)
*   Fix previous fire time computation in `CronTriggeringPolicy` (for [LOG4J2-3357](https://issues.apache.org/jira/browse/LOG4J2-3357) by Ralph Goers)
*   Correct default to not include location for `AsyncRootLogger`s (for [LOG4J2-3487](https://issues.apache.org/jira/browse/LOG4J2-3487) by Ralph Goers, Dave Messink)
*   Lazily evaluate the level of a SLF4J `LogEventBuilder` (for [LOG4J2-3598](https://issues.apache.org/jira/browse/LOG4J2-3598) by Piotr P. Karwasz)
*   Fixes priority of Legacy system properties, which are now back to having higher priority than Environment variables. (for [LOG4J2-3615](https://issues.apache.org/jira/browse/LOG4J2-3621) by `adwsingh`, Piotr P. Karwasz)
*   Protects `ServiceLoaderUtil` from unchecked `ServiceLoader` exceptions. (for [LOG4J2-3624](https://issues.apache.org/jira/browse/LOG4J2-3624) by Piotr P. Karwasz)
*   Fix `Configurator#setLevel` for internal classes (for [LOG4J2-3631](https://issues.apache.org/jira/browse/LOG4J2-3631) by Piotr P. Karwasz, Jeff Thomas)
*   Fix level propagation in `Log4jBridgeHandler` (for [LOG4J2-3634](https://issues.apache.org/jira/browse/LOG4J2-3634) by Piotr P. Karwasz, Marcel Koch)
*   Disable `OsgiServiceLocator` if not running in OSGI container. (for [LOG4J2-3642](https://issues.apache.org/jira/browse/LOG4J2-3642) by `adwsingh`, Piotr P. Karwasz)
*   When using a Date Lookup in the file pattern the current time should be used. (for [LOG4J2-3643](https://issues.apache.org/jira/browse/LOG4J2-3643) by Ralph Goers)
*   Fixed `LogBuilder` filtering in the presence of global filters. (for [LOG4J2-3647](https://issues.apache.org/jira/browse/LOG4J2-3647) by Piotr P. Karwasz)

* * *

Copyright © 1999-2023 [The Apache Software Foundation](https://www.apache.org). All Rights Reserved.  
Apache Logging, Apache Log4j, Log4j, Apache, the Apache feather logo, and the Apache Logging project logo are trademarks of The Apache Software Foundation.
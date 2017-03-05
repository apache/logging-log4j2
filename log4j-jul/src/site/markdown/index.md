<!-- vim: set syn=markdown : -->
<!--
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

# Log4j JDK Logging Adapter

The JDK Logging Adapter is a custom implementation of
[`java.util.logging.LogManager`](http://docs.oracle.com/javase/6/docs/api/java/util/logging/LogManager.html)
that uses [Log4j](../log4j-core/index.html). This adapter can be used with either the Log4j API or
Log4j Core. When used with the API, there are a couple features of JUL that aren't supported. However, this
does allow any other Log4j Provider besides the Core provider to be used with JUL.

## Requirements

The JDK Logging Adapter is dependent on the Log4j API and optionally Log4j Core.
For more information, see [Runtime Dependencies](../runtime-dependencies.html).

## Usage

To use the JDK Logging Adapter, you must set the system property `java.util.logging.manager` to
[`org.apache.logging.log4j.jul.LogManager`](apidocs/org/apache/logging/log4j/jul/LogManager.html)

This must be done either through the command line (i.e., using the
`-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager` argument) or by using
`System.setProperty()` before any calls are made to `LogManager` or `Logger`.

## Compatibility

The use of a
[`java.util.logging.Filter`](http://docs.oracle.com/javase/6/docs/api/java/util/logging/Filter.html)
is supported on a per-[`Logger`](http://docs.oracle.com/javase/6/docs/api/java/util/logging/Logger.html)
basis. However, it is recommended to use the standard [Filters](../manual/filters.html) feature in
Log4j instead.

The use of
[`java.util.logging.Handler`](http://docs.oracle.com/javase/6/docs/api/java/util/logging/Handler.html)
classes is _NOT_ supported. Custom Handlers should instead use an appropriate
[Appender](../manual/appenders.html) or code their own
[`Appender`](../log4j-core/apidocs/org/apache/logging/log4j/core/Appender.html)
plugin.

Java logging levels are translated into Log4j logging levels dynamically. The following table lists the
conversions between a Java logging level and its equivalent Log4j level. Custom levels should be implemented
as an implementation of
[`LevelConverter`](apidocs/org/apache/logging/log4j/jul/LevelConverter.html), and the
Log4j property `log4j.jul.levelConverter` must be set to your custom class name. Using the default
`LevelConverter` implementation, custom logging levels are mapped to whatever the current level of
the `Logger` being logged to is using.

### Default Level Conversions

Java Level | Log4j Level
---------- | -----------
[`OFF`](http://docs.oracle.com/javase/6/docs/api/java/util/logging/Level.html#OFF) | `OFF`
[`SEVERE`](http://docs.oracle.com/javase/6/docs/api/java/util/logging/Level.html#SEVERE) | `ERROR`
[`WARNING`](http://docs.oracle.com/javase/6/docs/api/java/util/logging/Level.html#WARNING) | `WARN`
[`INFO`](http://docs.oracle.com/javase/6/docs/api/java/util/logging/Level.html#INFO) | `INFO`
[`CONFIG`](http://docs.oracle.com/javase/6/docs/api/java/util/logging/Level.html#CONFIG) | [`CONFIG`](apidocs/org/apache/logging/log4j/jul/LevelTranslator.html#CONFIG)
[`FINE`](http://docs.oracle.com/javase/6/docs/api/java/util/logging/Level.html#FINE) | `DEBUG`
[`FINER`](http://docs.oracle.com/javase/6/docs/api/java/util/logging/Level.html#FINER) | `TRACE`
[`FINEST`](http://docs.oracle.com/javase/6/docs/api/java/util/logging/Level.html#FINEST) | [`FINEST`](apidocs/org/apache/logging/log4j/jul/LevelTranslator.html#FINEST)
[`ALL`](http://docs.oracle.com/javase/6/docs/api/java/util/logging/Level.html#ALL) | `ALL`

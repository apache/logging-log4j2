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
# Apache Log4j 2.11.2 Release Notes

The Apache Log4j 2 team is pleased to announce the Log4j 2.11.2 release!

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

As of Log4j 2.9.0, the Log4j API was modified to use java.util.ServiceLoader to locate Log4j implementations,
although the former binding mechanism is still supported. The Log4j API jar is now a multi-release jar
to provide implementations of Java 9 specific classes. Multi-release jars are not supported by
the OSGi specification so OSGi modules will not be able to take advantage of these implementations
but will not lose functionality as they will fall back to the implementations used in Java 7 and 8. Applications
using Spring Boot must add the Multi-Release header to the jar manifest or the Java 9+ classes will be
ignored.

More details on the  fixes are itemized below. Note that some tools are not compatible
with multi-release jars and may fail trying to process class files in the META-INF/versions/9 folder.
Those errors should be reported to the tool vendor.

Note that subsequent to the 2.9.0 release, for security reasons, Log4j does not process DTD in XML files.
If you used DTD for including snippets, you have to use XInclude or Composite Configuration instead.

Also subsequent to the 2.9.0 release, for security reasons, SerializedLayout is deprecated and no
longer used as default in the Socket and JMS appenders. SerializedLayout can still be used as before,
but has to be specified explicitly. To retain old behaviour, you have to change configuration like:

    <Appenders>
      <Socket name="socket" host="localhost" port="9500"/>
    </Appenders>

into:

    <Appenders>
      <Socket name="socket" host="localhost" port="9500">
        <SerializedLayout/>
      </Socket>
    </Appenders>

We do, however, discourage the use of SerializedLayout and recommend JsonLayout as a replacement:

    <Appenders>
      <Socket name="socket" host="localhost" port="9500">
        <JsonLayout properties="true"/>
      </Socket>
    </Appenders>

Note that the XML, JSON and YAML formats changed in the 2.11.0 release: they no longer have the "timeMillis" attribute
and instead have an "Instant" element with "epochSecond" and "nanoOfSecond" attributes.

The Log4j 2.11.2 API, as well as many core components, maintains binary compatibility with previous releases.

## GA Release 2.11.2

Changes in this version include:

### New Features
* [LOG4J2-2496](https://issues.apache.org/jira/browse/LOG4J2-2496):
JDBC Appender should reconnect to the database when a connection goes stale.
* [LOG4J2-2505](https://issues.apache.org/jira/browse/LOG4J2-2505):
Let JDBC PoolingDriverConnectionSource with Apache Commons DBCP configure a PoolableConnectionFactory.
* [LOG4J2-2509](https://issues.apache.org/jira/browse/LOG4J2-2509):
Allow a JDBC Appender to truncate strings to match a table's metadata column length limit.
* [LOG4J2-1246](https://issues.apache.org/jira/browse/LOG4J2-1246):
PatternLayout %date conversion pattern should render time zone designator for ISO-ISO8601.

### Fixed Bugs
* [LOG4J2-2500](https://issues.apache.org/jira/browse/LOG4J2-2500):
Document that Properties element must be the first configuration element.
* [LOG4J2-2543](https://issues.apache.org/jira/browse/LOG4J2-2543):
Add Log4j-to-SLF4J to BOM pom.xml. Thanks to Dermot Hardy.
* [LOG4J2-2061](https://issues.apache.org/jira/browse/LOG4J2-2061):
Use the file pattern as the FileManager "name" when no filename is present.
* [LOG4J2-2009](https://issues.apache.org/jira/browse/LOG4J2-2009):
Expose LoggerContext.setConfiguration as a public method.
* [LOG4J2-2542](https://issues.apache.org/jira/browse/LOG4J2-2542):
CronTriggeringPolicy was not rolling properly, especially when used with the SizeBasedTriggeringPolicy.
* [LOG4J2-2266](https://issues.apache.org/jira/browse/LOG4J2-2266):
Load PropertySources from any accessible ClassLoader. Hide any exceptions that may occur accessing a PropertySource.
* [LOG4J2-1570](https://issues.apache.org/jira/browse/LOG4J2-1570):
Logging with a lambda expression with a method call that also logs would cause logs within method call to reference line num and method name of the parent method.
* [LOG4J2-2485](https://issues.apache.org/jira/browse/LOG4J2-2485):
SizeBasedTriggeringPolicy was not honored when using the DirectWriteRolloverStrategy if the machine restarts. Thanks to Giovanni Matteo Fumarola.
* [LOG4J2-1906](https://issues.apache.org/jira/browse/LOG4J2-1906):
Direct write was creating files with the wrong date/time.
* [LOG4J2-2453](https://issues.apache.org/jira/browse/LOG4J2-2453):
Add Log4j-slf4j18-impl dependency to bom pom. Thanks to theit.
* [LOG4J2-2515](https://issues.apache.org/jira/browse/LOG4J2-2515):
Configuration documentation referenced incorrect method name. Thanks to MakarovS.
* [LOG4J2-2514](https://issues.apache.org/jira/browse/LOG4J2-2514):
Make Strings.toRootUpperCase a static method so it can be accessed. Thanks to smilebrian0515.
* [LOG4J2-1571](https://issues.apache.org/jira/browse/LOG4J2-1571):
Fixed Appenders section in Extending Log4j. Thanks to torbenmoeller.
* [LOG4J2-2397](https://issues.apache.org/jira/browse/LOG4J2-2397):
Predeployment of PersistenceUnit that using Log4j as session logger failed (#198). Thanks to EckelDong.
* [LOG4J2-2365](https://issues.apache.org/jira/browse/LOG4J2-2365):
NameAbbreviator correctly abbreviates first fragments (#188). Thanks to Eugene Zimichev.
* [LOG4J2-2201](https://issues.apache.org/jira/browse/LOG4J2-2201):
Fix memory leak in ReusableParameterizedMessage.
* [LOG4J2-2363](https://issues.apache.org/jira/browse/LOG4J2-2363):
ReusableObjectMessage parameter is properly passed to appenders (#203). Thanks to Brian Laub.
* [LOG4J2-2418](https://issues.apache.org/jira/browse/LOG4J2-2418):
NullPointerException when closing never used RollingRandomAccessFileAppender. Thanks to Jonas Rutishauser.
* [LOG4J2-2422](https://issues.apache.org/jira/browse/LOG4J2-2422):
Handle some unchecked exceptions while loading plugins. Thanks to rswart, Gary Gregory.
* [LOG4J2-2441](https://issues.apache.org/jira/browse/LOG4J2-2441):
Setting a null ErrorHandler on AbstractAppender is not allowed and will no-op as expected.
* [LOG4J2-2444](https://issues.apache.org/jira/browse/LOG4J2-2444):
ErrorHandler is invoked with a LogEvent and Throwable when possible, where previously only a string was used.
* [LOG4J2-2413](https://issues.apache.org/jira/browse/LOG4J2-2413):
Exceptions are added to all columns when a JDBC Appender's ColumnMapping uses a Pattern. Thanks to Andres Luuk, Gary Gregory.
* [LOG4J2-2466](https://issues.apache.org/jira/browse/LOG4J2-2466):
ColumnMapping literal not working. Thanks to Paolo Bonanomi, Gary Gregory.
* [LOG4J2-2478](https://issues.apache.org/jira/browse/LOG4J2-2478):
AbstractStringLayoutStringEncodingBenchmark returns the computed variables on each benchmark to avoid DCE. Thanks to Diego Elias Costa.
* [LOG4J2-2134](https://issues.apache.org/jira/browse/LOG4J2-2134):
StackOverflowError at AwaitCompletionReliabilityStrategy. Thanks to David del Amo Mateos, Gary Gregory.
* [LOG4J2-2481](https://issues.apache.org/jira/browse/LOG4J2-2481):
Avoid NullPointerExceptions in org.apache.logging.log4j.core.config.AbstractConfiguration for null arguments.
* [LOG4J2-2457](https://issues.apache.org/jira/browse/LOG4J2-2457):
RollingRandomAccessFileManager ignores new file patterns from programmatic reconfiguration. Thanks to Heiko Schwanke, Gary Gregory.
* [LOG4J2-2482](https://issues.apache.org/jira/browse/LOG4J2-2482):
BasicContextSelector cannot be used in a OSGI application. Thanks to Rob Gansevles.
* [LOG4J2-2476](https://issues.apache.org/jira/browse/LOG4J2-2476):
org.apache.log4j.SimpleLayout and ConsoleAppender missing in log4j-1.2-api. Thanks to Al Bundy.
* [LOG4J2-2497](https://issues.apache.org/jira/browse/LOG4J2-2497):
JmsAppender reconnectIntervalMillis cannot be set from a configuration file.
* [LOG4J2-2499](https://issues.apache.org/jira/browse/LOG4J2-2499):
JMS Appender may throw a NullPointerException when JMS is not up while the Appender is starting.
* [LOG4J2-2508](https://issues.apache.org/jira/browse/LOG4J2-2508):
JDBC Appender fails when using both parameter, source, and literal ColumnMapping elements.
* [LOG4J2-2527](https://issues.apache.org/jira/browse/LOG4J2-2527):
Prevent ConcurrentModificationException while iterating over ListAppender events.
* [LOG4J2-2522](https://issues.apache.org/jira/browse/LOG4J2-2522):
Fix regression using MapMessageLookup.lookup with MapMessages that do not implement StringMapMessage. Thanks to Adam Lesiak.
* [LOG4J2-2530](https://issues.apache.org/jira/browse/LOG4J2-2530):
Generalize checks using MapMessage implementations with do not extend StringMapMessage.
        Introduce new JAVA_UNQUOTED MapMessage format type based on the JAVA formatting, but without
        quoted values. Thanks to Travis Spencer.
* [LOG4J2-2533](https://issues.apache.org/jira/browse/LOG4J2-2533):
Fix a regression introduced by LOG4J2-2301 in 2.11.1 allowing allocation to occur in AsyncLoggerConfig. Thanks to Michail Prusakov.

### Changes
* [LOG4J2-1576](https://issues.apache.org/jira/browse/LOG4J2-1576):
Switch from CLIRR to RevAPI for detecting API changes.
* [LOG4J2-2391](https://issues.apache.org/jira/browse/LOG4J2-2391):
Improve exception logging performance. ThrowableProxy construction uses a faster
        method to discover the current stack trace. ThrowablePatternConverter and
        ExtendedThrowablePatternConverter default configurations no longer allocate
        an additional buffer for stack trace contents.
* [LOG4J2-2447](https://issues.apache.org/jira/browse/LOG4J2-2447):
Let the NullAppender default its name to "null".
* [LOG4J2-2468](https://issues.apache.org/jira/browse/LOG4J2-2468):
Update Jackson from 2.9.6 to 2.9.7.
* [LOG4J2-2469](https://issues.apache.org/jira/browse/LOG4J2-2469):
Update Apache Commons Compress from 1.17 to 1.18.
* [LOG4J2-2470](https://issues.apache.org/jira/browse/LOG4J2-2470):
Update Apache Commons CSV from 1.5 to 1.6.
* [LOG4J2-2471](https://issues.apache.org/jira/browse/LOG4J2-2471):
Update javax.mail from 1.6.1 to 1.6.2.
* [LOG4J2-2472](https://issues.apache.org/jira/browse/LOG4J2-2472):
Update mongo-java-driver 3 from 3.8.0 to 3.8.2.
* [LOG4J2-2489](https://issues.apache.org/jira/browse/LOG4J2-2489):
JDBC Appender should release parameter resources ASAP.
* [LOG4J2-2491](https://issues.apache.org/jira/browse/LOG4J2-2491):
Allow all Appenders to optionally carry a Property array.
* [LOG4J2-2405](https://issues.apache.org/jira/browse/LOG4J2-2405):
Better handling of %highlight pattern when using jul-bridge. Thanks to Marco Herrn.
* [LOG4J2-2503](https://issues.apache.org/jira/browse/LOG4J2-2503):
Update MongoDB driver from 3.8.2 to 3.9.0 for log4j-mongodb3 module.

---

Apache Log4j 2.11.2 requires a minimum of Java 7 to build and run. Log4j 2.3 was the
last release that supported Java 6.

Basic compatibility with Log4j 1.x is provided through the log4j-1.2-api component, however it
does not implement some of the very implementation specific classes and methods. The package
names and Maven groupId have been changed to org.apache.logging.log4j to avoid any conflicts
with log4j 1.x.

For complete information on Apache Log4j 2, including instructions on how to submit bug
reports, patches, or suggestions for improvement, see the Apache Apache Log4j 2 website:

https://logging.apache.org/log4j/2.x/
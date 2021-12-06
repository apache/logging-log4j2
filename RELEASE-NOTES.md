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
# Apache Log4j 2.15.0 Release Notes

The Apache Log4j 2 team is pleased to announce the Log4j 2.15.0 release!

Apache Log4j is a well known framework for logging application behavior. Log4j 2 is an upgrade
to Log4j that provides significant improvements over its predecessor, Log4j 1.x, and provides
many other modern features such as support for Markers, lambda expressions for lazy logging,
property substitution using Lookups, multiple patterns on a PatternLayout and asynchronous
Loggers. Another notable Log4j 2 feature is the ability to be "garbage-free" (avoid allocating
temporary objects) while logging. In addition, Log4j 2 will not lose events while reconfiguring.

The artifacts may be downloaded from https://logging.apache.org/log4j/2.x/download.html.

This release contains a number of bug fixes and minor enhancements which are listed below.

Due to a break in compatibility in the SLF4J binding, Log4j now ships with two versions of the SLF4J to Log4j adapters.
log4j-slf4j-impl should be used with SLF4J 1.7.x and earlier and log4j-slf4j18-impl should be used with SLF4J 1.8.x and
later. SLF4J-2.0.0 alpha releases are not fully supported. See https://issues.apache.org/jira/browse/LOG4J2-2975 and
https://jira.qos.ch/browse/SLF4J-511.

Some of the new features in Log4j 2.15.0 include:

* Support for Arbiters, which are conditionals that can enable sections of the logging configuration
for inclusion or exclusion. In particular, SpringProfile, SystemProperty, Script, and Class Arbiters have been
provided that use the Spring profile, System property, the result of a script, or the presence of a class respectively
to determine whether a section of configuration should be included.
* Support for Jakarta EE 9. This is functionally equivalent to Log4j's log4j-web module but uses the Jakarta project.
* Various performance improvements.

Key changes to note:

* Prior to this release Log4j would automatically resolve Lookups contained in the message or its parameters in the
Pattern Layout. Thisbehavior is no longer the default and must be enabled by specifying %msg{lookup}.
* The JNDI Lookup has been restricted to only support the java, ldap, and ldaps protocols by default. LDAP also no
longer supports classes that implement the Referenceable interface and restricts the Serializable classes to the
Java primative classes by default and requires an allow list to be specified to access remote LDAP servers.

The Log4j 2.15.0 API, as well as many core components, maintains binary compatibility with previous releases.

## GA Release 2.15.0

Changes in this version include:

### New Features
* [LOG4J2-3198](https://issues.apache.org/jira/browse/LOG4J2-3198):
Pattern layout no longer enables lookups within message text by default for cleaner API boundaries and reduced
        formatting overhead. The old 'log4j2.formatMsgNoLookups' which enabled this behavior has been removed as well
        as the 'nolookups' message pattern converter option. The old behavior can be enabled on a per-pattern basis
        using '%m{lookups}'.
* [LOG4J2-3194](https://issues.apache.org/jira/browse/LOG4J2-3194):
Allow fractional attributes for size attribute of SizeBsaedTriggeringPolicy. Thanks to markuss.
* [LOG4J2-2978](https://issues.apache.org/jira/browse/LOG4J2-2978):
Add support for Jakarta EE 9 (Tomcat 10 / Jetty 11) Thanks to Michael Seele.
* [LOG4J2-3189](https://issues.apache.org/jira/browse/LOG4J2-3189):
Improve NameAbbreviator worst-case performance.
* [LOG4J2-3170](https://issues.apache.org/jira/browse/LOG4J2-3170):
Make CRLF/HTML encoding run in O(n) worst-case time, rather than O(n^2). Thanks to Gareth Smith.
* [LOG4J2-3133](https://issues.apache.org/jira/browse/LOG4J2-3133):
Add missing slf4j-api singleton accessors to log4j-slf4j-impl (1.7) StaticMarkerBinder and StaticMDCBinder.
        This doesn't impact behavior or correctness, but avoids throwing and catching NoSuchMethodErrors when slf4j
        is initialized and avoids linkage linting warnings.
* [LOG4J2-2885](https://issues.apache.org/jira/browse/LOG4J2-2885):
Add support for US-style date patterns and micro/nano seconds to FixedDateTime. Thanks to Markus Spann.
* [LOG4J2-3116](https://issues.apache.org/jira/browse/LOG4J2-3116):
Add JsonTemplateLayout for Google Cloud Platform structured logging layout.
* [LOG4J2-3067](https://issues.apache.org/jira/browse/LOG4J2-3067):
Add CounterResolver to JsonTemplateLayout.
* [LOG4J2-3074](https://issues.apache.org/jira/browse/LOG4J2-3074):
Add replacement parameter to ReadOnlyStringMapResolver.
* [LOG4J2-3051](https://issues.apache.org/jira/browse/LOG4J2-3051):
Add CaseConverterResolver to JsonTemplateLayout.
* [LOG4J2-3064](https://issues.apache.org/jira/browse/LOG4J2-3064):
Add Arbiters and SpringProfile plugin.
* [LOG4J2-3056](https://issues.apache.org/jira/browse/LOG4J2-3056):
Refactor MD5 usage for sharing sensitive information. Thanks to Marcono1234.
* [LOG4J2-3004](https://issues.apache.org/jira/browse/LOG4J2-3004):
Add plugin support to JsonTemplateLayout.
* [LOG4J2-3050](https://issues.apache.org/jira/browse/LOG4J2-3050):
Allow AdditionalFields to be ignored if their value is null or a zero-length String.
* [LOG4J2-3049](https://issues.apache.org/jira/browse/LOG4J2-3049):
Allow MapMessage and ThreadContext attributes to be prefixed.
* [LOG4J2=3048](https://issues.apache.org/jira/browse/LOG4J2=3048):
Add improved MapMessge support to GelfLayout.
* [LOG4J2-3044](https://issues.apache.org/jira/browse/LOG4J2-3044):
Add RepeatPatternConverter.
* [LOG4J2-2940](https://issues.apache.org/jira/browse/LOG4J2-2940):
Context selectors are aware of their dependence upon the callers ClassLoader, allowing
        basic context selectors to avoid the unnecessary overhead of walking the stack to
        determine the caller's ClassLoader.
* [LOG4J2-2940](https://issues.apache.org/jira/browse/LOG4J2-2940):
Add BasicAsyncLoggerContextSelector equivalent to AsyncLoggerContextSelector for
        applications with a single LoggerContext. This selector avoids classloader lookup
        overhead incurred by the existing AsyncLoggerContextSelector.
* [LOG4J2-3041](https://issues.apache.org/jira/browse/LOG4J2-3041):
Allow a PatternSelector to be specified on GelfLayout.
* [LOG4J2-3141](https://issues.apache.org/jira/browse/LOG4J2-3141):
Avoid ThreadLocal overhead in RandomAccessFileAppender, RollingRandomAccessFileManager,
        and MemoryMappedFileManager due to the unused setEndOfBatch and isEndOfBatch methods.
        The methods on LogEvent are preferred.
* [LOG4J2-3144](https://issues.apache.org/jira/browse/LOG4J2-3144):
Prefer string.getBytes(Charset) over string.getBytes(String)
	based on performance improvements in modern Java releases.
* [LOG4J2-3171](https://issues.apache.org/jira/browse/LOG4J2-3171):
Improve PatternLayout performance by reducing unnecessary indirection and branching.

### Fixed Bugs
* [LOG4J2-3201](https://issues.apache.org/jira/browse/LOG4J2-3201):
Limit the protocols JNDI can use by default. Limit the servers and classes that can be accessed via LDAP.
* [LOG4J2-3114](https://issues.apache.org/jira/browse/LOG4J2-3114):
Enable immediate flush on RollingFileAppender when buffered i/o is not enabled. Thanks to Barnabas Bodnar.
* [LOG4J2-3168](https://issues.apache.org/jira/browse/LOG4J2-3168):
Fix bug when file names contain regex characters. Thanks to Benjamin Wöster.
* [LOG4J2-3110](https://issues.apache.org/jira/browse/LOG4J2-3110):
Fix the number of {}-placeholders in the string literal argument does not match the number of other arguments
        to the logging call. Thanks to Arturo Bernal.
* [LOG4J2-3060](https://issues.apache.org/jira/browse/LOG4J2-3060):
Fix thread-safety issues in DefaultErrorHandler. Thanks to Nikita Mikhailov.
* [LOG4J2-3185](https://issues.apache.org/jira/browse/LOG4J2-3185):
Fix thread-safety issues in DefaultErrorHandler. Thanks to mzbonnt.
* [LOG4J2-3183](https://issues.apache.org/jira/browse/LOG4J2-3183):
Avoid using MutableInstant of the event as a cache key in JsonTemplateLayout.
* [LOG4J2-2829](https://issues.apache.org/jira/browse/LOG4J2-2829):
SocketAppender should propagate failures when reconnection fails.
* [LOG4J2-3172](https://issues.apache.org/jira/browse/LOG4J2-3172):
Buffer immutable log events in the SmtpManager. Thanks to Barry Fleming.
* [LOG4J2-3175](https://issues.apache.org/jira/browse/LOG4J2-3175):
Avoid KafkaManager override when topics differ. Thanks to wuqian0808.
* [LOG4J2-3160](https://issues.apache.org/jira/browse/LOG4J2-3160):
Fix documentation on how to toggle log4j2.debug system property. Thanks to Lars Bohl.
* [LOG4J2-3159](https://issues.apache.org/jira/browse/LOG4J2-3159):
Fixed an unlikely race condition in Log4jMarker.getParents() volatile access.
* [LOG4J2-3153](https://issues.apache.org/jira/browse/LOG4J2-3153):
DatePatternConverter performance is not impacted by microsecond-precision clocks when such precision isn't
        required.
* [LOG4J2-2808](https://issues.apache.org/jira/browse/LOG4J2-2808):
LoggerContext skips resolving localhost when hostName is configured. Thanks to Asapha Halifa.
* [LOG4J2-3150](https://issues.apache.org/jira/browse/LOG4J2-3150):
RandomAccessFile appender uses the correct default buffer size of 256 kB
        rather than the default appender buffer size of 8 kB.
* [LOG4J2-3142](https://issues.apache.org/jira/browse/LOG4J2-3142):
log4j-1.2-api implements LogEventAdapter.getTimestamp() based on the original event timestamp
        instead of returning zero. Thanks to John Meikle.
* [LOG4J2-3083](https://issues.apache.org/jira/browse/LOG4J2-3083):
log4j-slf4j-impl and log4j-slf4j18-impl correctly detect the calling class using both LoggerFactory.getLogger
        methods as well as LoggerFactory.getILoggerFactory().getLogger.
* [LOG4J2-2816](https://issues.apache.org/jira/browse/LOG4J2-2816):
Handle Disruptor event translation exceptions. Thanks to Jacob Shields.
* [LOG4J2-3121](https://issues.apache.org/jira/browse/LOG4J2-3121):
log4j2 config modified at run-time may trigger incomplete MBean re-initialization due to InstanceAlreadyExistsException. Thanks to Markus Spann.
* [LOG4J2-3107](https://issues.apache.org/jira/browse/LOG4J2-3107):
SmtpManager.createManagerName ignores port. Thanks to Markus Spann.
* [LOG4J2-3080](https://issues.apache.org/jira/browse/LOG4J2-3080):
Use SimpleMessage in Log4j 1 Category whenever possible.
* [LOG4J2-3102](https://issues.apache.org/jira/browse/LOG4J2-3102):
Fix a regression in 2.14.1 which allowed the AsyncAppender background thread to keep the JVM alive because
        the daemon flag was not set.
* [LOG4J2-3103](https://issues.apache.org/jira/browse/LOG4J2-3103):
Fix race condition which can result in ConcurrentModificationException on context.stop. Thanks to Mike Glazer.
* [LOG4J2-3092](https://issues.apache.org/jira/browse/LOG4J2-3092):
Fix JsonWriter memory leaks due to retained excessive buffer growth. Thanks to xmh51.
* [LOG4J2-3089](https://issues.apache.org/jira/browse/LOG4J2-3089):
Fix sporadic JsonTemplateLayoutNullEventDelimiterTest failures on Windows. Thanks to Tim Perry.
* [LOG4J2-3075](https://issues.apache.org/jira/browse/LOG4J2-3075):
Fix formatting of nanoseconds in JsonTemplateLayout.
* [LOG4J2-3087](https://issues.apache.org/jira/browse/LOG4J2-3087):
Fix race in JsonTemplateLayout where a timestamp could end up unquoted. Thanks to Anton Klarén.
* [LOG4J2-3070](https://issues.apache.org/jira/browse/LOG4J2-3070):
Ensure EncodingPatternConverter#handlesThrowable is implemented. Thanks to Romain Manni-Bucau.
* [LOG4J2-3054](https://issues.apache.org/jira/browse/LOG4J2-3054):
BasicContextSelector hasContext and shutdown take the default context into account
* [LOG4J2-2940](https://issues.apache.org/jira/browse/LOG4J2-2940):
Slf4j implementations walk the stack at most once rather than twice to determine the caller's class loader.
* [LOG4J2-2965](https://issues.apache.org/jira/browse/LOG4J2-2965):
Fixed a deadlock between the AsyncLoggerContextSelector and java.util.logging.LogManager by updating Disruptor to 3.4.4.
* [LOG4J2-3095](https://issues.apache.org/jira/browse/LOG4J2-3095):
Category.setLevel should accept null value. Thanks to Kenny MacLeod, Gary Gregory.
* [LOG4J2-3174](https://issues.apache.org/jira/browse/LOG4J2-3174):
Wrong subject on mail when it depends on the LogEvent Thanks to romainmoreau.

### Changes
* [](https://issues.apache.org/jira/browse/):
Update Spring framework to 5.3.13, Spring Boot to 2.5.7, and Spring Cloud to 2020.0.4.
* [LOG4J2-2025](https://issues.apache.org/jira/browse/LOG4J2-2025):
Provide support for overriding the Tomcat Log class in Tomcat 8.5+.
* [](https://issues.apache.org/jira/browse/):
Updated dependencies.

        - com.fasterxml.jackson.core:jackson-annotations ................. 2.12.2 -> 2.12.4
        - com.fasterxml.jackson.core:jackson-core ........................ 2.12.2 -> 2.12.4
        - com.fasterxml.jackson.core:jackson-databind .................... 2.12.2 -> 2.12.4
        - com.fasterxml.jackson.dataformat:jackson-dataformat-xml ........ 2.12.2 -> 2.12.4
        - com.fasterxml.jackson.dataformat:jackson-dataformat-yaml ....... 2.12.2 -> 2.12.4
        - com.fasterxml.jackson.module:jackson-module-jaxb-annotations ... 2.12.2 -> 2.12.4
        - com.fasterxml.woodstox:woodstox-core ........................... 6.2.4 -> 6.2.6
        - commons-io:commons-io .......................................... 2.8.0 -> 2.11.0
        - net.javacrumbs.json-unit:json-unit ............................. 2.24.0 -> 2.25.0
        - net.javacrumbs.json-unit:json-unit ............................. 2.25.0 -> 2.27.0
        - org.apache.activemq:activemq-broker ............................ 5.16.1 -> 5.16.2
        - org.apache.activemq:activemq-broker ............................ 5.16.2 -> 5.16.3
        - org.apache.commons:commons-compress ............................ 1.20 -> 1.21
        - org.apache.commons:commons-csv ................................. 1.8 -> 1.9.0
        - org.apache.commons:commons-dbcp2 ............................... 2.8.0 -> 2.9.0
        - org.apache.commons:commons-pool2 ............................... 2.9.0 -> 2.11.1
        - org.apache.maven.plugins:maven-failsafe-plugin ................. 2.22.2 -> 3.0.0-M5
        - org.apache.maven.plugins:maven-surefire-plugin ................. 2.22.2 -> 3.0.0-M5
        - org.apache.rat:apache-rat-plugin ............................... 0.12 -> 0.13
        - org.assertj:assertj-core ....................................... 3.19.0 -> 3.20.2
        - org.codehaus.groovy:groovy-dateutil ............................ 3.0.7 -> 3.0.8
        - org.codehaus.groovy:groovy-jsr223 .............................. 3.0.7 -> 3.0.8
        - org.codehaus.plexus:plexus-utils ............................... 3.3.0 -> 3.4.0
        - org.eclipse.persistence:javax.persistence ...................... 2.1.1 -> 2.2.1
        - org.eclipse.persistence:org.eclipse.persistence.jpa ............ 2.6.5 -> 2.6.9
        - org.eclipse.persistence:org.eclipse.persistence.jpa ............ 2.7.8 -> 2.7.9
        - org.fusesource.jansi ........................................... 2.3.2 -> 2.3.4
        - org.fusesource.jansi:jansi ..................................... 2.3.1 -> 2.3.2
        - org.hsqldb:hsqldb .............................................. 2.5.1 -> 2.5.2
        - org.junit.jupiter:junit-jupiter-engine ......................... 5.7.1 -> 5.7.2
        - org.junit.jupiter:junit-jupiter-migrationsupport ............... 5.7.1 -> 5.7.2
        - org.junit.jupiter:junit-jupiter-params ......................... 5.7.1 -> 5.7.2
        - org.junit.vintage:junit-vintage-engine ......................... 5.7.1 -> 5.7.2
        - org.liquibase:liquibase-core ................................... 3.5.3 -> 3.5.5
        - org.mockito:mockito-core ....................................... 3.8.0 -> 3.11.2
        - org.mockito:mockito-junit-jupiter .............................. 3.8.0 -> 3.11.2
        - org.springframework:spring-aop ................................. 5.3.3 -> 5.3.9
        - org.springframework:spring-beans ............................... 5.3.3 -> 5.3.9
        - org.springframework:spring-context ............................. 5.3.3 -> 5.3.9
        - org.springframework:spring-context-support ..................... 5.3.3 -> 5.3.9
        - org.springframework:spring-core ................................ 5.3.3 -> 5.3.9
        - org.springframework:spring-expression .......................... 5.3.3 -> 5.3.9
        - org.springframework:spring-oxm ................................. 5.3.3 -> 5.3.9
        - org.springframework:spring-test ................................ 5.3.3 -> 5.3.9
        - org.springframework:spring-web ................................. 5.3.3 -> 5.3.9
        - org.springframework:spring-webmvc .............................. 5.3.3 -> 5.3.9
        - org.tukaani:xz ................................................. 1.8 -> 1.9

---

Apache Log4j 2.15.0 requires a minimum of Java 8 to build and run. Log4j 2.12.1 is the last release to support
Java 7. Java 7 is not longer supported by the Log4j team.

For complete information on Apache Log4j 2, including instructions on how to submit bug
reports, patches, or suggestions for improvement, see the Apache Apache Log4j 2 website:

https://logging.apache.org/log4j/2.x/
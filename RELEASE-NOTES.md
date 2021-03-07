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
# Apache Log4j 2.14.1 Release Notes

The Apache Log4j 2 team is pleased to announce the Log4j 2.14.1 release!

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
later. SLF4J-2.0.0 alpha releases are not fully supported. See https://issues.apache.org/jira/browse/LOG4J2-2975.

The Log4j 2.14.1 API, as well as many core components, maintains binary compatibility with previous releases.

## GA Release 2.14.1

Changes in this version include:

### New Features
* [LOG4J2-2962](https://issues.apache.org/jira/browse/LOG4J2-2962):
Enrich "map" resolver by unifying its backend with "mdc" resolver.
* [LOG4J2-2999](https://issues.apache.org/jira/browse/LOG4J2-2999):
Replace JsonTemplateLayout resolver configurations table in docs with sections.
* [LOG4J2-2993](https://issues.apache.org/jira/browse/LOG4J2-2993):
Support stack trace truncation in JsonTemplateLayout.

### Fixed Bugs
* [LOG4J2-3033](https://issues.apache.org/jira/browse/LOG4J2-3033):
Add log method with no parameters - i.e. it has an empty message.
* [LOG4J2-2947](https://issues.apache.org/jira/browse/LOG4J2-2947):
Document that LogBuilder default methods do nothing.
* [LOG4J2-2948](https://issues.apache.org/jira/browse/LOG4J2-2948):
Replace HashSet with IdentityHashMap in ParameterFormatter to detect cycles.
* [LOG4J2-3028](https://issues.apache.org/jira/browse/LOG4J2-3028):
OutputStreamManager.flushBuffer always resets the buffer, previously the buffer was not reset after an exception. Thanks to Jakub Kozlowski.
* [LOG4J2-2981](https://issues.apache.org/jira/browse/LOG4J2-2981):
OnStartupTriggeringPolicy would fail to cause the file to roll over with DirectWriteTriggeringPolicy
        unless minSize was set to 0.
* [LOG4J2-2990](https://issues.apache.org/jira/browse/LOG4J2-2990):
Reduce garbage by using putAll when copying the ThreadContext for SLF4J. Thanks to Diogo Monteiro.
* [LOG4J2-3006](https://issues.apache.org/jira/browse/LOG4J2-3006):
Directly create a thread instead of using the common ForkJoin pool when initializing ThreadContextDataInjector"
* [LOG4J2-2624](https://issues.apache.org/jira/browse/LOG4J2-2624):
Allow auto-shutdown of log4j in log4j-web to be turned off and provide a 
        ServletContextListener "Log4jShutdownOnContextDestroyedListener" to stop log4j.
        Register the listener at the top of web.xml to ensure the shutdown happens last. Thanks to Tim Perry.
* [LOG4J2-1606](https://issues.apache.org/jira/browse/LOG4J2-1606):
Allow auto-shutdown of log4j in log4j-web to be turned off and provide a 
        ServletContextListener "Log4jShutdownOnContextDestroyedListener" to stop log4j. 
        Register the listener at the top of web.xml to ensure the shutdown happens last. Thanks to Tim Perry.
* [LOG4J2-2998](https://issues.apache.org/jira/browse/LOG4J2-2998):
Fix truncation of excessive strings ending with a high surrogate in JsonWriter.
* [LOG4J2-2973](https://issues.apache.org/jira/browse/LOG4J2-2973):
Rename EventTemplateAdditionalField#type (conflicting with properties file parser) to "format". Thanks to Fabio Ricchiuti.
* [LOG4J2-2972](https://issues.apache.org/jira/browse/LOG4J2-2972):
Refactor AsyncAppender and AppenderControl for handling of Throwables.
* [LOG4J2-2985](https://issues.apache.org/jira/browse/LOG4J2-2985):
Add eventTemplateRootObjectKey parameter to JsonTemplateLayout.
* [LOG4J2-2974](https://issues.apache.org/jira/browse/LOG4J2-2974):
Log4j would fail to initialize in Java 8 with log4j-spring-boot.
* [LOG4J2-2964](https://issues.apache.org/jira/browse/LOG4J2-2964):
Merge packages from several Configurations in Composite Configuration. Thanks to Valery Yatsynovich.
* [LOG4J2-2961](https://issues.apache.org/jira/browse/LOG4J2-2961):
Fix reading of JsonTemplateLayout event additional fields from config.
* [LOG4J2-2916](https://issues.apache.org/jira/browse/LOG4J2-2916):
Avoid redundant Kafka producer instantiation causing thread leaks. Thanks to wuqian0808.
* [LOG4J2-2967](https://issues.apache.org/jira/browse/LOG4J2-2967):
Fix JsonTemplateLayout index based parameter resolution when messages contain too few parameters.
* [LOG4J2-2976](https://issues.apache.org/jira/browse/LOG4J2-2976):
JdbcAppender composes an incorrect INSERT statement without a ColumnMapping element.
* [LOG4J2-3014](https://issues.apache.org/jira/browse/LOG4J2-3014):
Log4j1ConfigurationConverter on Windows produces "" at end of every line. Thanks to Lee Breisacher, Gary Gregory.

### Changes
* [LOG4J2-2893](https://issues.apache.org/jira/browse/LOG4J2-2893):
Allow reconfiguration when Log4j 1 configuration files are updated.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update Spring dependencies to 5.3.2, Spring Boot to 2.3.6, and Spring Cloud to Hoxton.SR9
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update org.fusesource.jansi:jansi 1.17.1 -&gt; 2.0.1.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update commons-codec:commons-codec 1.14 -&gt; 1.15.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update org.apache.commons:commons-lang3 3.10 -&gt; 3.11.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update org.apache.commons:commons-pool2 2.8.1 -&gt; 2.9.0.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update org.apache.commons:commons-dbcp2 2.4.0 -&gt; 2.8.0.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update commons-io:commons-io 2.7 -&gt; 2.8.0.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update org.codehaus.groovy:* 3.0.5 -&gt; 3.0.6.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update com.fasterxml.jackson.*:* 2.11.2 - 2.11.3.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update org.springframework:* 5.2.8.RELEASE -&gt; 5.3.1.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update junit:junit 4.13 -&gt; 4.13.1.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update org.xmlunit:* 2.7.0 -&gt; 2.8.0.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update org.assertj:assertj-core 3.14.0 -&gt; 3.18.1.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update org.awaitility:awaitility 4.0.2 -&gt; 4.0.3.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update org.codehaus.plexus:plexus-utils 3.2.0 -&gt; 3.3.0.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update MongoDB 3 plugin: org.mongodb:mongodb-driver 3.12.6 -&gt; 3.12.7.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update MongoDB 4 plugin: org.mongodb:* 4.1.0 -&gt; 4.1.1.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update org.eclipse.tycho:org.eclipse.osgi 3.12.1.v20170821-1548 -&gt; 3.13.0.v20180226-1711.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update de.flapdoodle.embed:de.flapdoodle.embed.mongo 2.2.0 -&gt; 3.0.0.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update net.javacrumbs.json-unit:json-unit 1.31.1 -&gt; 2.22.0.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update Mockito 3.6.0 -&gt; 3.7.0.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update XML Unit 2.8.0 -&gt; 2.8.2.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update JSON Unit 2.21.0 -&gt; 2.22.0.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update JaCoCo 0.8.3 -&gt; 0.8.6.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update org.apache.activemq:* 5.16.0 -&gt; 5.16.1.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update org.mockito:mockito-* 3.7.0 -&gt; 3.7.7.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update org.springframework:* 5.3.2 -&gt; 5.3.3.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update mongodb4.version 4.1.1 -&gt; 4.2.0.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update org.fusesource.jansi:jansi 1.18 -&gt; 2.2.0.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update org.assertj:assertj-core 3.18.1 -&gt; 3.19.0.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update net.javacrumbs.json-unit:json-unit 2.22.0 -&gt; 2.23.0.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update Woodstox 5.0.3 -&gt; 6.2.3 to match Jackson 2.12.1.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update org.apache.activemq:* 5.16.0 -&gt; 5.16.1.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update org.mockito:mockito-* 3.7.0 -&gt; 3.7.7.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update org.springframework:* 5.3.2 -&gt; 5.3.3.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update mongodb4.version 4.1.1 -&gt; 4.2.0.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update org.fusesource.jansi:jansi 1.18 -&gt; 2.3.1.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update org.assertj:assertj-core 3.18.1 -&gt; 3.19.0.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update net.javacrumbs.json-unit:json-unit 2.22.0 -&gt; 2.23.0.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
Update net.javacrumbs.json-unit:json-unit 2.22.0 -&gt; 2.23.0.
* [](https://issues.apache.org/jira/browse/LOG4J2-2893):
- com.fasterxml.jackson.core:jackson-annotations ................. 2.12.1 -&gt; 2.12.2
        - com.fasterxml.jackson.core:jackson-core ........................ 2.12.1 -&gt; 2.12.2
        - com.fasterxml.jackson.core:jackson-databind .................... 2.12.1 -&gt; 2.12.2
        - com.fasterxml.jackson.dataformat:jackson-dataformat-xml ........ 2.12.1 -&gt; 2.12.2
        - com.fasterxml.jackson.dataformat:jackson-dataformat-yaml ....... 2.12.1 -&gt; 2.12.2
        - com.fasterxml.jackson.module:jackson-module-jaxb-annotations ... 2.12.1 -&gt; 2.12.2
        - org.apache.commons:commons-lang3 ............................... 3.11   -&gt; 3.12.0
        - org.junit.jupiter:junit-jupiter-engine ......................... 5.7.0  -&gt; 5.7.1
        - org.junit.jupiter:junit-jupiter-migrationsupport ............... 5.7.0  -&gt; 5.7.1
        - org.junit.jupiter:junit-jupiter-params ......................... 5.7.0  -&gt; 5.7.1
        - org.junit.vintage:junit-vintage-engine ......................... 5.7.0  -&gt; 5.7.1
        - org.mockito:mockito-core ....................................... 3.7.7  -&gt; 3.8.0
        - org.mockito:mockito-junit-jupiter .............................. 3.7.7  -&gt; 3.8.0
        - org.mongodb:bson ............................................... 4.2.0  -&gt; 4.2.2
        - org.mongodb:mongodb-driver-sync ................................ 4.2.0  -&gt; 4.2.2

---

Apache Log4j 2.14.1 requires a minimum of Java 8 to build and run. Log4j 2.12.1 is the last release to support
Java 7.

For complete information on Apache Log4j 2, including instructions on how to submit bug
reports, patches, or suggestions for improvement, see the Apache Apache Log4j 2 website:

https://logging.apache.org/log4j/2.x/
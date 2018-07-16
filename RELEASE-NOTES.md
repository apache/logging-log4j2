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
# Apache Log4j 2.11.0 Release Notes

The Apache Log4j 2 team is pleased to announce the Log4j 2.11.0 release!

Apache Log4j is a well known framework for logging application behavior. Log4j 2 is an upgrade
to Log4j that provides significant improvements over its predecessor, Log4j 1.x, and provides
many other modern features such as support for Markers, lambda expressions for lazy logging,
property substitution using Lookups, multiple patterns on a PatternLayout and asynchronous
Loggers. Another notable Log4j 2 feature is the ability to be "garbage-free" (avoid allocating
temporary objects) while logging. In addition, Log4j 2 will not lose events while reconfiguring.

This release contains new features, bugfixes and minor enhancements.

As of Log4j 2.9.0, the Log4j API was modified to use java.util.ServiceLoader to locate Log4j implementations,
although the former binding mechanism is still supported. The Log4j API jar is now a multi-release jar
to provide implementations of Java 9 specific classes. Multi-release jars are not supported by
the OSGi specification so OSGi modules will not be able to take advantage of these implementations
but will not lose functionality as they will fall back to the implementations used in Java 7 and 8.
More details on the new features and fixes are itemized below. Note that some tools are not compatible
with multi-release jars and may fail trying to process class files in the META-INF/versions/9 folder.
Those errors should be reported to the tool vendor.

Note that subsequent to the 2.9.0 release, for security reasons, SerializedLayout is deprecated and no
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

Note that the XML, JSON and YAML formats changed in the 2.11.0 release: they no longer have the "timeMillis" attribute and instead have an "Instant" element with "epochSecond" and "nanoOfSecond" attributes.

Note that subsequent to the 2.9.0 release, for security reasons, Log4j does not process DTD in XML files.
If you used DTD for including snippets, you have to use XInclude or Composite Configuration instead.

The Log4j 2.11.0 API, as well as many core components, maintains binary compatibility with previous releases.

## GA Release 2.11.0

Changes in this version include:

### New Features
* [LOG4J2-2253](https://issues.apache.org/jira/browse/LOG4J2-2253):
Add API to enable iterating over message parameters without creating temporary objects. Thanks to Carter Kozak.
* [LOG4J2-1883](https://issues.apache.org/jira/browse/LOG4J2-1883):
Added support for precise (micro and nanosecond) timestamps when running on Java 9. A limited number of precise %d date formats are supported with PatternLayout. POTENTIAL BREAKING CHANGE: The XML, JSON and YAML formats have changed: they no longer have the "timeMillis" attribute and instead have an "Instant" element with "epochSecond" and "nanoOfSecond" attributes. Thanks to Anthony Maire.
* [LOG4J2-2190](https://issues.apache.org/jira/browse/LOG4J2-2190):
Output JSON object for ObjectMessage in JsonLayout. Thanks to Franz Wong.
* [LOG4J2-2191](https://issues.apache.org/jira/browse/LOG4J2-2191):
Made log4j-core a multi-release ("multi-version") jar, added log4j-core-java9 module.
* [LOG4J2-2143](https://issues.apache.org/jira/browse/LOG4J2-2143):
Add missing converters to PatternLayout.
* [LOG4J2-2160](https://issues.apache.org/jira/browse/LOG4J2-2160):
Add API org.apache.logging.log4j.core.lookup.Interpolator.getStrLookupMap().
* [LOG4J2-2179](https://issues.apache.org/jira/browse/LOG4J2-2179):
The MongoDB Appender should use a keys and values for a Log4j MapMessage.
* [LOG4J2-2180](https://issues.apache.org/jira/browse/LOG4J2-2180):
Add a MongoDbProvider builder for and deprecate org.apache.logging.log4j.mongodb.MongoDbProvider.createNoSqlProvider().
* [LOG4J2-2181](https://issues.apache.org/jira/browse/LOG4J2-2181):
The JDBC Appender should use keys and values from a Log4j MapMessage.
* [LOG4J2-2185](https://issues.apache.org/jira/browse/LOG4J2-2185):
Add a simple JDBC DriverManager-based ConnectionSource that uses JDBC's DriverManager#getConnection(String, String, String).
* [LOG4J2-2186](https://issues.apache.org/jira/browse/LOG4J2-2186):
Add a JDBC ConnectionSource that provides pooling through Apache Commons DBCP 2.
* [LOG4J2-2187](https://issues.apache.org/jira/browse/LOG4J2-2187):
Add a hook for a Connection Source for a JDBC Appender to release its resources.
* [LOG4J2-2203](https://issues.apache.org/jira/browse/LOG4J2-2203):
Add org.apache.logging.log4j.core.util.WatchManager#unwatch(File).
* [LOG4J2-2206](https://issues.apache.org/jira/browse/LOG4J2-2206):
Add method org.apache.logging.log4j.core.util.WatchManager.reset(File) and reset().
* [LOG4J2-2208](https://issues.apache.org/jira/browse/LOG4J2-2208):
Add debug logging to org.apache.logging.log4j.mongodb.MongoDbConnection.

### Fixed Bugs
* [LOG4J2-2271](https://issues.apache.org/jira/browse/LOG4J2-2271):
Move module-info.class to META-INF/versions/9 directory.
* [LOG4J2-2254](https://issues.apache.org/jira/browse/LOG4J2-2254):
Incorrect automatics module name header was being included in manifests.
* [LOG4J2-2247](https://issues.apache.org/jira/browse/LOG4J2-2247):
NullPointerException would occur when header was provided to a Layout on RollingRandingAccessFileAppender
        with DirectWriteRolloverStrategy.
* [LOG4J2-2129](https://issues.apache.org/jira/browse/LOG4J2-2129):
Log4j2 throws NoClassDefFoundError in Java 9 in java.util.ServiceLoader. Thanks to Blazej Bucko.
* [LOG4J2-2158](https://issues.apache.org/jira/browse/LOG4J2-2158):
Fixed bug where ThreadContext map was cleared, resulting in entries being only available for one log event. Thanks to Björn Kautler.
* [LOG4J2-2002](https://issues.apache.org/jira/browse/LOG4J2-2002):
Avoid null attribute values in DefaultConfigurationBuilder. Thanks to Paul Burrowes.
* [LOG4J2-2175](https://issues.apache.org/jira/browse/LOG4J2-2175):
Fix typo in Property Substitution docs. Thanks to Behrang Saeedzadeh.
* [LOG4J2-2163](https://issues.apache.org/jira/browse/LOG4J2-2163):
Allow SortedArrayStringMap to be filtered upon deserialization. Fix build error in Java 9 when
        compiling log4j-core test classes.
* [LOG4J2-2157](https://issues.apache.org/jira/browse/LOG4J2-2157):
Don't create exit message in traceExit(R) when logging is disabled. Thanks to Malte Skoruppa.
* [LOG4J2-2123](https://issues.apache.org/jira/browse/LOG4J2-2123):
DefaultMergeStrategy did not merge filters on loggers correctly. Thanks to Jacob Tolar.
* [LOG4J2-2126](https://issues.apache.org/jira/browse/LOG4J2-2126):
Removed compile-time dependency on Java Management APIs from Log4J API module to improve compatibility with Android Platform which does not support JMX extensions. Thanks to Oleg Kalnichevski.
* [LOG4J2-2270](https://issues.apache.org/jira/browse/LOG4J2-2270):
Strings::join, when called with [null] returns "null" instead of EMPTY. Thanks to Cyril Martin.
* [LOG4J2-2276](https://issues.apache.org/jira/browse/LOG4J2-2276):
ConcurrentModificationException from org.apache.logging.log4j.status.StatusLogger.&lt;clinit&gt;(StatusLogger.java:71). Thanks to Sean Baxter.
* [LOG4J2-2274](https://issues.apache.org/jira/browse/LOG4J2-2274):
Allow EnvironmentPropertySource to run with a SecurityManager that rejects environment variable access. Thanks to Sebastien Lannez.
* [LOG4J2-2279](https://issues.apache.org/jira/browse/LOG4J2-2279):
Allow SystemPropertiesPropertySource to run with a SecurityManager that rejects system property access. Thanks to Gary Gregory.

### Changes
* [LOG4J2-2273](https://issues.apache.org/jira/browse/LOG4J2-2273):
Documentation fix in manual page for custom configurations. Thanks to Bruno P. Kinoshita.
* [LOG4J2-2252](https://issues.apache.org/jira/browse/LOG4J2-2252):
Reusable LogEvents now pass the original format string to downstream components like layouts and filters. Thanks to Carter Kozak.
* [LOG4J2-2250](https://issues.apache.org/jira/browse/LOG4J2-2250):
The internal status logger timestamp format is now configurable with system property `log4j2.StatusLogger.DateFormat`.
* [LOG4J2-2236](https://issues.apache.org/jira/browse/LOG4J2-2236):
Removed unnecessary dependency on jcommander since Log4j uses embedded picocli since 2.9.
* [LOG4J2-2146](https://issues.apache.org/jira/browse/LOG4J2-2146):
Update version of maven bundle plugin to 3.4.0. Convert bundle plugin error to a warning.
* [LOG4J2-2215](https://issues.apache.org/jira/browse/LOG4J2-2215):
Reduce compiler warnings in log4j-api.
* [LOG4J2-2127](https://issues.apache.org/jira/browse/LOG4J2-2127):
Removed unnecessary threadlocal StringBuilder field from MdcPatternConverter. Thanks to Carter Kozak.
* [LOG4J2-2194](https://issues.apache.org/jira/browse/LOG4J2-2194):
Require Java 9 to compile the log4j-perf module to allow benchmarking with Java 9 APIs.
* [LOG4J2-2193](https://issues.apache.org/jira/browse/LOG4J2-2193):
Update JMH to version 1.19 from 1.1.1.
* [LOG4J2-2132](https://issues.apache.org/jira/browse/LOG4J2-2132):
Update ZeroMQ's jeromq from 0.4.2 to 0.4.3.
* [LOG4J2-2165](https://issues.apache.org/jira/browse/LOG4J2-2165):
Update Jackson from 2.9.2 to 2.9.3.
* [LOG4J2-2184](https://issues.apache.org/jira/browse/LOG4J2-2184):
Update MongoDB driver from 3.0.4 to 3.6.1.
* [LOG4J2-2197](https://issues.apache.org/jira/browse/LOG4J2-2197):
Document default property value support. Thanks to Fabrice Daugan.
* [LOG4J2-2198](https://issues.apache.org/jira/browse/LOG4J2-2198):
Update MongoDB dependencies from classic to modern.
* [LOG4J2-2204](https://issues.apache.org/jira/browse/LOG4J2-2204):
org.apache.logging.log4j.core.util.WatchManager.getWatchers() should pre-allocate its new Map.
* [LOG4J2-2209](https://issues.apache.org/jira/browse/LOG4J2-2209):
Rename existing MongoDb plugin and related artifacts from MongoDb to MongoDb2.
* [LOG4J2-2210](https://issues.apache.org/jira/browse/LOG4J2-2210):
Fix error log message for Script which says ScriptFile instead. Thanks to Björn Kautler.
* [LOG4J2-2212](https://issues.apache.org/jira/browse/LOG4J2-2212):
Unnecessary contention in CopyOnWriteSortedArrayThreadContextMap. Thanks to Daniel Feist, Gary Gregory.
* [LOG4J2-2213](https://issues.apache.org/jira/browse/LOG4J2-2213):
Unnecessary contention in GarbageFreeSortedArrayThreadContextMap. Thanks to Daniel Feist, Gary Gregory.
* [LOG4J2-2214](https://issues.apache.org/jira/browse/LOG4J2-2214):
Unnecessary contention in DefaultThreadContextMap. Thanks to Daniel Feist, Gary Gregory.
* [LOG4J2-2182](https://issues.apache.org/jira/browse/LOG4J2-2182):
NullPointerException at org.apache.logging.log4j.util.Activator.loadProvider(Activator.java:81) in log4j 2.10.0. Thanks to liwenxian2017, Gary Gregory.
* [LOG4J2-2202](https://issues.apache.org/jira/browse/LOG4J2-2202):
MarkerFilter onMismatch invalid attribute in .properties. Thanks to Kilian, Gary Gregory.
* [LOG4J2-2219](https://issues.apache.org/jira/browse/LOG4J2-2219):
Configuration builder classes should look for "onMismatch", not "onMisMatch". Thanks to Kilian, Gary Gregory.
* [LOG4J2-2205](https://issues.apache.org/jira/browse/LOG4J2-2205):
New module log4j-mongodb3: Remove use of deprecated MongoDB APIs and code to the Java driver version 3 API. Thanks to Gary Gregory.
* [LOG4J2-2188](https://issues.apache.org/jira/browse/LOG4J2-2188):
Split off JPA support into a new module log4j-jpa. Thanks to Gary Gregory.
* [LOG4J2-2229](https://issues.apache.org/jira/browse/LOG4J2-2229):
Update Jackson from 2.9.3 to 2.9.4. Thanks to Gary Gregory.
* [LOG4J2-2243](https://issues.apache.org/jira/browse/LOG4J2-2243):
Cannot see or copy all of certain JAnsi exception messages on Windows due to NUL characters. Thanks to Gary Gregory.
* [LOG4J2-2245](https://issues.apache.org/jira/browse/LOG4J2-2245):
Update Apache Commons Compress from 1.15 to 1.16.1. Thanks to Gary Gregory.
* [LOG4J2-2259](https://issues.apache.org/jira/browse/LOG4J2-2259):
Update MongoDB 3 module from driver 3.6.1 to 3.6.3.
* [LOG4J2-2260](https://issues.apache.org/jira/browse/LOG4J2-2260):
[SMTP] Update javax.mail from 1.6.0 to 1.6.1.
* [LOG4J2-2264](https://issues.apache.org/jira/browse/LOG4J2-2264):
Update JAnsi from 1.16 to 1.17.

---

Apache Log4j 2.11.0 requires a minimum of Java 7 to build and run. Log4j 2.3 was the
last release that supported Java 6.

Basic compatibility with Log4j 1.x is provided through the log4j-1.2-api component, however it
does not implement some of the very implementation specific classes and methods. The package
names and Maven groupId have been changed to org.apache.logging.log4j to avoid any conflicts
with log4j 1.x.

For complete information on Apache Log4j 2, including instructions on how to submit bug
reports, patches, or suggestions for improvement, see the Apache Apache Log4j 2 website:

https://logging.apache.org/log4j/2.x/
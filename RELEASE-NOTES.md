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
# Apache Log4j 2.11.1 Release Notes

The Apache Log4j 2 team is pleased to announce the Log4j 2.11.1 release!

Apache Log4j is a well known framework for logging application behavior. Log4j 2 is an upgrade
to Log4j that provides significant improvements over its predecessor, Log4j 1.x, and provides
many other modern features such as support for Markers, lambda expressions for lazy logging,
property substitution using Lookups, multiple patterns on a PatternLayout and asynchronous
Loggers. Another notable Log4j 2 feature is the ability to be "garbage-free" (avoid allocating
temporary objects) while logging. In addition, Log4j 2 will not lose events while reconfiguring.

This release contains new features, bugfixes and minor enhancements.

Due to a break in compatibility in the SLF4J binding, Log4j now ships with two versions of the SLF4J to Log4j adapters.
log4j-slf4j-impl should be used with SLF4J 1.7.x and earlier and log4j-slf4j18-impl should be used with SLF4J 1.8.x and
later.

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

The Log4j 2.11.1 API, as well as many core components, maintains binary compatibility with previous releases.

## GA Release 2.11.1

Changes in this version include:


### Fixed Bugs
* [LOG4J2-2389](https://issues.apache.org/jira/browse/LOG4J2-2389):
ThrowableProxy was saving and retrieving cache entries using different keys. Thanks to Liu Wen.
* [LOG4J2-2316](https://issues.apache.org/jira/browse/LOG4J2-2316):
If root LoggerConfig does not have a Level return ERROR.
* [LOG4J2-2390](https://issues.apache.org/jira/browse/LOG4J2-2390):
Fix broken links in log4j web documentation. Thanks to anton-balaniuc.
* [LOG4J2-2343](https://issues.apache.org/jira/browse/LOG4J2-2343):
The OSGi Activator specified an incorrect version. Thanks to Raymond Augé.
* [LOG4J2-2305](https://issues.apache.org/jira/browse/LOG4J2-2305):
Make java.util.ServiceLoader properly work in OSGi by using the Service Loader Mediator Specification. Thanks to Björn Kautler.
* [LOG4J2-2305](https://issues.apache.org/jira/browse/LOG4J2-2305):
Split the SLF4J binding into 2 implementations - one for SLF4J 1.7.x and one for SLF4J 1.8+.
* [LOG4J2-2268](https://issues.apache.org/jira/browse/LOG4J2-2268):
Improve plugin error message when elements are missing. Thanks to Tilman Hausherr.
* [LOG4J2-2283](https://issues.apache.org/jira/browse/LOG4J2-2283):
ParserConfigurationException when using Log4j with oracle.xml.jaxp.JXDocumentBuilderFactory. Thanks to Vishnu Priya Matha.
* [LOG4J2-2300](https://issues.apache.org/jira/browse/LOG4J2-2300):
PoolingDriverConnectionSource does not take into account properties, user name, and password.
* [LOG4J2-2307](https://issues.apache.org/jira/browse/LOG4J2-2307):
MutableLogEvent and RingBufferLogEvent message mementos retain the original format string.
* [LOG4J2-2032](https://issues.apache.org/jira/browse/LOG4J2-2032):
Curly braces in parameters are not treated as placeholders. Thanks to Kostiantyn Shchepanovskyi.
* [LOG4J2-2317](https://issues.apache.org/jira/browse/LOG4J2-2317):
MutableLogEvent.getNonNullImmutableMessage and Log4jLogEvent.makeMessageImmutable retain format and parameters.
* [LOG4J2-2318](https://issues.apache.org/jira/browse/LOG4J2-2318):
Messages are no longer mutated when the asynchronous queue is full. A warning is logged to the status logger instead.
* [LOG4J2-2320](https://issues.apache.org/jira/browse/LOG4J2-2320):
Fix NPE in AbstractLogger when another exception is thrown, masking the root cause.
* [LOG4J2-2321](https://issues.apache.org/jira/browse/LOG4J2-2321):
AsyncLogger uses the correct level when unspecified. This provides parity between AsyncLogger and Logger.
* [LOG4J2-2322](https://issues.apache.org/jira/browse/LOG4J2-2322):
Custom ContextSelector implementations which select an AsyncLoggerContext disable LoggerConfig.includeLocation
        by default for parity with AsyncLoggerContextSelector.
* [LOG4J2-2269](https://issues.apache.org/jira/browse/LOG4J2-2269):
MutableLogEvent references to other objects are cleared after each use.
        Fix a memory leak causing references to parameters to be held after synchronous logging with thread locals enabled.
* [LOG4J2-2301](https://issues.apache.org/jira/browse/LOG4J2-2301):
Mixed async loggers no longer forget parameter values, providing some appenders with an array of nulls.
* [LOG4J2-2331](https://issues.apache.org/jira/browse/LOG4J2-2331):
RollingFileManager debug logging avoids string concatenation and errant braces in favor of parameterized logging. Thanks to Mike Baranski.
* [LOG4J2-2333](https://issues.apache.org/jira/browse/LOG4J2-2333):
Handle errors thrown in default disruptor ExceptionHandler implementations to avoid killing background threads.
* [LOG4J2-2334](https://issues.apache.org/jira/browse/LOG4J2-2334):
Add API org.apache.logging.log4j.core.appender.AsyncAppender.getQueueSize().
* [LOG4J2-2336](https://issues.apache.org/jira/browse/LOG4J2-2336):
Remove duplicate hyphen from the AsyncLoggerConfig background thread name.
* [LOG4J2-2347](https://issues.apache.org/jira/browse/LOG4J2-2347):
Update Apache Commons Compress from 1.16.1 to 1.17.
* [LOG4J2-2352](https://issues.apache.org/jira/browse/LOG4J2-2352):
RingBufferLogEvent memento messages provide the expected format string, and no longer attempt to substitute parameters into curly braces in parameter toString values.
        Both RingBufferLogEvent and MutableLogEvent memento implementations memoize results to avoid rebuilding formatted string values.
* [LOG4J2-2355](https://issues.apache.org/jira/browse/LOG4J2-2355):
PropertiesUtil ignores non-string system properties. Fixes a NoClassDefFoundError initializing StatusLogger
        caused by an NPE while initializing the static PropertiesUtil field. Thanks to Henrik Brautaset Aronsen.
* [LOG4J2-2362](https://issues.apache.org/jira/browse/LOG4J2-2362):
Fixed a memory leak in which ReusableObjectMessage would hold a reference to the most recently logged object.
* [LOG4J2-2312](https://issues.apache.org/jira/browse/LOG4J2-2312):
Jackson layouts used with AsyncLoggerContextSelector output the expected format rather than only a JSON string of the message text.
* [LOG4J2-2364](https://issues.apache.org/jira/browse/LOG4J2-2364):
Fixed a memory leak in which ReusableParameterizedMessage would hold a reference to the most recently
        logged throwable and provided varargs array.
* [LOG4J2-2368](https://issues.apache.org/jira/browse/LOG4J2-2368):
Nested logging doesn't clobber AbstractStringLayout cached StringBuidlers
* [LOG4J2-2373](https://issues.apache.org/jira/browse/LOG4J2-2373):
StringBuilders.escapeJson implementation runs in linear time. Escaping large JSON strings
        in EncodingPatternConverter and MapMessage will perform significantly better. Thanks to Kevin Meurer.
* [LOG4J2-2376](https://issues.apache.org/jira/browse/LOG4J2-2376):
StringBuilders.escapeXml implementation runs in linear time. Escaping large XML strings
        in EncodingPatternConverter and MapMessage will perform significantly better. Thanks to Kevin Meurer.
* [LOG4J2-2377](https://issues.apache.org/jira/browse/LOG4J2-2377):
NullPointerException in org.apache.logging.log4j.util.LoaderUtil.getClassLoaders() when using Bootstrap class loader. Thanks to Mirko Rzehak, Gary Gregory.
* [LOG4J2-2382](https://issues.apache.org/jira/browse/LOG4J2-2382):
Update Mongodb 3 driver from 3.6.3 to 3.8.0.
* [LOG4J2-2388](https://issues.apache.org/jira/browse/LOG4J2-2388):
Thread indefinitely blocked when logging a message in an interrupted thread. Thanks to Failled.

### Changes
* [LOG4J2-1721](https://issues.apache.org/jira/browse/LOG4J2-1721):
Allow composite configuration for context parameter. Thanks to Phokham Nonava.
* [LOG4J2-2302](https://issues.apache.org/jira/browse/LOG4J2-2302):
Status logger should show the Log4j name and version when initializing itself.
* [LOG4J2-2304](https://issues.apache.org/jira/browse/LOG4J2-2304):
Log4j2 2.8.2 JMX unregister NullPointerException. Thanks to wumengsheng.
* [LOG4J2-2311](https://issues.apache.org/jira/browse/LOG4J2-2311):
Update Jackson from 2.9.4 to 2.9.5.
* [LOG4J2-2313](https://issues.apache.org/jira/browse/LOG4J2-2313):
Update LMAX Disruptor from 3.3.7 to 3.4.2.
* [LOG4J2-548](https://issues.apache.org/jira/browse/LOG4J2-548):
Log4j 2.0 ERROR "Could not search jar" with JBoss EAP 6.2. Thanks to Shehata, Paresh Varke, Eric Victorson, Martin Laforet.
* [LOG4J2-2328](https://issues.apache.org/jira/browse/LOG4J2-2328):
Update JAnsi from 1.17 to 1.17.1.
* [LOG4J2-2351](https://issues.apache.org/jira/browse/LOG4J2-2351):
Added AbstractLogEvent.getMutableInstant to allow the MutableInstant instance to be modified by classes extending AbstractLogEvent.
* [LOG4J2-2357](https://issues.apache.org/jira/browse/LOG4J2-2357):
Update Jackson from 2.9.5 to 2.9.6.
* [LOG4J2-2358](https://issues.apache.org/jira/browse/LOG4J2-2358):
Update Kafka client from 1.0.0 to 1.1.0.
* [LOG4J2-2384](https://issues.apache.org/jira/browse/LOG4J2-2384):
Update Kafka client from 1.1.0 to 1.1.1.
* [LOG4J2-2385](https://issues.apache.org/jira/browse/LOG4J2-2385):
Update Groovy from 2.4.13 to 2.5.1.
* [LOG4J2-2386](https://issues.apache.org/jira/browse/LOG4J2-2386):
Update optional Apache Commons DBCP from 2.2.0 to 2.4.0.

---

Apache Log4j 2.11.1 requires a minimum of Java 7 to build and run. Log4j 2.3 was the
last release that supported Java 6.

Basic compatibility with Log4j 1.x is provided through the log4j-1.2-api component, however it
does not implement some of the very implementation specific classes and methods. The package
names and Maven groupId have been changed to org.apache.logging.log4j to avoid any conflicts
with log4j 1.x.

For complete information on Apache Log4j 2, including instructions on how to submit bug
reports, patches, or suggestions for improvement, see the Apache Apache Log4j 2 website:

https://logging.apache.org/log4j/2.x/
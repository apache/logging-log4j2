# Apache Log4j 2.10.0 Release Notes

The Apache Log4j 2 team is pleased to announce the Log4j 2.10.0 release!

Apache Log4j is a well known framework for logging application behavior. Log4j 2 is an upgrade
to Log4j that provides significant improvements over its predecessor, Log4j 1.x, and provides
many other modern features such as support for Markers, lambda expressions for lazy logging,
property substitution using Lookups, multiple patterns on a PatternLayout and asynchronous
Loggers. Another notable Log4j 2 feature is the ability to be "garbage-free" (avoid allocating
temporary objects) while logging. In addition, Log4j 2 will not lose events while reconfiguring.

This release contains new features, bugfixes and minor enhancements. Some of the new features include support
for the Java 9 module system, support for the new SLF4j 1.8 binding mechanism, simplification of the Log4j
property naming scheme, and native support of Jetty's logger. Log4j API is now a fully compliant module
while the other Log4j jars are named automatic modules.

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

Note that subsequent to the 2.9.0 release, for security reasons, Log4j does not process DTD in XML files.
If you used DTD for including snippets, you have to use XInclude or Composite Configuration instead.

The Log4j 2.10.0 API, as well as many core components, maintains binary compatibility with previous releases.

## GA Release 2.10.0

Changes in this version include:

### New Features
* [LOG4J2-2120](https://issues.apache.org/jira/browse/LOG4J2-2120):
Properly escape newlines and other control characters in JSON. Thanks to Carter Douglas Kozak.
* [LOG4J2-2109](https://issues.apache.org/jira/browse/LOG4J2-2109):
Add property to disable message pattern converter lookups. Thanks to Carter Douglas Kozak.
* [LOG4J2-2112](https://issues.apache.org/jira/browse/LOG4J2-2112):
MapMessage should use deep toString for values. Thanks to Carter Douglas Kozak.
* [LOG4J2-2103](https://issues.apache.org/jira/browse/LOG4J2-2103):
XML encoding for PatternLayout.
* [LOG4J2-2114](https://issues.apache.org/jira/browse/LOG4J2-2114):
Provide a native Log4j 2 implementation of Eclipse Jetty's org.eclipse.jetty.util.log.Logger.
* [LOG4J2-1203](https://issues.apache.org/jira/browse/LOG4J2-1203):
Allow filtering of line breaks in layout pattern. Thanks to Robert Turner.
* [LOG4J2-2098](https://issues.apache.org/jira/browse/LOG4J2-2098):
Add a noop AppenderSkeleton for applications still using Log4j 1.x.
* [LOG4J2-2062](https://issues.apache.org/jira/browse/LOG4J2-2062):
Add possibility of sending the key of a message to Kafka using KafkaAppender. Thanks to Jorge Sanchez.
* [LOG4J2-2056](https://issues.apache.org/jira/browse/LOG4J2-2056):
Modularize Log4j-api and make most other log4j jars automatic modules.
* [LOG4J2-1431](https://issues.apache.org/jira/browse/LOG4J2-1431):
Simplify log4j system property naming scheme.
* [LOG4J2-1809](https://issues.apache.org/jira/browse/LOG4J2-1809):
Add global configuration environment SPI.
* [LOG4J2-1694](https://issues.apache.org/jira/browse/LOG4J2-1694):
Add fields with fixed values to JSON/XML/YAML layouts. Thanks to Michal Dvořák.
* [LOG4J2-2054](https://issues.apache.org/jira/browse/LOG4J2-2054):
Provide ways to configure SSL that avoid plain-text passwords in the log4j configuration. The configuration may
        now specify a system environment variable that holds the password, or the path to a file that holds the password.
* [LOG4J2-2071](https://issues.apache.org/jira/browse/LOG4J2-2071):
Add org.apache.logging.log4j.core.config.composite.CompositeConfiguration#toString(). Thanks to Carter Kozak.

### Fixed Bugs
* [LOG4J2-2107](https://issues.apache.org/jira/browse/LOG4J2-2107):
MapMessage supports both StringBuilderFormattable and MultiformatMessage. Thanks to Carter Douglas Kozak.
* [LOG4J2-2102](https://issues.apache.org/jira/browse/LOG4J2-2102):
MapMessage JSON encoding will escape keys and values. Thanks to Carter Douglas Kozak.
* [LOG4J2-2101](https://issues.apache.org/jira/browse/LOG4J2-2101):
Non-string value in MapMessage caused ClassCastException. Thanks to Carter Douglas Kozak.
* [LOG4J2-2091](https://issues.apache.org/jira/browse/LOG4J2-2091):
Log4j respects the configured "log4j2.is.webapp" property Thanks to Carter Douglas Kozak.
* [LOG4J2-2100](https://issues.apache.org/jira/browse/LOG4J2-2100):
LevelMixIn class for Jackson is coded incorrectly
* [LOG4J2-2087](https://issues.apache.org/jira/browse/LOG4J2-2087):
Jansi now needs to be enabled explicitly (by setting system property `log4j.skipJansi` to `false`). To avoid causing problems for web applications, Log4j will no longer automatically try to load Jansi without explicit configuration. Thanks to Andy Gumbrecht.
* [LOG4J2-2060](https://issues.apache.org/jira/browse/LOG4J2-2060):
AbstractDatabaseManager should make a copy of LogEvents before holding references to them: AsyncLogger log events are mutable.
* [LOG4J2-2055](https://issues.apache.org/jira/browse/LOG4J2-2055):
If Log4j is used as the Tomcat logging implementation startup might fail if an application also uses Log4j.
* [LOG4J2-2031](https://issues.apache.org/jira/browse/LOG4J2-2031):
Until this change, messages appeared out of order in log file any time when the async logging queue was full.
        With this change, messages are only logged out of order to prevent deadlock when Log4j2 detects recursive
        logging while the queue is full.
* [LOG4J2-2053](https://issues.apache.org/jira/browse/LOG4J2-2053):
Exception java.nio.charset.UnsupportedCharsetException: cp65001 in 2.9.0.
* [LOG4J2-1216](https://issues.apache.org/jira/browse/LOG4J2-1216):
Nested pattern layout options broken. Thanks to Thies Wellpott, Barna Zsombor Klara, GFriedrich.
* [LOG4J2-2070](https://issues.apache.org/jira/browse/LOG4J2-2070):
Log4j1XmlLayout does not provide the entire stack trace, it is missing the caused by information. Thanks to Doug Hughes.
* [LOG4J2-2036](https://issues.apache.org/jira/browse/LOG4J2-2036):
CompositeConfiguration supports Reconfiguration. PR #115. Thanks to Robert Haycock.
* [LOG4J2-2073](https://issues.apache.org/jira/browse/LOG4J2-2073):
Log4j-config.xsd should make AppenderRef optional for each Logger element. Thanks to Patrick Lucas.
* [LOG4J2-2074](https://issues.apache.org/jira/browse/LOG4J2-2074):
The console appender should say why it cannot load JAnsi.
* [LOG4J2-2085](https://issues.apache.org/jira/browse/LOG4J2-2085):
Wrong Apache Commons CSV version referenced in the Javadoc of CsvParameterLayout. Thanks to István Neuwirth.

### Changes
* [LOG4J2-2076](https://issues.apache.org/jira/browse/LOG4J2-2076):
Split up log4j-nosql into one module per appender.
* [LOG4J2-2088](https://issues.apache.org/jira/browse/LOG4J2-2088):
Upgrade picocli to 2.0.3 from 0.9.8.
* [LOG4J2-2025](https://issues.apache.org/jira/browse/LOG4J2-2025):
Provide support for overriding the Tomcat Log class in Tomcat 8.5+.
* [LOG4J2-2057](https://issues.apache.org/jira/browse/LOG4J2-2057):
Support new SLF4J binding mechanism introduced in SLF4J 1.8.
* [LOG4J2-2052](https://issues.apache.org/jira/browse/LOG4J2-2052):
Disable thread name caching by default when running on Java 8u102 or later.
* [LOG4J2-1896](https://issues.apache.org/jira/browse/LOG4J2-1896):
Update classes in org.apache.logging.log4j.core.net.ssl in APIs from String to a PasswordProvider producing
        char[] for passwords.
* [LOG4J2-2078](https://issues.apache.org/jira/browse/LOG4J2-2078):
Update LMAX disruptor from 3.3.6 to 3.3.7.
* [LOG4J2-2081](https://issues.apache.org/jira/browse/LOG4J2-2081):
Update Apache Commons Compress from 1.14 to 1.15.
* [LOG4J2-2089](https://issues.apache.org/jira/browse/LOG4J2-2089):
[TagLib] Update servlet-api provided dependency from 2.5 to 3.0.1.
* [LOG4J2-2096](https://issues.apache.org/jira/browse/LOG4J2-2096):
Update Apache Kafka kafka-clients from 0.11.0.1 to 1.0.0.
* [LOG4J2-2077](https://issues.apache.org/jira/browse/LOG4J2-2077):
Update from Jackson 2.9.1 to 2.9.2.
* [LOG4J2-2117](https://issues.apache.org/jira/browse/LOG4J2-2117):
Jackson dependencies for 2.9.2 incorrectly bring in jackson-annotations 2.9.0 instead of 2.9.2.

---

Apache Log4j 2.10.0 requires a minimum of Java 7 to build and run. Log4j 2.3 was the
last release that supported Java 6.

Basic compatibility with Log4j 1.x is provided through the log4j-1.2-api component, however it
does not implement some of the very implementation specific classes and methods. The package
names and Maven groupId have been changed to org.apache.logging.log4j to avoid any conflicts
with log4j 1.x.

For complete information on Apache Log4j 2, including instructions on how to submit bug
reports, patches, or suggestions for improvement, see the Apache Apache Log4j 2 website:

https://logging.apache.org/log4j/2.x/
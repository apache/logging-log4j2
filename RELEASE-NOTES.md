# Apache Log4j 2.9.0 Release Notes

The Apache Log4j 2 team is pleased to announce the Log4j 2.9.0 release!

Apache Log4j is a well known framework for logging application behavior. Log4j 2 is an upgrade
to Log4j that provides significant improvements over its predecessor, Log4j 1.x, and provides
many other modern features such as support for Markers, lambda expressions for lazy logging,
property substitution using Lookups, multiple patterns on a PatternLayout and asynchronous
Loggers. Another notable Log4j 2 feature is the ability to be "garbage-free" (avoid allocating
temporary objects) while logging. In addition, Log4j 2 will not lose events while reconfiguring.

This release contains the first support of Java 9 as well as bugfixes and minor
enhancements. The Log4j API was modified to use java.util.ServiceLoader to locate Log4j implementations,
although the former binding mechanism is still supported. The Log4j jar is now a multi-release jar
to provide implementations of the Java 9 specific classes. Multi-release jars are not supported by
the OSGi specification so OSGi modules will not be able to take advantage of these implementations
but will not lose functionality as they will fall back to the implementations used in Java 7 and 8.
More details on the new features and fixes are itemized below.

Note that subsequent to the 2.9 release, for security reasons, SerializedLayout is deprecated and no
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

Note that subsequent to the 2.9 release, for security reasons, Log4j does not process DTD in XML files.
If you used DTD for including snippets, you have to use XInclude or Composite Configuration instead.

The Log4j 2.9.0 API, as well as many core components, maintains binary compatibility with previous releases.

## GA Release 2.9.0

Changes in this version include:

### New Features
* [LOG4J2-2008](https://issues.apache.org/jira/browse/LOG4J2-2008):
Support printing multiple StructuredData elements in RFC5424Layout.
* [LOG4J2-1986](https://issues.apache.org/jira/browse/LOG4J2-1986):
Public API for parsing the output from JsonLayout/XmlLayout/YamlLayout into a LogEvent.
* [LOG4J2-1981](https://issues.apache.org/jira/browse/LOG4J2-1981):
JsonLayout, XmlLayout and YamlLayout support 0-byte termination of log events.
* [LOG4J2-1864](https://issues.apache.org/jira/browse/LOG4J2-1864):
Support capped collections for MongoDb appender. Thanks to Matthias Kappeller.
* [LOG4J2-1813](https://issues.apache.org/jira/browse/LOG4J2-1813):
Log4j2 will now print all internal logging to the console if system property `log4j2.debug` is defined with any value (or no value).
* [LOG4J2-1766](https://issues.apache.org/jira/browse/LOG4J2-1766):
Temporary compress directory during rollover (#88). Thanks to Pierrick HYMBERT.
* [LOG4J2-1814](https://issues.apache.org/jira/browse/LOG4J2-1814):
Added wrapper classes CustomLoggerGenerator and ExtendedLoggerGenerator to avoid class name with a dollar ($) character which has special meaning in many *nix command line environments.
* [LOG4J2-1884](https://issues.apache.org/jira/browse/LOG4J2-1884):
Added process ID (pid) pattern converter.
* [LOG4J2-1699](https://issues.apache.org/jira/browse/LOG4J2-1699):
Configurable Log File Permissions with PosixFilePermission. Thanks to Demetrios Dimatos, Pierrick HYMBERT.
* [LOG4J2-1945](https://issues.apache.org/jira/browse/LOG4J2-1945):
Generate source jas for all test jars.
* [LOG4J2-1934](https://issues.apache.org/jira/browse/LOG4J2-1934):
JMS Appender does not know how to recover from a broken connection.
* [LOG4J2-1955](https://issues.apache.org/jira/browse/LOG4J2-1955):
JMS Appender should be able connect to a broker (later) even it is not present at configuration time.
* [LOG4J2-1874](https://issues.apache.org/jira/browse/LOG4J2-1874):
Added methods ::writeBytes(ByteBuffer) and ::writeBytes(byte[], int, int) to ByteBufferDestination interface and use these methods in TextEncoderHelper where possible to prepare for future enhancements to reduce lock contention. Thanks to Roman Leventov.
* [LOG4J2-1442](https://issues.apache.org/jira/browse/LOG4J2-1442):
Generic HTTP appender.
* [LOG4J2-1935](https://issues.apache.org/jira/browse/LOG4J2-1935):
Add with(String, primitive) methods to org.apache.logging.log4j.message.MapMessage.
* [LOG4J2-1930](https://issues.apache.org/jira/browse/LOG4J2-1930):
Add forEach() methods to org.apache.logging.log4j.message.MapMessage.
* [LOG4J2-1932](https://issues.apache.org/jira/browse/LOG4J2-1932):
Add containsKey() methods to org.apache.logging.log4j.message.MapMessage.
* [LOG4J2-1854](https://issues.apache.org/jira/browse/LOG4J2-1854):
Support null byte delimiter in GelfLayout. Thanks to Xavier Jodoin.
* [LOG4J2-1359](https://issues.apache.org/jira/browse/LOG4J2-1359):
Add support for Java 9 StackWalker.
* [LOG4J2-1880](https://issues.apache.org/jira/browse/LOG4J2-1880):
Warn when a configuration file for an inactive ConfigurationFactory is found.
* [LOG4J2-1855](https://issues.apache.org/jira/browse/LOG4J2-1855):
Add an optional random delay in TimeBasedTriggeringPolicy Thanks to Anthony Maire.
* [LOG4J2-1860](https://issues.apache.org/jira/browse/LOG4J2-1860):
Shortcut to add Property and KeyValuePair component in ConfigurationBuilder.
* [LOG4J2-1294](https://issues.apache.org/jira/browse/LOG4J2-1294):
The JMS Appender should use a JMS MapMessage for a Log4j MapMessage.

### Fixed Bugs
* [LOG4J2-1833](https://issues.apache.org/jira/browse/LOG4J2-1833):
Prevent NullPointerException when a file name is specified with the DirectWriteRolloverStrategy.
* [LOG4J2-2018](https://issues.apache.org/jira/browse/LOG4J2-2018):
Fix incorrect documentation for LoggerNameLevelRewritePolicy.
* [LOG4J2-922](https://issues.apache.org/jira/browse/LOG4J2-922):
Parameter of mdcId in SyslogAppender has no default value. Thanks to angus.aqlu, Paul Burrowes.
* [LOG4J2-2001](https://issues.apache.org/jira/browse/LOG4J2-2001):
StyleConverter.newInstance argument validation is incorrect. Thanks to Paul Burrowes.
* [LOG4J2-1999](https://issues.apache.org/jira/browse/LOG4J2-1999):
HighlightConverter converts all unrecognized levels to DEBUG. Thanks to Paul Burrowes.
* [LOG4J2-2013](https://issues.apache.org/jira/browse/LOG4J2-2013):
SslSocketManager does not apply SSLContext on TCP reconnect. Thanks to Taylor Patton, Gary Gregory.
* [LOG4J2-2016](https://issues.apache.org/jira/browse/LOG4J2-2016):
Mark FileRenameAction as successful when using alternative ways to move files. Thanks to Benjamin Jaton.
* [LOG4J2-2012](https://issues.apache.org/jira/browse/LOG4J2-2012):
No compression when using a separate drive in Linux. Thanks to Benjamin Jaton.
* [LOG4J2-1888](https://issues.apache.org/jira/browse/LOG4J2-1888):
Log4j throws a java.nio.charset.UnsupportedCharsetException: cp65001. Thanks to Misagh Moayyed.
* [LOG4J2-1990](https://issues.apache.org/jira/browse/LOG4J2-1990):
ConcurrentModificationException logging a parameter of type Map. Thanks to Philippe Mouawad.
* [LOG4J2-1311](https://issues.apache.org/jira/browse/LOG4J2-1311):
SocketAppender will lose several events after re-connection to server. Thanks to Xibing Liang.
* [LOG4J2-1977](https://issues.apache.org/jira/browse/LOG4J2-1977):
Consider the StringBuilder's capacity instead of content length when trimming. Thanks to Jerry xnslong.
* [LOG4J2-1971](https://issues.apache.org/jira/browse/LOG4J2-1971):
Register log4j-core as an OSGi service. Skip tests for LOG4J2-1766 on MacOS. Use group "staff" for LOG4J2-1699 test on MacOS.
* [LOG4J2-1994](https://issues.apache.org/jira/browse/LOG4J2-1994):
TcpSocketServer does not close accepted Sockets.
* [LOG4J2-1987](https://issues.apache.org/jira/browse/LOG4J2-1987):
Log4J JUL Bridge and RMI Security Manager causes access denied ("java.util.logging.LoggingPermission" "control") Thanks to Andreas Felder.
* [LOG4J2-1982](https://issues.apache.org/jira/browse/LOG4J2-1982):
Log4j-config.xsd only allows one AppenderRef element for each Logger element. Thanks to Christoph Lembeck.
* [LOG4J2-1985](https://issues.apache.org/jira/browse/LOG4J2-1985):
Fix default buffer size to match documentation (from 8102 to 8192 a.k.a. 8KB.) Thanks to Kenneth McFarland.
* [LOG4J2-1912](https://issues.apache.org/jira/browse/LOG4J2-1912):
CompositeConfiguration logs warning "Unable to determine URI for configuration." However, the reconfiguration is completed. Thanks to R Ri.
* [LOG4J2-1964](https://issues.apache.org/jira/browse/LOG4J2-1964):
Dynamic reconfiguration does not work for filePattern of RollingFile. Thanks to Pierrick HYMBERT.
* [LOG4J2-1961](https://issues.apache.org/jira/browse/LOG4J2-1961):
Reconfigure breaks DirectWriteRolloverStrategy. Thanks to Christian Vent.
* [LOG4J2-1943](https://issues.apache.org/jira/browse/LOG4J2-1943):
The eventPrefix attribute was being ignored in the RFC5424Layout.
* [LOG4J2-1953](https://issues.apache.org/jira/browse/LOG4J2-1953):
JndiManager is not released when the JmsAppender builder catches an exception trying to build itself.
* [LOG4J2-1911](https://issues.apache.org/jira/browse/LOG4J2-1911):
Improve the documentation of the DynamicThresholdFilter.
* [LOG4J2-1929](https://issues.apache.org/jira/browse/LOG4J2-1929):
EOFException with FormattedMessage. Thanks to Borys Sokolov.
* [LOG4J2-1948](https://issues.apache.org/jira/browse/LOG4J2-1948):
Trim levels read from properties file to remove trailing spaces. Thanks to Michael Lück.
* [LOG4J2-1971](https://issues.apache.org/jira/browse/LOG4J2-1971):
ClassCastException: org.eclipse.osgi.internal.loader.SystemBundleLoader$1 cannot be cast to java.lang.ClassLoader. Thanks to liwenxian2017.
* [LOG4J2-1876](https://issues.apache.org/jira/browse/LOG4J2-1876):
More reliable checking for runtime dependencies.
* [LOG4J2-1867](https://issues.apache.org/jira/browse/LOG4J2-1867):
Fix configuration documentation.
* [LOG4J2-1858](https://issues.apache.org/jira/browse/LOG4J2-1858):
Ensure the ThreadLocal StringBuilder in ParameterizedMessage won't hold excessively much memory after logging a long message.
* [LOG4J2-1885](https://issues.apache.org/jira/browse/LOG4J2-1885):
Fix documentation about default additivity value for loggers.
* [LOG4J2-1920](https://issues.apache.org/jira/browse/LOG4J2-1920):
ScriptEngineManager is not available in Android and causes a NoClassDefFoundError. Thanks to Ajitha.
* [LOG4J2-1989](https://issues.apache.org/jira/browse/LOG4J2-1989):
Clarify Javadoc for AbstractTriggeringPolicy. Thanks to Kenneth McFarland.
* [LOG4J2-1993](https://issues.apache.org/jira/browse/LOG4J2-1993):
Fix compiler warnings in LoggerConfigTest. Thanks to Kenneth McFarland.

### Changes
* [LOG4J2-1928](https://issues.apache.org/jira/browse/LOG4J2-1928):
Add support for DirectWriteRolloverStrategy to RollingRandomAcessFileAppender.
* [LOG4J2-2022](https://issues.apache.org/jira/browse/LOG4J2-2022):
RFC5424Layout now prints the process id.
* [LOG4J2-2020](https://issues.apache.org/jira/browse/LOG4J2-2020):
Remove default layout from KafkaAppender.
* [LOG4J2-2023](https://issues.apache.org/jira/browse/LOG4J2-2023):
Use a class' canonical name instead of name to create its logger name.
* [LOG4J2-2015](https://issues.apache.org/jira/browse/LOG4J2-2015):
Allow KeyStoreConfiguration and TrustStoreConfiguration to find files as resources.
* [LOG4J2-2011](https://issues.apache.org/jira/browse/LOG4J2-2011):
Replace JCommander command line parser with picocli to let users run Log4j2 utility applications without requiring an external dependency.
* [LOG4J2-1984](https://issues.apache.org/jira/browse/LOG4J2-1984):
Allow maxLength of StructuredData to be specified by the user.
* [LOG4J2-1071](https://issues.apache.org/jira/browse/LOG4J2-1071):
Allow for bufferSize=0 in SMTP appender. Thanks to Ben Ludkiewicz, Benjamin Jaton.
* [LOG4J2-1261](https://issues.apache.org/jira/browse/LOG4J2-1261):
Async Loggers no longer use deprecated LMAX Disruptor APIs. (Disruptor-3.3.3 or higher is now required.)
* [LOG4J2-1908](https://issues.apache.org/jira/browse/LOG4J2-1908):
Improved error message when misconfigured with multiple incompatible appenders targeting same file.
* [LOG4J2-1954](https://issues.apache.org/jira/browse/LOG4J2-1954):
Configurations with multiple root loggers now fail loudly.
* [LOG4J2-1958](https://issues.apache.org/jira/browse/LOG4J2-1958):
Deprecate SerializedLayout and remove it as default.
* [LOG4J2-1959](https://issues.apache.org/jira/browse/LOG4J2-1959):
Disable DTD processing in XML configuration files.
* [LOG4J2-1950](https://issues.apache.org/jira/browse/LOG4J2-1950):
Fix docker build with jdk9 requirements (#84). Thanks to Pierrick HYMBERT.
* [LOG4J2-1801](https://issues.apache.org/jira/browse/LOG4J2-1801):
Add more detail to WARN "Ignoring log event" messages printed to the console after log4j was shut down.
* [LOG4J2-1926](https://issues.apache.org/jira/browse/LOG4J2-1926):
Facilitate log4j use in Android applications: remove dependency on RMI and Management APIs from log4j-api.
* [LOG4J2-1956](https://issues.apache.org/jira/browse/LOG4J2-1956):
JMS Appender broker password should be a char[], not a String.
* [LOG4J2-1917](https://issues.apache.org/jira/browse/LOG4J2-1917):
Support using java.util.ServiceLoader to locate Log4j 2 API providers.
* [LOG4J2-1966](https://issues.apache.org/jira/browse/LOG4J2-1966):
Include separator option of PatternLayout in manual (and other updates). Thanks to M Sazzadul Hoque.
* [LOG4J2-1851](https://issues.apache.org/jira/browse/LOG4J2-1851):
Move server components from log4j-core to new log4-server module.
* [LOG4J2-1991](https://issues.apache.org/jira/browse/LOG4J2-1991):
Refactor SimpleMessage to be concise and clear (#100) Thanks to .
* [LOG4J2-2017](https://issues.apache.org/jira/browse/LOG4J2-2017):
Update Jackson from 2.8.9 to 2.9.0.
* [LOG4J2-1868](https://issues.apache.org/jira/browse/LOG4J2-1868):
Update ZeroMQ's JeroMQ from 0.3.6 to 0.4.0.
* [LOG4J2-1960](https://issues.apache.org/jira/browse/LOG4J2-1960):
Update ZeroMQ's JeroMQ from 0.4.0 to 0.4.1.
* [LOG4J2-1974](https://issues.apache.org/jira/browse/LOG4J2-1974):
Update ZeroMQ's JeroMQ from 0.4.1 to 0.4.2.
* [LOG4J2-1869](https://issues.apache.org/jira/browse/LOG4J2-1869):
Update Kafka client from 0.10.1.1 to 0.10.2.0
* [LOG4J2-1962](https://issues.apache.org/jira/browse/LOG4J2-1962):
Update Kafka client from 0.10.2.0 to 0.11.0.0
* [LOG4J2-1872](https://issues.apache.org/jira/browse/LOG4J2-1872):
Update JavaMail from 1.5.5 to 1.5.6.
* [LOG4J2-1879](https://issues.apache.org/jira/browse/LOG4J2-1879):
Update JAnsi from 1.14 to 1.15.
* [LOG4J2-1877](https://issues.apache.org/jira/browse/LOG4J2-1877):
Missing documentation for Max index limit in DefaultRolloverStrategy. Thanks to Chandra Tungathurthi.
* [LOG4J2-1899](https://issues.apache.org/jira/browse/LOG4J2-1899):
Add missing getters to classes in package org.apache.logging.log4j.core.net.ssl.
* [LOG4J2-1900](https://issues.apache.org/jira/browse/LOG4J2-1900):
Update JAnsi from 1.15 to 1.16.
* [LOG4J2-](https://issues.apache.org/jira/browse/LOG4J2-):
Update SLF4J from 1.7.24 to 1.7.25.
* [LOG4J2-1938](https://issues.apache.org/jira/browse/LOG4J2-1938):
Update Jackson from 2.8.7 to 2.8.9.
* [LOG4J2-1970](https://issues.apache.org/jira/browse/LOG4J2-1970):
Update HdrHistogram from 2.1.8 to 2.1.9.
* [LOG4J2-1975](https://issues.apache.org/jira/browse/LOG4J2-1975):
Update javax.persistence from 2.1.0 to 2.1.1.
* [LOG4J2-1976](https://issues.apache.org/jira/browse/LOG4J2-1976):
Update org.osgi.core from 4.3.1 to 6.0.0.

---

Apache Log4j 2.9.0 requires a minimum of Java 7 to build and run. Log4j 2.3 was the
last release that supported Java 6.

Basic compatibility with Log4j 1.x is provided through the log4j-1.2-api component, however it
does not implement some of the very implementation specific classes and methods. The package
names and Maven groupId have been changed to org.apache.logging.log4j to avoid any conflicts
with log4j 1.x.

For complete information on Apache Log4j 2, including instructions on how to submit bug
reports, patches, or suggestions for improvement, see the Apache Apache Log4j 2 website:

https://logging.apache.org/log4j/2.x/
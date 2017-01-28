# Apache Log4j 2.8 Release Notes

The Apache Log4j 2 team is pleased to announce the Log4j 2.8 release!

Apache Log4j is a well known framework for logging application behavior. Log4j 2 is an upgrade
to Log4j that provides significant improvements over its predecessor, Log4j 1.x, and provides
many other modern features such as support for Markers, lambda expressions for lazy logging,
property substitution using Lookups, multiple patterns on a PatternLayout and asynchronous
Loggers. Another notable Log4j 2 feature is the ability to be "garbage-free" (avoid allocating
temporary objects) while logging. In addition, Log4j 2 will not lose events while reconfiguring.

This release contains several bugfixes and new features. The new features include the ability
to have the RollingFileAppender log directly to the archive files. More details on the new features and
fixes are itemized below.

Note that subsequent to the 2.6 release a minor source incompatibility was found due to the
addition of new methods to the Logger interface. If you have code that does:

    logger.error(null, “This is the log message”, throwable);

or similar with any log level you will get a compiler error saying the reference is ambiguous.
To correct this either do:

    logger.error(“This is the log message”, throwable);

or

    logger.error((Marker) null, “This is the log message”, throwable);

The Log4j 2.8 API, as well as many core components, maintains binary compatibility with previous releases.

## GA Release 2.8

Changes in this version include:

### New Features
* [LOG4J2-1032](https://issues.apache.org/jira/browse/LOG4J2-1032):
Make DefaultRolloverStrategy more efficent when renaming files. Add nomax option to the fileIndex attribute.
* [LOG4J2-1101](https://issues.apache.org/jira/browse/LOG4J2-1101):
RollingFileAppender now supports omitting the file name and writing directly to the archive files.
* [LOG4J2-1243](https://issues.apache.org/jira/browse/LOG4J2-1243):
Allow default value in property to be a Lookup.
* [LOG4J2-1787](https://issues.apache.org/jira/browse/LOG4J2-1787):
Document how to exclude transitive conflicting dependencies in Maven and Gradle.
* [LOG4J2-1773](https://issues.apache.org/jira/browse/LOG4J2-1773):
Add StatusLoggerRule to allow unit tests to set a status level.
* [LOG4J2-424](https://issues.apache.org/jira/browse/LOG4J2-424):
Add non-string data type support to JdbcAppender via new ColumnMapping plugin.
* [LOG4J2-1771](https://issues.apache.org/jira/browse/LOG4J2-1771):
Add a Builder to ColumnConfig and deprecate ColumnConfig.createColumnConfig().
* [LOG4J2-1770](https://issues.apache.org/jira/browse/LOG4J2-1770):
Add a Builder to JdbcAppender and deprecate JdbcAppender.createAppender().
* [LOG4J2-1764](https://issues.apache.org/jira/browse/LOG4J2-1764):
Use MethodHandle in ContextDataFactory cached constructor.
* [LOG4J2-1730](https://issues.apache.org/jira/browse/LOG4J2-1730):
Add Apache Cassandra appender and ColumnMapping plugin.
* [LOG4J2-1759](https://issues.apache.org/jira/browse/LOG4J2-1759):
Add TypeConverter for java.util.UUID.
* [LOG4J2-1758](https://issues.apache.org/jira/browse/LOG4J2-1758):
Add TypeConverter for java.nio.file.Path.
* [LOG4J2-1755](https://issues.apache.org/jira/browse/LOG4J2-1755):
Add TypeConverter and constraint validators for java.net.InetAddress and port numbers.
* [LOG4J2-969](https://issues.apache.org/jira/browse/LOG4J2-969):
Refactor SyslogAppender so that Layout is a Plugin element.
* [LOG4J2-1660](https://issues.apache.org/jira/browse/LOG4J2-1660):
Added public method ThreadContext::getThreadContextMap; removed class ThreadContextAccess.
* [LOG4J2-1379](https://issues.apache.org/jira/browse/LOG4J2-1379):
Add documentation regarding YAML configuration format.
* [LOG4J2-1718](https://issues.apache.org/jira/browse/LOG4J2-1718):
Introduce marker interface AsynchronouslyFormattable.
* [LOG4J2-1681](https://issues.apache.org/jira/browse/LOG4J2-1681):
Introduce interfaces IndexedStringMap and IndexedReadOnlyStringMap, supporting garbage-free iteration over sorted map.
* [LOG4J2-1695](https://issues.apache.org/jira/browse/LOG4J2-1695):
Add a Builder to ScriptPatternSelector and deprecate ScriptPatternSelector.createSelector().
* [LOG4J2-1696](https://issues.apache.org/jira/browse/LOG4J2-1696):
Add a Builder to MarkerPatternSelector and deprecate MarkerPatternSelector.createSelector().
* [LOG4J2-1697](https://issues.apache.org/jira/browse/LOG4J2-1697):
Add a SerializerBuilder to PatternLayout and deprecate PatternLayout.createSerializer().
* [LOG4J2-1701](https://issues.apache.org/jira/browse/LOG4J2-1701):
Add a Builder to RandomAccessFileAppender and deprecate RandomAccessFileAppender.createAppender().
* [LOG4J2-1703](https://issues.apache.org/jira/browse/LOG4J2-1703):
Add a Builder to MemoryMappedFileAppender and deprecate MemoryMappedFileAppender.createAppender().
* [LOG4J2-1704](https://issues.apache.org/jira/browse/LOG4J2-1704):
Add a Builder to RollingRandomAccessFileAppender and deprecate RollingRandomAccessFileAppender.createAppender().
* [LOG4J2-1709](https://issues.apache.org/jira/browse/LOG4J2-1709):
Add a Builder to SyslogAppender and deprecate SyslogAppender.createAppender().
* [LOG4J2-1707](https://issues.apache.org/jira/browse/LOG4J2-1707):
Allow TCP Socket Appender to set socket options.
* [LOG4J2-1708](https://issues.apache.org/jira/browse/LOG4J2-1708):
Allow Secure Socket Appender to set socket options.
* [LOG4J2-1737](https://issues.apache.org/jira/browse/LOG4J2-1737):
Add a Builder to SyslogLayout and deprecate SyslogLayout.createLayout(Facility, boolean, String, Charset).
* [LOG4J2-1738](https://issues.apache.org/jira/browse/LOG4J2-1738):
Add a Builder to JsonLayout and deprecate org.apache.logging.log4j.core.layout.JsonLayout.createLayout(Configuration, boolean, boolean, boolean, boolean, boolean, boolean, String, String, Charset, boolean).
* [LOG4J2-1739](https://issues.apache.org/jira/browse/LOG4J2-1739):
Add Builder to KafkaAppender and deprecate KafkaAppender.createAppender(Layout, Filter, String, boolean, String, Property[], Configuration).
* [LOG4J2-1733](https://issues.apache.org/jira/browse/LOG4J2-1733):
Add SyncSend attribute to KafkaAppender (as in KafkaLog4jAppender). Thanks to Vincent Tieleman.

### Fixed Bugs
* [LOG4J2-1780](https://issues.apache.org/jira/browse/LOG4J2-1780):
Eliminate the use of the ExecutorServices in the LoggerContext.
* [LOG4J2-1786](https://issues.apache.org/jira/browse/LOG4J2-1786):
ConfigurationScheduler now preserves interrupt flag during stop.
* [LOG4J2-1779](https://issues.apache.org/jira/browse/LOG4J2-1779):
Fixed bug where AsyncLogger did not resolve configuration properties.
* [LOG4J2-1769](https://issues.apache.org/jira/browse/LOG4J2-1769):
Fixed concurrency issue affecting all layouts except PatternLayout and GelfLayout, which caused scrambled output and exceptions when logging synchronously from multiple threads. Thanks to Brandon Goodin.
* [LOG4J2-1724](https://issues.apache.org/jira/browse/LOG4J2-1724):
Using variables in GelfLayout's additional fields at runtime. Thanks to Alexander Krasnostavsky.
* [LOG4J2-1762](https://issues.apache.org/jira/browse/LOG4J2-1762):
Add Builder to GelfLayout.
* [LOG4J2-1649](https://issues.apache.org/jira/browse/LOG4J2-1649):
Insure the ConfigurationScheduler shuts down without blocking. Thanks to Georg Friedrich.
* [LOG4J2-1653](https://issues.apache.org/jira/browse/LOG4J2-1653):
CronTriggeringPolicy would use the wrong date/time when rolling over and create multiple triggering policies on reconfiguration. Thanks to Georg Friedrich.
* [LOG4J2-1748](https://issues.apache.org/jira/browse/LOG4J2-1748):
Do not use non-daemon thread pool for rollover tasks.
* [LOG4J2-1628](https://issues.apache.org/jira/browse/LOG4J2-1628):
Fixed file locking regression in FileAppender introduced in 2.6.
* [LOG4J2-1744](https://issues.apache.org/jira/browse/LOG4J2-1744):
The custom logger Generate tool no longer requires the log4j-api module on the classpath.
* [LOG4J2-1731](https://issues.apache.org/jira/browse/LOG4J2-1731):
SslSocketManager now respects connectTimeoutMillis. Thanks to Chris Ribble.
* [LOG4J2-1682](https://issues.apache.org/jira/browse/LOG4J2-1682):
Logger using LocalizedMessageFactory prints key instead of message. Thanks to Markus Waidhofer.
* [LOG4J2-1720](https://issues.apache.org/jira/browse/LOG4J2-1720):
Make GelfLayout independent of Jackson.
* [LOG4J2-1719](https://issues.apache.org/jira/browse/LOG4J2-1719):
Fixed race condition in ObjectMessage and SimpleMessage, ensuring that the log message contains the value the object has during the logging call.
* [LOG4J2-1688](https://issues.apache.org/jira/browse/LOG4J2-1688):
Fixed bug where elements of a log message parameter array were nulled out in garbage-free mode.
* [LOG4J2-1692](https://issues.apache.org/jira/browse/LOG4J2-1692):
Add putAll() method to CloseableThreadContext. Thanks to Greg Thomas.
* [LOG4J2-1689](https://issues.apache.org/jira/browse/LOG4J2-1689):
Add CleanableThreadContextMap interface supporting method removeAll(Iterable&lt;String&gt;).
* [LOG4J2-1685](https://issues.apache.org/jira/browse/LOG4J2-1685):
Option 'disableAnsi' in PatternLayout to unconditionally disable ANSI escape codes. Thanks to Raman Gupta.
* [LOG4J2-1706](https://issues.apache.org/jira/browse/LOG4J2-1706):
Make TimeFilter usable as global filter and as logger filter.
* [LOG4J2-1722](https://issues.apache.org/jira/browse/LOG4J2-1722):
(GC) Avoid allocating temporary objects in VariablesNotEmptyReplacementConverter.
* [LOG4J2-1717](https://issues.apache.org/jira/browse/LOG4J2-1717):
(GC) Avoid allocating temporary objects in EncodingPatternConverter.
* [LOG4J2-1716](https://issues.apache.org/jira/browse/LOG4J2-1716):
(GC) Avoid allocating temporary objects in MapPatternConverter. (Note that constructing a MapMessage is not garbage-free.)
* [LOG4J2-1683](https://issues.apache.org/jira/browse/LOG4J2-1683):
(GC) Avoid allocating temporary objects in MapMessage.
* [LOG4J2-1715](https://issues.apache.org/jira/browse/LOG4J2-1715):
(GC) Avoid allocating temporary objects in NdcPatternConverter. (Note that use of the ThreadContext stack is not garbage-free.)
* [LOG4J2-1714](https://issues.apache.org/jira/browse/LOG4J2-1714):
(GC) Avoid allocating temporary objects in AbstractStyleNameConverter.
* [LOG4J2-1680](https://issues.apache.org/jira/browse/LOG4J2-1680):
(GC) Avoid allocating temporary objects in TimeFilter.
* [LOG4J2-1679](https://issues.apache.org/jira/browse/LOG4J2-1679):
(GC) Avoid allocating temporary objects in StructuredDataFilter.
* [LOG4J2-1678](https://issues.apache.org/jira/browse/LOG4J2-1678):
(GC) Avoid allocating temporary objects in ThreadContextMapFilter.
* [LOG4J2-1677](https://issues.apache.org/jira/browse/LOG4J2-1677):
(GC) Avoid allocating temporary objects in MapFilter.
* [LOG4J2-1674](https://issues.apache.org/jira/browse/LOG4J2-1674):
(GC) Avoid allocating temporary objects in ThresholdFilter.
* [LOG4J2-1673](https://issues.apache.org/jira/browse/LOG4J2-1673):
(GC) Avoid allocating temporary objects in MarkerFilter.
* [LOG4J2-1672](https://issues.apache.org/jira/browse/LOG4J2-1672):
(GC) Avoid allocating temporary objects in LevelRangeFilter.
* [LOG4J2-1671](https://issues.apache.org/jira/browse/LOG4J2-1671):
(GC) Avoid allocating temporary objects in EqualsIgnoreCaseReplacementConverter.
* [LOG4J2-1670](https://issues.apache.org/jira/browse/LOG4J2-1670):
(GC) Avoid allocating temporary objects in EqualsReplacementConverter.
* [LOG4J2-1669](https://issues.apache.org/jira/browse/LOG4J2-1669):
(GC) Avoid allocating temporary objects in MaxLengthConverter.
* [LOG4J2-1668](https://issues.apache.org/jira/browse/LOG4J2-1668):
(GC) Avoid allocating temporary objects in MarkerPatternConverter.
* [LOG4J2-1667](https://issues.apache.org/jira/browse/LOG4J2-1667):
(GC) Avoid allocating temporary objects in SequenceNumberPatternConverter.
* [LOG4J2-1666](https://issues.apache.org/jira/browse/LOG4J2-1666):
(GC) Avoid allocating temporary objects in RelativeTimePatternConverter.
* [LOG4J2-1665](https://issues.apache.org/jira/browse/LOG4J2-1665):
(GC) Avoid allocating temporary objects in IntegerPatternConverter.
* [LOG4J2-1637](https://issues.apache.org/jira/browse/LOG4J2-1637):
Fixed problems when used in OSGi containers (IllegalAccessError, NoClassDefFoundError).
* [LOG4J2-1226](https://issues.apache.org/jira/browse/LOG4J2-1226):
Improve LogEvent serialization to handle non-serializable Messages and deserializing when required classes are missing.
* [LOG4J2-1663](https://issues.apache.org/jira/browse/LOG4J2-1663):
Ensure SortedArrayStringMap can be serialized and deserialized without errors regardless of content.
* [LOG4J2-1658](https://issues.apache.org/jira/browse/LOG4J2-1658):
Prevent NPE in ThreadContextMapFactory::createThreadContextMap when initializing Log4j with Configurator::initialize and the BasicContextSelector is used.
* [LOG4J2-1645](https://issues.apache.org/jira/browse/LOG4J2-1645):
Immutable empty StringMap.
* [LOG4J2-1623](https://issues.apache.org/jira/browse/LOG4J2-1623):
Configurable JVM shutdown hook timeout.
* [LOG4J2-1712](https://issues.apache.org/jira/browse/LOG4J2-1712):
Pick up bug fixes from Apache Commons Lang's org.apache.commons.lang3.time package.
* [LOG4J2-1636](https://issues.apache.org/jira/browse/LOG4J2-1636):
Console Appender does not pick up Oracle Java 8's sun.stdout.encoding and sun.stderr.encoding. Thanks to Eldar Gabdullin.
* [LOG4J2-1639](https://issues.apache.org/jira/browse/LOG4J2-1639):
Fix MemoryMappedFileAppender.createAppender() Javadoc for immediateFlush. Thanks to Sridhar Gopinath.
* [LOG4J2-1676](https://issues.apache.org/jira/browse/LOG4J2-1676):
Some LogEvents may not carry a Throwable (Use Message.getThrowable() in log(Message) methods.) Thanks to Joern Huxhorn.
* [LOG4J2-1723](https://issues.apache.org/jira/browse/LOG4J2-1723):
Unwanted transitive dependency on geronimo-jms_1.1_spec causes OSGi tests to fail. Thanks to Ludovic HOCHET.
* [LOG4J2-1664](https://issues.apache.org/jira/browse/LOG4J2-1664):
Improve OSGi unit tests. Thanks to Ludovic HOCHET.
* [LOG4J2-1687](https://issues.apache.org/jira/browse/LOG4J2-1687):
NPE in ThrowableProxy when resolving stack in Java EE/OSGi environment. Thanks to Robert Christiansen.
* [LOG4J2-1642](https://issues.apache.org/jira/browse/LOG4J2-1642):
DefaultShutdownCallbackRegistry can throw a NoClassDefFoundError. Thanks to Johno Crawford.
* [LOG4J2-1474](https://issues.apache.org/jira/browse/LOG4J2-1474):
CronTriggeringPolicy raise exception and fail to rollover log file when evaluateOnStartup is true. Thanks to yin mingjun, Neon.
* [LOG4J2-1734](https://issues.apache.org/jira/browse/LOG4J2-1734):
SslSocketManagerFactory might leak Sockets when certain startup errors occur.
* [LOG4J2-1736](https://issues.apache.org/jira/browse/LOG4J2-1736):
TcpSocketManagerFactory might leak Sockets when certain startup errors occur.
* [LOG4J2-1740](https://issues.apache.org/jira/browse/LOG4J2-1740):
Add CronTriggeringPolicy programmatically leads to NPE.
* [LOG4J2-1743](https://issues.apache.org/jira/browse/LOG4J2-1743):
CompositeConfiguration does not add filters to appenderRefs. Thanks to Toby Shepheard.
* [LOG4J2-1756](https://issues.apache.org/jira/browse/LOG4J2-1756):
Adds xmlns in schema and some other tags. Thanks to shubhankar1100.

### Changes
* [LOG4J2-1781](https://issues.apache.org/jira/browse/LOG4J2-1781):
Update Conversant Disruptor from 1.2.7 to 1.2.10
* [LOG4J2-1774](https://issues.apache.org/jira/browse/LOG4J2-1774):
Replace MockEJB dependency in unit tests with Spring Test and Mockito.
* [LOG4J2-1644](https://issues.apache.org/jira/browse/LOG4J2-1644):
Inefficient locking in AbstractLoggerAdapter. Thanks to Tim Gokcen, Pavel Sivolobtchik.
* [LOG4J2-1641](https://issues.apache.org/jira/browse/LOG4J2-1641):
Update JeroMQ from 0.3.5 to 0.3.6.
* [LOG4J2-1647](https://issues.apache.org/jira/browse/LOG4J2-1647):
Update Commons Lang from 3.4 to 3.5.
* [LOG4J2-1646](https://issues.apache.org/jira/browse/LOG4J2-1646):
Migrate to Mockito 2.x in unit tests.
* [LOG4J2-1655](https://issues.apache.org/jira/browse/LOG4J2-1655):
Update Jackson from 2.8.3 to 2.8.4.
* [LOG4J2-1735](https://issues.apache.org/jira/browse/LOG4J2-1735):
Update Jackson from 2.8.4 to 2.8.5.
* [LOG4J2-1656](https://issues.apache.org/jira/browse/LOG4J2-1656):
Update Apache Flume from 1.6.0 to 1.7.0.
* [LOG4J2-1698](https://issues.apache.org/jira/browse/LOG4J2-1698):
Update LMAX Disruptor from 3.3.5 to 3.3.6.
* [LOG4J2-1700](https://issues.apache.org/jira/browse/LOG4J2-1700):
Update Jansi from 1.13 to 1.14.
* [LOG4J2-1750](https://issues.apache.org/jira/browse/LOG4J2-1750):
Update Kafka from 0.10.0.1 to 0.10.1.1.
* [LOG4J2-1751](https://issues.apache.org/jira/browse/LOG4J2-1751):
Update liquibase-core from 3.5.1 to 3.5.3.
* [LOG4J2-1302](https://issues.apache.org/jira/browse/LOG4J2-1302):
The log4j-slf4j-impl module now declares a runtime dependency on log4j-core. While not technically required, this makes the log4j-slf4j-impl module behave similarly to slf4j-log4j12, and facilitates migration to Log4j 2.

---

Apache Log4j 2.8 requires a minimum of Java 7 to build and run. Log4j 2.3 was the
last release that supported Java 6.

Basic compatibility with Log4j 1.x is provided through the log4j-1.2-api component, however it
does not implement some of the very implementation specific classes and methods. The package
names and Maven groupId have been changed to org.apache.logging.log4j to avoid any conflicts
with log4j 1.x.

For complete information on Apache Log4j 2, including instructions on how to submit bug
reports, patches, or suggestions for improvement, see the Apache Apache Log4j 2 website:

https://logging.apache.org/log4j/2.x/
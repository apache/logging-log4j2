# Apache Log4j 2.8.1 Release Notes

The Apache Log4j 2 team is pleased to announce the Log4j 2.8.1 release!

Apache Log4j is a well known framework for logging application behavior. Log4j 2 is an upgrade
to Log4j that provides significant improvements over its predecessor, Log4j 1.x, and provides
many other modern features such as support for Markers, lambda expressions for lazy logging,
property substitution using Lookups, multiple patterns on a PatternLayout and asynchronous
Loggers. Another notable Log4j 2 feature is the ability to be "garbage-free" (avoid allocating
temporary objects) while logging. In addition, Log4j 2 will not lose events while reconfiguring.

This release primarily contains bugfixes and minor enhancements. More details on the new features and
fixes are itemized below.

Note that subsequent to the 2.6 release a minor source incompatibility was found due to the
addition of new methods to the Logger interface. If you have code that does:

    logger.error(null, “This is the log message”, throwable);

or similar with any log level you will get a compiler error saying the reference is ambiguous.
To correct this either do:

    logger.error(“This is the log message”, throwable);

or

    logger.error((Marker) null, “This is the log message”, throwable);

The Log4j 2.8.1 API, as well as many core components, maintains binary compatibility with previous releases.

## GA Release 2.8.1

Changes in this version include:

### New Features
* [LOG4J2-1823](https://issues.apache.org/jira/browse/LOG4J2-1823):
Remove deprecation on MessageSupplier lambda functions in Logger API.
* [LOG4J2-1807](https://issues.apache.org/jira/browse/LOG4J2-1807):
[core] Add and implement LogEvent.toImmutable().

### Fixed Bugs
* [LOG4J2-1804](https://issues.apache.org/jira/browse/LOG4J2-1804):
Allow %i in file pattern to be preceded with characters other than just '-'. Thanks to Pierrick Hymbert.
* [LOG4J2-1816](https://issues.apache.org/jira/browse/LOG4J2-1816):
Change minOccur to minOccurs in Log4j-config.xsd. Thanks to shubhankar1100.
* [LOG4J2-1803](https://issues.apache.org/jira/browse/LOG4J2-1803):
Fix Maven POM to ensure JMH generated classes in log4j-perf are included in benchmarks jar.
* [LOG4J2-1800](https://issues.apache.org/jira/browse/LOG4J2-1800):
Report errors when sending to Kafka when using syncSend=false. Thanks to Vincent Tieleman.
* [LOG4J2-1805](https://issues.apache.org/jira/browse/LOG4J2-1805):
Fixed rare race condition in FixedDateFormat, made FixedDateFormat::millisSinceMidnight method public.
* [LOG4J2-1799](https://issues.apache.org/jira/browse/LOG4J2-1799):
Fixed bug in PropertiesUtil::getCharsetProperty that caused UnsupportedCharsetException for ConsoleAppender. Thanks to Eduard Gizatullin.
* [LOG4J2-1806](https://issues.apache.org/jira/browse/LOG4J2-1806):
Fix Javadoc for DefaultRolloverStrategy::purgeAscending Thanks to challarao.
* [LOG4J2-1818](https://issues.apache.org/jira/browse/LOG4J2-1818):
Fix rollover to work when filePattern contains no directory components. Thanks to xkr47.

### Changes
* [LOG4J2-1822](https://issues.apache.org/jira/browse/LOG4J2-1822):
Update SLF4J to 1.7.24.
* [LOG4J2-1812](https://issues.apache.org/jira/browse/LOG4J2-1812):
Improved error message when log4j 2 configuration file not found.
* [LOG4J2-1810](https://issues.apache.org/jira/browse/LOG4J2-1810):
Update to use Logback 1.1.10 and then Logback 1.2 for tests.
* [LOG4J2-1819](https://issues.apache.org/jira/browse/LOG4J2-1819):
Update Jackson from 2.8.5 to 2.8.6.

---

Apache Log4j 2.8.1 requires a minimum of Java 7 to build and run. Log4j 2.3 was the
last release that supported Java 6.

Basic compatibility with Log4j 1.x is provided through the log4j-1.2-api component, however it
does not implement some of the very implementation specific classes and methods. The package
names and Maven groupId have been changed to org.apache.logging.log4j to avoid any conflicts
with log4j 1.x.

For complete information on Apache Log4j 2, including instructions on how to submit bug
reports, patches, or suggestions for improvement, see the Apache Apache Log4j 2 website:

https://logging.apache.org/log4j/2.x/
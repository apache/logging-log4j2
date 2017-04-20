# Apache Log4j 2.8.2 Release Notes

The Apache Log4j 2 team is pleased to announce the Log4j 2.8.2 release!

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

The Log4j 2.8.2 API, as well as many core components, maintains binary compatibility with previous releases.

## GA Release 2.8.2

Changes in this version include:

### New Features
* [LOG4J2-1863](https://issues.apache.org/jira/browse/LOG4J2-1863):
Add support for filtering input in TcpSocketServer and UdpSocketServer.
* [LOG4J2-1848](https://issues.apache.org/jira/browse/LOG4J2-1848):
Add JSON encoding support to EncodingPatternConverter %encode{}.
* [LOG4J2-1843](https://issues.apache.org/jira/browse/LOG4J2-1843):
Add support for appending common suffix to each line of throwable stack trace. Thanks to Zilong Song.
* [LOG4J2-1838](https://issues.apache.org/jira/browse/LOG4J2-1838):
Add support for appending common suffix to each line of extended and root throwable stack trace. Thanks to Zilong Song.

### Fixed Bugs
* [LOG4J2-1861](https://issues.apache.org/jira/browse/LOG4J2-1861):
Fix JavaDoc on org.apache.logging.log4j.ThreadContext about inheritance.
* [LOG4J2-1862](https://issues.apache.org/jira/browse/LOG4J2-1862):
Fix JavaDoc about @Order and OrderComparator ordering. Thanks to wangyuntao.
* [LOG4J2-1849](https://issues.apache.org/jira/browse/LOG4J2-1849):
Fixed daylight savings time issue with FixedDateFormat.
* [LOG4J2-1850](https://issues.apache.org/jira/browse/LOG4J2-1850):
Fix CassandraRule and unit tests on Windows. Thanks to Ludovic Hochet.
* [LOG4J2-1840](https://issues.apache.org/jira/browse/LOG4J2-1840):
Fix typo in %replace converter documentation. Thanks to Pradeep Balasundaram.
* [LOG4J2-1846](https://issues.apache.org/jira/browse/LOG4J2-1846):
Handle when LogEvent.getLoggerName() returns null in LoggerNameLevelRewritePolicy.
* [LOG4J2-1845](https://issues.apache.org/jira/browse/LOG4J2-1845):
Handle when LogEvent.getLoggerName() returns null in KafkaAppender.
* [LOG4J2-1853](https://issues.apache.org/jira/browse/LOG4J2-1853):
The default value of RandomAccessFileAppender.Builder append field is wrong. Thanks to wangyuntao.
* [LOG4J2-1835](https://issues.apache.org/jira/browse/LOG4J2-1835):
Fix documentation about the licensing for JeroMQ.
* [LOG4J2-1836](https://issues.apache.org/jira/browse/LOG4J2-1836):
Update the API version to 2.6.0.
* [LOG4J2-1831](https://issues.apache.org/jira/browse/LOG4J2-1831):
NullPointerException in HtmlLayout. Thanks to Edward Serebrinskiy.
* [LOG4J2-1820](https://issues.apache.org/jira/browse/LOG4J2-1820):
Log4j 2.8 can lose exceptions when a security manager is present. Thanks to Jason Tedor.

### Changes
* [LOG4J2-1827](https://issues.apache.org/jira/browse/LOG4J2-1827):
Move integration tests to their own module to speed up build.
* [LOG4J2-1856](https://issues.apache.org/jira/browse/LOG4J2-1856):
Update Jackson from 2.8.6 to 2.8.7.

---

Apache Log4j 2.8.2 requires a minimum of Java 7 to build and run. Log4j 2.3 was the
last release that supported Java 6.

Basic compatibility with Log4j 1.x is provided through the log4j-1.2-api component, however it
does not implement some of the very implementation specific classes and methods. The package
names and Maven groupId have been changed to org.apache.logging.log4j to avoid any conflicts
with log4j 1.x.

For complete information on Apache Log4j 2, including instructions on how to submit bug
reports, patches, or suggestions for improvement, see the Apache Apache Log4j 2 website:

https://logging.apache.org/log4j/2.x/
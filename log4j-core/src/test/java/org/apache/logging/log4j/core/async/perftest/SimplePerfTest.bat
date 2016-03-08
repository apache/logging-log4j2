@echo off
REM reject if no arg supplied
IF %1.==. echo Usage: %0 version & exit /b

set GC_OPTIONS=-verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintTenuringDistribution -XX:+PrintGCApplicationConcurrentTime -XX:+PrintGCApplicationStoppedTime
set GC_OPTIONS=

set LOG4J_OPTIONS=-Dlog4j.configurationFile=perf-CountingNoOpAppender.xml
:set LOG4J_OPTIONS=-Dlog4j.configurationFile=perf3PlainNoLoc.xml
set LOG4J_OPTIONS=%LOG4J_OPTIONS% -DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector
set LOG4J_OPTIONS=%LOG4J_OPTIONS% -Dlog4j2.enable.threadlocals=true

set CP=log4j-api-%1.jar;log4j-core-%1.jar;disruptor-3.3.4.jar;log4j-1.2.17.jar;slf4j-api-1.7.13.jar;logback-classic-1.1.3.jar;logback-core-1.1.3.jar
set CP=%CP%;C:\Users\remko\IdeaProjects\logging-log4j2\log4j-core\target\test-classes
:set CP=%CP%;log4j-core-2.0-tests.jar

set MAIN=org.apache.logging.log4j.core.async.perftest.SimplePerfTest

@echo on
java -Xms128m -Xmx128m %GC_OPTIONS% %LOG4J_OPTIONS% -cp %CP% %MAIN%

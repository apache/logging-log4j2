
We know Performance is critical for any applications and nobody wants the underlying logging framework to become a bottleneck.
We want lower logging latency and higher throughput. Also we want to increased the logging performance.

In that case, The `asynchronous` logger in `log4J2` does this by decoupling the logging overhead from the thread executing your code.
Async logger is designed to optimize this area by replacing the blocking queue with `disruptor` .
`Disruptor` is a Lock-free inter-thread communication library. So by using this its provide higher throughput and lower latency in `Log4J2` logging.

**Log4J2**  supports JSON,XML and YAML in addition to properties file configuration . Here I use XML file for the configuration.

### Add log4j Dependancy :
Add log4j Dependancy in  `pom.xml` file:

```xml
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-api</artifactId>
    <version>2.17.1</version>
</dependency>

<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <version>2.17.1</version>
</dependency>
```

### Add disruptor Dependancy :

Add disruptor in `pom.xml` file

```xml
<dependency>
    <groupId>com.lmax</groupId>
    <artifactId>disruptor</artifactId>
    <version>3.4.2</version>
</dependency>
```


### Configure `log4j2.xml` file for Log4J 2 async logger :
We will use XML to configure Log4J2 2 async logger.
Now Create a `log4j2.xml` file in the project classpath.

1. *Add basic configuration*

Path : `src/resources/log4j2.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="DEBUG">
    <Appenders>
        <Console name="LogToConsole" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
        </Console>
    </Appenders>
    <Loggers>
        <Logger name="org.apache.logging.log4j.async" level="debug" additivity="false">
            <AppenderRef ref="LogToConsole"/>
        </Logger>
        <Root level="error">
            <AppenderRef ref="LogToConsole"/>
        </Root>
    </Loggers>
</Configuration>
```
2. *Now add `RollingRandomAccessFile` Sections into `Appenders`  to configure the rolling log files*

```xml
 <RollingRandomAccessFile name="LogToRollingRandomAccessFile" fileName="logs/app.log"
                                 filePattern="logs/$${date:yyyy-MM}/app-%d{MM-dd-yyyy}-%i.log">
    <PatternLayout>
        <Pattern>%d %p %c{1.} [%t] %m%n</Pattern>
    </PatternLayout>
    <Policies>
        <TimeBasedTriggeringPolicy/>
        <SizeBasedTriggeringPolicy size="3 MB"/>
    </Policies>
    <DefaultRolloverStrategy max="5"/>
</RollingRandomAccessFile>


```
3. *Now add `AsyncAppender` Appender into the `Loggers` sections. So after the configuraiton, `log4j2.xml` file should be look like :* 

```xml
<Configuration status="DEBUG">
    <Appenders>
        <Console name="LogToConsole" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
        </Console>
        <RollingRandomAccessFile name="LogToRollingRandomAccessFile" fileName="logs/app.log"
                                 filePattern="logs/$${date:yyyy-MM}/app-%d{MM-dd-yyyy}-%i.log">
            <PatternLayout>
                <Pattern>%d %p %c{1.} [%t] %m%n</Pattern>
            </PatternLayout>
            <Policies>
                <TimeBasedTriggeringPolicy/>
                <SizeBasedTriggeringPolicy size="3 MB"/>
            </Policies>
            <DefaultRolloverStrategy max="5"/>
        </RollingRandomAccessFile>

    </Appenders>
    <Loggers>

        <!--  asynchronous loggers -->
        <AsyncLogger name="org.apache.logging.log4j.async" level="debug" additivity="false">
            <AppenderRef ref="LogToRollingRandomAccessFile"/>
            <AppenderRef ref="LogToConsole"/>
        </AsyncLogger>

        <!-- synchronous loggers -->
        <Root level="error">
            <AppenderRef ref="LogToConsole"/>
        </Root>
    </Loggers>
</Configuration>
```
*We can change the status to `trace`, `debug`, `info`, `warn`,  and `fatal` to enable the internal Log4j events.*

### Configure `Log4J2AsyncLogger.java` Class :
Now create a logger class `Log4J2AsyncLogger.java` that uses the Log4J2 API to log the messages.

```java
package org.apache.logging.log4j.async;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Log4J2AsyncLogger {

    private static final Logger logger = LogManager.getLogger(Log4J2AsyncLogger.class);

    public static void main(String[] args) {

        Log4J2AsyncLogger myLog = new Log4J2AsyncLogger();
        myLog.getLog("Log4j2 Log");

    }

    private void getLog(String param){

        logger.info("This is a info log");

        // Previously, need to check the log level log to increase performance
        if(logger.isDebugEnabled()){
            logger.debug("This is debug log with param : " + param);
        }

        if(logger.isWarnEnabled()){
            logger.info("This is warn log with param : " + param);
        }

        // In Java 8, No need to check the log level, we can do this
        while (true) //for test rolling file
            logger.debug("Hello print {}", () -> getValue());
    }

    static String getValue() {
        return "Debug Log";
    }
}
```

### Configure `Log4J2AsyncLoggerException.java` Class :
Create another logger class `Log4J2AsyncLoggerException.java` to create and check exceptions.

```java
package org.apache.logging.log4j.async;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Log4J2AsyncLoggerException {

    private static final Logger logger = LogManager.getLogger(Log4J2AsyncLoggerException.class);

    public static void main(String[] args) {

        try {
            System.out.println(getException());
        } catch (IllegalArgumentException e) {
            logger.error("{}", e);
        }
    }

    static int getException() throws IllegalArgumentException {
        throw new IllegalArgumentException("Hello, Something Went Wrong. Exception Occured!!");
    }

}
```

To enable all loggers to asynchronous, we need 2 things :

 - Need to be present `disruptor` in project class path.
 - Set system property `log4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector`
 
For more detials you can check this link.
https://logging.apache.org/log4j/2.x/manual/async.html

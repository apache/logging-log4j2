# [Apache Log4j 2](https://logging.apache.org/log4j/2.x/)

Apache Log4j 2 is an upgrade to Log4j that provides significant improvements over its predecessor, Log4j 1.x,
and provides many of the improvements available in Logback while fixing some inherent problems in Logback's architecture.

[![Jenkins Status](https://img.shields.io/jenkins/s/https/builds.apache.org/job/Log4j%202.x.svg)](https://builds.apache.org/job/Log4j%202.x/)
[![Travis Status](https://travis-ci.org/apache/logging-log4j2.svg?branch=master)](https://travis-ci.org/apache/logging-log4j2)
[![Maven Central](https://img.shields.io/maven-central/v/org.apache.logging.log4j/log4j-api.svg)](http://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-api)


## Pull Requests on Github

By sending a pull request you grant the Apache Software Foundation sufficient rights to use and release the submitted 
work under the Apache license. You grant the same rights (copyright license, patent license, etc.) to the 
Apache Software Foundation as if you have signed a Contributor License Agreement. For contributions that are 
judged to be non-trivial, you will be asked to actually signing a Contributor License Agreement.

## Usage

Users should refer to [Maven, Ivy, Gradle, and SBT Artifacts](http://logging.apache.org/log4j/2.x/maven-artifacts.html)
on the Log4j web site for instructions on how to include Log4j into their project using their chosen build tool.

Basic usage of the `Logger` API:

```java
package com.example;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class Example {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void main(String... args) {
        String thing = args.length > 0 ? args[0] : "world";
        LOGGER.info("Hello, {}!", thing);
        LOGGER.debug("Got calculated value only if debug enabled: {}", () -> doSomeCalculation());
    }

    private static Object doSomeCalculation() {
        // do some complicated calculation
    }
}
```

And an example `log4j2.xml` configuration file:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration>
  <Appenders>
    <Console name="Console" target="SYSTEM_OUT">
      <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
    </Console>
  </Appenders>
  <Loggers>
    <Logger name="com.example" level="INFO"/>
    <Root level="error">
      <AppenderRef ref="Console"/>
    </Root>
  </Loggers>
</Configuration>
```

## Documentation

The Log4j 2 User's Guide is available [here](https://logging.apache.org/log4j/2.x/manual/index.html) or as a downloadable
[PDF](https://logging.apache.org/log4j/2.x/log4j-users-guide.pdf).

## Requirements

Log4j 2.4 and greater requires Java 7, versions 2.0-alpha1 to 2.3 required Java 6.
Some features require optional dependencies; the documentation for these features specifies the dependencies.

## License

Apache Log4j 2 is distributed under the [Apache License, version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).

## Download

[How to download Log4j](http://logging.apache.org/log4j/2.x/download.html),
and [how to use it from Maven, Ivy and Gradle](http://logging.apache.org/log4j/2.x/maven-artifacts.html).
You can access the latest development snapshot by using the Maven repository `https://repository.apache.org/snapshots`, 
see [Snapshot builds](https://logging.apache.org/log4j/2.x/maven-artifacts.html#Snapshot_builds).

## Issue Tracking

Issues, bugs, and feature requests should be submitted to the 
[JIRA issue tracking system for this project](https://issues.apache.org/jira/browse/LOG4J2).

Pull request on GitHub are welcome, but please open a ticket in the JIRA issue tracker first, and mention the 
JIRA issue in the Pull Request.

## Building From Source

Log4j requires Apache Maven 3.x. To build from source and install to your local Maven repository, execute the following:

```sh
mvn install
```

## Contributing

We love contributions! Take a look at
[our contributing page](https://github.com/apache/logging-log4j2/blob/master/CONTRIBUTING.md).

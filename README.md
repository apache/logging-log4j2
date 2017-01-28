# [Apache Log4j 2](https://logging.apache.org/log4j/2.x/)

Apache Log4j 2 is an upgrade to Log4j that provides significant improvements over its predecessor, Log4j 1.x,
and provides many of the improvements available in Logback while fixing some inherent problems in Logback's architecture.

[![Jenkins Status](https://builds.apache.org/buildStatus/icon?job=Log4j 2.x)](https://builds.apache.org/job/Log4j%202.x/)
[![Travis Status](https://travis-ci.org/apache/logging-log4j2.svg?branch=master)](https://travis-ci.org/apache/logging-log4j2)
[![Coverage Status](https://coveralls.io/repos/github/apache/logging-log4j2/badge.svg?branch=master)](https://coveralls.io/github/apache/logging-log4j2?branch=master)

## Usage

Maven users can add the following dependencies to their `pom.xml` file:

```xml
<dependencies>
  <dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-api</artifactId>
    <version>2.8</version>
  </dependency>
  <dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <version>2.8</version>
  </dependency>
</dependencies>
```

Gradle users can add the following to their `build.gradle` file:

```gradle
dependencies {
  compile group: 'org.apache.logging.log4j', name: 'log4j-api', version: '2.8'
  compile group: 'org.apache.logging.log4j', name: 'log4j-core', version: '2.8'
}
```

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

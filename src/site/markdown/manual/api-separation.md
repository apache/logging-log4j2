<!-- vim: set syn=markdown : -->
<!--
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

# API Separation

When selecting a logging library, some care must be taken in order to ensure
that multiple different logging libraries are properly accounted for.  For
example, library code that you depend on may use slf4j, while other libraries
may simply use java.util.logging.  All of these can be routed to the log4j
core in order to be logged.

If however you want to use a different logging implementation (such as logback),
it is possible to route messages from the Log4j API to logback, ensuring that
your application is not tied to a specific logging framework.

A typical class using the Log4j2 API looks like the following:

```java
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Log4j2Test {
    private static final Logger logger = LogManager.getLogger();

    public Log4j2Test(){
        logger.info( "Hello World!" );
    }
}
```

In order to use the API portion of Log4j2, we only need to provide a single
dependency, log4j-api.  Using Maven, you would add the following to your
dependencies:

```xml
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-api</artifactId>
    <version>2.17.0</version>
</dependency>
```

## Using Log4j2 API and Core

Using the Log4j2 API and the implementation (Core) together means that log messages will be routed
through the Log4j2 Core.  The Log4j2 core implementation is responsible for the
following (note: this is not an exhaustive list):

* Configuration of the system (via an XML file for example)
* Routing messages to Appenders
* Opening files and other resources for logging (e.g. network sockets)

The [configuration](configuration.html) page in the manual describes
the configuration format supported by the Log4j2 core implementation.

To use both the API and the core implementation, you would add the following to your
dependencies (assuming that you are using Maven):

```xml
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-api</artifactId>
    <version>2.17.0</version>
</dependency>
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <version>2.17.0</version>
</dependency>
```

Note that having two different versions of log4j-api and log4j-core on your
classpath is not guaranteed to work correctly (e.g., log4j-api version 2.15 and 
 log4j-core version 2.17 are not guaranteed to work correctly together).

##  Using Log4j2 API with Logback

Since the Log4j2 API is generic, we can use it to send messages via SLF4J
and then have Logback do the actual logging of the messages.  This means
that you can write your code tied to the Log4j2 API, but users of your
code do not need to use the Log4j2 core if they are already using Logback.

To switch to using Logback as your logging backend, you will need to add the following to your
dependencies (assuming that you are using Maven):

```xml
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-api</artifactId>
    <version>2.17.0</version>
</dependency>
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-to-slf4j</artifactId>
    <version>2.17.0</version>
</dependency>
<dependency>
      <groupId>ch.qos.logback</groupId>
      <artifactId>logback-classic</artifactId>
      <version>1.2.10</version>
</dependency>
```

## Using Log4j2 as an SLF4J Implementation

If you don't want to depend on the Log4j2 API and instead want to use SLF4J,
that is possible as well.  Assuming that our code looks like the following:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Log4j2Test {

    private static final Logger logger = LoggerFactory.getLogger(Log4j2Test.class);

    public Log4j2Test(){
        logger.info( "Hello World!" );
    }
}
```

We can then route the messages to Log4j2 using the log4j-slf4j-impl like the following:

```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>1.7.32</version>
</dependency>
<dependency>
      <groupId>org.apache.logging.log4j</groupId>
      <artifactId>log4j-slf4j-impl</artifactId>
      <version>2.17.0</version>
</dependency>
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <version>2.17.0</version>
</dependency>
```

Note that if we were using SLF4J 1.8 instead of 1.7, that requires us to use
log4j-slf4j18-impl instead of log4j-slf4j-impl.

## Using Log4j2 with JUL

It is also possible to route messages that are logged using java.util.logging
to Log4j2.  Assuming that the code looks like the following:

```java
import java.util.logging.Logger;

public class Log4j2Test {

    private static final Logger logger = Logger.getLogger(Log4j2Test.class.getName());

    public Log4j2Test() {
        logger.info("Hello World!");
    }
}
```

We can then also route these messages to the Log4j2 core by adding in the JUL bridge,
and setting `-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager` on the JVM (see the documentation on
[the JUL adapter](../log4j-jul/index.html) for
more information as to how this works).

In order to route these messages to Log4j2, your dependencies would look like the following:

```xml
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-jul</artifactId>
    <version>2.17.0</version>
</dependency>
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <version>2.17.0</version>
</dependency>
```

## Using Log4j2 as a backend for Log4j1

Some software may still depend on Log4j1, and in some cases it may be infeasible
to modify this software to migrate it to Log4j2.

However, it may be possible to start using Log4j2 without modifying the application.

Assuming that our code looks like the following:

```java
import org.apache.log4j.Logger;

public class Log4j2Test {

    private static final Logger logger = Logger.getLogger(Log4j2Test.class);

    public Log4j2Test(){
        logger.info( "Hello World!" );
    }
}
```

we can then quickly and easily configure these messages to use Log4j2
as the logging implementation by depending on the `log4j-1.2-api` bridge, like so:

```xml
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-1.2-api</artifactId>
    <version>2.17.0</version>
</dependency>
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <version>2.17.0</version>
</dependency>
```

There are some limitations to this, but it is expected to work for the majority of common cases.
See the [manual page on migration](migration.html) for more information on this feature.

# Conclusion

With the API separation that Log4j2 provides, it is possible to use multiple
logging APIs and implementations in the same project.  This allows for greater
flexibility, ensuring that you are not tied to a single API or implementation.


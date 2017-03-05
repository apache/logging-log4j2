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

# Log4j IOStreams

## Log4j IOStreams

The IOStreams component is a [Log4j API](../log4j-api/index.html) extension that provides numerous
classes from
[`java.io`](http://docs.oracle.com/javase/6/docs/api/java/io/package-summary.html)
that can either write to a
[`Logger`](../log4j-api/apidocs/org/apache/logging/log4j/Logger.html)
while writing to another `OutputStream` or `Writer`, or the contents read by an
`InputStream` or `Reader` can be
[wiretapped](http://www.eaipatterns.com/WireTap.html) by a `Logger`.

## Requirements

The Log4j IOStreams API extension requires the Log4j 2 API. This component was introduced in Log4j 2.1.
For more information, see [Runtime Dependencies](../runtime-dependencies.html).

## Usage
The main entry point for the IOStreams module is the builder class
[`IoBuilder`](apidocs/org/apache/logging/log4j/io/IoBuilder.html), and in particular,
the `IoBuilder.forLogger()` methods. One primary usage of this API extension is for setting up
loggers in the JDBC API. For example:

```
PrintWriter logger = IoBuilder.forLogger(DriverManager.class)
                              .setLevel(Level.DEBUG)
                              .buildPrintWriter();
DriverManager.setLogWriter(logger);
```

Using the `IoBuilder` class, there are a few more options that can be set. In general, there are six
primary classes one can build from it: `Reader`, `Writer`, `PrintWriter`,
`InputStream`, `OutputStream`, and `PrintStream`. The input-oriented classes
are for wiretapping, and the output-oriented classes are for creating either an output class that solely outputs
its lines as log messages, or an output filter class that logs all lines output through it to its delegate
output class.

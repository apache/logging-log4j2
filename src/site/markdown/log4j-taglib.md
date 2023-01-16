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

# Log4j Tag Library

The Log4j Log Tag Library creates the capability of inserting log
statements in JSPs without the use of Java scripting. It uses the standard Log4j 2 API to log
messages according to your Log4j configuration.

This tag library is based on the
[Jakarta Commons Log Taglib](http://jakarta.apache.org/taglibs/log/) by Joseph Ottinger and James
Strachan. For the most part, logging tags written against Jakarta Commons Log Taglib should
work against this library as well. However, the "category" attribute from Jakarta has become
the "logger" attribute in this library.

## Requirements

The Log4j Tag Library requires at least Servlet 2.5 (or Java EE 5), at least
JSP 2.1 (or Java EE 5), and is dependent on the Log4j 2 API.
For more information, see [Runtime Dependencies](../runtime-dependencies.html).

<span class="label label-important">Important Note!</span>
For performance reasons, containers often ignore certain JARs known not to
contain TLDs and do not scan them for TLD files. Importantly, Tomcat 7 &lt;7.0.43 ignores all JAR files named
log4j*.jar, which prevents the JSP tag library from being automatically discovered. This does not affect
Tomcat 6.x and has been fixed in Tomcat 7.0.43, Tomcat 8, and later. In Tomcat 7 &lt;7.0.43 you
will need to change `catalina.properties` and remove "log4j*.jar" from the `jarsToSkip`
property. You may need to do something similar on other containers if they skip scanning Log4j JAR files.

## Usage
In accordance with the [Logger](../log4j-api/apidocs/org/apache/logging/log4j/Logger.html)
API, this tag library has tags to
support the following logging calls: "catching", "entry", "exit", "log", "trace", "debug",
"info", "warn", "error", and "fatal". The "trace" tag was not supported in Jakarta Commons Log
Taglib. The "setLogger", "catching", "entry", and "trace" tags are new to this library. This
tag library also supports the conditional tag "ifEnabled" (new) and troubleshooting tag "dump"
(existed in Jakarta Commons Log Taglib).

By default, this tag library uses a different Logger for each JSP named after the JSP ID.
You can customize the Logger in any of the logging tags or the "ifEnabled" tag with the
"logger" attribute. You can also use the "setLogger" tag to specify the Logger that should apply
for the rest of a JSP's execution. If the "setLogger" tag comes before any other logging tags in
a JSP, the default Logger for that JSP will not be created, but instead the specified logger is
the only one that will be used.

Every effort was made to optimize these tags and ensure decent performance, but users of
this library should keep in mind that the creation and execution of JSP tags adds significant
overhead to the standard Log4j method calls. While the "trace", "debug", and "info" options are
available in this library, their uses are not nano-second order of magnitude with logging off
that users may be used to with standard Log4j method calls. Therefore, they should be used
sparingly. Use of the "ifEnabled" tag does not improve this performance; this tag is available
to conditionally evaluate other actions in addition to logging actions.

For detailed documentation of each tag, see the [Tag Library Reference](tagreference.html) or the
[TLDDoc Documentation](tlddoc/index.html).

////
    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements.  See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License.  You may obtain a copy of the License at

         https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
////

[#release-notes-${release.version?replace("[^a-zA-Z0-9]", "-", "r")}]
== ${release.version}

<#if release.date?has_content>Release date:: ${release.date}</#if>

This minor release introduces bug fixes, behavior improvements, and complete support for GraalVM native image generation.

[#release-notes-2-25-0-graalvm]
=== GraalVM Reachability Metadata

Log4j Core and all extension modules now include embedded
https://www.graalvm.org/latest/reference-manual/native-image/metadata/[GraalVM reachability metadata],
enabling seamless generation of native images with GraalVM out of the box—no manual configuration required.
For more information, refer to our xref:graalvm.adoc[GraalVM guide].

[NOTE]
====
When building third-party Log4j plugins, using the new `GraalVmProcessor`
introduced in version `2.25.0` will automatically generate the required reachability metadata for GraalVM native images.
However, the processor will fail the build if the required `log4j.graalvm.groupId` and `log4j.graalvm.artifactId` parameters are not provided.
For detailed instructions, see xref:manual/plugins.adoc#plugin-registry[Registering plugins].
====

[#release-notes-2-25-0-PL-ex]
=== Exception Handling in Pattern Layout

Exception handling in xref:manual/pattern-layout.adoc[Pattern Layout] has undergone a significant rewrite.
This update resolves several bugs and ensures consistent behavior across all exception converters.
Key improvements include:

* Stack traces are now consistently prefixed with a newline instead of other whitespace.
* The default exception converter has changed from xref:manual/pattern-layout.adoc#converter-exception-extended[extended] to xref:manual/pattern-layout.adoc#converter-exception[plain], offering better performance.
* Support for the `\{ansi}` option in exception converters has been removed.

[#release-notes-2-25-0-instant-format]
=== Date & Time Formatting

Log4j has historically provided custom date and time formatters for performance, such as
link:javadoc/log4j-core/org/apache/logging/log4j/core/util/datetime/FixedDateFormat.html[`FixedDateFormat`] and
link:javadoc/log4j-core/org/apache/logging/log4j/core/util/datetime/FastDateFormat.html[`FastDateFormat`].
These are now deprecated in favor of Java’s standard
https://docs.oracle.com/javase/{java-target-version}/docs/api/java/time/format/DateTimeFormatter.html[`DateTimeFormatter`].

If you encounter formatting issues after upgrading—particularly with `n` or `x` directives—you can temporarily revert to the legacy formatters by setting the xref:manual/systemproperties.adoc#log4j2.instantFormatter[`log4j2.instantFormatter`] property to `legacy`.
Please report any issues via our {logging-services-url}/support.html#issues[issue tracker].

[#release-notes-2-25-0-windows-ansi]
=== ANSI Support on Windows

Modern Windows versions (10 and newer) provide native ANSI escape sequence support.
As a result, dependency on the outdated JAnsi 1.x library has been removed.
For details, refer to xref:manual/pattern-layout.adoc#jansi[ANSI styling on Windows].

[#release-notes-2-25-0-jakarta-jms]
=== Jakarta JMS Appender

A Jakarta-compatible version of the xref:manual/appenders/message-queue.adoc#JmsAppender[JMS Appender] is now included in the core distribution.

<#include "../.changelog.adoc.ftl">

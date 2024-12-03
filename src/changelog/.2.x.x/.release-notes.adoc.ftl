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

This minor release contains bug fixes, behavioral improvements, and a fully-fledged support for the GraalVM native image generation.

[#release-notes-2-25-0-graalvm]
=== GraalVM reachability metadata

Log4j Core and all its extension modules have been enriched with embedded
https://www.graalvm.org/latest/reference-manual/native-image/metadata/[GraalVM reachability metadata].
This allows the generation of GraalVM native images out-of-the-box without any additional steps.
See our xref:graalvm.adoc[GraalVM guide] for details.

[#release-notes-2-25-0-PL-ex]
=== Exception handling in Pattern Layout

Exception handling in xref:manual/pattern-layout.adoc[Pattern Layout] went through a major rewrite.
This effectively helped with fixing some bugs by matching the feature parity of all exception converters.
Some important highlights from this rewrite:

* Rendered stack traces are ensured to be prefixed with a newline, which used to be a whitespace in earlier versions.
* Switched the default exception converter from xref:manual/pattern-layout.adoc#converter-exception-extended[the extended exception converter] to xref:manual/pattern-layout.adoc#converter-exception[the plain exception converter], which is more performant.
* The support for the `\{ansi}` option in exception converters is removed.

[#release-notes-2-25-0-instant-format]
=== Date & time formatting

Historically, Log4j contains custom date & time formatting utilities for performance reasons, i.e., link:javadoc/log4j-core/org/apache/logging/log4j/core/util/datetime/FixedDateFormat.html[`FixedDateFormat`] and link:javadoc/log4j-core/org/apache/logging/log4j/core/util/datetime/FastDateFormat.html[`FastDateFormat`].
These have been deprecated for removal in favor of Java's https://docs.oracle.com/javase/{java-target-version}/docs/api/java/time/format/DateTimeFormatter.html[`DateTimeFormatter`].
After upgrading, if you experience any date & time formatting problems (in particular, related with the usage of `n` and `x` directives), please {logging-services-url}/support.html#issues[submit an issue ticket] – as a temporary workaround, you can set xref:manual/systemproperties.adoc#log4j2.instant.formatter[the `log4j2.instant.formatter` property] to `legacy` to switch to the old behaviour.

=== ANSI support on Windows

Since 2017, Windows 10 and newer have offered native support for ANSI escapes.
The support for the outdated Jansi 1.x library has therefore been removed.
See xref:manual/pattern-layout.adoc#jansi[ANSI styling on Windows] for more information.

<#include "../.changelog.adoc.ftl">

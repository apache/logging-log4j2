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

This release terminates the modularization process of Log4j Core by:

* Moving all the code that uses optional `log4j-core` dependencies into new modules.
  See ${'<<release-notes-3-0-0-beta3-modularization,Modularization>>'}.
* Moving some modules to their own lifecycle.
  This includes the Flume Appender and all the modules that don't depend on `log4j-core`.
  See ${'<<release-notes-3-0-0-beta3-separate-lifecycle,Separate Lifecycle>>'}.

[#release-notes-3-0-0-beta3-modularization]
=== Modularization

* Support for the JAnsi library has been removed since recent Windows OSes support ANSI escape sequences.
See
xref:manual/pattern-layout.adoc#ansi-windows[ANSI styling on Windows]
for more details.
* The
xref:manual/appenders/delegating.adoc#DisruptorBlockingQueueFactory[`DisruptorBlockinQueue` queue factory]
has been moved to a new
xref:components.adoc#log4j-conversant[`log4j-conversant` artifact].
* Support for
xref:manual/appenders/rolling-file.adoc#RolloverStrategy-compress[advanced compression algorithms]
has been moved to a new
xref:components.adoc#log4j-compress[`log4j-compress` artifact].
The Gzip and Zip formats are still supported out-of-the-box.

[#release-notes-3-0-0-beta3-separate-lifecycle]
=== Separate lifecycle

* The
xref:manual/appenders/message-queue.adoc#FlumeAppender[Flume Appender]
releases will follow their own lifecycle.
* All the
xref:manual/installation.adoc#impl-core-bridges[logging bridges]
from and to Log4j API have been removed from the Log4j Core 3.x release.
Please manage your dependencies with
xref:components.adoc#log4j-bom[`log4j-bom`]
to always use the compatible version of the logging bridges.

<#include "../.changelog.adoc.ftl">

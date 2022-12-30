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

= ${release.version}<#if release.date?has_content> (${release.date})</#if>

The major changes contained in this release include:

* Address CVE-2021-45105 by disabling recursive evaluation of Lookups during log event processing.
Recursive evaluation is still allowed while generating the configuration.
* The `JndiLookup`, `JndiContextSelector`, and `JMSAppender` now require individual system properties to be enabled.
* Remove LDAP and LDAPS as supported protocols from JNDI.

The single `log4j2.enableJndi` property introduced in Log4j 2.16.0 has been replaced with three individual properties; `log4j2.enableJndiContextSelector`, `log4j2.enableJndiJms`, and `log4j2.enableJndiLookup`.

The Log4j 2.17.0 API, as well as many core components, maintains binary compatibility with previous releases.

Apache Log4j 2.17.0 requires a minimum of Java 8 to build and run.
Log4j 2.12.2 is the last release to support Java 7.
Java 7 is no longer supported by the Log4j team.

For complete information on Apache Log4j 2, including instructions on how to submit bug reports, patches, or suggestions for improvement, see http://logging.apache.org/log4j/2.x/[the Apache Log4j 2 website].

<#include "../.changelog-entries.adoc.ftl">

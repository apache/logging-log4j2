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

This release provides a continuation of the modularisation process of Log4j Core.
The following features were moved to separate artifacts:

* The async logger feature was moved to `log4j-async-logger` and it was upgraded to use LMAX Disruptor 4.x.
The async appender is still available by default in `log4j-core`.
* The YAML configuration is available now in `log4j-config-yaml`.
* The Java properties configuration was removed and replaced with a similar format based on https://github.com/FasterXML/jackson-dataformats-text/tree/2.17/properties[`jackson-dataformat-properties`].

Other features were removed:

* Jetty 9.x users are encouraged to migrate to Jetty 10.x or later and replace `log4j-appserver` with `log4j-slf4j2-impl`.
* Tomcat JULI support will be available from a third-party (cf. https://github.com/copernik-eu/log4j-plugins/pull/102[copernik-eu/log4j-plugins]).
* Apache Commons Logging users are encouraged to upgrade `commons-logging` to version 1.3.0 or later and remove `log4j-jcl`.
* Support for the XML layout was dropped.
* Support for JMX was dropped and will be replaced with a more recent technology.

<#include "../.changelog.adoc.ftl">

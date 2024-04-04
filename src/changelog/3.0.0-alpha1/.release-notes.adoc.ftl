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

As the Java ecosystem has evolved, requests have been received from users, and the need for improved security has
become more apparent, changes were necessariy in Log4j's design:

* With the introduction of the Java Platform Module System (JPMS) changes were needed to how the various log4j modules
are packaged. While not every log4j module is now a fully compliant JPMS module with its own module-info.java file,
all the modules likely to be used in a JPMS environment are.
* Many optional components, such as Scripting, JNDI, JPA and JMS, have been moved to their own modules. This makes
Log4j-core slightly smaller in 3.x and aids in security by not having jars with unwanted behaviors present, making
disabling them via system properties unnecessary.
* All plugins constructed using Log4j 3.x are now located using Java's ServiceLoader. This avoids many of the problems
users had packaging plugins in "shaded" jars as that technology directly supports ServiceLoader. Plugins constructed
using Log4j 2.x will still function in Log4j 3.x.
* Log4j's annotation processor has been individually packaged separate from Log4j-core and the plugin system it enables.
For applications using the module path this makes it easier to provide the annotation processor since it must be
explicitly declared in those cases.
* Log4j 3.x now uses an internal dependency injection framework to allow plugins to be injected with instances of
classes they are dependent on.
* Many system properties used by Log4j can now be set to apply to a single LoggerContext making configuration
in application frameworks that support multiple applications more flexible.
* Some deprecated classes have been removed. However, every attempt has been made to ensure that user code compiled
for Log4j 2.x will continue to operate with the Log4j 3.x libraries present instead.

<#include "../.changelog.adoc.ftl">

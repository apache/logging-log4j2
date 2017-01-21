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

# Release Changelog

[JIRA-generated changelog](jira-report.html)

[Manual change log](changes-report.html)

Apache Log4j 2 is not compatible with the previous versions. Please have the following in mind when upgrading to
Log4j 2 in your project:

* Log4j 2.4 and greater requires Java 7, versions 2.0-alpha1 to 2.3 required Java 6.
* The XML configuration has been simplified and is not compatible with Log4j 1.x.
* Configuration via property files is supported from version 2.4, but is not compatible with Log4j 1.x.
* Configuration via JSON or YAML is supported, but these formats require
[additional runtime dependencies](runtime-dependencies.html).
* Although Log4j 2 is not directly compatible with Log4j 1.x a compatibility bridge has been provided to reduce the
need to make coding changes.

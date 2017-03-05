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

# Log4j 2 Liquibase Binding

The Log4j 2 Liquibase Binding enables [Liquibase](http://www.liquibase.org/) to log via Log4j 2.

## Requirements

The Log4j 2 Liquibase Binding has a dependency on the Log4j 2 API as well as the Liquibase core.
For more information, see [Runtime Dependencies](../runtime-dependencies.html).

## Usage

Including this module will automatically make Liquibase log via Log4j 2, due the the plugin auto discovery mechanism of
Liquibase.

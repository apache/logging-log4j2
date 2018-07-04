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

# Log4j 2 SLF4J Binding

The Log4j 2 SLF4J Binding allows applications coded to the SLF4J API to use
Log4j 2 as the implementation.

## Requirements

The Log4j 2 SLF4J Binding has a dependency on the Log4j 2 API as well as the SLF4J API.
For more information, see [Runtime Dependencies](../runtime-dependencies.html).

## Usage

The SLF4J binding provided in this component cause all the SLF4J APIs to be routed to Log4j 2. Simply
include the Log4j 2 SLF4J Binding jar along with the Log4j 2 jars and SLF4J API jar to cause all SLF4J
logging to be handled by Log4j 2.

<div class="alert alert-danger">
Use of the Log4j 2 SLF4J Binding (log4j-slf4j-impl-2.0.jar) together with 
the SLF4J adapter (log4j-to-slf4j-2.0.jar) should 
never be attempted, as it will cause events to endlessly be routed between
SLF4J and Log4j 2.
</div>

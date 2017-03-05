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

# Log4j to SLF4J Adapter

The Log4j 2 to SLF4J Adapter allows applications coded to the Log4j 2 API to be routed to SLF4J.
Use of this adapter may cause some loss of performance as the Log4j 2 Messages must be formatted
before they can be passed to SLF4J. With Log4j 2 as the implementation these would normally be
formatted only when they are accessed by a Filter or Appender.

## Requirements

The Log4j 2 to SLF4J adapter is dependent on the Log4j 2 API and the SLF4J API.
For more information, see [Runtime Dependencies](../runtime-dependencies.html).

## Usage

Include this jar, the SLF4J jar(s) and an SLF4J logging implementation jar together. Configure
the logging implementation as required.

<div class="alert alert-danger">
Use of the SLF4J adapter (log4j-to-slf4j-2.x.jar) together with
the SLF4J bridge (log4j-slf4j-impl-2.x.jar) should
never be attempted as it will cause events to endlessly be routed between
SLF4J and Log4j 2.
</div>

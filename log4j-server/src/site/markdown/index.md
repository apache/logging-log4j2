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

# Log4j Server components

## Log4j Server components

Standalone server classes for consuming log events over a network. Each of the various servers should be used with
another Log4j configuration to handle incoming log events. It is recommended to consider using the 
[Flume Appender](../manual/appenders.html#FlumeAppender) for highly reliable networked logging.

## Requirements

The Log4j Server components requires the Log4j 2 API and core. This component was introduced in Log4j 2.8.2, 
before it was part of log4j-core. For more information, see [Runtime Dependencies](../manual/runtime-dependencies.html).

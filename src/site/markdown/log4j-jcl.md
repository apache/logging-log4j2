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

# Commons Logging Bridge

The Commons Logging Bridge allows applications coded to the Commons Logging API to use
Log4j 2 as the implementation.

## Requirements

The Commons Logging Bridge is dependent on the Log4j 2 API and Commons Logging.
For more information, see [Runtime Dependencies](../runtime-dependencies.html).

## Usage

Using the Commons Logging Bridge is straightforward. Simply add the bridge jar along with
the other Log4j 2 jars and the Commons Logging jar, and all logging done using the
Commons Logging API will be routed to Log4j.

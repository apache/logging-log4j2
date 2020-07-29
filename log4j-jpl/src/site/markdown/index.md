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

# Log4j 2 JDK Platform Logging Adapter

The Log4j 2 JDK Logging Adapter allow to route all System.Logger events to Log4j 2 APIs.

## Requirements

The JDK Platform Logging Adapter is dependent on the Log4j API as well as Java 11.

## Usage

Simply include the Log4j 2 JDK Platform Logging Adapter jar along with the Log4j2 jars to cause all System.Logger 
logging to be handled by Log4j 2.

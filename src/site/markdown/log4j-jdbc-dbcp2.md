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

# Log4j 2 JDBC Connection Source with Apache Commons DBCP 2

This module adds a connection source for JDBC Appenders. The PoolingDriverConnectionSource 
provides connection pooling through Apache Commons DBCP 2. 

## Requirements

This module was introduced in Log4j 2.11.0 and requires Apache Commons DBCP 2 to provide
connection pooling.

Some features may require optional
[dependencies](../runtime-dependencies.html). These dependencies are specified in the
documentation for those features.

Some Log4j features require external dependencies.
See the [Dependency Tree](dependencies.html#Dependency_Tree)
for the exact list of JAR files needed for these features.

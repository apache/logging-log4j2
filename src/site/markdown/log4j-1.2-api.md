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

# Log4j 1.2 Bridge

The Log4j 1.2 Bridge allows applications coded to use Log4j 1.2 API to use Log4j 2 instead.

## Requirements

The Log4j 1.2 bridge is dependent on the Log4j 2 API. The following Log4j 1.x methods will behave differently when
the Log4j 2 Core module is included then when it is not:

| Method                        | Without log4j-core | With log4j-core                      |
| ----------------------------- | ------------------ | ------------------------------------ |
| Category.getParent()          | Returns null       | Returns parent logger                |
| Category.setLevel()           | NoOp               | Sets Logger Level                    |
| Category.setPriority()        | NoOp               | Sets Logger Level                    | 
| Category.getAdditivity()      | Returns false      | Returns Logger's additivity setting  | 
| Category.setAdditivity()      | NoOp               | Sets additivity of LoggerConfig      |
| Category.getResourceBundle()  | NoOp               | Returns the resource bundle associated with the Logger |
| BasicConfigurator.configure() | NoOp               | Reconfigures Log4j 2                 |

If log4j-core is not present location information will not be accurate in calls using the Log4j 1.2 API. The config
package which attempts to convert Log4j 1.x configurations to Log4j 2 is not supported without Log4j 2.    

For more information, see [Runtime Dependencies](runtime-dependencies.html).

## Usage

To use the Log4j Legacy Bridge, you must first identify and remove all the Log4j 1.x JARs from the application and 
replace them with the bridge JAR. Once in place, all logging that uses Log4j 1.x will be routed to Log4j 2.

If you are using Maven, you can easily identify any Log4j 1.x dependencies in your project by 
running the following command in your terminal:

    mvn dependency:tree | grep 'log4j:log4j:'

This command will generate a dependency tree for your project and filter the results to show only Log4j 1.x dependencies. 
If you find any, you will need to update your pom.xml file to remove Log4j1 or exclude them otherwise.

While we have improved the compatibility of the bridge in the past, applications that try to modify legacy Log4j 
by adding Appenders, Filters, etc, may experience problems. In this case, migrating those components to Log4j 2 
first is recommended.

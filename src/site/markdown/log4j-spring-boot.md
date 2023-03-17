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

# Log4j Spring Boot Support

This module provides enhanced support for Spring Boot beyond what Spring Boot itself provides. 

## Overview

The components in this module require a Spring Environment to have been created. Spring Boot 
applications initialize logging multiple times. The first initialization occurs before
any initialization work is performed by Spring, thus no Environment will have been created
and the components implemented in this module will not produce the desired results. Subsequent
initializations of logging will have a Spring Environment. 


## Usage

### Spring Lookup

The Spring Lookup allows configuration files to reference properties defined in Spring
configuration files from a Log4j configuration file. For example:

    <property name="applicationName">${spring:spring.application.name}</property>
    
would set the Log4j applicationName property to the value of spring.application.name set in the 
Spring configuration.     

### Spring Property Source

Log4j uses property sources when resolving properties it uses internally. This support allows
most of Log4j's [System Properties](manual/configuration.html#SystemProperties)
to be specified in the Spring Configuration. However, some properties that are only referenced
during the first Log4j initialization, such as the property Log4j uses to allow the default 
Log4j implementation to be chosen, would not be available.

### Spring Profile Arbiter

New with Log4j 2.15.0 are "Arbiters" which are conditionals that can cause a portion of the Log4j configuration to 
be included or excluded. log4j-spring-boot provides an Arbiter that allows a Spring profile value to be used for 
this purpose. Below is an example:
```
<Configuration name="ConfigTest" status="ERROR" monitorInterval="5">
  <Appenders>

    <SpringProfile name="dev | staging">
      <Console name="Out">
        <PatternLayout pattern="%m%n"/>
      </Console>
    </SpringProfile>
    <SpringProfile name="prod">
      <List name="Out">
      </List>
    </SpringProfile>

  </Appenders>
  <Loggers>
    <Logger name="org.apache.test" level="trace" additivity="false">
      <AppenderRef ref="Out"/>
    </Logger>
    <Root level="error">
      <AppenderRef ref="Out"/>
    </Root>
  </Loggers>
</Configuration>
```

## Requirements

The Log4j 2 Spring Cloud Configuration integration has a dependency on Log4j 2 API, Log4j 2 Core, and 
Spring Boot versions 2.0.3.RELEASE or 2.1.1.RELEASE or later versions it either release series.
For more information, see [Runtime Dependencies](runtime-dependencies.html).

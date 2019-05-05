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

# Log4j Spring Cloud Configuration

This module allows logging configuration files to be dynamically updated when new versions are available in
Spring Cloud Configuration. 

## Overview

Spring Boot applications initialize logging 3 times.
1. SpringApplication declares a Logger. This Logger will be initialized using Log4j's "normal" mechanisms. Thus 
a system property named log4j2.configurationFile will be checked to see if a specific configuration file has been
provided, otherwise it will search for a configuration file on the classpath. The property may also be declare 
in log4j2.component.properties. 

## Usage

Log4j configuration files that specify a monitor interval of greater than zero will use polling to determine
whether the configuration has been updated. If the monitor interval is zero then Log4j will listen for notifications
from Spring Cloud Config and will check for configuration changes each time an event is generated. If the 
monitor interval is less than zero Log4j will not check for changes to the logging configuration.

When referencing a configuration located in Spring Cloud Config the configuration should be referenced similar to

```
log4j.configurationFile=http://host.docker.internal:8888/ConfigService/sampleapp/default/master/log4j2.xml
```
When running in a docker container host.docker.internal may be used as the domain name to access an application
running on the same hose outside of the docker container. Note that in accordance with Spring Cloud Config
practices but the application, profile, and label should be specified in the url.

The Spring Cloud Config support also allows connections using TLS and/or basic authentication. When using basic 
authentication the userid and password may be specified as system properties or in log4j2.component.properties as

```
log4j2.configurationUserName=guest
log4j2.configurationPassword=guest
```
Note that Log4j currently does not support encrypting the password. 

If more extensive authentication is required an ```AuthorizationProvider``` can be implemented and defined in
the log4j2.authorizationProvider system property or in log4j2.component.properties.

TLS can be enabled by adding the following system properties or defining them in log4j2.component.properties

| Property      | Optional or Default Value | Description   |
| ------------- |-------|:-------------| 
| log4j2.trustStoreLocation  | Optional | The location of the trust store. If not provided the default trust store will be used.| 
| log4j2.trustStorePassword  | Optional | Password needed to access the trust store. |
| log4j2.trustStorePasswordFile | Optinoal | The location of a file that contains the password for the trust store. |
| log4j2.trustStorePasswordEnvironmentVariable | Optional | The name of the environment variable that contains the trust store password. |
| log4j2.trustStoreKeyStoreType | Required if keystore location provided | The type of key store.  |
| log4j2.trustStoreKeyManagerFactoryAlgorithm | Optional | Java cryptographic algorithm. |
| log4j2.keyStoreLocation | Optional | The location of the key store. If not provided the default key store will be used.|
| log4j2.keyStorePassword | Optional | Password needed to access the key store. | 
| log4j2.keyStorePasswordFile | Optional | The location of a file that contains the password for the key store. |
| log4j2.keyStorePasswordEnvironmentVariable | Optional | The name of the environment variable that contains the key store password.|
| log4j2.keyStoreType | Required if trust store location provided. | The type of key store. |
| log4j2.keyStoreKeyManagerFactoryAlgorithm | Optional | Java cryptographic algorithm.  |
| log4j2.sslVerifyHostName | false | true or false |



## Requirements

The Log4j 2 Spring Cloud Configuration integration has a dependency on Log4j 2 API, Log4j 2 Core, and 
Spring Cloud Configuration versions 2.0.3.RELEASE or 2.1.1.RELEASE or later versions it either release series.
For more information, see [Runtime Dependencies](../../runtime-dependencies.html).





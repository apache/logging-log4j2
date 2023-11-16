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

Log4j also supports Composite Configurations. The standard way to do that is to concatentate the paths to the files in
a comma separated string. Unfortunately, Spring validates the URL being provided and commas are not allowed. 
Therefore, additional configurations must be supplied as "override" query parametes.

```
log4j.configurationFile=http://host.docker.internal:8888/ConfigService/sampleapp/default/master/log4j2.xml
?override=http://host.docker.internal:8888/ConfigService/sampleapp/default/master/log4j2-sampleapp.xml
```
Note that the location within the directory structure and how configuration files are located is completely 
dependent on the searchPaths setting in the Spring Cloud Config server.

When running in a docker container host.docker.internal may be used as the domain name to access an application
running on the same hose outside of the docker container. Note that in accordance with Spring Cloud Config
practices but the application, profile, and label should be specified in the url.

The Spring Cloud Config support also allows connections using TLS and/or basic authentication. When using basic 
authentication the userid and password may be specified as system properties, log4j2.component.properties or Spring
Boot's bootstrap.yml. The table below shows the alternate names that may be used to specify the properties. Any of
the alternatives may be used in any configuration location.

| Property | Alias  | Spring-like alias | Purpose |
|----------|---------|---------|---------|
| log4j2.configurationUserName | log4j2.config.username | logging.auth.username | User name for basic authentication |
| log4j2.configurationPassword | log4j2.config.password | logging.auth.password | Password for basic authentication |
| log4j2.configurationAuthorizationEncoding | | logging.auth.encoding | Encoding for basic authentication (defaults to UTF-8) |
| log4j2.configurationAuthorizationProvider | log4j2.config.authorizationProvider | logging.auth.authorizationProvider | Class used to create HTTP Authorization header |

```
log4j2.configurationUserName=guest
log4j2.configurationPassword=guest
```
As noted above, Log4j supports accessing logging configuration from bootstrap.yml. As an example, to configure reading 
from a Spring Cloud Configuration service using basic authoriztion you can do:
```
spring:
  application:
    name: myApp
  cloud:
    config:
      uri: https://spring-configuration-server.mycorp.com
      username: appuser
      password: changeme

logging:
  config: classpath:log4j2.xml
  label: ${spring.cloud.config.label}

---
spring:
  profiles: dev

logging:
  config: https://spring-configuration-server.mycorp.com/myApp/default/${logging.label}/log4j2-dev.xml
  auth:
    username: appuser
    password: changeme
```

Note that Log4j currently does not directly support encrypting the password. However, Log4j does use Spring's 
standard APIs to access properties in the Spring configuration so any customizations made to Spring's property
handling would apply to the properties Log4j uses as well.

If more extensive authentication is required an ```AuthorizationProvider``` can be implemented and the fully
qualified class name in
the ```log4j2.authorizationProvider``` system property, in log4j2.component.properties or in Spring's bootstrap.yml
using either the ```log4j2.authorizationProvider``` key or with the key ```logging.auth.authorizationProvider```.

TLS can be enabled by adding the following system properties or defining them in log4j2.component.properties

| Property                                     | Optional or Default Value                  | Description                                                                            |
|:---------------------------------------------|:-------------------------------------------|:---------------------------------------------------------------------------------------|
| log4j2.trustStoreLocation                    | Optional                                   | The location of the trust store. If not provided the default trust store will be used. | 
| log4j2.trustStorePassword                    | Optional                                   | Password needed to access the trust store.                                             |
| log4j2.trustStorePasswordFile                | Optional                                   | The location of a file that contains the password for the trust store.                 |
| log4j2.trustStorePasswordEnvironmentVariable | Optional                                   | The name of the environment variable that contains the trust store password.           |
| log4j2.trustStoreKeyStoreType                | Required if keystore location provided     | The type of key store.                                                                 |
| log4j2.trustStoreKeyManagerFactoryAlgorithm  | Optional                                   | Java cryptographic algorithm.                                                          |
| log4j2.keyStoreLocation                      | Optional                                   | The location of the key store. If not provided the default key store will be used.     |
| log4j2.keyStorePassword                      | Optional                                   | Password needed to access the key store.                                               | 
| log4j2.keyStorePasswordFile                  | Optional                                   | The location of a file that contains the password for the key store.                   |
| log4j2.keyStorePasswordEnvironmentVariable   | Optional                                   | The name of the environment variable that contains the key store password.             |
| log4j2.keyStoreType                          | Required if trust store location provided. | The type of key store.                                                                 |
| log4j2.keyStoreKeyManagerFactoryAlgorithm    | Optional                                   | Java cryptographic algorithm.                                                          |
| log4j2.sslVerifyHostName                     | false                                      | true or false                                                                          |

## Requirements

The Log4j 2 Spring Cloud Configuration integration has a dependency on Log4j 2 API, Log4j 2 Core, and 
Spring Cloud Configuration versions 2.0.3.RELEASE or 2.1.1.RELEASE or later versions it either release series.
For more information, see [Runtime Dependencies](runtime-dependencies.html).

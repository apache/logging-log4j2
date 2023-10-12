<!---
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

# Requirements

* JDK 11+
* JDK 8 (optional)
* A modern Linux, OSX, or Windows host

<a name="building"></a>
# Building the sources

You can build and verify the sources as follows:
```sh
./mvnw verify
```

`verify` goal runs validation and test steps next to building (i.e., compiling) the sources.
To speed up the build, you can skip verification:

```sh
./mvnw -DskipTests package
```

If you want to install generated artifacts to your local Maven repository, replace above `verify` and/or `package` goals with `install`.

<a name="dns"></a>
## DNS lookups in tests

Note that if your `/etc/hosts` file does not include an entry for your computer's hostname, then
many unit tests may execute slow due to DNS lookups to translate your hostname to an IP address in
[`InetAddress.getLocalHost()`](http://docs.oracle.com/javase/7/docs/api/java/net/InetAddress.html#getLocalHost()).
To remedy this, you can execute the following:
```sh
printf '127.0.0.1 %s\n::1 %s\n' `hostname` `hostname` | sudo tee -a /etc/hosts
```

<a name="java8-tests"></a>
# Java 8 tests

To test the library against the target JRE (JRE 8), you need to configure a JDK 8 toolchains as explained below and run Maven with the `java8-tests` profile:
```sh
./mvnw verify -Pjava8-tests
```

<a name="toolchains"></a>
## Configuring Maven Toolchains

Maven Toolchains is used to employ additional JDKs required for tests.
You either need to have a user-level configuration in `~/.m2/toolchains.xml` or explicitly provide one to the Maven: `./mvnw --global-toolchains /path/to/toolchains.xml`.
```xml
<?xml version="1.0" encoding="UTF8"?>
<toolchains>
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>1.8.0_372</version>
    </provides>
    <configuration>
      <jdkHome>/usr/lib/jvm/java-8-openjdk-amd64</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
```

<a name="website"></a>
# Building the website and manual

You can build the website and manual as follows:
```sh
./mvnw site
```
And view it using a simple HTTP server, e.g., the one comes with the Python:
```sh
python3 -m http.server -d target/site
```

<!-- vim: set syn=markdown : -->
<!--
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements. See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

# Building and Installing Log4j

*The information below is for developers who want to modify Log4j or contribute
to Log4j. If your goal is to add logging to your application you don't need to
build from the source code, you can [download](download.html) the pre-built
binaries instead.*

Log4j 2 is hosted in the Apache Software Foundation's Git repository. Details on obtaining the
most current source code can be found at
[Log4j Source Repository](source-repository.html). The source from the latest release may be
obtained by downloading it using the instructions at [Log4j Downloads](download.html).

Log4j 2.x uses Maven 3 as its build tool. To build and install Log4j in your local Maven cache, from
the root directory run: `mvn install`

Note that if your `/etc/hosts` file does not include an entry for your computer's hostname, then
many unit tests may execute slowly due to DNS lookups to translate your hostname to an IP address in
<a class="javadoc" href="http://docs.oracle.com/javase/7/docs/api/java/net/InetAddress.html#getLocalHost()">InetAddress::getLocalHost</a>.
To remedy this, you can execute the following:

```
printf '127.0.0.1 %s\n::1 %s\n' `hostname` `hostname` | sudo tee -a /etc/hosts
```

Then to build the full site, you must use a local staging directory:

```
mvn site
[Windows] mvn site:stage-deploy -DstagingSiteURL=file:///%HOME%/log4j
[Unix] mvn site:stage-deploy -DstagingSiteURL=file:///$HOME/log4j
```

To rebuild only what's changed and execute the tests, run: `mvn test`

To rebuild from scratch, add "clean", for example: `mvn clean test`

## Releasing Log4j

Please see the wiki [Log4j2ReleaseGuide](https://wiki.apache.org/logging/Log4j2ReleaseGuide).

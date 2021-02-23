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
# Building Log4j 2

To build Log4j 2, you need a JDK implementation version 1.7 or greater, JDK
version 9, and Apache Maven 3.x.

Log4j 2.x uses the Java 9 compiler in addition to
the Java version installed in the path. This is accomplished by using Maven's toolchains support.
Log4j 2 provides sample toolchains XML files in the root folder. This may be used by
modifying it and installing the file as toolchains.xml in the .m2 folder or by using the
following when invoking Maven.

```
[Macintosh] -t ./toolchains-sample-mac.xml
[Windows] -t ./toolchains-sample-win.xml
[Linux] -t ./toolchains-sample-linux.xml
```

To perform the license release audit, a.k.a. "RAT check", run.

    mvn apache-rat:check

To build the site with Java 7, make sure you give Maven enough memory using
`MAVEN_OPTS` with options appropriate for your JVM. Alternatively, you can
build with Java 8 and not deal with `MAVEN_OPTS`.

To install the jars in your local Maven repository, from a command line, run:

    mvn clean install

Once install is run, you can run the Clirr check on the API and 1.2 API modules:

    mvn clirr:check -pl log4j-api

    mvn clirr:check -pl log4j-1.2-api

Next, to build the site:

If Java 8 runs out of memory building the site, you will need:

    set MAVEN_OPTS=-Xmx2000m -XX:MaxPermSize=384m

    mvn site

On Windows, use a local staging directory, for example:

    mvn site:stage-deploy -DstagingSiteURL=file:///%HOMEDRIVE%%HOMEPATH%/log4j

On UNIX, use a local staging directory, for example:

    mvn site:stage-deploy -DstagingSiteURL=file:///$HOME/log4j

To test, run:

    mvn clean install

## Testing in Docker

In order to run a clean test using the minimum version of the JDK along with a
proper Linux environment, run:

    docker build .

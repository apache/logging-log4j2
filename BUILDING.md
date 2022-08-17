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
# Building Log4j 3.x

To build Log4j 3.x, you need a JDK implementation version 11 (at least version 11.0.13 if building on macOS) or greater and Apache Maven 3.x.

To perform the license release audit, a.k.a. "RAT check", run.

    mvn apache-rat:check

To install the jars in your local Maven repository, from a command line, run:

    mvn clean install

Once install is run, you can run the Clirr check on the API and 1.2 API modules:

    mvn clirr:check -pl log4j-api

    mvn clirr:check -pl log4j-1.2-api

Next, to build the site:

    mvn site

On Windows, use a local staging directory, for example:

    mvn site:stage-deploy -DstagingSiteURL=file:///%HOMEDRIVE%%HOMEPATH%/log4j

On UNIX, use a local staging directory, for example:

    mvn site:stage-deploy -DstagingSiteURL=file:///$HOME/log4j

To test, run:

    mvn clean install

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# FROM openjdk:7-alpine
# Reverted to debian yet alpine does not include jdk9
FROM openjdk:7-jdk

# Require while jdk9 is unstable on debian
RUN echo 'deb http://deb.debian.org/debian unstable main' >> /etc/apt/sources.list

RUN set -ex \
    && mkdir /src \
    && apt-get update \
    && apt-get install -y \
       curl \
       openjdk-9-jdk-headless \
    && ln -svT "/usr/lib/jvm/java-9-openjdk-$(dpkg --print-architecture)" /docker-java-9-home \
    && cd /opt \
    && curl -fsSL http://www-us.apache.org/dist/maven/maven-3/3.5.0/binaries/apache-maven-3.5.0-bin.tar.gz -o maven.tar.gz \
    && tar -xzf maven.tar.gz \
    && rm -f maven.tar.gz

COPY . /src

RUN set -ex \
    && cd /src \
    && /opt/apache-maven-3.5.0/bin/mvn verify --global-toolchains toolchains-docker.xml

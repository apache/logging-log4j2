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

FROM openjdk:17

RUN set -ex \
    && apt-get update \
    && apt-get install -y openjdk-11-jdk-headless \
    && ln -svT "/usr/lib/jvm/java-11-openjdk-$(dpkg --print-architecture)" /usr/local/openjdk-11

VOLUME /src /root/.m2/repository

COPY . /src

RUN set -ex \
    && cd /src \
    && ./mvnw --toolchains toolchains-docker.xml install

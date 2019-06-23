#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache license, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the license for the specific language governing permissions and
# limitations under the license.
#
# Alpine Linux with OpenJDK
#FROM openjdk:8-jdk-alpine
FROM openjdk:11-jdk-slim

ARG build_version
ENV BUILD_VERSION=${build_version}
RUN mkdir /service

ADD ./target/sampleapp.jar /service/
WORKDIR /service

#EXPOSE 8080 5005
EXPOSE 8080

#CMD java "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005" -jar sampleapp.jar
CMD java -jar -Xmx2G sampleapp.jar

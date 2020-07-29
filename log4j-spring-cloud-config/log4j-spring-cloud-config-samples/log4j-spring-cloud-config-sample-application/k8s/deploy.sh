#!/usr/bin/env bash
#
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

echo "Building, (re)creating, starting, and attaching to containers for a service."

imageName=sampleapp
containerName=sampleapp-container
networkName=docker_sampleapp
debug_port=5005
#debug_expose="-p $debug_port:$debug_port"
exposed_ports="-p 8080:8090 $debug_expose"

mvn clean package -DskipTests=true
docker build --no-cache -t $imageName -f Dockerfile  .

docker tag $imageName localhost:5000/$imageName
docker push localhost:5000/$imageName
kubectl apply -f k8s/sampleapp-deployment.yaml
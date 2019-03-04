#!/usr/bin/env bash
imageName=sampleapp
containerName=app-container
networkName=docker_sampleapp
debug_port=5005
debug_expose="-p $debug_port:$debug_port"
exposed_ports="-p 8080:10010 $debug_expose"

mvn clean package -DskipTests=true

docker build -t $imageName -f Dockerfile  .

echo Delete old container...
docker rm -f $containerName

echo Run new container...
docker run  -e "SERVICE_PARAMS=--spring.config.location=classpath:/,classpath:/application-local-docker.yml" \
    --network=$networkName -d $exposed_ports --name $containerName $imageName
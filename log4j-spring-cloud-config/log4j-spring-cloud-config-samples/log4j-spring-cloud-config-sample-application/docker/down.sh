#!/usr/bin/env bash
echo Stopping containers and removing containers, networks, volumes, and images created by up.
DIR=`dirname "$0"`
docker-compose --file ${DIR}/docker-compose.yml down --rmi local -v
docker-compose --file ${DIR}/docker-compose.yml rm --force -v

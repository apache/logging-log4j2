#!/usr/bin/env bash

DIR=`dirname "$0"`
# dump existing logs and start tailing them
# https://docs.docker.com/compose/reference/logs
docker-compose --file ${DIR}/../docker/docker-compose.yml logs --follow --tail=all


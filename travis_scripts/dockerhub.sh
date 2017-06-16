#!/bin/bash

docker login -u="$DOCKER_USERNAME" -p="$DOCKER_PASSWORD"
docker build -t slidewiki/nlpservice:latest-dev ./
docker push slidewiki/nlpservice:latest-dev

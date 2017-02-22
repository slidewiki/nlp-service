#!/bin/bash

docker login -u="$DOCKER_USERNAME" -p="$DOCKER_PASSWORD"
docker build -t slidewiki/nlpservice ./
docker push slidewiki/nlpservice

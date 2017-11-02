#!/bin/bash

docker login -u="$DOCKER_USERNAME" -p="$DOCKER_PASSWORD"
if [ $TRAVIS_TAG =~ ^[0-9]+(\.[0-9]+)+$ ]
then
	docker build -t slidewiki/nlpservice:$TRAVIS_TAG ./
	docker push slidewiki/nlpservice:$TRAVIS_TAG
else
	docker build -t slidewiki/nlpservice:latest-dev ./
	docker push slidewiki/nlpservice:latest-dev
fi

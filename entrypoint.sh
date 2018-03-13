#!/bin/bash

cat ./application.conf.template | envsubst > ./conf/application.conf  # destination path is probably not correct!
./bin/nlp-services

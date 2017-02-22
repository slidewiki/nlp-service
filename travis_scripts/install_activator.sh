#!/bin/bash

curl -L https://downloads.typesafe.com/typesafe-activator/$ACTIVATOR_VERSION/typesafe-activator-$ACTIVATOR_VERSION-minimal.zip > activator.zip
unzip -uo activator.zip -d ./activator/
chmod +x ./activator/bin/activator

export $PATH:$PWD/activator/bin/
